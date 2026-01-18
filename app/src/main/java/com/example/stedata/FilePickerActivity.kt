package com.example.stedata

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.stedata.databinding.ActivityFilePickerBinding
import com.example.stedata.models.EvaDtsReport
import com.example.stedata.parser.EvaDtsParser
import com.example.stedata.repository.MachineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FilePickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilePickerBinding
    private val parser = EvaDtsParser()
    private val repository = MachineRepository()
    private var currentReport: EvaDtsReport? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                parseFile(uri)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    private fun setupUI() {
        // seleziona file
        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }
        // torna indietro
        binding.toolbar.setNavigationOnClickListener {
            finish() // Chiude l'activity
        }
        //conferma
        binding.btnSave.setOnClickListener {
            currentReport?.let { report ->
                saveToFirebase(report)
            }
        }
        //annulla
        binding.btnCancel.setOnClickListener {
            resetUI()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/plain",
                "application/octet-stream",
                "*/*"
            ))
        }
        filePickerLauncher.launch(intent)
    }

    private fun parseFile(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSelectFile.isEnabled = false // Evita doppi click

        lifecycleScope.launch {
            try {
                // Parsing
                val report = withContext(Dispatchers.IO) {
                    parser.parseFromUri(this@FilePickerActivity, uri)
                }

                if (report != null) {
                    currentReport = report
                    showReportPreview(report)
                } else {
                    showError(getString(R.string.msg_read_error))
                }
            } catch (e: Exception) {
                showError(getString(R.string.msg_generic_error, e.message))
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showReportPreview(report: EvaDtsReport) {
        val preview = buildString {
            appendLine(getString(R.string.preview_header))
            appendLine(getString(R.string.preview_separator))
            appendLine(getString(R.string.preview_serial, report.machineInfo.serialNumber))

            val incassoFormatted = String.format("€%.2f", report.salesData.paidVendValueInit)
            appendLine(getString(R.string.preview_total_cash, incassoFormatted))

            appendLine(getString(R.string.preview_sales_count, report.salesData.paidVendCountInit))
            appendLine(getString(R.string.preview_products_count, report.products.size))
        }

        binding.tvPreview.text = preview

        // nascondo pulsante seleziona e mostro gli altri
        binding.btnSelectFile.visibility = View.GONE
        binding.actionButtonsLayout.visibility = View.VISIBLE
    }

    private fun resetUI() {
        currentReport = null
        binding.tvPreview.text = getString(R.string.picker_preview_placeholder)

        // Torna allo stato iniziale
        binding.actionButtonsLayout.visibility = View.GONE
        binding.btnSelectFile.visibility = View.VISIBLE
        binding.btnSelectFile.isEnabled = true
    }

    private fun saveToFirebase(report: EvaDtsReport) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        binding.btnCancel.isEnabled = false

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val machineId = report.machineInfo.assetNumber ?: report.machineInfo.serialNumber
        val rilevazioneMap = hashMapOf<String, Any>(
            "timestamp" to timestamp,
            "machineId" to machineId,
            "incasso" to (report.salesData.paidVendValueInit ?: 0.0),
            "numeroVendite" to (report.salesData.paidVendCountInit ?: 0),
            "contanti" to (report.cashData.cashSalesValueInit ?: 0.0),
            "file" to (report.header.communicationId ?: "")
        )


        lifecycleScope.launch {
            try {
                repository.saveRilevazione(machineId, rilevazioneMap)
                Toast.makeText(this@FilePickerActivity, getString(R.string.msg_save_success), Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                showError(getString(R.string.msg_save_error, e.message))

                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                binding.btnCancel.isEnabled = true
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.tvPreview.text = getString(R.string.error_prefix, message)

        binding.btnSelectFile.visibility = View.VISIBLE
        binding.btnSelectFile.isEnabled = true
        binding.actionButtonsLayout.visibility = View.GONE
    }
}