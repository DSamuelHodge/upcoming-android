package com.example.navigation

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
import androidx.compose.ui.text.font.FontWeight
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
import com.example.core.designsystem.UpcomingTokens
import com.example.core.repository.UpcomingRepository
import com.example.feature.availability.AvailabilityScreen
import com.example.feature.availability.AvailabilityViewModel
import com.example.feature.bookingflow.InviteeBookingScreen
import com.example.feature.bookingflow.InviteeBookingViewModel
import com.example.feature.bookings.BookingDetailScreen
import com.example.feature.bookings.BookingsListScreen
import com.example.feature.bookings.BookingsViewModel
import com.example.feature.dashboard.DashboardScreen
import com.example.feature.dashboard.DashboardViewModel
import com.example.feature.eventtypes.EventTypeEditorScreen
import com.example.feature.eventtypes.EventTypeListScreen
import com.example.feature.eventtypes.EventTypesViewModel
import com.example.feature.notifications.NotificationsScreen
import com.example.ui.theme.*

object UpcomingDestinations {
    const val DASHBOARD = "dashboard"
    const val EVENT_TYPES = "event_types"
    const val EVENT_TYPE_EDITOR = "event_type_editor"
    const val AVAILABILITY = "availability"
    const val BOOKINGS = "bookings"
    const val BOOKING_DETAIL = "booking_detail"
    const val BOOKING_FLOW = "book"
    const val NOTIFICATIONS = "notifications"
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
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
            route = UpcomingDestinations.NOTIFICATIONS,
            label = "Settings",
            selectedIcon = Icons.Filled.Tune,
            unselectedIcon = Icons.Outlined.Tune
        )
    )

    val showBottomBar = currentRoute in listOf(
        UpcomingDestinations.DASHBOARD,
        UpcomingDestinations.EVENT_TYPES,
        UpcomingDestinations.AVAILABILITY,
        UpcomingDestinations.BOOKINGS,
        UpcomingDestinations.NOTIFICATIONS
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
            startDestination = UpcomingDestinations.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Host Dashboard
            composable(UpcomingDestinations.DASHBOARD) {
                val viewModel = rememberViewModel { DashboardViewModel(repository) }
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToEventTypes = { navController.navigate(UpcomingDestinations.EVENT_TYPES) },
                    onNavigateToAvailability = { navController.navigate(UpcomingDestinations.AVAILABILITY) },
                    onNavigateToBookings = { navController.navigate(UpcomingDestinations.BOOKINGS) },
                    onNavigateToNotifications = { navController.navigate(UpcomingDestinations.NOTIFICATIONS) },
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

            // 8. Notifications & Alarms Center
            composable(UpcomingDestinations.NOTIFICATIONS) {
                NotificationsScreen(
                    repository = repository,
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
        color = GeoNavBg,
        border = BorderStroke(1.dp, GeoBorderSubtleLight),
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
                val contentColor = if (isSelected) GeoOnPrimaryContainerLight else GeoTextSecondaryLight

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
                                if (isSelected) GeoPrimaryContainerLight else Color.Transparent
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
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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

