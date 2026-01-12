package com.example.stedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stedata.models.Machine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // LiveData che la UI osserverà. È privata in scrittura (_machines) e pubblica in sola lettura (machines)
    private val _machines = MutableLiveData<List<Machine>>()
    val machines: LiveData<List<Machine>> get() = _machines

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun loadMachines() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _error.value = "Utente non loggato"
            return
        }

        db.collection("users").document(uid)
            .collection("vending_machines")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<Machine>()
                for (doc in snapshot.documents) {
                    val m = doc.toObject(Machine::class.java)
                    if (m != null) list.add(m)
                }
                _machines.value = list // Aggiorna la UI
            }
            .addOnFailureListener { e ->
                _error.value = e.message
            }
    }

    // Puoi aggiungere qui anche la logica per deleteMachine, addRilevazione, ecc.
}