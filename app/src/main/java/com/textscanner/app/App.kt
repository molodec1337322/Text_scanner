package com.textscanner.app

import android.app.Application
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class App : Application() {

    companion object {
        var instance: App? = null
    }

    init{
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        copyTessDataForTextRecognizer("rus.traineddata")
    }

    private fun tessDataPath(): String {
        return instance!!.getExternalFilesDir(null).toString() + "/tessdata/"
    }

    val tessDataParentDirectory: String
        get() = instance!!.getExternalFilesDir(null)!!.absolutePath

    private fun copyTessDataForTextRecognizer(trainedDataName: String) {
        val run = Runnable {
            val assetManager = instance!!.assets
            var out: OutputStream? = null
            try {
                val `in` = assetManager.open("tessdata/$trainedDataName")
                val tessPath = instance!!.tessDataPath()
                val tessFolder = File(tessPath)
                if (!tessFolder.exists()) {
                    tessFolder.mkdir()
                }
                val tessData = "$tessPath/$trainedDataName"
                val tessFile = File(tessData)
                if (!tessFile.exists()) {
                    out = FileOutputStream(tessData)
                    val buffer = ByteArray(1024)
                    var read = `in`.read(buffer)
                    while (read != -1) {
                        out.write(buffer, 0, read)
                        read = `in`.read(buffer)
                    }
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    out?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        Thread(run).start()
    }
}