package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.FindItRepository
import com.example.data.repository.PreferencesRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.MyItemsScreen
import com.example.ui.screens.ReportItemScreen
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FindItCampusTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OnCyanContainer
import com.example.ui.theme.SlateSurface
import com.example.ui.viewmodel.FindItViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object Routes {
    const val FEED = "feed"
    const val ALERTS = "alerts"
    const val MY_ITEMS = "my_items"
    const val REPORT = "report"
    const val DETAIL = "detail/{itemId}"
    fun detail(itemId: String) = "detail/$itemId"
}

private data class MainTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val MainTabs = listOf(
    MainTab(
        route = Routes.FEED,
        labelRes = R.string.tab_feed,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_tab_feed"
    ),
    MainTab(
        route = Routes.ALERTS,
        labelRes = R.string.tab_alerts,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        testTag = "nav_tab_alerts"
    ),
    MainTab(
        route = Routes.MY_ITEMS,
        labelRes = R.string.tab_my_items,
        selectedIcon = Icons.Filled.AssignmentInd,
        unselectedIcon = Icons.Outlined.AssignmentInd,
        testTag = "nav_tab_my_items"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            FindItCampusTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener drives navigation */ })
    } else {
        key(user.uid) {
            FindItMainShell(user = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindItMainShell(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: FindItViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FindItViewModel(
                    repository = FindItRepository(db, Firebase.auth),
                    preferences = PreferencesRepository(app),
                    appContext = app,
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: "Campus Student",
                    userEmail = user.email ?: "",
                    userPhotoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val filteredItemsState by viewModel.filteredItemsState.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val myItemsState by viewModel.myItemsState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val unreadAlertsCount by viewModel.unreadAlertsCount.collectAsStateWithLifecycle()
    val savedPhone by viewModel.savedPhoneNumber.collectAsStateWithLifecycle()
    val systemAlertsEnabled by viewModel.systemNotificationsEnabled.collectAsStateWithLifecycle()
    val isSubmittingReport by viewModel.isSubmittingReport.collectAsStateWithLifecycle()
    val isSubmittingClaim by viewModel.isSubmittingClaim.collectAsStateWithLifecycle()

    // Keep campus items & notifications listeners active across tabs for real-time alerts
    val unusedCampusItems by viewModel.campusItemsState.collectAsStateWithLifecycle()

    val isMainTab = currentRoute in setOf(Routes.FEED, Routes.ALERTS, Routes.MY_ITEMS)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (isMainTab) {
                    TopAppBar(
                        title = {
                            val titleText = when (currentRoute) {
                                Routes.ALERTS -> "Campus Alerts"
                                Routes.MY_ITEMS -> "My Items"
                                else -> stringResource(R.string.app_name)
                            }
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ObsidianBg,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            },
            bottomBar = {
                if (isMainTab && !useRail) {
                    NavigationBar(
                        containerColor = SlateSurface,
                        tonalElevation = 0.dp
                    ) {
                        MainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = if (tab.route == Routes.ALERTS) unreadAlertsCount else 0
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = OnCyanContainer,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isMainTab) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(Routes.REPORT) },
                        containerColor = ElectricCyan,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("fab_report_item")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_report_item),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_report_item),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isMainTab && useRail) {
                    NavigationRail(
                        containerColor = SlateSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = if (tab.route == Routes.ALERTS) unreadAlertsCount else 0
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = OnCyanContainer,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.FEED
                    ) {
                        composable(Routes.FEED) {
                            FeedScreen(
                                itemsState = filteredItemsState,
                                filters = filters,
                                currentUserId = user.uid,
                                onSearchChange = viewModel::updateSearchQuery,
                                onFilterSelect = viewModel::selectFeedFilter,
                                onCategoryToggle = viewModel::toggleCategoryFilter,
                                onClearFilters = viewModel::clearFilters,
                                onItemClick = { itemId ->
                                    navController.navigate(Routes.detail(itemId))
                                }
                            )
                        }

                        composable(Routes.ALERTS) {
                            AlertsScreen(
                                notificationsState = notificationsState,
                                systemNotificationsEnabled = systemAlertsEnabled,
                                onToggleSystemNotifications = viewModel::setSystemNotificationsEnabled,
                                onMarkRead = viewModel::markNotificationRead,
                                onMarkAllRead = viewModel::markAllNotificationsRead,
                                onOpenItem = { itemId ->
                                    navController.navigate(Routes.detail(itemId))
                                }
                            )
                        }

                        composable(Routes.MY_ITEMS) {
                            MyItemsScreen(
                                userName = viewModel.userDisplayName,
                                userEmail = viewModel.userEmail,
                                savedPhone = savedPhone,
                                myItemsState = myItemsState,
                                onSavePhone = viewModel::saveDefaultPhone,
                                onSignOut = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                },
                                onItemClick = { itemId ->
                                    navController.navigate(Routes.detail(itemId))
                                }
                            )
                        }

                        composable(Routes.REPORT) {
                            ReportItemScreen(
                                savedPhone = savedPhone,
                                isSubmitting = isSubmittingReport,
                                onCompressPhoto = viewModel::compressSelectedPhoto,
                                onSubmit = { title, desc, cat, type, loc, phone, photo ->
                                    viewModel.submitItemReport(
                                        title = title,
                                        description = desc,
                                        category = cat,
                                        type = type,
                                        location = loc,
                                        contactPhone = phone,
                                        photoBase64 = photo,
                                        onSuccess = { newId ->
                                            navController.popBackStack()
                                            navController.navigate(Routes.detail(newId))
                                        }
                                    )
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Routes.DETAIL,
                            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getString("itemId").orEmpty()
                            val detailState by remember(itemId) {
                                viewModel.observeItemDetail(itemId)
                            }.collectAsStateWithLifecycle(initialValue = UiState.Loading)

                            val claims by remember(itemId) {
                                viewModel.observeClaimsForDetail(itemId)
                            }.collectAsStateWithLifecycle(initialValue = emptyList())

                            ItemDetailScreen(
                                itemState = detailState,
                                claims = claims,
                                currentUserId = user.uid,
                                savedPhone = savedPhone,
                                isSubmittingClaim = isSubmittingClaim,
                                onUpdateStatus = { newStatus ->
                                    viewModel.updateItemStatus(itemId, newStatus)
                                },
                                onDeleteItem = {
                                    viewModel.deleteItem(itemId) {
                                        navController.popBackStack()
                                    }
                                },
                                onSubmitClaim = { phone, msg, onDone ->
                                    val currentItem = (detailState as? UiState.Success)?.data
                                    if (currentItem != null) {
                                        viewModel.submitClaim(currentItem, phone, msg, onDone)
                                    }
                                },
                                onUpdateClaimStatus = { claimId, status ->
                                    viewModel.updateClaimStatus(itemId, claimId, status)
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: MainTab,
    selected: Boolean,
    unreadCount: Int
) {
    val label = stringResource(tab.labelRes)
    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (unreadCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = ElectricCyan,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                }
            }
        ) {
            Icon(imageVector = iconVector, contentDescription = label)
        }
    } else {
        Icon(imageVector = iconVector, contentDescription = label)
    }
}
