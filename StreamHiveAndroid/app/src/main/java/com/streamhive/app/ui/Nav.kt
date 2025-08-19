package com.streamhive.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun StreamHiveNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("player/{key}") { backStackEntry ->
            val key = backStackEntry.arguments?.getString("key") ?: ""
            VideoPlayerScreen(key = key)
        }
    }
}

