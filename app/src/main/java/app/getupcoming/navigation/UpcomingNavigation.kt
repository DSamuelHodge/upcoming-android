package app.getupcoming.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.getupcoming.core.designsystem.UpcomingTokens
import app.getupcoming.core.repository.UpcomingRepository
import app.getupcoming.feature.availability.AvailabilityScreen
import app.getupcoming.feature.availability.AvailabilityViewModel
import app.getupcoming.feature.bookingflow.InviteeBookingScreen
import app.getupcoming.feature.bookingflow.InviteeBookingViewModel
import app.getupcoming.feature.bookings.BookingDetailScreen
import app.getupcoming.feature.bookings.BookingsListScreen
import app.getupcoming.feature.bookings.BookingsViewModel
import app.getupcoming.feature.dashboard.DashboardScreen
import app.getupcoming.feature.dashboard.DashboardViewModel
import app.getupcoming.feature.eventtypes.EventTypeEditorScreen
import app.getupcoming.feature.scheduling.SchedulingScreen
import app.getupcoming.feature.scheduling.SchedulingViewModel
import app.getupcoming.feature.eventtypes.EventTypeListScreen
import app.getupcoming.feature.eventtypes.EventTypesViewModel
import app.getupcoming.feature.notifications.NotificationsScreen
import app.getupcoming.feature.notifications.NotificationsViewModel
import app.getupcoming.feature.settings.SettingsScreen
import app.getupcoming.feature.settings.SettingsViewModel
import app.getupcoming.ui.theme.*

object UpcomingDestinations {
    const val DASHBOARD = "dashboard"
    const val EVENT_TYPES = "event_types"
    const val EVENT_TYPE_EDITOR = "event_type_editor"
    const val AVAILABILITY = "availability"
    const val BOOKINGS = "bookings"
    const val BOOKING_DETAIL = "booking_detail"
    const val BOOKING_FLOW = "book"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "settings/notifications"
    const val SCHEDULING = "scheduling"
    const val AUTH = "auth"
    const val TERMS = "legal/terms"
    const val PRIVACY = "legal/privacy"
    const val PERMISSIONS = "settings/permissions"
}

