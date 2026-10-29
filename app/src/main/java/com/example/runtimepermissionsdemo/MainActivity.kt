
package com.example.runtimepermissionsdemo

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        private const val INITIAL_TIME = 10_000L
        private const val ALARM_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var remainingMillis by remember {
                mutableLongStateOf(INITIAL_TIME)
            }
            var isRunning by remember {
                mutableStateOf(false)
            }
            var endTime by remember {
                mutableLongStateOf(0L)
            }

            val permissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        remainingMillis = INITIAL_TIME
                        startTimer(INITIAL_TIME)
                        endTime = SystemClock.elapsedRealtime() +
                                INITIAL_TIME
                        isRunning = true
                    }
                }

            LaunchedEffect(isRunning, endTime) {
                while (isRunning) {
                    val left = (
                            endTime - SystemClock.elapsedRealtime()
                            ).coerceAtLeast(0L)

                    remainingMillis = left

                    if (left == 0L) {
                        isRunning = false
                        break
                    }

                    delay(200L)
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Countdown Reminder",
                            fontSize = 28.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "${(remainingMillis + 999L) / 1000L}",
                            fontSize = 64.sp
                        )

                        Text(text = "seconds remaining")

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!isRunning &&
                                        remainingMillis > 0L
                                    ) {
                                        if (
                                            Build.VERSION.SDK_INT >= 33 &&
                                            ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            permissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        } else {
                                            startTimer(remainingMillis)
                                            endTime =
                                                SystemClock.elapsedRealtime() +
                                                        remainingMillis
                                            isRunning = true
                                        }
                                    }
                                },
                                enabled = !isRunning &&
                                        remainingMillis > 0L
                            ) {
                                Text("Start")
                            }

                            Button(
                                onClick = {
                                    if (isRunning) {
                                        remainingMillis = (
                                                endTime -
                                                        SystemClock.elapsedRealtime()
                                                ).coerceAtLeast(0L)

                                        cancelTimer()
                                        isRunning = false
                                    }
                                },
                                enabled = isRunning
                            ) {
                                Text("Pause")
                            }

                            Button(
                                onClick = {
                                    cancelTimer()
                                    isRunning = false
                                    remainingMillis = INITIAL_TIME
                                }
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startTimer(durationMillis: Long) {
        val alarmManager = getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        val intent = Intent(this, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + durationMillis,
            pendingIntent
        )
    }

    private fun cancelTimer() {
        val intent = Intent(this, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
