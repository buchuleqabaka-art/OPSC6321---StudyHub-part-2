package com.studyhub.app.ui.navigation

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studyhub.app.ui.components.CreateGroupDialog
import com.studyhub.app.ui.components.CreateSessionDialog
import com.studyhub.app.ui.components.CreateTaskDialog
import com.studyhub.app.ui.components.StudyHubBottomBar
import com.studyhub.app.ui.screens.CalendarScreen
import com.studyhub.app.ui.screens.ChatScreen
import com.studyhub.app.ui.screens.GroupsScreen
import com.studyhub.app.ui.screens.HomeScreen
import com.studyhub.app.ui.screens.LoginScreen
import com.studyhub.app.ui.screens.ProfileScreen
import com.studyhub.app.ui.screens.RegisterScreen
import com.studyhub.app.ui.screens.SessionsScreen
import com.studyhub.app.ui.screens.TasksScreen
import com.studyhub.app.ui.screens.WelcomeScreen
import com.studyhub.app.ui.theme.Ink
import com.studyhub.app.ui.theme.InkMuted
import com.studyhub.app.ui.theme.LilacPrimary
import com.studyhub.app.viewmodel.AuthViewModel
import com.studyhub.app.viewmodel.MainViewModel
import com.studyhub.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/** Which routes show the bottom navigation bar. */
private val BAR_ROUTES = setOf(
    Routes.HOME, Routes.GROUPS, Routes.TASKS, Routes.CALENDAR,
    Routes.SESSIONS, Routes.CHATS, Routes.PROFILE
)

