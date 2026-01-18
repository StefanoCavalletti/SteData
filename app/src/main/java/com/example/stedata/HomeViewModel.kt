package com.example.stedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stedata.models.Machine
import com.example.stedata.repository.MachineRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel : ViewModel() {

    private val repository = MachineRepository()

    // LiveData per la lista delle gettoniere
    private val _machines = MutableLiveData<List<Machine>>()
    val machines: LiveData<List<Machine>> get() = _machines

    // LiveData per messaggi di stato
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> get() = _statusMessage

    fun loadMachines() {
        viewModelScope.launch {
            try {
                val list = repository.getMachines()
                _machines.value = list
            } catch (e: Exception) {
                _statusMessage.value = "Errore: ${e.message}"
            }
        }
    }

    fun addRilevazione(machineId: String, incasso: Double, resti: Double) {
        viewModelScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                val dati = mapOf(
                    "timestamp" to timestamp,
                    "incasso" to incasso,
                    "resti" to resti,
                    "file" to ""
                )
                repository.saveRilevazione(machineId, dati)
                _statusMessage.value = "Rilevazione salvata"
                loadMachines()
            } catch (e: Exception) {
                _statusMessage.value = "Errore salvataggio: ${e.message}"
            }
        }
    }

    fun deleteMachine(machineId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMachine(machineId)
                _statusMessage.value = "Gettoniera eliminata"
                loadMachines()
            } catch (e: Exception) {
                _statusMessage.value = "Errore eliminazione: ${e.message}"
            }
        }
    }
}