package com.sea.quickstart.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sea.quickstart.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    fun openPermission(view: View) {
        val intent = Intent(this, PermissionActivity::class.java)
        startActivity(intent)
    }
}