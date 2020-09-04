package com.textscanner.app.activities.camera


import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Size
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.textscanner.app.CameraSettings
import com.textscanner.app.cameraAPI.CameraService
import com.textscanner.app.cameraAPI.OnImageCapturedHandler
import com.textscanner.app.R
import com.textscanner.app.TinyDB.TinyDB
import com.textscanner.app.activities.result.ResultActivity
import com.textscanner.app.activities.settings.SettingsActivity
import com.textscanner.app.extensions.rotate
import com.textscanner.app.ocr.MLKitOCRService
import com.textscanner.app.ocr.OnTextExtractedHandler
import kotlinx.android.synthetic.main.activity_camera.*
import java.lang.IndexOutOfBoundsException

class CameraActivity : AppCompatActivity() {

    companion object {
        const val STATUS: String = "STATUS"

        const val RESOLUTION_LIST_DISPLAY: String = "RESOLUTION_LIST_DISPLAY"
        const val RESOLUTION_CURRENT: String = "RESOLUTION_CURRENT"
        const val RESOLUTION_LIST: String = "RESOLUTION_LIST"
        const val CAMERA_SETTINGS: String = "CAMERA_SETTINGS"

        const val EXTRACTED_TEXT: String = "EXTRACTED_TEXT"
    }

    private val PERMISSION_CODE: Int = 1000
    private val OPERATION_CHOOSE_PHOTO = 100

    var status: Status = Status.MAKING_PHOTO
    var progressBar: ProgressDialog? = null

    lateinit var mCameraManager: CameraManager
    var cameraService: CameraService? = null
    var mCameraBack: Int = 0
    var cameraBackResolutionsList: MutableList<Size> = mutableListOf()
    var displayCameraBackResolutionsList: MutableList<String> = mutableListOf()
    var cameraInfoSettings: CameraSettings? = null
    var currentCameraBackResolution: Int = 0
    var mBackgroundThread: HandlerThread? = null
    var mBackgroundHandler: Handler? = null

    var bitmapImage: Bitmap? = null

    val context: Context = this
    val activity: Activity = this

