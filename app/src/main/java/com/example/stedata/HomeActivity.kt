package com.example.stedata

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.stedata.databinding.ActivityHomeBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init Firebase
        auth = FirebaseAuth.getInstance()

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)

        // 1. Setup Navigation Component
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navigationView

        // Configura la AppBar per lavorare col Drawer
        // Definisci qui gli ID dei fragment "top-level" (dove mostrare l'hamburger invece della freccia indietro)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_settings),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // 2. RECUPERO FUNZIONALITÀ MANCANTE: Mostra email utente nell'header
        val headerView = navView.getHeaderView(0)
        val navUserEmail = headerView.findViewById<TextView>(R.id.headerEmail)
        val user = auth.currentUser
        navUserEmail.text = user?.email ?: "Utente anonimo"

        // 3. Gestione personalizzata del menu (per il Logout)
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    // Logica di logout originale
                    auth.signOut()
                    Toast.makeText(this, "Logout effettuato", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> {
                    // Per le altre voci (Home, Settings), lascia fare al Navigation Component
                    // Chiude il drawer e naviga
                    val handled = NavigationUI.onNavDestinationSelected(item, navController)
                    if (handled) {
                        drawerLayout.closeDrawers()
                    }
                    handled
                }
            }
        }
    }

    // Gestisce il click sul tasto "hamburger" o "indietro" nella toolbar
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}

