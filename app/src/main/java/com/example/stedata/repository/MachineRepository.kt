package com.example.stedata.repository

import com.example.stedata.models.Machine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MachineRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Recupera l'UID o lancia eccezione se non loggato
    private val uid: String
        get() = auth.currentUser?.uid ?: throw Exception("Utente non loggato")

    // CARICA MACCHINE
    suspend fun getMachines(): List<Machine> {
        val snapshot = db.collection("users").document(uid)
            .collection("vending_machines")
            .get()
            .await() // .await() trasforma la chiamata Firebase in una suspend function
        return snapshot.documents.mapNotNull { it.toObject(Machine::class.java) }
    }

    // SALVA RILEVAZIONE
    suspend fun saveRilevazione(machineId: String, rilevazione: Map<String, Any>) {
        val machineRef = db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)

        val timestamp = rilevazione["timestamp"] as? String ?: ""

        // 1. Controlla/Crea Macchina
        val doc = machineRef.get().await()
        if (!doc.exists()) {
            val newMachine = mapOf(
                "machineId" to machineId,
                "lastUpdate" to timestamp,
                "totalRilevazioni" to 1
            )
            machineRef.set(newMachine).await()
        } else {
            val total = doc.getLong("totalRilevazioni") ?: 0
            machineRef.update(
                "lastUpdate", timestamp,
                "totalRilevazioni", total + 1
            ).await()
        }

        // 2. Aggiungi Rilevazione
        machineRef.collection("rilevazioni").add(rilevazione).await()
    }

    // ELIMINA MACCHINA
    suspend fun deleteMachine(machineId: String) {
        val machineRef = db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)

        // Cancella sottocollezione rilevazioni
        val snapshot = machineRef.collection("rilevazioni").get().await()
        val batch = db.batch()
        for (doc in snapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()

        // Cancella documento macchina
        machineRef.delete().await()
    }
}