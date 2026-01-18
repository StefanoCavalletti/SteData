package com.example.stedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // campi databinding
    val name = MutableLiveData<String>("")
    val email = MutableLiveData<String>("")
    val password = MutableLiveData<String>("")
    val confirmPassword = MutableLiveData<String>("")

    // stati ui
    private val _authResult = MutableLiveData<Boolean?>()
    val authResult: LiveData<Boolean?> get() = _authResult

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // funzione login
    fun login() {
        val currentEmail = email.value?.trim() ?: ""
        val currentPassword = password.value?.trim() ?: ""

        if (currentEmail.isEmpty() || currentPassword.isEmpty()) {
            _errorMessage.value = "Compila email e password"
            return
        }

        _isLoading.value = true
        auth.signInWithEmailAndPassword(currentEmail, currentPassword)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _authResult.value = true
                } else {
                    _errorMessage.value = "Errore: ${task.exception?.message}"
                }
            }
    }

    // funzione Register
    fun register() {
        val currentName = name.value?.trim() ?: ""
        val currentEmail = email.value?.trim() ?: ""
        val currentPassword = password.value?.trim() ?: ""
        val currentConfirm = confirmPassword.value?.trim() ?: ""

        if (currentName.isEmpty() || currentEmail.isEmpty() || currentPassword.isEmpty()) {
            _errorMessage.value = "Compila tutti i campi"
            return
        }

        if (currentPassword != currentConfirm) {
            _errorMessage.value = "Le password non coincidono"
            return
        }

        _isLoading.value = true
        auth.createUserWithEmailAndPassword(currentEmail, currentPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(currentName)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        _isLoading.value = false
                        _authResult.value = true
                    }
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Errore: ${task.exception?.message}"
                }
            }
    }

    fun resetState() {
        // Non resetto email/password qui per permettere la persistenza tra schermate
        _errorMessage.value = null
        _authResult.value = null
    }
}