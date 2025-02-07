package com.example.docchat.ui.chat

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.docchat.R
import com.github.chrisbanes.photoview.PhotoView

class PhotoViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view)

        val imageUrl = intent.getStringExtra("imageUrl")
        val photoView: PhotoView = findViewById(R.id.photoView)

        Glide.with(this)
            .load(imageUrl)
            .into(photoView)
    }
}
