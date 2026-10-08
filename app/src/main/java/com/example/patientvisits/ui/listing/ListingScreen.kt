package com.example.patientvisits.ui.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.patientvisits.R
import com.example.patientvisits.domain.model.BmiStatus
import com.example.patientvisits.ui.components.DateField
import com.example.patientvisits.ui.components.ScreenHeader
import com.example.patientvisits.ui.theme.NormalBg
import com.example.patientvisits.ui.theme.NormalFg
import com.example.patientvisits.ui.theme.OverweightBg
import com.example.patientvisits.ui.theme.OverweightFg
import com.example.patientvisits.ui.theme.UnderweightBg
import com.example.patientvisits.ui.theme.UnderweightFg
import java.time.LocalDate

private const val NAME_WEIGHT = 5f
private const val AGE_WEIGHT = 2f
private const val STATUS_WEIGHT = 3f

@Composable
fun ListingScreen(
    viewModel: ListingViewModel,
    onRegisterPatient: () -> Unit,
    onPatientClick: (patientId: String) -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ListingContent(
        state = state,
        onFilterDateChange = viewModel::onFilterDateChange,
        onRegisterPatient = onRegisterPatient,
        onPatientClick = onPatientClick,
        onLogout = onLogout
    )
}

@Composable
fun ListingContent(
    state: ListingViewModel.UiState,
    onFilterDateChange: (LocalDate?) -> Unit,
    onRegisterPatient: () -> Unit,
    onPatientClick: (patientId: String) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = { ScreenHeader(stringResource(R.string.title_listing)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onRegisterPatient) {
                Text(stringResource(R.string.action_register_patient))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateField(
                    label = stringResource(R.string.listing_filter_date),
                    date = state.filterDate,
                    onDateSelected = { onFilterDateChange(it) },
                    modifier = Modifier.weight(1f)
                )
                if (state.filterDate != null) {
                    TextButton(onClick = { onFilterDateChange(null) }) {
                        Text(stringResource(R.string.action_clear))
                    }
                }
                TextButton(onClick = onLogout) {
                    Text(stringResource(R.string.action_logout))
                }
            }

            TableHeader()

            when {
                state.loading -> Unit
                state.rows.isEmpty() -> EmptyState(filtered = state.filterDate != null)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    itemsIndexed(state.rows, key = { _, row -> row.patientId }) { index, row ->
                        PatientRow(
                            row = row,
                            striped = index % 2 == 1,
                            onClick = { onPatientClick(row.patientId) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell(stringResource(R.string.listing_col_name), NAME_WEIGHT, TextAlign.Start)
        HeaderCell(stringResource(R.string.listing_col_age), AGE_WEIGHT, TextAlign.Center)
        HeaderCell(stringResource(R.string.listing_col_bmi), STATUS_WEIGHT, TextAlign.Center)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float, align: TextAlign) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        textAlign = align,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onTertiaryContainer
    )
}

@Composable
private fun PatientRow(row: ListingViewModel.Row, striped: Boolean, onClick: () -> Unit) {
    val background = if (striped) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.name,
            modifier = Modifier.weight(NAME_WEIGHT),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = row.age.toString(),
            modifier = Modifier.weight(AGE_WEIGHT),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(modifier = Modifier.weight(STATUS_WEIGHT), contentAlignment = Alignment.Center) {
            StatusPill(row.bmiStatus)
        }
    }
}

@Composable
private fun StatusPill(status: BmiStatus?) {
    if (status == null) {
        Text(
            text = stringResource(R.string.listing_no_status),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val (background, foreground) = when (status) {
        BmiStatus.UNDERWEIGHT -> UnderweightBg to UnderweightFg
        BmiStatus.NORMAL -> NormalBg to NormalFg
        BmiStatus.OVERWEIGHT -> OverweightBg to OverweightFg
    }
    Surface(shape = RoundedCornerShape(50), color = background) {
        Text(
            text = status.label,
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyState(filtered: Boolean) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(if (filtered) R.string.listing_empty_for_date else R.string.listing_empty),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
