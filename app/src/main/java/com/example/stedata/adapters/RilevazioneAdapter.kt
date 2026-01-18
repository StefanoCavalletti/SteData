package com.example.stedata.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stedata.databinding.ItemRilevazioneBinding
import com.example.stedata.models.Rilevazione

class RilevazioneAdapter(
    private val rilevazioni: List<Rilevazione>,
    private val onClick: (Rilevazione) -> Unit
) : RecyclerView.Adapter<RilevazioneAdapter.RilevazioneViewHolder>() {

    inner class RilevazioneViewHolder(val binding: ItemRilevazioneBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RilevazioneViewHolder {
        val binding = ItemRilevazioneBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RilevazioneViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RilevazioneViewHolder, position: Int) {
        val r = rilevazioni[position]

        // PASSA L'OGGETTO ALLA VARIABILE XML
        holder.binding.rilevazione = r

        // FORZA L'AGGIORNAMENTO IMMEDIATO
        holder.binding.executePendingBindings()

        // GESTIONE CLICK
        holder.binding.root.setOnClickListener {
            onClick(r)
        }
    }

    override fun getItemCount() = rilevazioni.size
}