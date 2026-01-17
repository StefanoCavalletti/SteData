package com.example.stedata

import android.app.AlertDialog
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs // Safe Args
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stedata.adapters.RilevazioneAdapter
import com.example.stedata.databinding.FragmentMachineDetailBinding // O il nuovo nome del layout
import com.example.stedata.models.Rilevazione

class MachineDetailFragment : Fragment() {

    private var _binding: FragmentMachineDetailBinding? = null
    private val binding get() = _binding!!

    // ViewModel e Args funzionano UGUALE
    private val viewModel: MachineDetailViewModel by viewModels()
    private val args: MachineDetailFragmentArgs by navArgs() // Nota: il nome della classe Args cambierà dopo il rebuild

    private val rilevazioniList = mutableListOf<Rilevazione>()
    private lateinit var adapter: RilevazioneAdapter
    private lateinit var machineId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMachineDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        machineId = args.machineId

        // Imposta il titolo nella Toolbar della HomeActivity
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Gettoniera ${args.machineId}"

        setupRecyclerView()
        setupObservers()

        viewModel.loadRilevazioni(machineId)
    }

    private fun setupRecyclerView() {
        adapter = RilevazioneAdapter(rilevazioniList) { rilevazione ->
            showRilevazioneDialog(rilevazione)
        }
        binding.rilevazioniRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.rilevazioniRecyclerView.adapter = adapter

        // Collega lo swipe
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rilevazioniRecyclerView)
    }

    private fun setupObservers() {
        viewModel.rilevazioni.observe(viewLifecycleOwner) { list ->
            rilevazioniList.clear()
            rilevazioniList.addAll(list)
            adapter.notifyDataSetChanged()
        }
        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    // ... [Inserisci qui showRilevazioneDialog e swipeHandler uguali a prima] ...
    // Nota: dentro swipeHandler usa 'requireContext()' invece di 'this' o 'this@MachineDetailActivity'
    private fun showRilevazioneDialog(r: Rilevazione) {
        val message = """
             Data: ${r.timestamp}
             Incasso: €${String.format("%.2f", r.incasso)}
             Resti: €${String.format("%.2f", r.resti)}
             File: ${if(r.file.isNotEmpty()) r.file else "Nessun file"}
         """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Dettagli Rilevazione")
            .setMessage(message)
            .setPositiveButton("Chiudi", null)
            .show()
    }

    // 3. Gestione Swipe per eliminare (Versione LEFT - Da destra a sinistra)
    // Se vuoi coerenza con la Home (che abbiamo messo RIGHT), cambia qui sotto "ItemTouchHelper.LEFT" in "RIGHT"
    // e adatta il rettangolo nel metodo onChildDraw come abbiamo fatto in HomeFragment.
    private val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) { // 1. CAMBIATO: RIGHT

        override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.absoluteAdapterPosition
            val rilevazione = rilevazioniList[position]

            // Conferma eliminazione
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminare rilevazione?")
                .setMessage("Questa operazione non può essere annullata.")
                .setPositiveButton("Elimina") { _, _ ->
                    viewModel.deleteRilevazione(machineId, rilevazione)
                }
                .setNegativeButton("Annulla") { _, _ ->
                    // Ripristina la riga se l'utente annulla
                    adapter.notifyItemChanged(position)
                }
                .show()
        }

        override fun onChildDraw(
            c: Canvas,
            rv: RecyclerView,
            vh: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isActive: Boolean
        ) {
            val itemView = vh.itemView
            val paint = Paint().apply { color = Color.RED }

            // 2. CAMBIATO: Calcolo rettangolo per swipe verso DESTRA (dX > 0)
            // Parte dal bordo sinistro (itemView.left) e si estende per dX
            val background = RectF(
                itemView.left.toFloat(),
                itemView.top.toFloat(),
                itemView.left.toFloat() + dX,
                itemView.bottom.toFloat()
            )
            c.drawRect(background, paint)

            val icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
            icon?.let {
                val margin = (itemView.height - it.intrinsicHeight) / 2
                val top = itemView.top + margin
                val bottom = top + it.intrinsicHeight

                // 3. CAMBIATO: Posizione icona (ancorata a SINISTRA)
                val left = itemView.left + margin
                val right = itemView.left + margin + it.intrinsicWidth

                it.setBounds(left, top, right, bottom)
                it.draw(c)
            }
            super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}