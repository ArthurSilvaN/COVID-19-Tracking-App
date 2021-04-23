package com.example.covidinfoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main)

        var buttonNext = findViewById<View>(R.id.ivButton) as ImageView
        buttonNext!!.setOnClickListener { startActivity(Intent(this, CovidActivity::class.java)) }
    }
}