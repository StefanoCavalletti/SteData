package com.example.stedata

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stedata.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.stedata.models.Machine
import com.example.stedata.models.Rilevazione
import com.example.stedata.adapters.MachineAdapter
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.graphics.*
import androidx.core.content.ContextCompat



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
        adapter = MachineAdapter(machines) { machine ->
            val intent = Intent(this, MachineDetailActivity::class.java)
            intent.putExtra("MACHINE_ID", machine.machineId)
            startActivity(intent)
        }
        binding.machinesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.machinesRecyclerView.adapter = adapter
        binding.addMachineFab.setOnClickListener {
            showAddRilevazioneDialog()
        }
        //Swipe sinistro per eliminare gettoniera
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.getAbsoluteAdapterPosition()
                val machine = machines[position]
                confirmDeleteMachine(machine.machineId, position)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()
                paint.color = Color.RED
                val background = RectF(
                    itemView.right.toFloat() + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat()
                )
                c.drawRect(background, paint)

                val icon = ContextCompat.getDrawable(this@HomeActivity, android.R.drawable.ic_menu_delete)
                icon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + it.intrinsicHeight
                    val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.machinesRecyclerView)

        loadMachines()
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
    private val machines = mutableListOf<Machine>()
    private lateinit var adapter: MachineAdapter

    private fun loadMachines() {
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid)
            .collection("vending_machines")
            .get()
            .addOnSuccessListener { snapshot ->
                machines.clear()
                for (doc in snapshot.documents) {
                    val machine = doc.toObject(Machine::class.java)
                    if (machine != null) machines.add(machine)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Errore nel caricamento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddRilevazioneDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val idInput = EditText(this)
        idInput.hint = "ID Gettoniera (es. VM1234)"
        layout.addView(idInput)

        val incassoInput = EditText(this)
        incassoInput.hint = "Incasso (€)"
        incassoInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        layout.addView(incassoInput)

        val restiInput = EditText(this)
        restiInput.hint = "Resti (€)"
        restiInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        layout.addView(restiInput)

        AlertDialog.Builder(this)
            .setTitle("Nuova Rilevazione")
            .setMessage("Inserisci i dati della lettura:")
            .setView(layout)
            .setPositiveButton("Salva") { _, _ ->
                val machineId = idInput.text.toString().trim()
                val incasso = incassoInput.text.toString().toDoubleOrNull() ?: 0.0
                val resti = restiInput.text.toString().toDoubleOrNull() ?: 0.0

                if (machineId.isNotEmpty()) {
                    addRilevazione(machineId, incasso, resti)
                } else {
                    Toast.makeText(this, "ID gettoniera obbligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
    private fun addRilevazione(machineId: String, incasso: Double, resti: Double) {
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val rilevazione = mapOf(
            "timestamp" to timestamp,
            "incasso" to incasso,
            "resti" to resti,
            "file" to "DXS*EXAMPLE*VA*V1/6*$machineId"
        )

        val machineRef = db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)

        // 🔹 Verifica se la macchinetta esiste
        machineRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                // Se non esiste, crea la macchinetta
                val newMachine = mapOf(
                    "machineId" to machineId,
                    "lastUpdate" to timestamp,
                    "totalRilevazioni" to 1
                )
                machineRef.set(newMachine)
            } else {
                // Se esiste, aggiorna il contatore e la data
                val currentTotal = doc.getLong("totalRilevazioni") ?: 0
                machineRef.update(
                    "lastUpdate", timestamp,
                    "totalRilevazioni", currentTotal + 1
                )
            }

            // 🔹 In ogni caso, aggiungi la rilevazione nella sottocollezione
            machineRef.collection("rilevazioni").add(rilevazione)
                .addOnSuccessListener {
                    Toast.makeText(this, "Rilevazione aggiunta ✅", Toast.LENGTH_SHORT).show()
                    loadMachines() // aggiorna la lista
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun confirmDeleteMachine(machineId: String, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminare la gettoniera $machineId?")
            .setMessage("⚠️ Verranno eliminati anche tutti i dati e le rilevazioni associate a questa gettoniera. L'operazione non può essere annullata.")
            .setPositiveButton("Elimina") { _, _ ->
                deleteMachineAndRilevazioni(machineId, position)
            }
            .setNegativeButton("Annulla") { _, _ ->
                adapter.notifyItemChanged(position)
            }
            .show()
    }
    private fun deleteMachineAndRilevazioni(machineId: String, position: Int) {
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val machineRef = db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)
        val rilevazioniRef = machineRef.collection("rilevazioni")

        // 🔹 1. Elimina tutte le rilevazioni sotto la macchina
        rilevazioniRef.get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }

            // 🔹 2. Dopo aver eliminato le rilevazioni, elimina la macchina stessa
            batch.commit().addOnSuccessListener {
                machineRef.delete()
                    .addOnSuccessListener {
                        machines.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        Toast.makeText(this, "Gettoniera $machineId e relative rilevazioni eliminate ✅", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Errore eliminando la gettoniera: ${e.message}", Toast.LENGTH_SHORT).show()
                        adapter.notifyItemChanged(position)
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Errore eliminando rilevazioni: ${e.message}", Toast.LENGTH_SHORT).show()
                adapter.notifyItemChanged(position)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Errore caricando le rilevazioni: ${e.message}", Toast.LENGTH_SHORT).show()
            adapter.notifyItemChanged(position)
        }
    }

}
