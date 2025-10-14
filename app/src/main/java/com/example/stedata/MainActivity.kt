package com.example.stedata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.stedata.ui.theme.SteDataTheme
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        auth.addAuthStateListener {
            val user = it.currentUser
            if (user == null) {
                // se la sessione è invalidata o l'account cancellato
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        // se vuoi, puoi anche controllare all'avvio se l'utente esiste ancora
        val user = auth.currentUser
//        if (user == null) {
//            startActivity(Intent(this, LoginActivity::class.java))
//            finish()
//            return
//        }
        if (user != null) {
            user.reload().addOnCompleteListener { task ->
                if (!task.isSuccessful || auth.currentUser == null) {
                    // L'utente non esiste più su Firebase
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut() // 👈 disconnette completamente l’utente
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}