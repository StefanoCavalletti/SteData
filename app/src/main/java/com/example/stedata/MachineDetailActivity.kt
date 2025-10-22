package com.example.stedata

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stedata.adapters.RilevazioneAdapter
import com.example.stedata.databinding.ActivityMachineDetailBinding
import com.example.stedata.models.Rilevazione
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        adapter = RilevazioneAdapter(rilevazioni)
        binding.rilevazioniRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.rilevazioniRecyclerView.adapter = adapter

        loadRilevazioni(machineId)
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
}
