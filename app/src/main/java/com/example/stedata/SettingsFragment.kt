package com.example.stedata

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.stedata.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser
        binding.userEmail.text = user?.email ?: "Utente anonimo"

        binding.btnLogout.setOnClickListener {
            // 1. Disconnessione da Firebase
            auth.signOut()

            // 2. Feedback all'utente
            Toast.makeText(requireContext(), "Logout effettuato ✅", Toast.LENGTH_SHORT).show()

            // 3. Reindirizzamento al Login (Copia esatta della logica HomeActivity)
            val intent = Intent(requireContext(), LoginActivity::class.java)

            // Opzionale ma consigliato: Pulisce lo stack delle activity per evitare che
            // premendo "Indietro" dal login si torni qui.
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            // 4. Chiude l'activity corrente
            requireActivity().finish()
        }

        binding.btnTheme.setOnClickListener {
            Toast.makeText(requireContext(), "Cambio tema in sviluppo 🎨", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
