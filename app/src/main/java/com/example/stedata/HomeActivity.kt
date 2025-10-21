package com.example.stedata

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.stedata.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Imposta la toolbar come ActionBar
        setSupportActionBar(binding.toolbar)

        val user = auth.currentUser
        if (user == null) {
            navigateToLogin()
            return
        }

        loadUserInfo()
    }

    private fun loadUserInfo() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: "Utente"
                val email = document.getString("email") ?: ""
                binding.welcomeText.text = "Ciao, $name 👋"
                binding.emailText.text = email
            }
            .addOnFailureListener {
                binding.welcomeText.text = "Benvenuto!"
            }
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "Logout effettuato", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    // 🔽 MENU TOOLBAR
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                Toast.makeText(this, "Aggiornamento dati...", Toast.LENGTH_SHORT).show()
                // TODO: qui puoi richiamare una funzione loadMachinesFromFirebase()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
