package com.example.covidinfoapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.covidinfoapp.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main)

        var buttonNext = findViewById<View>(R.id.ivButton) as ImageView
        buttonNext!!.setOnClickListener { startActivity(Intent(this, CovidActivity::class.java)) }
    }
}