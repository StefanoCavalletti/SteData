package com.example.stedata

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Non serve setContentView(R.layout.activity_main) perché reindirizziamo subito

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        if (user != null) {
            // Controllo opzionale: ricarica l'utente per vedere se è stato disabilitato
            user.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, HomeActivity::class.java))
                } else {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                finish()
            }
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}