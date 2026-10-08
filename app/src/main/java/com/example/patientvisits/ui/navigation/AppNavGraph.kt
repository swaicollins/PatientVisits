package com.example.patientvisits.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.patientvisits.di.AppContainer
import com.example.patientvisits.ui.assessment.AssessmentScreen
import com.example.patientvisits.ui.assessment.AssessmentViewModel
import com.example.patientvisits.ui.auth.LoginScreen
import com.example.patientvisits.ui.auth.LoginViewModel
import com.example.patientvisits.ui.auth.SignupScreen
import com.example.patientvisits.ui.auth.SignupViewModel
import com.example.patientvisits.ui.listing.ListingScreen
import com.example.patientvisits.ui.listing.ListingViewModel
import com.example.patientvisits.ui.registration.RegistrationScreen
import com.example.patientvisits.ui.registration.RegistrationViewModel
import com.example.patientvisits.ui.vitals.VitalsScreen
import com.example.patientvisits.ui.vitals.VitalsViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope


@Composable
fun AppNavGraph(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val repository = container.repository
    val auth = container.authRepository
    val scope = rememberCoroutineScope()

    val loggedIn by auth.isLoggedIn.collectAsStateWithLifecycle<Boolean?>(initialValue = null)
    var startLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(loggedIn) {
        if (startLoggedIn == null && loggedIn != null) startLoggedIn = loggedIn
    }
    val initialLoggedIn = startLoggedIn ?: return

    LaunchedEffect(loggedIn) {
        if (loggedIn == false) {
            val current = navController.currentDestination
            val onAuthScreen = current?.hasRoute<LoginDestination>() == true ||
                current?.hasRoute<SignupDestination>() == true
            if (current != null && !onAuthScreen) {
                navController.navigate(LoginDestination) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }

    val enterApp: () -> Unit = {
        container.syncScheduler.schedule()
        navController.navigate(ListingDestination) {
            popUpTo<LoginDestination> { inclusive = true }
        }
    }

    val backToListing: () -> Unit = {
        navController.popBackStack<ListingDestination>(inclusive = false)
    }

    NavHost(
        navController = navController,
        startDestination = if (initialLoggedIn) ListingDestination else LoginDestination
    ) {

        composable<LoginDestination> {
            val viewModel: LoginViewModel = viewModel(factory = LoginViewModel.factory(auth))
            LoginScreen(
                viewModel = viewModel,
                onLoggedIn = enterApp,
                onCreateAccount = { navController.navigate(SignupDestination) }
            )
        }

        composable<SignupDestination> {
            val viewModel: SignupViewModel = viewModel(factory = SignupViewModel.factory(auth))
            SignupScreen(
                viewModel = viewModel,
                onSignedUp = enterApp,
                onHaveAccount = { navController.popBackStack() }
            )
        }

        composable<ListingDestination> {
            val viewModel: ListingViewModel = viewModel(factory = ListingViewModel.factory(repository))
            ListingScreen(
                viewModel = viewModel,
                onRegisterPatient = { navController.navigate(RegistrationDestination) },
                onPatientClick = { patientId -> navController.navigate(VitalsDestination(patientId)) },
                onLogout = { scope.launch { auth.logout() } }
            )
        }

        composable<RegistrationDestination> {
            val viewModel: RegistrationViewModel = viewModel(factory = RegistrationViewModel.factory(repository))
            RegistrationScreen(
                viewModel = viewModel,
                onClose = backToListing,
                onRegistered = { patientId ->
                    navController.navigate(VitalsDestination(patientId)) {
                        popUpTo<RegistrationDestination> { inclusive = true }
                    }
                }
            )
        }

        composable<VitalsDestination> { entry ->
            val args = entry.toRoute<VitalsDestination>()
            val viewModel: VitalsViewModel =
                viewModel(factory = VitalsViewModel.factory(repository, args.patientId))
            VitalsScreen(
                viewModel = viewModel,
                onClose = backToListing,
                onSaved = { assessmentType, visitDate ->
                    navController.navigate(
                        AssessmentDestination(args.patientId, assessmentType, visitDate.toString())
                    ) {
                        popUpTo<VitalsDestination> { inclusive = true }
                    }
                }
            )
        }

        composable<AssessmentDestination> { entry ->
            val args = entry.toRoute<AssessmentDestination>()
            val viewModel: AssessmentViewModel = viewModel(
                factory = AssessmentViewModel.factory(
                    repository = repository,
                    patientId = args.patientId,
                    type = args.type,
                    initialVisitDate = LocalDate.parse(args.visitDate)
                )
            )
            AssessmentScreen(
                viewModel = viewModel,
                type = args.type,
                onClose = backToListing,
                onSaved = backToListing
            )
        }
    }
}
