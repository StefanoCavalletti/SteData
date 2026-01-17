package com.example.stedata

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Imposta il tema chiaro (codice preso dalla tua vecchia MainActivity)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // 2. CONTROLLO LOGIN IMMEDIATO
        // Se l'utente è già loggato, vai alla Home e chiudi questa activity.
        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return // IMPORTANTE: Interrompe l'esecuzione, così non carica il layout sotto
        }

        // 3. Se non è loggato, carica il contenitore dei Fragment (Login/Register)
        setContentView(R.layout.activity_login)
    }
}
