package com.example.stedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // LiveData per comunicare il risultato alla UI
    // Usiamo un semplice Boolean: true = successo, false = fallimento
    private val _authResult = MutableLiveData<Boolean?>()
    val authResult: LiveData<Boolean?> get() = _authResult

    // LiveData per i messaggi di errore (es. "Password errata")
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // LiveData per mostrare il caricamento (es. disabilitare bottoni)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    var sharedEmail: String = ""

    fun login(email: String, password: String) {
        _isLoading.value = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _isLoading.value = false
                _authResult.value = true
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = "Login fallito: ${e.message}"
                _authResult.value = false
            }
    }

    fun register(name: String, email: String, password: String) {
        _isLoading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                // Se la creazione Auth ha successo, salviamo su Firestore
                val userId = result.user?.uid ?: return@addOnSuccessListener
                val userMap = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("users").document(userId).set(userMap)
                    .addOnSuccessListener {
                        _isLoading.value = false
                        _authResult.value = true // Tutto ok!
                    }
                    .addOnFailureListener { e ->
                        _isLoading.value = false
                        _errorMessage.value = "Errore salvataggio dati: ${e.message}"
                    }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = "Registrazione fallita: ${e.message}"
            }
    }

    // Serve per resettare lo stato quando si cambia schermata
    fun resetState() {
        _authResult.value = null
        _errorMessage.value = null
    }
}