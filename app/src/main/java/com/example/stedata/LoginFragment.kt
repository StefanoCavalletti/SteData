package com.example.stedata

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // Importante
import androidx.navigation.fragment.findNavController
import com.example.stedata.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Istanziamo il ViewModel
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.sharedEmail.isNotEmpty()) {
            binding.emailInput.setText(viewModel.sharedEmail)
        }
        setupListeners()
        setupObservers() // Osserviamo il ViewModel
    }

    private fun setupListeners() {
        binding.loginBtn.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password) // Delega al ViewModel
            } else {
                Toast.makeText(requireContext(), "Compila i campi", Toast.LENGTH_SHORT).show()
            }
        }

        binding.toRegisterText.setOnClickListener {
            // 2. Prima di andare via, salviamo quello che l'utente ha scritto
            viewModel.sharedEmail = binding.emailInput.text.toString().trim()
            viewModel.resetState() // Pulisci eventuali errori vecchi
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun setupObservers() {
        // 1. Osserva il risultato del login
        viewModel.authResult.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                navigateToHome()
                viewModel.resetState() // Evita loop se torni indietro
            }
        }

        // 2. Osserva gli errori
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        // 3. Osserva il caricamento (Opzionale: disabilita il bottone)
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loginBtn.isEnabled = !isLoading
            binding.loginBtn.text = if (isLoading) "Attendi..." else "Accedi"
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(requireContext(), HomeActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}