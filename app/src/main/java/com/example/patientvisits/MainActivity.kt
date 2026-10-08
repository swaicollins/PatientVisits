package com.example.patientvisits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.patientvisits.ui.navigation.AppNavGraph
import com.example.patientvisits.ui.theme.PatientVisitsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PatientVisitsApp).container
        setContent {
            PatientVisitsTheme {
                AppNavGraph(container)
            }
        }
    }
}
