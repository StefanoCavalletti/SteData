package com.example.stedata

import android.graphics.*
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stedata.adapters.RilevazioneAdapter
import com.example.stedata.databinding.ActivityMachineDetailBinding
import com.example.stedata.models.Rilevazione

class MachineDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMachineDetailBinding
    private val viewModel: MachineDetailViewModel by viewModels() // Usa il ViewModel

    // Lista locale per l'adapter (aggiornata dal LiveData)
    private val rilevazioniList = mutableListOf<Rilevazione>()
    private lateinit var adapter: RilevazioneAdapter
    private lateinit var machineId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMachineDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        machineId = intent.getStringExtra("MACHINE_ID") ?: return
        binding.toolbar.title = "Gettoniera $machineId"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Freccia indietro

        setupRecyclerView()
        setupObservers()

        // Carica i dati
        viewModel.loadRilevazioni(machineId)
    }

    private fun setupRecyclerView() {
        adapter = RilevazioneAdapter(rilevazioniList) { rilevazione ->
            showRilevazioneDialog(rilevazione)
        }
        binding.rilevazioniRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.rilevazioniRecyclerView.adapter = adapter

        // Collega lo swipe
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rilevazioniRecyclerView)
    }

    private fun setupObservers() {
        viewModel.rilevazioni.observe(this) { list ->
            rilevazioniList.clear()
            rilevazioniList.addAll(list)
            adapter.notifyDataSetChanged()
        }

        viewModel.statusMessage.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRilevazioneDialog(r: Rilevazione) {
        // ... (Codice identico al tuo originale per mostrare il dialog) ...
        val message = """
             Data: ${r.timestamp}
             Incasso: €${r.incasso}
             Resti: €${r.resti}
             File: ${if(r.file.isNotEmpty()) r.file else "Nessun file"}
         """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Dettagli")
            .setMessage(message)
            .setPositiveButton("Chiudi", null)
            .show()
    }

    // Gestione Swipe (UI)
    private val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.absoluteAdapterPosition
            val rilevazione = rilevazioniList[position]

            // Chiamata al ViewModel per eliminare
            viewModel.deleteRilevazione(machineId, rilevazione)

            // Nota: L'adapter si aggiornerà quando il ViewModel ricarica i dati,
            // ma per evitare "buchi" visivi immediati potresti chiamare notifyItemChanged(position)
            // nel caso l'eliminazione fallisca. Qui ci fidiamo del reload.
        }

        override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isActive: Boolean) {
            // ... (Copia il tuo codice di disegno sfondo rosso e icona cestino qui) ...
            val itemView = vh.itemView
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
            super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}