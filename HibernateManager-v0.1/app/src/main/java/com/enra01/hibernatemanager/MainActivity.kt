package com.enra01.hibernatemanager

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.Formatter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : androidx.appcompat.app.AppCompatActivity() {

    private val shizukuRequestCode = 100
    private lateinit var statusText: TextView
    private lateinit var ramText: TextView
    private lateinit var appList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "HIBERNATE MANAGER"
            textSize = 24f
        }

        statusText = TextView(this).apply {
            textSize = 16f
        }

        ramText = TextView(this).apply {
            textSize = 18f
        }

        val refreshButton = Button(this).apply {
            text = "Refresh"
            setOnClickListener { refresh() }
        }

        val shizukuButton = Button(this).apply {
            text = "Request Shizuku Permission"
            setOnClickListener { requestShizukuPermission() }
        }

        val hibernateAllButton = Button(this).apply {
            text = "Hibernate Selected (MVP)"
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Pilih aplikasi di daftar terlebih dahulu. Tombol ini akan diaktifkan pada tahap berikutnya.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        appList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(title)
        root.addView(statusText)
        root.addView(ramText)
        root.addView(shizukuButton)
        root.addView(refreshButton)
        root.addView(hibernateAllButton)
        root.addView(appList)

        setContentView(root)

        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == shizukuRequestCode) {
                updateShizukuStatus()
            }
        }

        refresh()
    }

    private fun refresh() {
        updateShizukuStatus()
        updateRam()
        loadApps()
    }

    private fun updateShizukuStatus() {
        statusText.text = when {
            Shizuku.isPreV11() -> "Shizuku: tidak didukung"
            !Shizuku.pingBinder() -> "Shizuku: TIDAK AKTIF"
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                "Shizuku: AKTIF • izin diberikan"
            else -> "Shizuku: AKTIF • izin belum diberikan"
        }
    }

    private fun requestShizukuPermission() {
        if (Shizuku.isPreV11()) return
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Jalankan Shizuku terlebih dahulu.", Toast.LENGTH_LONG).show()
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin Shizuku sudah diberikan.", Toast.LENGTH_SHORT).show()
            return
        }
        Shizuku.requestPermission(shizukuRequestCode)
    }

    private fun updateRam() {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)

        val total = Formatter.formatFileSize(this, info.totalMem)
        val available = Formatter.formatFileSize(this, info.availMem)
        val used = info.totalMem - info.availMem
        val percent = (used * 100 / info.totalMem).toInt()

        ramText.text = "RAM: ${usedBytes(used)} / $total • Available: $available • $percent%"
    }

    private fun usedBytes(value: Long): String =
        Formatter.formatFileSize(this, value)

    private fun loadApps() {
        appList.removeAllViews()

        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        apps.forEach { app ->
            val name = pm.getApplicationLabel(app).toString()
            val row = TextView(this).apply {
                text = "• $name\n  ${app.packageName}"
                textSize = 15f
                setPadding(0, 14, 0, 14)
            }
            appList.addView(row)
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener { _, _ -> }
        super.onDestroy()
    }
}
