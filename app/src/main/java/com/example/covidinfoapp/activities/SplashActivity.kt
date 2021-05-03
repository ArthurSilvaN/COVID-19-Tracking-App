package com.example.covidinfoapp.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.example.covidinfoapp.R

@Suppress("DEPRECATION")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash)
        changeToLogin()
    }

    private fun changeToLogin() {
        val intent = Intent( this, MainActivity::class.java);
        Handler().postDelayed( {
            intent.change()
        },  5000)
    }
    fun Intent.change() {
        startActivity(this)
        finish()
    }
}