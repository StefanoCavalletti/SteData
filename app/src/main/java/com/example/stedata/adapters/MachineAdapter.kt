package com.example.stedata.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stedata.databinding.ItemMachineBinding
import com.example.stedata.models.Machine

class MachineAdapter(
    private val machines: List<Machine>,
    private val onClick: (Machine) -> Unit
) : RecyclerView.Adapter<MachineAdapter.MachineViewHolder>() {

    inner class MachineViewHolder(val binding: ItemMachineBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MachineViewHolder {
        val binding = ItemMachineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MachineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MachineViewHolder, position: Int) {
        val machine = machines[position]
        holder.binding.machineIdText.text = "ID: ${machine.machineId}"
        holder.binding.machineLastUpdate.text = "Ultimo aggiornamento: ${machine.lastUpdate}"
        holder.binding.root.setOnClickListener { onClick(machine) }
    }

    override fun getItemCount() = machines.size
}
