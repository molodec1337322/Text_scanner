package com.textscanner.app.activities


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_camera.*
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Size
import android.view.TextureView
import android.view.ViewGroup
import android.widget.*
import com.textscanner.app.CameraService
import com.textscanner.app.ImageHandler
import com.textscanner.app.R
import com.textscanner.app.Status
import com.textscanner.app.custom.AutoFitImageView
import com.textscanner.app.custom.AutoFitTextureView

class CameraActivity : AppCompatActivity() {

    companion object {
        const val STATUS: String = "STATUS"

        const val MY_TAG: String = "My_log "

        const val RESOLUTION_LIST: String = "RESOLUTION_LIST"
        const val RESOLUTION_CURRENT: String = "RESOLUTION_CURRENT"
    }

    private val PERMISSION_CODE: Int = 1000
    private val OPERATION_CHOOSE_PHOTO = 100

    lateinit var btnMakePhoto: ImageButton
    lateinit var btnProcessPhoto: ImageButton
    lateinit var btnRemakePhoto: ImageButton
    lateinit var btnSettings: ImageButton
    lateinit var btnGallery: ImageButton
    lateinit var surfaceTextureImage: AutoFitTextureView
    lateinit var surfaceImageView: AutoFitImageView

    lateinit var tvProcess: TextView
    lateinit var tvRemake: TextView
    lateinit var tvSettings: TextView
    lateinit var tvGallery: TextView

    var status: Status = Status.MAKING_PHOTO

    lateinit var mCameraManager: CameraManager
    var cameraService: CameraService? = null
    var mCameraBack: Int = 0
    var cameraBackResolutionsList: MutableList<Size> = mutableListOf()
    var displayCameraBackResolutionsList: MutableList<String> = mutableListOf()
    var currentCameraBackResolution: Int = 0

    var mBackgroundThread: HandlerThread? = null
    var mBackgroundHandler: Handler? = null

    val activity: Activity = this
    val context: Context = this

