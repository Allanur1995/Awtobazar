package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.data.auth.authStateFlow
import com.example.data.auth.signOutUser
import com.example.data.repository.MarketplaceRepository
import com.example.ui.screens.AddListingScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.AwtoBazarTheme
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object Routes {
    const val ROUTE_HOME = "home"
    const val ROUTE_SEARCH = "search"
    const val ROUTE_ADD = "add?listingId={listingId}"
    const val ROUTE_ADD_BASE = "add"
    const val ROUTE_FAVORITES = "favorites"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_DETAIL = "detail/{listingId}"

    fun detailRoute(listingId: String): String = "detail/$listingId"
    fun addOrEditRoute(listingId: String? = null): String =
        if (listingId.isNullOrBlank()) ROUTE_ADD_BASE else "add?listingId=$listingId"
}

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            AwtoBazarTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: MarketplaceViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        MarketplaceViewModel(
                            repository = MarketplaceRepository(db),
                            currentUserId = user.uid,
                            initialDisplayName = user.displayName ?: "AwtoBazar Ulanyjy",
                            initialPhotoUrl = user.photoUrl?.toString(),
                        )
                    }
                }
            )
            MainMarketplaceShell(
                currentUserId = user.uid,
                fallbackDisplayName = user.displayName ?: "AwtoBazar Ulanyjy",
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )
        }
    }
}

@Composable
private fun MainMarketplaceShell(
    currentUserId: String,
    fallbackDisplayName: String,
    viewModel: MarketplaceViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredListingsState by viewModel.filteredListingsState.collectAsStateWithLifecycle()
    val activeListingsState by viewModel.activeListingsState.collectAsStateWithLifecycle()
    val myListingsState by viewModel.myListingsState.collectAsStateWithLifecycle()
    val favoritesState by viewModel.favoritesState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val inquiriesState by viewModel.inquiriesState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val filter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val tabs = remember {
        listOf(
            NavTab(Routes.ROUTE_HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
            NavTab(Routes.ROUTE_SEARCH, R.string.nav_search, Icons.Rounded.Search, Icons.Outlined.Search, "nav_search"),
            NavTab(Routes.ROUTE_ADD_BASE, R.string.nav_add, Icons.Rounded.AddCircle, Icons.Outlined.AddCircleOutline, "nav_add"),
            NavTab(Routes.ROUTE_FAVORITES, R.string.nav_favorites, Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder, "nav_favorites"),
            NavTab(Routes.ROUTE_PROFILE, R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile"),
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showTopLevelNav = currentRoute != Routes.ROUTE_DETAIL

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = currentRoute?.startsWith(tab.route) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            val selected = currentRoute?.startsWith(tab.route) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.ROUTE_HOME,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (showTopLevelNav) Modifier.statusBarsPadding() else Modifier),
                ) {
                    composable(Routes.ROUTE_HOME) {
                        HomeScreen(
                            listingsState = filteredListingsState,
                            filter = filter,
                            favoriteIds = favoriteIds,
                            isDarkTheme = isDarkTheme,
                            isBusy = isBusy,
                            onToggleTheme = onToggleTheme,
                            onSelectRegion = viewModel::selectRegion,
                            onSelectCategory = viewModel::selectCategory,
                            onToggleQuickFlag = viewModel::updateFilter,
                            onOpenSearch = { navigateTopLevel(Routes.ROUTE_SEARCH) },
                            onOpenAddListing = { navController.navigate(Routes.addOrEditRoute()) },
                            onOpenListing = { id -> navController.navigate(Routes.detailRoute(id)) },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onLoadStarterCatalog = viewModel::loadStarterCatalog,
                        )
                    }

                    composable(Routes.ROUTE_SEARCH) {
                        SearchScreen(
                            listingsState = filteredListingsState,
                            filter = filter,
                            favoriteIds = favoriteIds,
                            onUpdateFilter = viewModel::updateFilter,
                            onResetFilter = viewModel::resetFilter,
                            onOpenListing = { id -> navController.navigate(Routes.detailRoute(id)) },
                            onToggleFavorite = viewModel::toggleFavorite,
                        )
                    }

                    composable(
                        route = Routes.ROUTE_ADD,
                        arguments = listOf(
                            navArgument("listingId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val editId = backStackEntry.arguments?.getString("listingId")
                        val existing = if (editId != null) {
                            (myListingsState as? UiState.Success)?.data?.firstOrNull { it.id == editId }
                                ?: (activeListingsState as? UiState.Success)?.data?.firstOrNull { it.id == editId }
                        } else {
                            null
                        }
                        AddListingScreen(
                            existingListing = existing,
                            isBusy = isBusy,
                            onSave = { updated ->
                                viewModel.saveListing(editId, updated) {
                                    navigateTopLevel(Routes.ROUTE_HOME)
                                }
                            },
                            onCancel = {
                                if (!navController.popBackStack()) {
                                    navigateTopLevel(Routes.ROUTE_HOME)
                                }
                            },
                        )
                    }

                    composable(Routes.ROUTE_FAVORITES) {
                        FavoritesScreen(
                            favoritesState = favoritesState,
                            onOpenListing = { id -> navController.navigate(Routes.detailRoute(id)) },
                            onRemoveFavorite = viewModel::removeFavorite,
                            onBrowseHome = { navigateTopLevel(Routes.ROUTE_HOME) },
                        )
                    }

                    composable(Routes.ROUTE_PROFILE) {
                        ProfileScreen(
                            currentUserId = currentUserId,
                            userProfile = userProfile,
                            fallbackDisplayName = fallbackDisplayName,
                            myListingsState = myListingsState,
                            inquiriesState = inquiriesState,
                            isBusy = isBusy,
                            onOpenListing = { id -> navController.navigate(Routes.detailRoute(id)) },
                            onUpdateProfile = viewModel::updateProfile,
                            onReplyInquiry = viewModel::replyToInquiry,
                            onSignOut = {
                                signOutUser(credentialManager = credentialManager, scope = scope)
                            },
                        )
                    }

                    composable(
                        route = Routes.ROUTE_DETAIL,
                        arguments = listOf(navArgument("listingId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val listingId = backStackEntry.arguments?.getString("listingId").orEmpty()
                        val listing = (activeListingsState as? UiState.Success)?.data?.firstOrNull { it.id == listingId }
                            ?: (myListingsState as? UiState.Success)?.data?.firstOrNull { it.id == listingId }

                        DetailScreen(
                            listing = listing,
                            currentUserId = currentUserId,
                            isFavorite = favoriteIds.contains(listingId),
                            isBusy = isBusy,
                            onBack = { navController.popBackStack() },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onEditListing = { item -> navController.navigate(Routes.addOrEditRoute(item.id)) },
                            onMarkSold = viewModel::markListingSold,
                            onDeleteListing = { id ->
                                viewModel.deleteListing(id) {
                                    navController.popBackStack()
                                }
                            },
                            onSendInquiry = viewModel::sendInquiry,
                        )
                    }
                }
            }
        }
    }
}
