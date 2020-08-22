package com.textscanner.app.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import com.textscanner.app.R
import com.textscanner.app.Status
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    lateinit var btnReturnBack: ImageButton
    lateinit var spinnerResolution: Spinner

    var displayCameraBackResolutionsList: MutableList<String>? = mutableListOf()
    var currentCameraBackResolution: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        spinnerResolution = spinner_resolutions

        initViews()
    }

    fun initViews(){
        btnReturnBack = btn_back
        spinnerResolution = spinner_resolutions

        btnReturnBack.setOnClickListener(View.OnClickListener {
            val intent: Intent = Intent(this, CameraActivity::class.java)
            intent.putExtra(CameraActivity.RESOLUTION_CURRENT, currentCameraBackResolution)
            startActivity(intent)
        })

        displayCameraBackResolutionsList = intent.getStringArrayExtra(CameraActivity.RESOLUTION_LIST)?.toMutableList() ?: mutableListOf("N/D")
        currentCameraBackResolution = intent.getIntExtra(CameraActivity.RESOLUTION_CURRENT, 0)
        val adapter: ArrayAdapter<String> = ArrayAdapter(
            this,
            R.layout.spinner_item,
            R.id.tv_resoulution,
            displayCameraBackResolutionsList!!
        )
        adapter.setDropDownViewResource(R.layout.spinner_item)
        spinnerResolution.setSelection(currentCameraBackResolution)
        spinnerResolution.adapter = adapter
        spinnerResolution.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (currentCameraBackResolution != position) {
                    currentCameraBackResolution = position
                }
            }
        }
    }
}