    var bitmapImage: Bitmap? = null
    var cameraImage: Image? = null

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
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        surfaceTextureImage.surfaceTextureListener = surfaceTextureListener
    }

    fun initViews(savedInstanceState: Bundle?){
        btnMakePhoto = btn_make_photo
        btnProcessPhoto = btn_process_photo
        btnRemakePhoto = btn_remake_photo
        btnSettings = btn_settings
        btnGallery = btn_gallery
        surfaceTextureImage = tv_image
        surfaceImageView = iv_image

        tvProcess = tv_process
        tvRemake = tv_remake
        tvGallery = tv_gallery
        tvSettings = tv_settings

        val statusSaved = savedInstanceState?.getString(STATUS) ?: "MAKING_PHOTO"
        currentCameraBackResolution = intent.getIntExtra(RESOLUTION_CURRENT, 0)

        status = Status.valueOf(statusSaved)
        enableButtonByStatus(status)

        btnMakePhoto.setOnClickListener(View.OnClickListener {
            status = Status.CHECKING_PHOTO
            enableButtonByStatus(status)
        })

        btnProcessPhoto.setOnClickListener(View.OnClickListener {
            status = Status.PROCESING_PHOTO
            enableButtonByStatus(status)
        })

        btnRemakePhoto.setOnClickListener(View.OnClickListener {
            status = Status.MAKING_PHOTO
            enableButtonByStatus(status)
        })

        btnSettings.setOnClickListener(View.OnClickListener{
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("RESOLUTION_LIST", displayCameraBackResolutionsList.toTypedArray())
            intent.putExtra("RESOLUTION_CURRENT", currentCameraBackResolution)
            startActivity(intent)
        })

        btnGallery.setOnClickListener(View.OnClickListener{
            stopCameraPreview()
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, OPERATION_CHOOSE_PHOTO)
        })
    }

    fun enableButtonByStatus(status: Status){
        when(status){
            Status.MAKING_PHOTO ->{
                btnMakePhoto.visibility = ImageButton.VISIBLE
                btnProcessPhoto.visibility = ImageButton.INVISIBLE
                btnRemakePhoto.visibility = ImageButton.INVISIBLE
                btnSettings.visibility = ImageButton.VISIBLE
                btnGallery.visibility = ImageButton.VISIBLE

                tvProcess.visibility = TextView.INVISIBLE
                tvRemake.visibility = TextView.INVISIBLE
                tvGallery.visibility = TextView.VISIBLE
                tvSettings.visibility = TextView.VISIBLE
                initCameraPreview()
            }
            Status.CHECKING_PHOTO ->{
                btnMakePhoto.visibility = ImageButton.INVISIBLE
                btnProcessPhoto.visibility = ImageButton.VISIBLE
                btnRemakePhoto.visibility = ImageButton.VISIBLE
                btnSettings.visibility = ImageButton.INVISIBLE
                btnGallery.visibility = ImageButton.INVISIBLE

                tvProcess.visibility = TextView.VISIBLE
                tvRemake.visibility = TextView.VISIBLE
                tvGallery.visibility = TextView.INVISIBLE
                tvSettings.visibility = TextView.INVISIBLE
                stopCameraPreview()
            }
            Status.PROCESING_PHOTO ->{ // сделать нормально, когда прикручу камеру
                btnMakePhoto.visibility = ImageButton.VISIBLE
                btnProcessPhoto.visibility = ImageButton.INVISIBLE
                btnRemakePhoto.visibility = ImageButton.INVISIBLE
                btnSettings.visibility = ImageButton.VISIBLE
                btnGallery.visibility = ImageButton.VISIBLE

                tvProcess.visibility = TextView.INVISIBLE
                tvRemake.visibility = TextView.INVISIBLE
                tvGallery.visibility = TextView.VISIBLE
                tvSettings.visibility = TextView.VISIBLE
                initCameraPreview()
            }
        }
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
            bitmapImage = MediaStore.Images.Media.getBitmap(this.contentResolver, data?.data)
            status = Status.CHECKING_PHOTO
            enableButtonByStatus(status)
            surfaceImageView.setImageBitmap(bitmapImage)
        }
    }

    fun getCameraInfo(){
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
            else{
                mCameraBack = -1
            }
        }
        currentCameraBackResolution = 0
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
        if(cameraService == null && surfaceTextureImage.isAvailable){
            changeVisibilityOfImageViews(status)
            val previewRes = getSmallestPossiblePreviewSize(
                cameraBackResolutionsList[currentCameraBackResolution],
                Size(surfaceTextureImage.measuredWidth, surfaceTextureImage.measuredHeight)
            )
            surfaceTextureImage.setAspectRatio(previewRes.height, previewRes.width)
            cameraService = CameraService(
                context,
                activity,
                mCameraManager,
                surfaceTextureImage,
                mBackgroundHandler,
                mCameraBack.toString(),
                cameraBackResolutionsList[currentCameraBackResolution],
                previewRes
            )
            cameraService!!.openCamera()
        }
    }

    fun getSmallestPossiblePreviewSize(cameraSize: Size, previewSize: Size): Size{
        if(cameraSize.height <= previewSize.height && cameraSize.width <= previewSize.width){
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
        return Size(cameraSize.width / 2, cameraSize.height / 2)
    }

    fun stopCameraPreview(){
        cameraService?.closeCamera()
        cameraService = null
        changeVisibilityOfImageViews(status)
    }

    fun changeVisibilityOfImageViews(status: Status){
        val visibleViewParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0
        )
        visibleViewParams.weight = 1.0f
        val invisibleViewParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0
        )
        invisibleViewParams.weight = 0.0f

        if (status == Status.MAKING_PHOTO){
            surfaceTextureImage.layoutParams = visibleViewParams
            surfaceImageView.layoutParams = invisibleViewParams
        }
        else{
            surfaceTextureImage.layoutParams = invisibleViewParams
            surfaceImageView.layoutParams = visibleViewParams
        }
    }
}
