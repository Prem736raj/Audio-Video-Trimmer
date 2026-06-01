package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MediaViewModel,
    onNavigateToPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // We can use the MaterialTheme surface color to adapt automatically
    // or provide specialized colors for dark mode if preferred.
    
    val containerCol = MaterialTheme.colorScheme.background
    val cardCol = MaterialTheme.colorScheme.surface
    val onCardCol = MaterialTheme.colorScheme.onSurface

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = containerCol
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Customize your experience",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            }

            // Preferences Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardCol),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Preferences",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    // Day / Night Mode Switch
                    SettingSwitchRow(
                        icon = Icons.Default.DarkMode,
                        title = "Day / Night Mode",
                        subtitle = "Toggle dark theme across the app",
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() },
                        textColor = onCardCol
                    )
                }
            }

            // Support & Feedback Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardCol),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Support",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    // Provide Feedback
                    SettingClickableRow(
                        icon = Icons.Default.Email,
                        title = "Feedback",
                        subtitle = "Email us at developeraisteps@gmail.com",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:developeraisteps@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "App Feedback")
                                }
                                context.startActivity(Intent.createChooser(intent, "Send Email"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        textColor = onCardCol
                    )

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.08f), modifier = Modifier.padding(horizontal = 20.dp))

                    // Rate us
                    SettingClickableRow(
                        icon = Icons.Default.StarRate,
                        title = "Rate Us",
                        subtitle = "Enjoying the app? Leave a review!",
                        onClick = {
                            Toast.makeText(context, "Redirecting to Play Store...", Toast.LENGTH_SHORT).show()
                        },
                        textColor = onCardCol
                    )
                }
            }

            // About Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardCol),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "About",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    // Privacy Policy
                    SettingClickableRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "Read our data policy",
                        onClick = onNavigateToPrivacyPolicy,
                        textColor = onCardCol
                    )
                }
            }

            Text(
                text = "Media Trimmer v1.0.1",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
fun SettingClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

