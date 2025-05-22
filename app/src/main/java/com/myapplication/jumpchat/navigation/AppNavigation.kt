package com.myapplication.jumpchat.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()
    AppNavGraph(navController = navController, startDestination = startDestination)
}
