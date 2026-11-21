package com.nagabhushansn95.financetracker.ui.help

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nagabhushansn95.financetracker.BuildConfig
import com.nagabhushansn95.financetracker.R

private const val EMAIL = "nagabhushansn95@gmail.com"
private const val REPO = "https://github.com/NagabhushanSN95/FinanceTracker"

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)) {
            Image(
                painterResource(R.mipmap.ic_launcher_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text("Finance Tracker", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Formerly Finance Manager",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Devoted to Lord Krishna, my parents, teachers, relatives, friends and all those who helped me reach this position.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text("Nagabhushan S N", style = MaterialTheme.typography.titleMedium)
        Text(
            "Special thanks to Aaditya Ravindran for designing the original Finance Manager logo.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = {
            val intent = Intent(
                Intent.ACTION_SENDTO,
                Uri.parse("mailto:$EMAIL")
            ).putExtra(Intent.EXTRA_SUBJECT, "Query about Finance Tracker")
            runCatching { context.startActivity(Intent.createChooser(intent, "Send mail using")) }
        }) { Text(EMAIL) }
        TextButton(onClick = {
            runCatching {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(REPO)
                    )
                )
            }
        }) { Text("Source on GitHub") }
        Spacer(Modifier.height(24.dp))
        Text(
            "Made in India \uD83C\uDDEE\uD83C\uDDF3",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
