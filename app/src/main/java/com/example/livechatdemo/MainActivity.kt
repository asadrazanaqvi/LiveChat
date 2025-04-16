package com.example.livechatdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.livechatdemo.presentation.ui.ChatScreen
import com.example.livechatdemo.ui.theme.LiveChatDemoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiveChatDemoTheme {
                ChatApp()
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    ChatApp()
////                    Greeting(
////                        name = "Android",
////                        modifier = Modifier.padding(innerPadding)
////                    )
//                }
            }
        }
    }
}

@Composable
fun ChatApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "chat_list") {
        composable("chat_list") {
            ChatScreen(
//                onChatClick = { chatId ->
//                    navController.navigate("chat_detail/$chatId")
//                }
            )
        }
        composable(
            route = "chat_detail/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
           // ChatDetailScreen(chatId = chatId, onBackClick = { navController.popBackStack() })
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LiveChatDemoTheme {
        Greeting("Android")
    }
}