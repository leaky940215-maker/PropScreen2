package com.propscreen.player

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var listProps: ListView
    private lateinit var btnOrientation: Button
    private lateinit var switchLock: Switch

    private val prefs by lazy { getSharedPreferences("propscreen_prefs", Context.MODE_PRIVATE) }
    private val propsDir by lazy {
        File(filesDir, "props").apply { if (!exists()) mkdirs() }
    }

    private var propFiles: List<File> = emptyList()

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importHtmlFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listProps = findViewById(R.id.listProps)
        btnOrientation = findViewById(R.id.btnOrientation)
        switchLock = findViewById(R.id.switchLock)
        val btnImport: Button = findViewById(R.id.btnImport)

        ensureSampleFileExists()

        // Restore settings
        updateOrientationButtonText()
        switchLock.isChecked = prefs.getBoolean("lock_mode", true)

        btnImport.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("text/html", "application/xhtml+xml", "*/*"))
        }

        btnOrientation.setOnClickListener {
            val current = prefs.getInt("orientation_mode", 0) // 0=Portrait, 1=Landscape, 2=Auto
            val next = (current + 1) % 3
            prefs.edit().putInt("orientation_mode", next).apply()
            updateOrientationButtonText()
        }

        switchLock.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("lock_mode", isChecked).apply()
        }

        listProps.setOnItemClickListener { _, _, position, _ ->
            val file = propFiles[position]
            launchPlayer(file)
        }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun updateOrientationButtonText() {
        val label = when (prefs.getInt("orientation_mode", 0)) {
            0 -> "세로 고정"
            1 -> "가로 고정"
            else -> "자동 회전"
        }
        btnOrientation.text = label
    }

    private fun ensureSampleFileExists() {
        if (propsDir.listFiles().isNullOrEmpty()) {
            val sampleFile = File(propsDir, "00_풀스크린_테스트.html")
            sampleFile.writeText(
                """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                  <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                      background: #00B140;
                      color: #ffffff;
                      font-family: sans-serif;
                      height: 100vh;
                      display: flex;
                      flex-direction: column;
                      align-items: center;
                      justify-content: center;
                      text-align: center;
                      user-select: none;
                    }
                    .box {
                      background: rgba(0,0,0,0.65);
                      padding: 24px 28px;
                      border-radius: 14px;
                      max-width: 85%;
                    }
                    h1 { font-size: 20px; margin-bottom: 10px; }
                    p { font-size: 13px; line-height: 1.6; color: #d1d5db; }
                  </style>
                </head>
                <body>
                  <div class="box">
                    <h1>100% 풀스크린 테스트 성공</h1>
                    <p>상단 상태바/주소창 없이 꽉 찬 화면입니다.<br><br>
                    <b>[나가기 / NG 리셋]</b><br>
                    볼륨 하(-) 버튼 1.5초 길게 누르기<br>
                    또는 좌측 상단 모서리 5회 연타</p>
                  </div>
                </body>
                </html>
                """.trimIndent()
            )
        }
    }

    private fun importHtmlFile(uri: Uri) {
        try {
            var fileName = getFileName(uri) ?: "prop_${System.currentTimeMillis()}.html"
            if (!fileName.lowercase(Locale.ROOT).endsWith(".html") &&
                !fileName.lowercase(Locale.ROOT).endsWith(".htm")
            ) {
                fileName += ".html"
            }

            val destFile = File(propsDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, "추가됨: $fileName", Toast.LENGTH_SHORT).show()
            refreshList()
        } catch (e: Exception) {
            Toast.makeText(this, "파일 가져오기 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun refreshList() {
        propFiles = propsDir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".html", true) || it.name.endsWith(".htm", true)) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        listProps.adapter = PropAdapter()
    }

    private fun launchPlayer(file: File) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("file_path", file.absolutePath)
        }
        startActivity(intent)
    }

    private inner class PropAdapter : BaseAdapter() {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

        override fun getCount(): Int = propFiles.size
        override fun getItem(position: Int): Any = propFiles[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.item_prop, parent, false)

            val file = propFiles[position]
            val tvFileName = view.findViewById<TextView>(R.id.tvFileName)
            val tvFileMeta = view.findViewById<TextView>(R.id.tvFileMeta)
            val btnDelete = view.findViewById<Button>(R.id.btnDelete)

            tvFileName.text = file.name
            val sizeKb = (file.length() / 1024).coerceAtLeast(1)
            tvFileMeta.text = "${dateFormat.format(Date(file.lastModified()))} · ${sizeKb}KB"

            btnDelete.setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("소품 삭제")
                    .setMessage("'${file.name}' 파일을 목록에서 삭제할까요?")
                    .setPositiveButton("삭제") { _, _ ->
                        file.delete()
                        refreshList()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }

            return view
        }
    }
}
