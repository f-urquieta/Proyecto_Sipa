package com.example.sipa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sipa.ui.theme.SipaTheme
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sipa.ui.screens.*
import com.example.sipa.ui.theme.SipaBackground


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SipaBackground),
                    color = SipaBackground
                ) {
                    SipaApp()
                }
            }
        }
    }
}

@Composable
fun SipaApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("houses") { HousesScreen(navController) }
        composable("history") {HistoryScreen(navController)}
        composable("location/{nombreCasa}") { backStackEntry ->
            val nombreCasa = backStackEntry.arguments?.getString("nombreCasa") ?: "Mi casa"
            LocationScreen(navController, nombreCasa)
        }
        composable("add_lights") { AddLightsScreen(navController) }
        composable("edit_schedule/{lightId}") { backStackEntry ->
            val lightId = backStackEntry.arguments?.getString("lightId") ?: ""
            EditScheduleScreen(navController, lightId)
        }
        composable("home") { HomeScreen(navController) }
    }
}