@Composable
fun StudyHubNavGraph(startLoggedIn: Boolean) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val mainViewModel: MainViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val mainState by mainViewModel.state.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.WELCOME

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Which "+" dialog is open, if any
    var dialog by remember { mutableStateOf<String?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute in BAR_ROUTES,
        drawerContent = {
            if (currentRoute in BAR_ROUTES) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor = Ink
                ) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val photoUrl = mainState.profile?.photoUrl
                        val profileBitmap = remember(photoUrl) {
                            if (photoUrl.isNullOrBlank()) null
                            else try {
                                val bytes = Base64.decode(photoUrl, Base64.NO_WRAP)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = LilacPrimary,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = mainState.profile?.name?.ifBlank { "Student" } ?: "Student",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Text(
                                text = mainState.profile?.email?.ifBlank { "StudyHub Member" } ?: "StudyHub Member",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    NavigationDrawerItem(
                        label = { Text("Home", color = Ink, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Home, contentDescription = null, tint = Ink) },
                        selected = currentRoute == Routes.HOME,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.HOME) {
                                launchSingleTop = true
                                popUpTo(Routes.HOME)
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("My Groups", color = Ink, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Groups, contentDescription = null, tint = Ink) },
                        selected = currentRoute == Routes.GROUPS,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.GROUPS) { launchSingleTop = true }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Tasks", color = Ink, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = Ink) },
                        selected = currentRoute == Routes.TASKS,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.TASKS) { launchSingleTop = true }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Calendar", color = Ink, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Ink) },
                        selected = currentRoute == Routes.CALENDAR,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.CALENDAR) { launchSingleTop = true }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Study Sessions", color = Ink, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = Ink) },
                        selected = currentRoute == Routes.SESSIONS,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.SESSIONS) { launchSingleTop = true }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Chats", color = Ink, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Ink) },
                        selected = currentRoute == Routes.CHATS,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.CHATS) { launchSingleTop = true }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Profile", color = Ink, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Person, contentDescription = null, tint = Ink) },
                        selected = currentRoute == Routes.PROFILE,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (settingsState.notifications) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = Ink
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Notifications", color = Ink, fontWeight = FontWeight.SemiBold)
                            Text("Reminders & alerts", color = InkMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = settingsState.notifications,
                            onCheckedChange = { settingsViewModel.setNotifications(it) }
                        )
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    NavigationDrawerItem(
                        label = { Text("Sign Out", color = Ink, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Ink) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            authViewModel.signOut()
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (currentRoute in BAR_ROUTES) {
                    StudyHubBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                                popUpTo(Routes.HOME)
                            }
                        },
                        onAddClick = {
                            dialog = when (currentRoute) {
                                Routes.GROUPS -> "group"
                                Routes.SESSIONS -> "session"
                                else -> "task"
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = if (startLoggedIn) Routes.HOME else Routes.WELCOME
                ) {
                    composable(Routes.WELCOME) {
                        WelcomeScreen(
                            onGetStarted = { navController.navigate(Routes.REGISTER) },
                            onLogIn = { navController.navigate(Routes.LOGIN) }
                        )
                    }

                    composable(Routes.REGISTER) {
                        RegisterScreen(
                            viewModel = authViewModel,
                            onRegistered = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.WELCOME) { inclusive = true }
                                }
                            },
                            onGoToLogin = { navController.navigate(Routes.LOGIN) }
                        )
                    }

                    composable(Routes.LOGIN) {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoggedIn = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.WELCOME) { inclusive = true }
                                }
                            },
                            onGoToRegister = { navController.navigate(Routes.REGISTER) }
                        )
                    }

                    composable(Routes.HOME) {
                        HomeScreen(
                            state = mainState,
                            notificationsEnabled = settingsState.notifications,
                            onToggleNotifications = settingsViewModel::setNotifications,
                            onOpenGroups = { navController.navigate(Routes.GROUPS) },
                            onOpenTasks = { navController.navigate(Routes.TASKS) },
                            onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                            onOpenSessions = { navController.navigate(Routes.SESSIONS) },
                            onOpenProfile = { navController.navigate(Routes.PROFILE) },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    composable(Routes.GROUPS) {
                        GroupsScreen(
                            state = mainState,
                            onBack = { navController.popBackStack() },
                            onCreateGroup = { dialog = "group" },
                            onOpenGroup = { group ->
                                mainViewModel.openGroupChat(group.id)
                                navController.navigate(Routes.CHATS)
                            }
                        )
                    }

                    composable(Routes.TASKS) {
                        TasksScreen(
                            state = mainState,
                            onBack = { navController.popBackStack() },
                            onNewTask = { dialog = "task" },
                            onToggle = mainViewModel::toggleTask,
                            onDelete = mainViewModel::deleteTask
                        )
                    }

                    composable(Routes.CALENDAR) {
                        CalendarScreen(state = mainState, onBack = { navController.popBackStack() })
                    }

                    composable(Routes.SESSIONS) {
                        SessionsScreen(
                            state = mainState,
                            onBack = { navController.popBackStack() },
                            onSchedule = { dialog = "session" },
                            onToggleAttendance = mainViewModel::toggleAttendance
                        )
                    }

                    composable(Routes.CHATS) {
                        ChatScreen(
                            state = mainState,
                            onBack = { navController.popBackStack() },
                            onSelectGroup = mainViewModel::openGroupChat,
                            onOpenGroups = { navController.navigate(Routes.GROUPS) },
                            onSend = mainViewModel::sendMessage
                        )
                    }

                    composable(Routes.PROFILE) {
                        ProfileScreen(
                            state = mainState,
                            settings = settingsState,
                            onDarkModeChange = settingsViewModel::setDarkMode,
                            onOfflineSyncChange = settingsViewModel::setOfflineSync,
                            onDataSaverChange = settingsViewModel::setDataSaver,
                            onNotificationsChange = settingsViewModel::setNotifications,
                            onLanguageChange = settingsViewModel::setLanguage,
                            onSaveProfile = { n, c, e -> mainViewModel.updateProfile(n, c, e) },
                            onPhotoSelected = mainViewModel::updatePhoto,
                            onSignOut = {
                                authViewModel.signOut()
                                navController.navigate(Routes.WELCOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    when (dialog) {
        "group" -> CreateGroupDialog(
            onDismiss = { dialog = null },
            onCreate = mainViewModel::createGroup
        )
        "task" -> CreateTaskDialog(
            onDismiss = { dialog = null },
            onCreate = mainViewModel::addTask
        )
        "session" -> CreateSessionDialog(
            onDismiss = { dialog = null },
            onCreate = mainViewModel::addSession
        )
    }
}