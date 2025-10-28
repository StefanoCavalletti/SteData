package com.example.stedata

import android.app.AlertDialog
import android.graphics.*
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stedata.adapters.MachineAdapter
import com.example.stedata.databinding.FragmentHomeBinding
import com.example.stedata.models.Machine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val machines = mutableListOf<Machine>()
    private lateinit var adapter: MachineAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MachineAdapter(machines) { machine ->
            val intent = android.content.Intent(requireContext(), MachineDetailActivity::class.java)
            intent.putExtra("MACHINE_ID", machine.machineId)
            startActivity(intent)
        }

        binding.machinesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.machinesRecyclerView.adapter = adapter

        binding.addMachineFab.setOnClickListener { showAddRilevazioneDialog() }

        setupSwipeToDelete()
        loadMachines()
    }

    private fun loadMachines() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("vending_machines")
            .get()
            .addOnSuccessListener { snapshot ->
                machines.clear()
                for (doc in snapshot.documents) {
                    val m = doc.toObject(Machine::class.java)
                    if (m != null) machines.add(m)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val position = vh.absoluteAdapterPosition
                val machine = machines[position]
                confirmDeleteMachine(machine.machineId, position)
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = vh.itemView
                val paint = Paint().apply { color = Color.RED }
                val background = RectF(
                    itemView.right.toFloat() + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat()
                )
                c.drawRect(background, paint)
                val icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
                icon?.let {
                    val margin = (itemView.height - it.intrinsicHeight) / 2
                    val top = itemView.top + margin
                    val bottom = top + it.intrinsicHeight
                    val left = itemView.right - margin - it.intrinsicWidth
                    val right = itemView.right - margin
                    it.setBounds(left, top, right, bottom)
                    it.draw(c)
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.machinesRecyclerView)
    }

    private fun confirmDeleteMachine(machineId: String, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminare la gettoniera $machineId?")
            .setMessage("⚠️ Verranno eliminati anche tutti i dati e le rilevazioni associate.")
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
        val machineRef = db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)

        machineRef.collection("rilevazioni").get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }

                batch.commit().addOnSuccessListener {
                    machineRef.delete().addOnSuccessListener {
                        machines.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        Toast.makeText(requireContext(), "Gettoniera $machineId eliminata ✅", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun showAddRilevazioneDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val idInput = EditText(requireContext()).apply { hint = "ID Gettoniera (es. VM1234)" }
        val incassoInput = EditText(requireContext()).apply {
            hint = "Incasso (€)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val restiInput = EditText(requireContext()).apply {
            hint = "Resti (€)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        layout.addView(idInput)
        layout.addView(incassoInput)
        layout.addView(restiInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Nuova Rilevazione")
            .setMessage("Inserisci i dati della lettura:")
            .setView(layout)
            .setPositiveButton("Salva") { _, _ ->
                val machineId = idInput.text.toString().trim()
                val incasso = incassoInput.text.toString().toDoubleOrNull() ?: 0.0
                val resti = restiInput.text.toString().toDoubleOrNull() ?: 0.0
                if (machineId.isNotEmpty()) addRilevazione(machineId, incasso, resti)
                else Toast.makeText(requireContext(), "ID obbligatorio", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun addRilevazione(machineId: String, incasso: Double, resti: Double) {
        val uid = auth.currentUser?.uid ?: return
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val rilevazione = mapOf(
            "timestamp" to timestamp,
            "incasso" to incasso,
            "resti" to resti,
            "file" to "DXS*EXAMPLE*VA*V1/6*$machineId"
        )

        val machineRef = db.collection("users").document(uid)
            .collection("vending_machines").document(machineId)

        machineRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val newMachine = mapOf(
                    "machineId" to machineId,
                    "lastUpdate" to timestamp,
                    "totalRilevazioni" to 1
                )
                machineRef.set(newMachine)
            } else {
                val currentTotal = doc.getLong("totalRilevazioni") ?: 0
                machineRef.update(
                    "lastUpdate", timestamp,
                    "totalRilevazioni", currentTotal + 1
                )
            }

            machineRef.collection("rilevazioni").add(rilevazione)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Rilevazione aggiunta ✅", Toast.LENGTH_SHORT).show()
                    loadMachines()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
