package com.textscanner.app.activities.settings

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import com.textscanner.app.models.CameraSettings
import com.textscanner.app.R
import com.textscanner.app.libs.TinyDB
import com.textscanner.app.activities.camera.CameraActivity
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    lateinit var btnReturnBack: ImageButton
    lateinit var resolutionsSpinner: Spinner

    var displayCameraBackResolutionsList: MutableList<String>? = mutableListOf()
    var cameraBackResolutionList: MutableList<Size>? = mutableListOf()
    var currentCameraBackResolution: Int = 0
    var isLaunching: Boolean = true

    val onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
        override fun onNothingSelected(parent: AdapterView<*>?) {

        }

        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            if (currentCameraBackResolution != position && !isLaunching) {
                currentCameraBackResolution = position
            }
            else if(isLaunching){
                isLaunching = false
                resolutionsSpinner.setSelection(currentCameraBackResolution)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        resolutionsSpinner = spinner_resolutions

        initViews()
    }

    fun initViews(){
        btnReturnBack = btn_back
        resolutionsSpinner = spinner_resolutions

        btnReturnBack.setOnClickListener(View.OnClickListener {
            saveInfoToSharedPreference()
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra(CameraActivity.RESOLUTION_CURRENT, currentCameraBackResolution)
            startActivity(intent)
        })

        getInfoFromSharedPreferences()

        val adapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            R.layout.spinner_item,
            R.id.tv_resoulution,
            displayCameraBackResolutionsList!!
        )
        adapter.setDropDownViewResource(R.layout.spinner_item)
        resolutionsSpinner.adapter = adapter
        resolutionsSpinner.onItemSelectedListener = onItemSelectedListener
    }

    fun getInfoFromSharedPreferences(){
        val tinyDB = TinyDB(this)
        val cameraSettings = tinyDB.getObject(CameraActivity.CAMERA_SETTINGS, CameraSettings::class.java) ?: null
        currentCameraBackResolution = cameraSettings!!.currentResolutionIndex
        cameraBackResolutionList = cameraSettings.backCameraResolutionsList
        displayCameraBackResolutionsList = cameraSettings.displayCameraResolutionList
    }

    fun saveInfoToSharedPreference(){
        val tinyDB = TinyDB(this)
        tinyDB.putObject(CameraActivity.CAMERA_SETTINGS,
            CameraSettings(
                currentCameraBackResolution,
                cameraBackResolutionList!!,
                displayCameraBackResolutionsList!!
            )
        )
    }
}