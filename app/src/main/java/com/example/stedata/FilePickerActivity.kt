package com.example.stedata

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.stedata.databinding.ActivityFilePickerBinding
import com.example.stedata.models.EvaDtsReport
import com.example.stedata.parser.EvaDtsParser
import com.example.stedata.repository.MachineRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FilePickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilePickerBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val parser = EvaDtsParser()
    private val repository = MachineRepository()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                parseAndSaveFile(uri)
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
        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }

        binding.btnBack.setOnClickListener {
            finish()
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

    private fun parseAndSaveFile(uri: Uri) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSelectFile.isEnabled = false

        lifecycleScope.launch {
            try {
                val report = withContext(Dispatchers.IO) {
                    parser.parseFromUri(this@FilePickerActivity, uri)
                }

                if (report != null) {
                    showReportPreview(report)
                    saveToFirebase(report)
                } else {
                    showError("Errore durante il parsing del file")
                }
            } catch (e: Exception) {
                showError("Errore: ${e.message}")
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSelectFile.isEnabled = true
            }
        }
    }

    private fun showReportPreview(report: EvaDtsReport) {
        val preview = buildString {
            appendLine("📊 REPORT EVA DTS")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()

            appendLine("🏷️ MACCHINA")
            appendLine("Serial: ${report.machineInfo.serialNumber}")
            report.machineInfo.modelNumber?.let { appendLine("Modello: $it") }
            report.machineInfo.assetNumber?.let { appendLine("Asset: $it") }
            appendLine()

            appendLine("💰 VENDITE")
            appendLine("Valore: €${String.format("%.2f", report.salesData.paidVendValueInit)}")
            appendLine("Numero: ${report.salesData.paidVendCountInit}")
            appendLine()

            appendLine("💵 CONTANTI")
            appendLine("Vendite: €${String.format("%.2f", report.cashData.cashSalesValueInit)}")
            appendLine("Numero: ${report.cashData.cashSalesCountInit}")
            appendLine()

            if (report.cashlessData != null) {
                appendLine("💳 CASHLESS")
                report.cashlessData.cashless1SalesValueInit?.let {
                    appendLine("Vendite: €${String.format("%.2f", it)}")
                }
                report.cashlessData.cashless1SalesCountInit?.let {
                    appendLine("Numero: $it")
                }
                appendLine()
            }

            appendLine("📦 PRODOTTI")
            appendLine("Totale: ${report.products.size}")
            report.products.take(5).forEach { product ->
                appendLine("  • ${product.productId}: €${String.format("%.2f", product.price ?: 0.0)}")
            }
            if (report.products.size > 5) {
                appendLine("  ... e altri ${report.products.size - 5}")
            }
        }

        binding.tvPreview.text = preview
        binding.tvPreview.visibility = android.view.View.VISIBLE
        binding.btnSave.visibility = android.view.View.VISIBLE

        binding.btnSave.setOnClickListener {
            saveToFirebase(report)
        }
    }

    /*private fun saveToFirebase(report: EvaDtsReport) {
        val uid = auth.currentUser?.uid ?: return
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val machineId = report.machineInfo.assetNumber ?: report.machineInfo.serialNumber

        // Crea documento rilevazione
        // costruisco la rilevazione principale come MutableMap<String, Any>
        val rilevazione: MutableMap<String, Any> = hashMapOf(
            "timestamp" to timestamp,
            "machineId" to machineId,
            "incasso" to (report.salesData.paidVendValueInit ?: 0.0),
            "numeroVendite" to (report.salesData.paidVendCountInit ?: 0),
            "contanti" to (report.cashData.cashSalesValueInit ?: 0.0),
            "cashless" to (report.cashlessData?.cashless1SalesValueInit ?: 0.0),
            "file" to (report.header.communicationId ?: "")
        )

// costruisco esplicitamente una HashMap<String, Any> per i prodotti
        val prodottiMap = hashMapOf<String, Any>()
        for (product in report.products) {
            val prodottoInfo: Map<String, Any> = mapOf(
                "prezzo" to (product.price ?: 0.0),
                "vendite" to (product.paidCountInit ?: 0),
                "valore" to (product.paidValueInit ?: 0.0)
            )
            // usa productId come key (assicurati che non sia null)
            val pid = product.productId ?: "unknown"
            prodottiMap[pid] = prodottoInfo
        }

// assegno la mappa dei prodotti alla rilevazione
        rilevazione["prodotti"] = prodottiMap

// ora puoi salvare 'rilevazione' su Firestore, ad esempio:
// db.collection("users").document(uid).collection("...").add(rilevazione)



        val machineRef = db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)

        // Aggiorna o crea macchina
        machineRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val newMachine = mapOf(
                    "machineId" to machineId,
                    "serialNumber" to report.machineInfo.serialNumber,
                    "model" to (report.machineInfo.modelNumber ?: ""),
                    "lastUpdate" to timestamp,
                    "totalRilevazioni" to 1
                )
                machineRef.set(newMachine)
            } else {
                val currentTotal = doc.getLong("totalRilevazioni") ?: 0
                machineRef.update(
                    "lastUpdate", timestamp,
                    "totalRilevazioni", currentTotal + 1
                )
            }

            // Salva rilevazione
            machineRef.collection("rilevazioni").add(rilevazione)
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        "✅ Rilevazione salvata con successo!",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    showError("Errore nel salvataggio: ${e.message}")
                }
        }
    }


     */

    private fun saveToFirebase(report: EvaDtsReport) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val machineId = report.machineInfo.assetNumber ?: report.machineInfo.serialNumber

        // Prepariamo la mappa dati (identica struttura del ViewModel!)
        val rilevazioneMap = hashMapOf<String, Any>(
            "timestamp" to timestamp,
            "machineId" to machineId,
            "incasso" to (report.salesData.paidVendValueInit ?: 0.0),
            "numeroVendite" to (report.salesData.paidVendCountInit ?: 0),
            "contanti" to (report.cashData.cashSalesValueInit ?: 0.0),
            "file" to (report.header.communicationId ?: "")
            // ... aggiungi altri campi se vuoi ...
        )

        lifecycleScope.launch {
            try {
                repository.saveRilevazione(machineId, rilevazioneMap)
                Toast.makeText(this@FilePickerActivity, "✅ Salvato con successo!", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                showError("Errore nel salvataggio: ${e.message}")
            }
        }
    }


    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.tvPreview.text = "❌ $message"
        binding.tvPreview.visibility = android.view.View.VISIBLE
    }
}