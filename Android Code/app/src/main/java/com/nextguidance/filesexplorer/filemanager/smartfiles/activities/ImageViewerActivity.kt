package com.nextguidance.filesexplorer.filemanager.smartfiles.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.nextguidance.filesexplorer.filemanager.smartfiles.R

class ImageViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)
        
        val photoView: PhotoView = findViewById(R.id.photo_view)
        
        findViewById<android.view.View>(R.id.btn_back).setOnClickListener {
            finish()
        }
        
        val uri = intent.data
        
        if (uri != null) {
            Glide.with(this)
                .load(uri)
                .into(photoView)
        }
    }
}
