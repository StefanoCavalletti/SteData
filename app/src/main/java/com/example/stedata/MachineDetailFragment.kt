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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stedata.adapters.RilevazioneAdapter
import com.example.stedata.databinding.FragmentMachineDetailBinding
import com.example.stedata.models.Rilevazione

class MachineDetailFragment : Fragment() {

    private var _binding: FragmentMachineDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MachineDetailViewModel by viewModels()
    private val args: MachineDetailFragmentArgs by navArgs()

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

    private fun showRilevazioneDialog(r: Rilevazione) {
        val sb = StringBuilder()

        sb.appendLine("📅 Data: ${r.timestamp}")
        sb.appendLine("💰 Incasso Totale: €${String.format("%.2f", r.getImporto())}")

        if (r.isEvaDts()) {
            // DATI COMPLETI (EVA-DTS)
            sb.appendLine("\n📊 STATISTICHE VENDITA")
            sb.appendLine("Numero Vendite: ${r.numeroVendite}")

            r.contabilitaCash?.let { cash ->
                sb.appendLine("\n💵 CONTANTI")
                sb.appendLine("Vendite: €${String.format("%.2f", cash["venditeContanti"] ?: 0.0)}")
                sb.appendLine("Nel Box: €${String.format("%.2f", cash["incassoBox"] ?: 0.0)}")
                sb.appendLine("Nei Tubi: €${String.format("%.2f", cash["incassoTubi"] ?: 0.0)}")
            }

            r.contabilitaCashless?.let { cashless ->
                sb.appendLine("\n💳 CASHLESS")
                val totCashless = (cashless["sistema1_vendite"] ?: 0.0) + (cashless["sistema2_vendite"] ?: 0.0)
                sb.appendLine("Totale Elettronico: €${String.format("%.2f", totCashless)}")
            }

            if (!r.prodotti.isNullOrEmpty()) {
                sb.appendLine("\n🍫 TOP 5 PRODOTTI")
                // Ordina per quantità venduta (assume che la mappa abbia "venditeQta" come Number)
                val topProducts = r.prodotti.sortedByDescending {
                    (it["venditeQta"] as? Number)?.toInt() ?: 0
                }.take(5)

                topProducts.forEach { p ->
                    val nome = p["nome"] as? String ?: "Prodotto ${p["id"]}"
                    val qta = (p["venditeQta"] as? Number)?.toInt() ?: 0
                    if (qta > 0) {
                        sb.appendLine("- $nome: $qta pz")
                    }
                }
            }

            if (!r.fileId.isNullOrEmpty()) {
                sb.appendLine("\n📄 File: ${r.fileId}")
            }

        } else {
            // DATI MANUALI
            sb.appendLine("\n📝 INSERIMENTO MANUALE")
            if (r.resti != null) {
                sb.appendLine("Resti dichiarati: €${String.format("%.2f", r.resti)}")
            }
            if (!r.file.isNullOrEmpty()) {
                sb.appendLine("Note: ${r.file}")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(if (r.isEvaDts()) "Dettaglio EVA-DTS" else "Dettaglio Manuale")
            .setMessage(sb.toString())
            .setPositiveButton("Chiudi", null)
            .show()
    }

    private val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

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