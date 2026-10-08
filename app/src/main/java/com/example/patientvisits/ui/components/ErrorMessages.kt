package com.example.patientvisits.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.patientvisits.R
import com.example.patientvisits.domain.validation.ValidationError


@Composable
fun ValidationError?.asMessage(range: ClosedFloatingPointRange<Double>? = null): String? =
    when (this) {
        null -> null
        ValidationError.REQUIRED -> stringResource(R.string.error_required)
        ValidationError.DUPLICATE_PATIENT_ID -> stringResource(R.string.error_duplicate_patient_id)
        ValidationError.DATE_IN_FUTURE -> stringResource(R.string.error_date_future)
        ValidationError.DOB_AFTER_REGISTRATION -> stringResource(R.string.error_dob_after_registration)
        ValidationError.VISIT_BEFORE_REGISTRATION -> stringResource(R.string.error_visit_before_registration)
        ValidationError.DUPLICATE_VISIT_DATE -> stringResource(R.string.error_duplicate_visit)
        ValidationError.INVALID_EMAIL -> stringResource(R.string.error_invalid_email)
        ValidationError.PASSWORD_TOO_SHORT -> stringResource(R.string.error_password_short)
        ValidationError.PASSWORD_MISMATCH -> stringResource(R.string.error_password_mismatch)
        ValidationError.OUT_OF_RANGE ->
            if (range != null) {
                stringResource(
                    R.string.error_out_of_range,
                    "%.0f".format(range.start),
                    "%.0f".format(range.endInclusive)
                )
            } else {
                stringResource(R.string.error_invalid_value)
            }
    }
