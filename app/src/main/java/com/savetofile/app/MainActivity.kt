package com.savetofile.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.savetofile.app.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var sharedContent: Intent? = null

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { saveFileToFolder(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedContent = intent
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedContent = intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    showTextSaveDialog(intent)
                } else {
                    showFileSaveDialog(intent)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                showMultipleFilesDialog(intent)
            }
            else -> {
                showMainMenu()
            }
        }
    }

    private fun showMainMenu() {
        binding.buttonSave.setOnClickListener {
            showSharePicker()
        }
    }

    private fun showSharePicker() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.select_folder)))
    }

    private fun showTextSaveDialog(intent: Intent) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrEmpty()) {
            Toast.makeText(this, R.string.no_content, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.apply {
            textContent.text = text
            buttonSave.setOnClickListener {
                folderPicker.launch(null)
            }
        }
    }

    private fun showFileSaveDialog(intent: Intent) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        if (uri == null) {
            Toast.makeText(this, R.string.no_content, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.apply {
            buttonSave.setOnClickListener {
                saveUriToFolder(uri)
            }
        }
    }

    private fun showMultipleFilesDialog(intent: Intent) {
        binding.apply {
            buttonSave.setOnClickListener {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.firstOrNull()?.let { saveUriToFolder(it) }
            }
        }
    }

    private fun saveFileToFolder(treeUri: Uri) {
        val text = binding.textContent.text.toString()
        if (text.isNotEmpty()) {
            saveTextContent(text, treeUri)
            return
        }

        val sharedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sharedContent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            sharedContent?.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        sharedUri?.let { saveUriToFolderUsingTree(it, treeUri) }
    }

    private fun saveTextContent(text: String, treeUri: Uri) {
        try {
            val documentsDir = DocumentFileCompat.fromTreeUri(this, treeUri)
            val fileName = "shared_${System.currentTimeMillis()}.txt"
            val file = documentsDir?.createFile("text/plain", fileName)

            file?.uri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(text.toByteArray())
                }
                Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
            }
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveUriToFolder(uri: Uri) {
        folderPicker.launch(null)
    }

    private fun saveUriToFolderUsingTree(sourceUri: Uri, treeUri: Uri) {
        try {
            val documentsDir = DocumentFileCompat.fromTreeUri(this, treeUri)
            val fileName = "file_${System.currentTimeMillis()}"
            val mimeType = contentResolver.getType(sourceUri) ?: "application/octet-stream"

            val file = documentsDir?.createFile(mimeType, fileName)
            file?.uri?.let { destUri ->
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
            }
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}