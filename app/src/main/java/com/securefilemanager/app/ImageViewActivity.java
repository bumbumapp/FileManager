package com.securefilemanager.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class ImageViewActivity extends AppCompatActivity {
    ImageView photoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        photoView=findViewById(R.id.photoView);
        Glide.with(getApplicationContext())
            .load(getIntent().getStringExtra("photopaths"))
            .into(photoView);
    }

}
