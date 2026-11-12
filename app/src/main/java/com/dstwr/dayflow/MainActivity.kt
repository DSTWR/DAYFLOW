package com.dstwr.dayflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DayflowApp() }
    }
}

private val Night = Color(0xFF090713)
private val Violet = Color(0xFF9B6CFF)
private val Gold = Color(0xFFE7C78A)

@Composable
private fun DayflowApp() {
    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, secondary = Gold, background = Night, surface = Color(0xFF171326))) {
        Surface(modifier = Modifier.fillMaxSize(), color = Night) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF21133D), Night)))) {
                LazyColumn(contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    item {
                        Text("DAYFLOW", color = Gold, fontSize = 13.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(22.dp))
                        Text("مساء الخير 👋", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("لنرتّب يومك بخطوات هادئة.", color = Color(0xFFBDB5CF), fontSize = 14.sp)
                    }
                    item {
                        GlassPanel {
                            Text("إنجاز اليوم", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))
                            Text("ابدأ بأول خطوة", color = Gold, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("ستظهر مهامك ومواعيدك هنا بعد إضافتها.", color = Color(0xFFBDB5CF), fontSize = 13.sp)
                        }
                    }
                    item {
                        Text("مساحتك اليومية", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickTile("المهام", "خطط وأنجز", Modifier.weight(1f))
                            QuickTile("المواعيد", "وقتك بيدك", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickTile("الملاحظات", "أفكارك بأمان", Modifier.weight(1f))
                            QuickTile("العادات", "خطوات تتكرر", Modifier.weight(1f))
                        }
                    }
                    item {
                        GlassPanel {
                            Text("قريباً في يومك", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("التقويم • التذكيرات • التركيز • الأهداف • الإحصائيات", color = Color(0xFFBDB5CF), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color(0xB3211B35), shape = RoundedCornerShape(24.dp), tonalElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().padding(20.dp), content = content)
    }
}

@Composable
private fun QuickTile(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color(0x99251D39), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = Color(0xFFBDB5CF), fontSize = 11.sp)
        }
    }
}
