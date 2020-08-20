package com.textscanner.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.view.Surface
import android.widget.Toast
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import android.os.Handler
import android.util.Log
import android.view.TextureView


class CameraService(
    val context: Context,
    val activity: Activity,
    val cameraManager: CameraManager,
    val textureView: TextureView,
    val backgroundHandler: Handler?,
    val cameraID:String,
    val size: Pair<Int, Int>
) {
    private val PERMISSION_CODE:Int = 1000
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var builder: CaptureRequest.Builder? = null
    private var surface: Surface? = null
    private var captureSession: CameraCaptureSession? = null
    private val cameraCallback = object: CameraDevice.StateCallback(){
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startCamera()
        }

        override fun onDisconnected(camera: CameraDevice) {
            closeCamera()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            closeCamera()
        }
    }
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult
        ) {

        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {

        }
    }
    val cameraCaptureSession = object: CameraCaptureSession.StateCallback(){
        override fun onConfigureFailed(session: CameraCaptureSession) {
            closeCamera()
        }

        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            makePreview()
        }
    }
    /*
    val onImageAvailableListener = object: ImageReader.OnImageAvailableListener{
        override fun onImageAvailable(reader: ImageReader) {
            val image = reader.acquireNextImage()
            backgroundHandler?.post()
        }
    }
     */

    private fun makePreview() {
        builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        builder!!.addTarget(surface!!)
        builder!!.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
        captureSession?.setRepeatingRequest(
            builder!!.build(),
            captureCallback,
            backgroundHandler
        )
    }

    private fun startCamera(){
        val texture = textureView.surfaceTexture
        texture.setDefaultBufferSize(size.first, size.second)
        surface = Surface(texture)

        imageReader = ImageReader.newInstance(
            size.first,
            size.second,
            ImageFormat.RAW_SENSOR,
            1
        )
        cameraDevice?.createCaptureSession(
            mutableListOf(surface, imageReader!!.surface),
            cameraCaptureSession,
            backgroundHandler
        )
    }


    fun isOpen(): Boolean{
        return cameraDevice != null
    }

    fun openCamera(){
        if(!isOpen()){
            if(checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                cameraManager.openCamera(
                    cameraID,
                    cameraCallback,
                    backgroundHandler
                )
            }
            else{
                requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), PERMISSION_CODE)
            }
        }
    }

    fun closeCamera(){
        cameraDevice?.close()
        cameraDevice = null
    }


    fun makePhoto(handler: ImageHandler){
        imageReader?.setOnImageAvailableListener(object: ImageReader.OnImageAvailableListener{
            override fun onImageAvailable(reader: ImageReader) {
                val image = reader.acquireNextImage()
                backgroundHandler?.post(handler.handleImage(image))
            }
        }, backgroundHandler)
    }
}