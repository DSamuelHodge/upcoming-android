package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.core.database.UpcomingDatabase
import com.example.core.engine.NotificationAndReminderManager
import com.example.core.network.UpcomingApiClient
import com.example.core.repository.UpcomingRepository
import com.example.navigation.UpcomingNavHost
import com.example.ui.theme.UpcomingTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: UpcomingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = UpcomingDatabase.getInstance(applicationContext)
        repository = UpcomingRepository(
            database = db,
            context = applicationContext,
            api = UpcomingApiClient.create()
        )

        NotificationAndReminderManager.setupChannels(applicationContext)

        setContent {
            UpcomingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UpcomingNavHost(
                        repository = repository,
                        context = applicationContext
                    )
                }
            }
        }
    }
}

