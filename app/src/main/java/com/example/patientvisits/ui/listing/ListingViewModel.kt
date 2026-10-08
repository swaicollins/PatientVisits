package com.example.patientvisits.ui.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.patientvisits.domain.model.BmiStatus
import com.example.patientvisits.domain.repository.PatientRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class ListingViewModel(
    repository: PatientRepository,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    data class Row(
        val patientId: String,
        val name: String,
        val age: Int,
        val bmiStatus: BmiStatus?
    )

    data class UiState(
        val loading: Boolean = true,
        val filterDate: LocalDate? = null,
        val rows: List<Row> = emptyList()
    )

    private val filterDate = MutableStateFlow<LocalDate?>(null)


    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState> = filterDate
        .flatMapLatest { date ->
            repository.observePatientList(date).map { items ->
                val now = today()
                UiState(
                    loading = false,
                    filterDate = date,
                    rows = items.map { item ->
                        Row(
                            patientId = item.patient.patientId,
                            name = item.patient.fullName,
                            age = item.patient.ageOn(now),
                            bmiStatus = item.bmiStatus
                        )
                    }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun onFilterDateChange(date: LocalDate?) {
        filterDate.value = date
    }

    companion object {
        fun factory(repository: PatientRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ListingViewModel(repository) }
        }
    }
}
