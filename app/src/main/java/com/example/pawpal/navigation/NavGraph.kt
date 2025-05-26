package com.example.pawpal.navigation
import com.example.pawpal.layout.LoginScreen
import com.example.pawpal.layout.RegisterScreen
import com.example.pawpal.layout.ProfileScreen
import com.example.pawpal.layout.SettingScreen
import com.example.pawpal.layout.LauncherScreen
import com.example.pawpal.layout.TaskSwipeScreen
import com.example.pawpal.layout.ChatRoomScreen
import com.example.pawpal.layout.MapScreen
import com.example.pawpal.layout.SocialScreen

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.navigation.NavController
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.filled.Map
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.google.firebase.auth.FirebaseAuth
import com.example.pawpal.layout.ChatScreen
import androidx.navigation.NavType
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.navArgument
import java.net.URLDecoder
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.input.TextFieldValue
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import com.google.firebase.storage.FirebaseStorage
import androidx.navigation.compose.rememberNavController
fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val SETTING = "setting"
    const val PROFILE = "profile"
    const val LAUNCHER = "launcher"
    const val SWIPE = "swipe"
    const val CHATS = "chats"
    const val MAP = "map"
    const val SOCIAL = "social"
}

@Composable
fun AppNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.REGISTER) { RegisterScreen(navController) }
        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.LAUNCHER) { LauncherScreen(navController) }
        composable(Routes.SOCIAL) {
            MainWithBottomBar(navController, currentRoute = Routes.SOCIAL)
        }
        composable(Routes.PROFILE) {
            MainWithBottomBar(navController, currentRoute = Routes.PROFILE)
        }
        composable(Routes.SETTING) {
            MainWithBottomBar(navController, currentRoute = Routes.SETTING)
        }
        composable(Routes.SWIPE) {
            MainWithBottomBar(navController, currentRoute = Routes.SWIPE)
        }
        composable(Routes.MAP) {
            MainWithBottomBar(navController, currentRoute = Routes.MAP)
        }
        composable(Routes.CHATS) {
            MainWithBottomBar(navController, currentRoute = Routes.CHATS)
        }
        composable(
            "chat/{chatId}/{otherId}/{username}/{profileUrl}?taskTitle={taskTitle}&taskId={taskId}&petName={petName}&manualLocation={manualLocation}&lat={lat}&lng={lng}",
            arguments = listOf(
                navArgument("chatId") { defaultValue = "" },
                navArgument("otherId") { defaultValue = "" },
                navArgument("username") { defaultValue = "" },
                navArgument("profileUrl") { defaultValue = "" },
                navArgument("taskTitle") { nullable = true },
                navArgument("taskId") { nullable = true },
                navArgument("petName") { nullable = true },
                navArgument("manualLocation") { nullable = true },
                navArgument("lat") { nullable = true },
                navArgument("lng") { nullable = true }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            val chatId = args.getString("chatId")!!
            val otherId = args.getString("otherId")!!
            val username = URLDecoder.decode(args.getString("username") ?: "Unknown", "UTF-8")
            val profileUrl = URLDecoder.decode(args.getString("profileUrl") ?: "", "UTF-8")

            val task = mapOf(
                "taskTitle" to args.getString("taskTitle"),
                "taskId" to args.getString("taskId"),
                "petName" to args.getString("petName"),
                "manualLocation" to args.getString("manualLocation"),
                "lat" to args.getString("lat")?.toDoubleOrNull(),
                "lng" to args.getString("lng")?.toDoubleOrNull()
            )

            ChatScreen(
                navController = navController,
                chatId = chatId,
                currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                otherUserId = otherId,
                otherUsername = username,
                otherUserProfileUrl = profileUrl,
                preloadTaskInfo = task
            )
        }
    }
}
@Composable
fun MainWithBottomBar(navController: NavController, currentRoute: String, userId: String = "YOUR_DEFAULT_ID"){
    var hasUnreadMessages by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users").document(userId).collection("chatRefs")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        var unreadFound = false
                        snapshot.documents.forEach { doc ->
                            val lastSeen = doc.getLong("lastSeen") ?: 0L
                            val chatId = doc.id

                            db.collection("chats").document(chatId).collection("messages")
                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { msgSnap ->
                                    val msg = msgSnap.documents.firstOrNull()
                                    val timestamp = msg?.getLong("timestamp") ?: 0L
                                    val senderId = msg?.getString("senderId") ?: ""
                                    if (senderId != userId && timestamp > lastSeen) {
                                        unreadFound = true
                                        hasUnreadMessages = true
                                    }
                                }
                        }
                        // If none were unread after looping
                        if (!unreadFound) {
                            hasUnreadMessages = false
                        }
                    }
                }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Box {
                            Icon(Icons.Default.Chat, contentDescription = "Chats")
                            if (hasUnreadMessages) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Red, shape = CircleShape)
                                )
                            }
                        }
                    },
                    selected = currentRoute == Routes.CHATS,
                    onClick = {
                        if (currentRoute != Routes.CHATS) {
                            navController.navigate(Routes.CHATS) {
                                popUpTo(Routes.PROFILE) { inclusive = false }
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Social") },
                    selected = currentRoute == Routes.SOCIAL,
                    onClick = {
                        if (currentRoute != Routes.SOCIAL) {
                            navController.navigate(Routes.SOCIAL) {
                                popUpTo(Routes.PROFILE) { inclusive = false }
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                    selected = currentRoute == Routes.MAP,
                    onClick = {
                        if (currentRoute != Routes.MAP) {
                            navController.navigate(Routes.MAP) {
                                popUpTo(Routes.PROFILE) { inclusive = false }
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Swipe, contentDescription = "Swipe Tasks") },
                    selected = currentRoute == Routes.SWIPE,
                    onClick = { navController.navigate(Routes.SWIPE) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    selected = currentRoute == Routes.PROFILE,
                    onClick = {
                        if (currentRoute != Routes.PROFILE) {
                            navController.navigate(Routes.PROFILE) {
                                popUpTo(Routes.PROFILE) { inclusive = true }
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    selected = currentRoute == Routes.SETTING,
                    onClick = {
                        if (currentRoute != Routes.SETTING) {
                            navController.navigate(Routes.SETTING) {
                                popUpTo(Routes.PROFILE) { inclusive = false }
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentRoute) {
                Routes.LOGIN -> LauncherScreen(navController)
                Routes.PROFILE -> ProfileScreen(navController)
                Routes.SETTING -> SettingScreen(navController)
                Routes.SWIPE -> TaskSwipeScreen(navController)
                Routes.MAP -> MapScreen(navController)
                Routes.SOCIAL -> SocialScreen(navController)
                Routes.CHATS -> {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        ChatRoomScreen(navController, currentUserId = userId)
                    } else {
                        Text("Not signed in")
                    }
                }
            }
        }
    }
}


