package com.example.a762map.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.a762map.R

class ExportPdfActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_pdf)
        title = "导出PDF"

        Toast.makeText(this, "这里后续接入真正的PDF导出逻辑", Toast.LENGTH_SHORT).show()
    }
}
