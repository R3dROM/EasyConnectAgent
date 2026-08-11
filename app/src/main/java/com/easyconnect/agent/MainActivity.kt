package com.easyconnect.agent

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.easyconnect.agent.ui.theme.EasyDeployAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startBootConnection(this)
        enableEdgeToEdge()
        setContent {
            EasyDeployAgentTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
fun startBootConnection(context: Context?)
{
    val request = OneTimeWorkRequestBuilder<BootNotifier>()
        .build()
    Log.e("BOOT_RECEIVER", "Enqueieng ${request.id}")
    Log.e("BOOT_RECEIVER","context boot $context")
    if (context != null)
    {
        PersistentData.readNetworkingConfiguration(context)
        WorkManager
            .getInstance(context)
            .enqueue(request)
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
    EasyDeployAgentTheme {
        Greeting("Android")
    }
}