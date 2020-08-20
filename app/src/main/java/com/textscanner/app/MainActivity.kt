package com.textscanner.app


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.view.TextureView
import android.widget.*
import com.textscanner.app.custom.AutoFitTextureView

class MainActivity : AppCompatActivity() {

    companion object {
        const val STATUS: String = "STATUS"

        const val MY_TAG: String = "My_log "
    }

    private val PERMISSION_CODE: Int = 1000

    lateinit var btnMakePhoto: ImageButton
    lateinit var btnProcessPhoto: ImageButton
    lateinit var btnRemakePhoto: ImageButton
    lateinit var surfaceTextureImage: AutoFitTextureView
    lateinit var spinnerSettingsList: Spinner

    var status: Status = Status.MAKING_PHOTO

    lateinit var mCameraManager: CameraManager
    var cameraService: CameraService? = null
    var mCameraBack: Int = 0
    var cameraBackResolutionsList: MutableList<Pair<Int, Int>> = mutableListOf()
    var displayCameraBackResolutionsList: MutableList<String> = mutableListOf()
    var currentCameraBackResolution: Int = 0

    var mBackgroundThread: HandlerThread? = null
    var mBackgroundHandler: Handler? = null

    val activity: Activity = this
    val context: Context = this

    var bitmapImage: Bitmap? = null

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
            cameraService?.closeCamera()
            cameraService = null
            return true
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            initCameraPreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        initViews(savedInstanceState)

        checkPermissions()
        mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        getCameraInfo()

    }

    override fun onPause() {
        super.onPause()
        if (cameraService != null && cameraService!!.isOpen()) cameraService?.closeCamera()
        cameraService = null
        stopBackgroundThread()
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        surfaceTextureImage.surfaceTextureListener = surfaceTextureListener
        if(status == Status.MAKING_PHOTO){
            initCameraPreview()
        }
    }

    fun initViews(savedInstanceState: Bundle?){
        btnMakePhoto = btn_make_photo
        btnProcessPhoto = btn_process_photo
        btnRemakePhoto = btn_remake_photo
        surfaceTextureImage = tv_image
        spinnerSettingsList = spinner_settings


        val adapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            displayCameraBackResolutionsList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSettingsList.adapter = adapter

        val statusSaved = savedInstanceState?.getString(STATUS) ?: "MAKING_PHOTO"
        status = Status.valueOf(statusSaved)
        enableButtonByStatus(status)

        btnMakePhoto.setOnClickListener(View.OnClickListener {
            status = Status.CHECKING_PHOTO
            enableButtonByStatus(status)
            //bitmapImage = cameraService?.makePhoto()
            cameraService?.closeCamera()
            cameraService = null
        })

        btnProcessPhoto.setOnClickListener(View.OnClickListener {
            status = Status.PROCESING_PHOTO
            enableButtonByStatus(status)
            initCameraPreview()
        })

        btnRemakePhoto.setOnClickListener(View.OnClickListener {
            status = Status.MAKING_PHOTO
            enableButtonByStatus(status)
            initCameraPreview()
        })

        spinnerSettingsList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentCameraBackResolution = position
                cameraService?.closeCamera()
                cameraService = null
                initCameraPreview()
            }
        }
    }

    fun enableButtonByStatus(status: Status){
        when(status){
            Status.MAKING_PHOTO ->{
                btnMakePhoto.visibility = ImageButton.VISIBLE
                btnProcessPhoto.visibility = ImageButton.INVISIBLE
                btnRemakePhoto.visibility = ImageButton.INVISIBLE
            }
            Status.CHECKING_PHOTO ->{
                btnMakePhoto.visibility = ImageButton.INVISIBLE
                btnProcessPhoto.visibility = ImageButton.VISIBLE
                btnRemakePhoto.visibility = ImageButton.VISIBLE
            }
            Status.PROCESING_PHOTO ->{ // сделать нормально, когда прикручу камеру
                btnMakePhoto.visibility = ImageButton.VISIBLE
                btnProcessPhoto.visibility = ImageButton.INVISIBLE
                btnRemakePhoto.visibility = ImageButton.INVISIBLE
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
                        cameraBackResolutionsList.add(size.width to size.height)
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
            surfaceTextureImage.setAspectRatio(cameraBackResolutionsList[currentCameraBackResolution].second, cameraBackResolutionsList[currentCameraBackResolution].first)
            cameraService = CameraService(
                context,
                activity,
                mCameraManager,
                surfaceTextureImage,
                mBackgroundHandler,
                mCameraBack.toString(),
                cameraBackResolutionsList[currentCameraBackResolution]
            )
            cameraService!!.openCamera()
        }
    }
}
