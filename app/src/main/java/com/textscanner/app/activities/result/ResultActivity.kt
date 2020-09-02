package com.textscanner.app.activities.result

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import com.textscanner.app.R
import com.textscanner.app.activities.camera.CameraActivity
import kotlinx.android.synthetic.main.activity_result.*

class ResultActivity : AppCompatActivity() {

    var extractedText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        extractedText = intent.getStringExtra(CameraActivity.EXTRACTED_TEXT)//savedInstanceState?.getString(CameraActivity.EXTRACTED_TEXT, "Не удалось извлечь текст") ?: "Не удалось извлечь текст"

        initViews()
    }

    private fun initViews() {
        ed_result.setText(extractedText)

        btn_back.setOnClickListener(View.OnClickListener {
            val intent: Intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        })
    }
}