    private val surfaceTextureListener = object: TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture?,
            width: Int,
            height: Int
        ) {

        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            stopCameraPreview()
            return true
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            initCameraPreview()
        }
    }

    private val onImageCapturedHandler = object: OnImageCapturedHandler {
        override fun onCaptured(bitmap: Bitmap) {
            bitmapImage = bitmap
            setPictureOnDisplay(bitmapImage)
            changeVisibilityOfImageViews(status)
            status = Status.CHECKING_PHOTO
            enableButtonsAndCameraByStatus(status)
        }
    }

    private val onTextExtractedHandler = object: OnTextExtractedHandler {
        override fun onTextExtracted(text: String) {
            hideProgress()
            val intent = Intent(context, ResultActivity::class.java)
            intent.putExtra(EXTRACTED_TEXT, text)
            startActivity(intent)
        }

        override fun onTextExtractionFailure() {
            hideProgress()
            Toast.makeText(context, "Не удалось связаться с сервером\nПопробуйте чуть позже", Toast.LENGTH_SHORT).show()
            status = Status.CHECKING_PHOTO
            enableButtonsAndCameraByStatus(status)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        checkPermissions()
        mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        getCameraInfo()

        initViews(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        stopBackgroundThread()
        stopCameraPreview()
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        tv_image.surfaceTextureListener = surfaceTextureListener
        if(status == Status.MAKING_PHOTO){
            initCameraPreview()
        }
    }

    fun initViews(savedInstanceState: Bundle?){

        val statusSaved = savedInstanceState?.getString(STATUS) ?: "MAKING_PHOTO"

        status = Status.valueOf(statusSaved)
        enableButtonsAndCameraByStatus(status)

        btn_make_photo.setOnClickListener(View.OnClickListener {
            if (status == Status.MAKING_PHOTO){
                cameraService?.makePhoto()
                stopCameraPreview()
                btn_make_photo.isClickable = false
            }
            else if(status == Status.CHECKING_PHOTO){
                status = Status.MAKING_PHOTO
                enableButtonsAndCameraByStatus(status)
            }
        })

        btn_process_photo.setOnClickListener(View.OnClickListener {
            status = Status.PROCESING_PHOTO
            enableButtonsAndCameraByStatus(status)
            extractTextFromBitmap()
        })

        btn_rotate.setOnClickListener(View.OnClickListener {
            bitmapImage = bitmapImage!!.rotate(90f)
            setPictureOnDisplay(bitmapImage)
            changeVisibilityOfImageViews(status)
        })

        btn_settings.setOnClickListener(View.OnClickListener{
            stopCameraPreview()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        })

        btn_gallery.setOnClickListener(View.OnClickListener{
            stopCameraPreview()
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, OPERATION_CHOOSE_PHOTO)
        })
    }

    fun enableButtonsAndCameraByStatus(status: Status){
        when(status){
            Status.MAKING_PHOTO ->{
                btn_make_photo.isClickable = true
                btn_make_photo.visibility = ImageButton.VISIBLE
                btn_process_photo.visibility = ImageButton.INVISIBLE
                btn_rotate.visibility = ImageButton.INVISIBLE
                btn_settings.visibility = ImageButton.VISIBLE
                btn_gallery.visibility = ImageButton.VISIBLE

                tv_process.visibility = TextView.INVISIBLE
                tv_rotate.visibility = TextView.INVISIBLE
                tv_gallery.visibility = TextView.VISIBLE
                tv_settings.visibility = TextView.VISIBLE

                tv_image.setAspectRatio(
                    cameraBackResolutionsList[currentCameraBackResolution].height,
                    cameraBackResolutionsList[currentCameraBackResolution].width
                )
                initCameraPreview()
                changeVisibilityOfImageViews(status)
            }
            Status.CHECKING_PHOTO ->{
                btn_process_photo.isClickable = true
                btn_rotate.isClickable = true
                btn_make_photo.isClickable = true

                btn_process_photo.visibility = ImageButton.VISIBLE
                btn_rotate.visibility = ImageButton.VISIBLE
                btn_settings.visibility = ImageButton.INVISIBLE
                btn_gallery.visibility = ImageButton.INVISIBLE

                tv_process.visibility = TextView.VISIBLE
                tv_rotate.visibility = TextView.VISIBLE
                tv_gallery.visibility = TextView.INVISIBLE
                tv_settings.visibility = TextView.INVISIBLE

                //waiting for get bitmap from camera
            }
            Status.PROCESING_PHOTO ->{
                btn_process_photo.isClickable = false
                btn_rotate.isClickable = false

                btn_process_photo.visibility = ImageButton.VISIBLE
                btn_rotate.visibility = ImageButton.VISIBLE
                btn_settings.visibility = ImageButton.INVISIBLE
                btn_gallery.visibility = ImageButton.INVISIBLE

                tv_process.visibility = TextView.VISIBLE
                tv_rotate.visibility = TextView.VISIBLE
                tv_gallery.visibility = TextView.INVISIBLE
                tv_settings.visibility = TextView.INVISIBLE
            }
        }
    }

    private fun extractTextFromBitmap() {
        showProgress()
        val ocrService = MLKitOCRService(this, this, onTextExtractedHandler)
        ocrService.recognizeText(bitmapImage!!)
    }

    fun setPictureOnDisplay(bitmapImage: Bitmap?){
        iv_image.setAspectRatio(bitmapImage!!.width, bitmapImage.height)
        iv_image.setImageBitmap(bitmapImage)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATUS, status.name)
    }

    fun checkPermissions(){
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        for (permission in permissions){
            if(checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED){
                requestPermissions(permissions, PERMISSION_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_CODE ->{
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                }
                else{
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
                if(grantResults.isNotEmpty() && grantResults[1] == PackageManager.PERMISSION_GRANTED){

                }
                else{
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == OPERATION_CHOOSE_PHOTO){
            bitmapImage = MediaStore.Images.Media.getBitmap(this.contentResolver, data?.data).rotate(90F)
            status = Status.CHECKING_PHOTO
            setPictureOnDisplay(bitmapImage)
            changeVisibilityOfImageViews(status)
            enableButtonsAndCameraByStatus(status)
        }
        else{
            status = Status.MAKING_PHOTO
            enableButtonsAndCameraByStatus(status)
        }
    }

    fun getCameraInfo(){
        val tinyDB = TinyDB(this)
        cameraInfoSettings = tinyDB.getObject(CAMERA_SETTINGS, CameraSettings::class.java)

        if (cameraInfoSettings == null || cameraInfoSettings?.backCameraResolutionsList?.get(0) == Size(0, 0)){
            getInfoFromHardware()
            currentCameraBackResolution = intent.getIntExtra(RESOLUTION_CURRENT, 0)
            putInfoInSharedPreference(tinyDB)
        }
        else{
            getInfoFromSharedPreference()
        }
    }

    fun putInfoInSharedPreference(tinyDB: TinyDB){
        tinyDB.putObject(
            CAMERA_SETTINGS,
            CameraSettings(
                currentCameraBackResolution,
                cameraBackResolutionsList,
                displayCameraBackResolutionsList
            )
        )
    }

    fun getInfoFromSharedPreference(){
        cameraBackResolutionsList = cameraInfoSettings!!.backCameraResolutionsList
        displayCameraBackResolutionsList = cameraInfoSettings!!.displayCameraResolutionList
        currentCameraBackResolution = cameraInfoSettings!!.currentResolutionIndex
    }

    fun getInfoFromHardware(){
        val cameraList = mCameraManager.cameraIdList
        for(camera in cameraList){
            val cc = mCameraManager.getCameraCharacteristics(camera)
            when(cc.get(CameraCharacteristics.LENS_FACING)){
                CameraCharacteristics.LENS_FACING_BACK -> {
                    mCameraBack = camera.toInt()
                }
            }

            val configurationMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizesJPEG = configurationMap?.getOutputSizes(ImageFormat.JPEG)
            if(sizesJPEG != null){
                for(size in sizesJPEG){
                    if(cc.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK){
                        cameraBackResolutionsList.add(size)
                        displayCameraBackResolutionsList.add("${size.width}X${size.height}")
                    }
                }
            }
        }
        deleteUnsupportedResolutions()
        currentCameraBackResolution = 0
    }

    fun deleteUnsupportedResolutions(){
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        var deletedResolutions = 0
        for(i in 0 until cameraBackResolutionsList.size){
            val aspectRatio = cameraBackResolutionsList[i - deletedResolutions].width.toFloat() / cameraBackResolutionsList[i - deletedResolutions].height
            var overlaps = 0
            if(cameraBackResolutionsList[i - deletedResolutions].width <= displayMetrics.heightPixels && cameraBackResolutionsList[i - deletedResolutions].height <= displayMetrics.widthPixels){
                break
            }
            for(j in i - deletedResolutions until cameraBackResolutionsList.size){
                if(cameraBackResolutionsList[j].width.toFloat() / cameraBackResolutionsList[j].height == aspectRatio &&
                        cameraBackResolutionsList[j].width <= displayMetrics.heightPixels && cameraBackResolutionsList[j].height <= displayMetrics.widthPixels){
                    overlaps++
                    break
                }
            }
            if(overlaps == 0){
                cameraBackResolutionsList.removeAt(i - deletedResolutions)
                displayCameraBackResolutionsList.removeAt(i - deletedResolutions)
                deletedResolutions++
            }
        }
    }

    fun startBackgroundThread(){
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    fun stopBackgroundThread(){
        mBackgroundThread?.quitSafely()
        mBackgroundThread?.join()
        mBackgroundThread = null
        mBackgroundHandler = null
    }

    fun initCameraPreview(){
        if(cameraService == null && tv_image.isAvailable){
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            val previewRes = getSmallestPossiblePreviewSize(
                cameraBackResolutionsList[currentCameraBackResolution],
                Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
            )

            cameraService = CameraService(
                this,
                this,
                mCameraManager,
                tv_image,
                mBackgroundHandler,
                mCameraBack.toString(),
                cameraBackResolutionsList[currentCameraBackResolution],
                previewRes,
                onImageCapturedHandler
            )
            cameraService!!.openCamera()
        }
    }

    fun getSmallestPossiblePreviewSize(cameraSize: Size, previewSize: Size): Size{
        if(cameraSize.height <= previewSize.height*2 && cameraSize.width <= previewSize.width*2){
            return cameraSize
        }
        else{
            val cameraAspectRatio: Float = cameraSize.width.toFloat() / cameraSize.height.toFloat()
            for(i in currentCameraBackResolution..cameraBackResolutionsList.size-1){
                if(cameraBackResolutionsList[i].width <= previewSize.width*2 && cameraBackResolutionsList[i].height <= previewSize.height*2 &&
                    cameraBackResolutionsList[i].width.toFloat() / cameraBackResolutionsList[i].height.toFloat() == cameraAspectRatio)
                    return cameraBackResolutionsList[i]
            }
        }
        throw IndexOutOfBoundsException()
    }

    fun stopCameraPreview(){
        cameraService?.closeCamera()
        cameraService = null
    }

    fun changeVisibilityOfImageViews(status: Status){
        val visibleViewParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0,
            1.0f
        )
        visibleViewParams.gravity = Gravity.CENTER_HORIZONTAL
        val invisibleViewParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0,
            0.0f
        )

        if (status == Status.MAKING_PHOTO){
            tv_image.layoutParams = visibleViewParams
            iv_image.layoutParams = invisibleViewParams
        }
        else{
            tv_image.layoutParams = invisibleViewParams
            iv_image.layoutParams = visibleViewParams
        }
    }

    fun showProgress(){
        progressBar = ProgressDialog.show(this, null, null)
        progressBar!!.setCancelable(false)
    }

    fun hideProgress(){
        progressBar?.dismiss()
        progressBar = null
    }
}
