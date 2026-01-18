package com.example.stedata

import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stedata.adapters.MachineAdapter
import com.example.stedata.databinding.FragmentHomeBinding
import com.example.stedata.models.Machine

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // inizializzo viewmodel
    private val viewModel: HomeViewModel by viewModels()

    private val machines = mutableListOf<Machine>()
    private lateinit var adapter: MachineAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
        setupSwipeToDelete()

        //carico i dati all'avvio
        viewModel.loadMachines()
    }

    private fun setupRecyclerView() {
        adapter = MachineAdapter(machines) { machine ->
            val action = HomeFragmentDirections.actionNavHomeToNavMachineDetail(machine.machineId)

            findNavController().navigate(action)
        }
        binding.machinesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.machinesRecyclerView.adapter = adapter
        setupSwipeToDelete()
    }

    private fun setupObservers() {
        viewModel.machines.observe(viewLifecycleOwner) { newMachines ->
            // aggiorno la lista locale
            machines.clear()
            machines.addAll(newMachines)

            // aggiorna l'adapter per la visualizzazione
            adapter.updateList(newMachines)
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    // setup dello swipe per cancellare le gettoniere
    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val machineToDelete = machines[position]
                confirmDeleteMachine(machineToDelete, position)
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

                //Calcolo del rettangolo rosso per
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

                    //Posizione icona
                    val left = itemView.left + margin
                    val right = itemView.left + margin + it.intrinsicWidth

                    it.setBounds(left, top, right, bottom)
                    it.draw(c)
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.machinesRecyclerView)
    }

    //dialog di conferma
    private fun confirmDeleteMachine(machine: Machine, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminare ${machine.machineId}?")
            .setMessage("⚠️ Verranno eliminati permanentemente anche tutti i dati e le rilevazioni di questa gettoniera.")
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                // Chiama il ViewModel
                viewModel.deleteMachine(machine.machineId)
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { _, _ ->
                // Se annulla, ripristina visivamente l'elemento swipato
                adapter.notifyItemChanged(position)
            }
            .show()
    }

    private fun setupListeners() {
        binding.addMachineFab.setOnClickListener { showAddRilevazioneDialog() }
        binding.importFileFab.setOnClickListener {
            //startActivity(Intent(requireContext(), FilePickerActivity::class.java))
            findNavController().navigate(R.id.nav_file_picker)
        }
    }

    private fun showAddRilevazioneDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val idInput = EditText(requireContext()).apply {
            hint = getString(R.string.dialog_machine_id)
        }
        val incassoInput = EditText(requireContext()).apply {
            hint = getString(R.string.dialog_incasso)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val restiInput = EditText(requireContext()).apply {
            hint = getString(R.string.dialog_resti)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        layout.addView(idInput)
        layout.addView(incassoInput)
        layout.addView(restiInput)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_add_title))
            .setView(layout)
            .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
                val machineId = idInput.text.toString().trim()
                val incasso = incassoInput.text.toString().toDoubleOrNull() ?: 0.0
                val resti = restiInput.text.toString().toDoubleOrNull() ?: 0.0

                if (machineId.isNotEmpty()) {
                    viewModel.addRilevazione(machineId, incasso, resti)
                } else {
                    Toast.makeText(requireContext(), "ID obbligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
