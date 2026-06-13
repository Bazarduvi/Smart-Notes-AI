package com.example

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        handleDeepLink(intent)

        setContent {
            val isDark = com.example.ui.AppSettingsStore.isDarkMode.value
            AppTheme(darkTheme = isDark) {
                AppMainNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && "nexusassistant" == data.scheme && "login" == data.host) {
            val status = data.getQueryParameter("status") ?: "success"
            val user = data.getQueryParameter("user") ?: "cuentaeducativausa@gmail.com"
            val token = data.getQueryParameter("token") ?: "google_cl_speech"
            val service = data.getQueryParameter("service") ?: "Google TTS & Voice"
            
            if (status == "success") {
                AppSettingsStore.isGoogleAuthorized.value = true
                AppSettingsStore.googleAuthorizedUser.value = user
                if (service.contains("Azure")) {
                    AppSettingsStore.microsoftAzureSpeechKey.value = "AZ-PRO-${token.take(12)}"
                } else {
                    AppSettingsStore.googleCloudTtsKey.value = "GCTS-${token.take(12)}"
                }
                Toast.makeText(this, "🔐 ¡Autenticación con $service correcta para $user!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Recordatorios"
            val descriptionText = "Canal para recordatorios de AdSense & Clima"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("reminders_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun AppMainNavigation() {
    var selectedTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Clima, 2: Notas/Recordatorios, 3: Ajustes
    var showTutorial by remember { mutableStateOf(true) }

    if (showTutorial) {
        TutorialOverlay(onDismiss = { showTutorial = false })
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF3B82F6),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Bolsa") },
                    label = { Text("Bolsa", fontSize = 11.sp, color = Color.White) },
                    modifier = Modifier.testTag("nav_dashboard_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = "Clima") },
                    label = { Text("Clima", fontSize = 11.sp, color = Color.White) },
                    modifier = Modifier.testTag("nav_weather_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Notas") },
                    label = { Text("Notas", fontSize = 11.sp, color = Color.White) },
                    modifier = Modifier.testTag("nav_reminders_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes", fontSize = 11.sp, color = Color.White) },
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        },
        containerColor = Color(0xFF0B0F19)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(onBack = {})
                1 -> WeatherScreen(onBack = { selectedTab = 0 })
                2 -> NotesAndRemindersScreen(onBack = { selectedTab = 0 })
                3 -> SettingsScreen(onBack = { selectedTab = 0 })
            }
        }
    }
}
