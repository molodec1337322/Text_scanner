package com.textscanner.app.cameraAPI

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission


class CameraService(
    val context: Context,
    val activity: Activity,
    val cameraManager: CameraManager,
    val textureView: TextureView,
    val backgroundHandler: Handler?,
    val cameraID:String,
    val size: Size,
    val previewSize: Size,
    val onImageCapturedHandler: OnImageCapturedHandler
) {
    enum class Status(){
        PREVIEW,
        WAITING_LOCK,
        WAITING_PRECAPTURE,
        WAITING_NON_PRECAPTURE,
        TAKEN
    }

    private val PERMISSION_CODE:Int = 1000
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var builder: CaptureRequest.Builder? = null
    private var surface: Surface? = null
    private var captureSession: CameraCaptureSession? = null
    private var status: Status =
        Status.PREVIEW
    private val cameraCallback = object: CameraDevice.StateCallback(){
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            closeCamera()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            closeCamera()
        }
    }

    val captureCallback = object : CameraCaptureSession.CaptureCallback() {
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

    val imageReaderListener = object: ImageReader.OnImageAvailableListener{
        override fun onImageAvailable(reader: ImageReader) {
            val image = reader.acquireNextImage()
            backgroundHandler?.post(
                ImageHandler(
                    image,
                    onImageCapturedHandler,
                    activity
                )
            )
        }
    }

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

    private fun startCameraPreview(){
        status = Status.PREVIEW
        val texture = textureView.surfaceTexture
        texture.setDefaultBufferSize(previewSize.width, previewSize.height)
        surface = Surface(texture)

        imageReader = ImageReader.newInstance(
            size.width,
            size.height,
            ImageFormat.JPEG,
            1
        )
        imageReader!!.setOnImageAvailableListener(imageReaderListener, backgroundHandler)
        cameraDevice!!.createCaptureSession(
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
                status =
                    Status.PREVIEW
            }
            else{
                requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), PERMISSION_CODE)
            }
        }
    }

    fun closeCamera(){
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
    }

    fun makePhoto() {
        val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureBuilder.addTarget(imageReader!!.surface)
        captureSession!!.capture(captureBuilder.build(), captureCallback, backgroundHandler)
    }
}