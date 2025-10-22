package com.example.stedata

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stedata.adapters.RilevazioneAdapter
import com.example.stedata.databinding.ActivityMachineDetailBinding
import com.example.stedata.models.Rilevazione
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.graphics.*
import android.widget.Toast
import androidx.core.content.ContextCompat



class MachineDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMachineDetailBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val rilevazioni = mutableListOf<Rilevazione>()
    private lateinit var adapter: RilevazioneAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMachineDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val machineId = intent.getStringExtra("MACHINE_ID") ?: return
        binding.toolbar.title = "Gettoniera $machineId"

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        adapter = RilevazioneAdapter(rilevazioni) { rilevazione ->
            showRilevazioneDialog(rilevazione)
        }
        binding.rilevazioniRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.rilevazioniRecyclerView.adapter = adapter


        loadRilevazioni(machineId)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rilevazioniRecyclerView)
    }

    private fun loadRilevazioni(machineId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)
            .collection("rilevazioni")
            .get()
            .addOnSuccessListener { snapshot ->
                rilevazioni.clear()
                for (doc in snapshot.documents) {
                    val r = doc.toObject(Rilevazione::class.java)
                    if (r != null) rilevazioni.add(r)
                }
                adapter.notifyDataSetChanged()
            }
    }
    private fun showRilevazioneDialog(r: Rilevazione) {
        val message = """
         ${r.timestamp}
        
        Incasso: €${r.incasso}
        Resti: €${r.resti}
        
        File EVA DTS:
        ${r.file}
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Dettagli rilevazione")
            .setMessage(message)
            .setPositiveButton("Chiudi", null)
            .show()
    }
    // 🔹 Swipe sinistro per eliminare
    val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val rilevazione = rilevazioni[position]
            deleteRilevazione(rilevazione, position)
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
            // Sfondo rosso + icona cestino
            val itemView = viewHolder.itemView
            val paint = Paint()
            paint.color = Color.RED
            val background = RectF(
                itemView.right.toFloat() + dX, itemView.top.toFloat(),
                itemView.right.toFloat(), itemView.bottom.toFloat()
            )
            c.drawRect(background, paint)

            val icon = ContextCompat.getDrawable(this@MachineDetailActivity, android.R.drawable.ic_menu_delete)
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



    private fun deleteRilevazione(r: Rilevazione, position: Int) {
        val uid = auth.currentUser?.uid ?: return
        val machineId = intent.getStringExtra("MACHINE_ID") ?: return

        val db = FirebaseFirestore.getInstance()
        val rilevazioniRef = db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)
            .collection("rilevazioni")

        // Cerca la rilevazione con timestamp e importi uguali (modo semplice per identificare)
        rilevazioniRef
            .whereEqualTo("timestamp", r.timestamp)
            .whereEqualTo("incasso", r.incasso)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val docId = snapshot.documents.first().id
                    rilevazioniRef.document(docId).delete()
                        .addOnSuccessListener {
                            rilevazioni.removeAt(position)
                            adapter.notifyItemRemoved(position)
                            Toast.makeText(this, "Rilevazione eliminata", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    adapter.notifyItemChanged(position)
                    Toast.makeText(this, "Errore: rilevazione non trovata", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                adapter.notifyItemChanged(position)
                Toast.makeText(this, "Errore: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


}
