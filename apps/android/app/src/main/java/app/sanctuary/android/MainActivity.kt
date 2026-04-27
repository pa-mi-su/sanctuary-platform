package app.sanctuary.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.sanctuary.android.ui.theme.SanctuaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SanctuaryTheme {
                SanctuaryApp()
            }
        }
    }
}

private data class SanctuarySection(
    val title: String,
    val subtitle: String
)

private enum class AppTab(val label: String) {
    Home("Home"),
    Novenas("Novenas"),
    Liturgical("Liturgical"),
    Saints("Saints"),
    Me("Me")
}

@Composable
private fun SanctuaryApp() {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }

    val sections = listOf(
        SanctuarySection("Auth foundation", "Next: Cognito login, register, confirm, and reset flows."),
        SanctuarySection("Shared API model", "Android will follow the same Sanctuary API and refresh-token session pattern."),
        SanctuarySection("Content surfaces", "Saints, novenas, intentions, prayers, and the liturgical calendar come next."),
        SanctuarySection("Account tools", "Favorites, novena progress, and reminders will live under Me.")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1B2E3D),
                tonalElevation = 0.dp
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (tab == selectedTab) Color(0xFF79D7FF) else Color(0xFF5A7489))
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0x3346C4FF),
                            selectedIconColor = Color(0xFFEDF7FF),
                            selectedTextColor = Color(0xFFEDF7FF),
                            unselectedIconColor = Color(0xFFB7C8D6),
                            unselectedTextColor = Color(0xFFB7C8D6)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF132433),
                            Color(0xFF173246),
                            Color(0xFF102230)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Text(
                        text = "Sanctuary Android",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFF4FAFF),
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Text(
                        text = "Native Android foundation for Sanctuary is now in place. This shell is intentionally small so we can add auth and API slices cleanly.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFD0DFEA),
                        lineHeight = 24.sp
                    )
                }

                item {
                    StatusCard(selectedTab = selectedTab)
                }

                items(sections) { section ->
                    SectionCard(section)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(selectedTab: AppTab) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xCC22394C)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0E1C28)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", color = Color(0xFFF4FAFF), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Current focus",
                        color = Color(0xFF7AC8EA),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = selectedTab.label,
                        color = Color(0xFFF4FAFF),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Android stays independent in GitHub: API, web, iOS, and Android should only validate and release when their own surfaces change.",
                color = Color(0xFFD0DFEA),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun SectionCard(section: SanctuarySection) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xB323394C)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = section.title,
                color = Color(0xFFF4FAFF),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.subtitle,
                color = Color(0xFFC9D8E4),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }
}

