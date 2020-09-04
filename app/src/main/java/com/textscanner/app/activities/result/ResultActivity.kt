package com.textscanner.app.activities.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.textscanner.app.R
import com.textscanner.app.activities.camera.CameraActivity
import kotlinx.android.synthetic.main.activity_result.*

class ResultActivity : AppCompatActivity() {

    var extractedText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        extractedText = intent.getStringExtra(CameraActivity.EXTRACTED_TEXT)

        initViews()
    }

    private fun initViews() {
        ed_result.setText(extractedText)

        btn_back.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        })

        btn_copy.setOnClickListener(View.OnClickListener {
            val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("", ed_result.text)
            clipboard.setPrimaryClip(clip)
        })

        btn_share.setOnClickListener(View.OnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.setType("text/plain")

            intent.putExtra(Intent.EXTRA_TEXT, ed_result.text.toString())
            startActivity(Intent.createChooser(intent, "Поделиться"))
        })
    }
}