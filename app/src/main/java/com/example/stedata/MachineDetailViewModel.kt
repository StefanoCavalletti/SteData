package com.example.stedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stedata.models.Rilevazione
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MachineDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _rilevazioni = MutableLiveData<List<Rilevazione>>()
    val rilevazioni: LiveData<List<Rilevazione>> get() = _rilevazioni

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> get() = _statusMessage

    fun loadRilevazioni(machineId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)
            .collection("rilevazioni")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toObject(Rilevazione::class.java) }
                // Ordinato per data decrescente
                _rilevazioni.value = list.sortedByDescending { it.timestamp }
            }
            .addOnFailureListener { e ->
                _statusMessage.value = "Errore: ${e.message}"
            }
    }

    fun deleteRilevazione(machineId: String, rilevazione: Rilevazione) {
        val uid = auth.currentUser?.uid ?: return
        val rilevazioniRef = db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)
            .collection("rilevazioni")

        // Query per trovare il documento specifico
        rilevazioniRef
            .whereEqualTo("timestamp", rilevazione.timestamp)
            .whereEqualTo("incasso", rilevazione.incasso)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val docId = snapshot.documents.first().id
                    rilevazioniRef.document(docId).delete()
                        .addOnSuccessListener {
                            _statusMessage.value = "Rilevazione eliminata"
                            loadRilevazioni(machineId) // Ricarica la lista
                        }
                } else {
                    _statusMessage.value = "Impossibile trovare la rilevazione da eliminare"
                }
            }
            .addOnFailureListener {
                _statusMessage.value = "Errore eliminazione: ${it.message}"
            }
    }
}