data class GeometricNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun UpcomingNavHost(
    repository: UpcomingRepository,
    context: android.content.Context,
    authRepository: app.getupcoming.core.auth.AuthRepository,
    pendingBookingUid: String? = null,
    onConsumePendingBooking: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Auth gate: resolved synchronously at startup (token restore), so this
    // decides the start destination before the first frame lands.
    val authState by authRepository.authState.collectAsState()
    val authenticated = authState is app.getupcoming.core.auth.AuthState.LoggedIn ||
        authState is app.getupcoming.core.auth.AuthState.Demo

    // Widget deep link: once signed in, route the tapped booking straight to
    // Booking Detail, then consume the pending value so it doesn't re-fire.
    androidx.compose.runtime.LaunchedEffect(authenticated, pendingBookingUid) {
        if (authenticated && pendingBookingUid != null) {
            navController.navigate("${UpcomingDestinations.BOOKING_DETAIL}/$pendingBookingUid")
            onConsumePendingBooking()
        }
    }

    val bottomNavItems = listOf(
        GeometricNavItem(
            route = UpcomingDestinations.DASHBOARD,
            label = "Events",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth
        ),
        GeometricNavItem(
            route = UpcomingDestinations.BOOKINGS,
            label = "Bookings",
            selectedIcon = Icons.Filled.Schedule,
            unselectedIcon = Icons.Outlined.Schedule
        ),
        GeometricNavItem(
            route = UpcomingDestinations.AVAILABILITY,
            label = "Availability",
            selectedIcon = Icons.Filled.DateRange,
            unselectedIcon = Icons.Outlined.DateRange
        ),
        GeometricNavItem(
            route = UpcomingDestinations.SCHEDULING,
            label = "Scheduling",
            selectedIcon = Icons.Filled.Link,
            unselectedIcon = Icons.Outlined.Link
        )
    )

    val showBottomBar = authenticated && currentRoute in listOf(
        UpcomingDestinations.DASHBOARD,
        UpcomingDestinations.EVENT_TYPES,
        UpcomingDestinations.AVAILABILITY,
        UpcomingDestinations.BOOKINGS,
        UpcomingDestinations.SCHEDULING,
        UpcomingDestinations.SETTINGS
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                GeometricBottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (authenticated) UpcomingDestinations.DASHBOARD else UpcomingDestinations.AUTH,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ---- Auth gate (outside the bottom bar) -----------------------
            composable(UpcomingDestinations.AUTH) {
                val viewModel = rememberViewModel { app.getupcoming.feature.auth.AuthViewModel(authRepository) }
                app.getupcoming.feature.auth.AuthScreen(
                    viewModel = viewModel,
                    onAuthenticated = {
                        navController.navigate(UpcomingDestinations.DASHBOARD) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOpenTerms = { navController.navigate(UpcomingDestinations.TERMS) },
                    onOpenPrivacy = { navController.navigate(UpcomingDestinations.PRIVACY) }
                )
            }

            composable(UpcomingDestinations.TERMS) {
                app.getupcoming.feature.legal.LegalScreen(
                    title = "Terms of Use",
                    isTerms = true,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(UpcomingDestinations.PRIVACY) {
                app.getupcoming.feature.legal.LegalScreen(
                    title = "Privacy Policy",
                    isTerms = false,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 1. Host Dashboard
            composable(UpcomingDestinations.DASHBOARD) {
                val viewModel = rememberViewModel { DashboardViewModel(repository, context.applicationContext) }
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToEventTypes = { navController.navigate(UpcomingDestinations.EVENT_TYPES) },
                    onNavigateToAvailability = { navController.navigate(UpcomingDestinations.AVAILABILITY) },
                    onNavigateToBookings = { navController.navigate(UpcomingDestinations.BOOKINGS) },
                    onNavigateToNotifications = { navController.navigate(UpcomingDestinations.NOTIFICATIONS) },
                    onNavigateToSettings = { navController.navigate(UpcomingDestinations.SETTINGS) },
                    onOpenBookingFlow = { eventTypeId ->
                        navController.navigate("${UpcomingDestinations.BOOKING_FLOW}/$eventTypeId")
                    },
                    onOpenBookingDetail = { uid ->
                        navController.navigate("${UpcomingDestinations.BOOKING_DETAIL}/$uid")
                    },
                    onNavigateToCreateEventType = {
                        navController.navigate("${UpcomingDestinations.EVENT_TYPE_EDITOR}/0")
                    }
                )
            }

            // 2. Event Types List
            composable(UpcomingDestinations.EVENT_TYPES) {
                val viewModel = rememberViewModel { EventTypesViewModel(repository) }
                EventTypeListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreate = { navController.navigate("${UpcomingDestinations.EVENT_TYPE_EDITOR}/0") },
                    onNavigateToEdit = { id -> navController.navigate("${UpcomingDestinations.EVENT_TYPE_EDITOR}/$id") },
                    onOpenBookingFlow = { id -> navController.navigate("${UpcomingDestinations.BOOKING_FLOW}/$id") }
                )
            }

            // 3. Event Type Editor
            composable(
                route = "${UpcomingDestinations.EVENT_TYPE_EDITOR}/{eventTypeId}",
                arguments = listOf(navArgument("eventTypeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val eventTypeId = backStackEntry.arguments?.getLong("eventTypeId") ?: 0L
                val viewModel = rememberViewModel { EventTypesViewModel(repository) }
                EventTypeEditorScreen(
                    eventTypeId = eventTypeId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 4. Availability & Working Hours
            composable(UpcomingDestinations.AVAILABILITY) {
                val viewModel = rememberViewModel { AvailabilityViewModel(repository) }
                AvailabilityScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 5. Bookings List
            composable(UpcomingDestinations.BOOKINGS) {
                val viewModel = rememberViewModel { BookingsViewModel(repository, context) }
                BookingsListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenBookingDetail = { uid -> navController.navigate("${UpcomingDestinations.BOOKING_DETAIL}/$uid") }
                )
            }

            // 6. Booking Detail
            composable(
                route = "${UpcomingDestinations.BOOKING_DETAIL}/{bookingUid}",
                arguments = listOf(navArgument("bookingUid") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingUid = backStackEntry.arguments?.getString("bookingUid") ?: ""
                val viewModel = rememberViewModel { BookingsViewModel(repository, context) }
                BookingDetailScreen(
                    bookingUid = bookingUid,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 7. Public Invitee Booking Flow
            composable(
                route = "${UpcomingDestinations.BOOKING_FLOW}/{eventTypeId}",
                arguments = listOf(navArgument("eventTypeId") { type = NavType.LongType })
            ) { backStackEntry ->
                val eventTypeId = backStackEntry.arguments?.getLong("eventTypeId") ?: 1L
                val viewModel = rememberViewModel(eventTypeId) {
                    InviteeBookingViewModel(repository, eventTypeId)
                }
                InviteeBookingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onFinishBooking = {
                        navController.navigate(UpcomingDestinations.DASHBOARD) {
                            popUpTo(UpcomingDestinations.DASHBOARD) { inclusive = true }
                        }
                    }
                )
            }

            // 8. Settings Hub
            // Scheduling — Calendly-style share surface (4th bottom tab).
            composable(UpcomingDestinations.SCHEDULING) {
                val viewModel = rememberViewModel { SchedulingViewModel(repository) }
                SchedulingScreen(
                    viewModel = viewModel,
                    onOpenSettings = { navController.navigate(UpcomingDestinations.SETTINGS) }
                )
            }

            composable(UpcomingDestinations.SETTINGS) {
                val viewModel = rememberViewModel { SettingsViewModel(repository, authRepository) }
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToNotifications = { navController.navigate(UpcomingDestinations.NOTIFICATIONS) },
                    onNavigateBack = { navController.popBackStack() },
                    onLoggedOut = {
                        navController.navigate(UpcomingDestinations.AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToPermissions = { navController.navigate(UpcomingDestinations.PERMISSIONS) },
                    onNavigateToTerms = { navController.navigate(UpcomingDestinations.TERMS) },
                    onNavigateToPrivacy = { navController.navigate(UpcomingDestinations.PRIVACY) }
                )
            }

            composable(UpcomingDestinations.PERMISSIONS) {
                app.getupcoming.feature.permissions.PermissionsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 9. Notifications & Alarms Center (nested under Settings)
            composable(UpcomingDestinations.NOTIFICATIONS) {
                val viewModel = rememberViewModel { NotificationsViewModel(repository) }
                NotificationsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun GeometricBottomNavBar(
    items: List<GeometricNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Surface(
        color = CanvasCream,
        border = BorderStroke(1.dp, HairlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = when (item.route) {
                    UpcomingDestinations.DASHBOARD -> currentRoute == UpcomingDestinations.DASHBOARD || currentRoute == UpcomingDestinations.EVENT_TYPES
                    else -> currentRoute == item.route
                }

                val icon = if (isSelected) item.selectedIcon else item.unselectedIcon
                val contentColor = if (isSelected) Ink else MutedText

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(UpcomingTokens.RadiusFull)
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 28.dp)
                            .clip(UpcomingTokens.RadiusFull)
                            .background(
                                if (isSelected) SurfaceCreamStrong else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
inline fun <reified T : androidx.lifecycle.ViewModel> rememberViewModel(
    key: Any? = null,
    crossinline factory: () -> T
): T {
    return androidx.lifecycle.viewmodel.compose.viewModel(
        key = key?.toString(),
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : androidx.lifecycle.ViewModel> create(modelClass: Class<VM>): VM {
                return factory() as VM
            }
        }
    )
}

