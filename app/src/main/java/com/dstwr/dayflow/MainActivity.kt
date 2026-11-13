package com.dstwr.dayflow

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dstwr.dayflow.core.notifications.ReminderChannels
import com.dstwr.dayflow.core.notifications.ReminderScheduler
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Night = Color(0xFF090713)
private val Panel = Color(0xCC211A35)
private val Violet = Color(0xFFAA83FF)
private val Gold = Color(0xFFE7C78A)
private val Muted = Color(0xFFBDB5CF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderChannels.create(this)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 301)
        }
        setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { DayflowApp(this) } }
    }
}

private enum class Section(val title: String, val symbol: String) {
    HOME("الرئيسية", "⌂"), TASKS("المهام", "✓"), CALENDAR("التقويم", "▦"), NOTES("الملاحظات", "▤"),
    FOCUS("التركيز", "◷"), GOALS("الأهداف", "◎"), HABITS("العادات", "◇"), REMINDERS("التذكيرات", "♧"),
    STATS("إحصائيات", "▥"), SETTINGS("الإعدادات", "⚙")
}

private data class Entry(val id: Long, val title: String, val detail: String = "", val done: Boolean = false, val priority: Int = 1)

private class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("dayflow_local", Context.MODE_PRIVATE)
    fun read(key: String): List<Entry> {
        val result = mutableListOf<Entry>()
        val array = try { JSONArray(prefs.getString(key, "[]")) } catch (_: Exception) { JSONArray() }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            result.add(Entry(item.optLong("id"), item.optString("title"), item.optString("detail"), item.optBoolean("done"), item.optInt("priority", 1)))
        }
        return result
    }
    fun write(key: String, entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().put("id", entry.id).put("title", entry.title).put("detail", entry.detail).put("done", entry.done).put("priority", entry.priority))
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}

@Composable
private fun DayflowApp(context: Context) {
    val store = remember { LocalStore(context) }
    val tasks = remember { mutableStateListOf<Entry>().apply { addAll(store.read("tasks")) } }
    val notes = remember { mutableStateListOf<Entry>().apply { addAll(store.read("notes")) } }
    val reminders = remember { mutableStateListOf<Entry>().apply { addAll(store.read("reminders")) } }
    val habits = remember { mutableStateListOf<Entry>().apply {
        val today = java.time.LocalDate.now().toString()
        addAll(store.read("habits").map { it.copy(done = it.detail == today) })
    } }
    val goals = remember { mutableStateListOf<Entry>().apply { addAll(store.read("goals")) } }
    var section by remember { mutableStateOf(Section.HOME) }
    var showEditor by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf("") }
    var draftDetail by remember { mutableStateOf("") }
    var draftPriority by remember { mutableIntStateOf(1) }
    var selectedEntry by remember { mutableStateOf<Entry?>(null) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, secondary = Gold, background = Night, surface = Panel)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF21133D), Night)))) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("DAYFLOW", color = Gold, fontSize = 12.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                        Text(section.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    }
                    Image(painterResource(R.drawable.ic_dayflow_mark), contentDescription = "DAYFLOW", modifier = Modifier.size(48.dp))
                    Text("DSTWR", color = Muted, fontSize = 11.sp)
                }
                when (section) {
                    Section.HOME -> HomeScreen(tasks, notes, reminders) { section = it }
                    Section.CALENDAR -> CalendarScreen(tasks, context)
                    Section.FOCUS -> FocusScreen()
                    Section.GOALS -> EntryScreen("أهدافك الكبيرة", "حوّل طموحاتك إلى خطوات واضحة", goals, "أضف هدفك الأول", onDelete = { entry -> goals.remove(entry); store.write("goals", goals) }, onToggle = { entry -> val i = goals.indexOfFirst { it.id == entry.id }; if (i >= 0) { goals[i] = goals[i].copy(done = !goals[i].done); store.write("goals", goals) } }) { selectedEntry = it; draftTitle = it.title; draftDetail = it.detail; draftPriority = it.priority; showEditor = true }
                    Section.STATS -> StatsScreen(tasks, habits, notes, reminders, goals)
                    Section.SETTINGS -> SettingsScreen(context)
                    Section.TASKS -> EntryScreen("مهامك", "أنجز ما يهمك، خطوة بخطوة", tasks, "لا توجد مهام بعد", onDelete = { entry -> tasks.remove(entry); store.write("tasks", tasks) }, onToggle = { entry -> val i = tasks.indexOfFirst { it.id == entry.id }; if (i >= 0) { tasks[i] = tasks[i].copy(done = !tasks[i].done); store.write("tasks", tasks) } }) { selectedEntry = it; draftTitle = it.title; draftDetail = it.detail; showEditor = true }
                    Section.NOTES -> EntryScreen("مساحة أفكارك", "دوّن أفكارك وارجع إليها متى شئت", notes, "دفتر ملاحظاتك فارغ", onDelete = { entry -> notes.remove(entry); store.write("notes", notes) }) { selectedEntry = it; draftTitle = it.title; draftDetail = it.detail; showEditor = true }
                    Section.REMINDERS -> EntryScreen("تذكيراتك", "احتفظ بالأمور المهمة أمام عينيك", reminders, "لا توجد تذكيرات بعد", onDelete = { entry -> ReminderScheduler.cancel(context, entry.id.toInt()); reminders.remove(entry); store.write("reminders", reminders) }) { selectedEntry = it; draftTitle = it.title; draftDetail = it.detail; showEditor = true }
                    Section.HABITS -> HabitScreen(habits, onAdd = { title -> habits.add(0, Entry(System.currentTimeMillis(), title)); store.write("habits", habits) }, onToggle = { entry -> val i = habits.indexOfFirst { it.id == entry.id }; if (i >= 0) { val today = java.time.LocalDate.now().toString()
                    habits[i] = if (habits[i].detail == today) habits[i].copy(done = false, detail = "") else habits[i].copy(done = true, detail = today)
                    store.write("habits", habits) } }, onDelete = { entry -> habits.remove(entry); store.write("habits", habits) })
                }
                Row(Modifier.fillMaxWidth().background(Color(0xF0120E20)).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Section.values().forEach { item ->
                        Column(Modifier.width(66.dp).clickable { section = item }.background(if (section == item) Color(0x553D2B62) else Color.Transparent, RoundedCornerShape(14.dp)).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(item.symbol, color = if (section == item) Gold else Muted, fontSize = 19.sp)
                            Text(item.title, color = if (section == item) Gold else Muted, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
            }
            if (section == Section.TASKS || section == Section.NOTES || section == Section.REMINDERS || section == Section.GOALS) {
                FloatingActionButton(
                    onClick = { selectedEntry = null; draftTitle = ""; draftDetail = ""; draftPriority = 1; showEditor = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 88.dp, end = 22.dp),
                    containerColor = Violet, contentColor = Night
                ) { Text("+", fontSize = 28.sp) }
            }
        }
    }

    if (showEditor) {
        val heading = if (selectedEntry == null) "إضافة جديد" else "تعديل العنصر"
        AlertDialog(
            onDismissRequest = { showEditor = false },
            containerColor = Color(0xFF211A35),
            title = { Text(heading, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorField(draftTitle, { draftTitle = it }, "العنوان")
                    EditorField(draftDetail, { draftDetail = it }, when (section) { Section.REMINDERS -> "موعد: YYYY-MM-DD HH:mm ثم وصف التذكير"; Section.TASKS -> "تاريخ اختياري: YYYY-MM-DD ثم تفاصيل المهمة"; else -> "التفاصيل (اختياري)" })
                    if (section == Section.TASKS) {
                        Text("أولوية المهمة", color = Muted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1 to "عادية", 2 to "مهمة", 3 to "عاجلة").forEach { (level, label) ->
                                FilterChip(selected = draftPriority == level, onClick = { draftPriority = level }, label = { Text(label) })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val title = draftTitle.trim()
                    if (title.isNotEmpty()) {
                        val list = when (section) { Section.TASKS -> tasks; Section.NOTES -> notes; Section.GOALS -> goals; else -> reminders }
                        val savedId = if (selectedEntry == null) {
                            val newId = System.currentTimeMillis()
                            list.add(0, Entry(newId, title, draftDetail.trim(), priority = if (section == Section.TASKS) draftPriority else 1))
                            newId
                        } else {
                            val existing = selectedEntry!!
                            val index = list.indexOfFirst { it.id == existing.id }
                            if (index >= 0) list[index] = existing.copy(title = title, detail = draftDetail.trim(), priority = if (section == Section.TASKS) draftPriority else existing.priority)
                            existing.id
                        }
                        val key = when (section) { Section.TASKS -> "tasks"; Section.NOTES -> "notes"; Section.GOALS -> "goals"; else -> "reminders" }
                        store.write(key, list)
                        if (section == Section.REMINDERS) {
                            val reminder = list.firstOrNull { it.id == savedId }
                            if (reminder != null) ReminderScheduler.cancel(context, reminder.id.toInt())
                            if (reminder != null && !scheduleReminder(context, reminder.id, reminder.title, reminder.detail)) {
                                Toast.makeText(context, "اكتب الموعد أولاً بهذا الشكل: 2026-11-13 18:30", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    showEditor = false
                }) { Text("حفظ", color = Gold) }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("إلغاء", color = Muted) } }
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.HomeScreen(tasks: List<Entry>, notes: List<Entry>, reminders: List<Entry>, navigate: (Section) -> Unit) {
    val completed = tasks.count { it.done }
    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("مساحة هادئة ليوم أكثر إنجازاً", color = Muted, fontSize = 14.sp)
            HeroLandscape()
            Spacer(Modifier.height(8.dp))
            GlassCard {
                Text("إنجاز المهام", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$completed", color = Gold, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Text(" / ${tasks.size} مكتملة", color = Muted, modifier = Modifier.padding(start = 8.dp, bottom = 7.dp))
                }
                LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Violet, trackColor = Color(0xFF44385A))
            }
        }
        item { SectionHeader("مساحتك اليومية", "كل شيء في مكانه") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("المهام", tasks.size.toString(), "خطط وأنجز", Modifier.weight(1f)) { navigate(Section.TASKS) }
                SummaryCard("الملاحظات", notes.size.toString(), "أفكارك محفوظة", Modifier.weight(1f)) { navigate(Section.NOTES) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("التذكيرات", reminders.size.toString(), "لا تنس المهم", Modifier.weight(1f)) { navigate(Section.REMINDERS) }
                SummaryCard("العادات", "∞", "ابنِ روتينك", Modifier.weight(1f)) { navigate(Section.HABITS) }
            }
        }
        item { SectionHeader("أقرب مهامك", "قائمة سريعة") }
        if (tasks.isEmpty()) item { EmptyState("ابدأ بإضافة أول مهمة إلى يومك") }
        items(tasks.sortedWith(compareBy<Entry> { it.done }.thenByDescending { it.priority }.thenByDescending { it.id }).take(3), key = { it.id }) { entry ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (entry.done) "✓" else "○", color = if (entry.done) Gold else Violet, fontSize = 22.sp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(entry.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                        if (entry.detail.isNotBlank()) Text(entry.detail, color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.EntryScreen(title: String, subtitle: String, entries: MutableList<Entry>, empty: String, onDelete: (Entry) -> Unit, onToggle: ((Entry) -> Unit)? = null, edit: (Entry) -> Unit) {
    val orderedEntries = if (title == "مهامك") entries.sortedWith(compareBy<Entry> { it.done }.thenByDescending { it.priority }.thenByDescending { it.id }) else entries.toList()
    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(subtitle, color = Muted, fontSize = 13.sp) }
        if (entries.isEmpty()) item { EmptyState(empty) }
        items(orderedEntries, key = { it.id }) { entry ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { edit(entry) }) {
                        Text(entry.title, color = if (entry.done) Muted else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (entry.priority > 1) Text(if (entry.priority == 3) "أولوية عاجلة" else "أولوية مهمة", color = if (entry.priority == 3) Color(0xFFFF9A9A) else Gold, fontSize = 10.sp)
                        if (entry.detail.isNotBlank()) Text(entry.detail, color = Muted, fontSize = 12.sp)
                    }
                    if (onToggle != null) Text(if (entry.done) "✓" else "○", color = if (entry.done) Gold else Violet, fontSize = 22.sp, modifier = Modifier.clickable { onToggle(entry) }.padding(8.dp))
                    Text("تحرير", color = Gold, fontSize = 12.sp, modifier = Modifier.clickable { edit(entry) }.padding(8.dp))
                    Text("×", color = Color(0xFFFF8F9B), fontSize = 20.sp, modifier = Modifier.clickable { onDelete(entry) }.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.HabitScreen(
    habits: List<Entry>, onAdd: (String) -> Unit, onToggle: (Entry) -> Unit, onDelete: (Entry) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard {
            Text("عاداتك تصنع يومك", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("تابع التزامك اليومي وسجّل إنجازاتك.", color = Muted, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                EditorField(draft, { draft = it }, "عادة جديدة")
                TextButton(onClick = { if (draft.isNotBlank()) { onAdd(draft.trim()); draft = "" } }) { Text("إضافة", color = Gold) }
            }
        }
        if (habits.isEmpty()) EmptyState("أضف أول عادة تريد الالتزام بها")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(habits, key = { it.id }) { habit ->
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (habit.done) "✓" else "○", color = if (habit.done) Gold else Violet, fontSize = 22.sp, modifier = Modifier.clickable { onToggle(habit) })
                        Text(habit.title, color = Color.White, modifier = Modifier.weight(1f).padding(start = 12.dp))
                        Text("حذف", color = Color(0xFFFF8F9B), modifier = Modifier.clickable { onDelete(habit) }.padding(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorField(value: String, onValueChange: (String) -> Unit, hint: String) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF171226), RoundedCornerShape(14.dp)).padding(13.dp)) {
        if (value.isEmpty()) Text(hint, color = Muted, fontSize = 13.sp)
        BasicTextField(value, onValueChange, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp))
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(subtitle, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun SummaryCard(title: String, value: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.clickable(onClick = onClick)) {
        Text(title, color = Muted, fontSize = 12.sp)
        Text(value, color = Gold, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun EmptyState(message: String) {
    GlassCard {
        Text("مساحتك بانتظارك", color = Gold, fontWeight = FontWeight.SemiBold)
        Text(message, color = Muted, fontSize = 13.sp, textAlign = TextAlign.Start)
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), color = Panel, shape = RoundedCornerShape(22.dp), tonalElevation = 3.dp) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CalendarScreen(tasks: List<Entry>, context: Context) {
    val today = remember { java.time.LocalDate.now() }
    var selected by remember { mutableStateOf(today) }
    var displayedMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }
    val saturdayStart = remember { context.getSharedPreferences("dayflow_settings", Context.MODE_PRIVATE).getBoolean("week_saturday", true) }
    val first = displayedMonth
    val days = first.lengthOfMonth()
    Column(Modifier.fillMaxWidth().weight(1f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Gold, fontSize = 26.sp, modifier = Modifier.clickable { displayedMonth = displayedMonth.minusMonths(1); selected = displayedMonth }.padding(horizontal = 10.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(displayedMonth.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("ar")), color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(displayedMonth.year.toString(), color = Muted)
                }
                Text("›", color = Gold, fontSize = 26.sp, modifier = Modifier.clickable { displayedMonth = displayedMonth.plusMonths(1); selected = displayedMonth }.padding(horizontal = 10.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (if (saturdayStart) listOf("س", "ح", "ن", "ث", "ر", "خ", "ج") else listOf("ح", "ن", "ث", "ر", "خ", "ج", "س")).forEach { Text(it, color = Muted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
            }
            val offset = if (saturdayStart) (first.dayOfWeek.value + 1) % 7 else first.dayOfWeek.value % 7
            for (week in 0..((offset + days - 1) / 7)) {
                Row(Modifier.fillMaxWidth()) {
                    for (day in 0..6) {
                        val number = week * 7 + day - offset + 1
                        Box(Modifier.weight(1f).padding(2.dp).aspectRatio(1f).background(if (number in 1..days && selected.dayOfMonth == number) Color(0xFF624B91) else Color.Transparent, RoundedCornerShape(12.dp)).clickable { if (number in 1..days) selected = first.withDayOfMonth(number) }, contentAlignment = Alignment.Center) {
                            if (number in 1..days) Text(number.toString(), color = if (selected.dayOfMonth == number) Color.White else Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        Text("مهام ${selected.dayOfMonth} ${selected.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("ar"))}", color = Color.White, fontWeight = FontWeight.Bold)
        val dayTasks = tasks.filter { it.detail.contains(selected.toString()) }
        if (dayTasks.isEmpty()) EmptyState("لا توجد مهام مرتبطة بهذا اليوم. أضف التاريخ في تفاصيل المهمة.")
        else dayTasks.forEach { GlassCard { Text(it.title, color = Color.White) } }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.FocusScreen() {
    var seconds by remember { mutableIntStateOf(25 * 60) }
    var running by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(25) }
    LaunchedEffect(running, seconds) {
        if (running && seconds > 0) { delay(1000); seconds -= 1 }
        if (seconds == 0) running = false
    }
    Column(Modifier.fillMaxWidth().weight(1f).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        GlassCard {
            Text("مساحة التركيز", color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("ابتعد عن المشتتات وامنح مهمتك وقتها", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Text(String.format("%02d:%02d", seconds / 60, seconds % 60), color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            LinearProgressIndicator(progress = { 1f - seconds.toFloat() / (duration * 60f) }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Violet)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = { running = !running }) { Text(if (running) "إيقاف مؤقت" else "ابدأ التركيز", color = Gold) }
                TextButton(onClick = { running = false; seconds = duration * 60 }) { Text("إعادة", color = Muted) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                listOf(15, 25, 45, 60).forEach { m -> TextButton(onClick = { running = false; duration = m; seconds = m * 60 }) { Text("$m د", color = if (duration == m) Gold else Muted) } }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.StatsScreen(tasks: List<Entry>, habits: List<Entry>, notes: List<Entry>, reminders: List<Entry>, goals: List<Entry>) {
    val done = tasks.count { it.done }
    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("نظرة على تقدمك", color = Muted) }
        item { GlassCard { Text("إنجاز المهام", color = Muted); Text("$done من ${tasks.size}", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold); LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth(), color = Violet) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SummaryCard("العادات", habits.size.toString(), "عادات مسجلة", Modifier.weight(1f)) {}; SummaryCard("الأهداف", goals.size.toString(), "أهدافك", Modifier.weight(1f)) {} } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SummaryCard("الملاحظات", notes.size.toString(), "ملاحظات", Modifier.weight(1f)) {}; SummaryCard("التذكيرات", reminders.size.toString(), "تذكيرات", Modifier.weight(1f)) {} } }
        item { EmptyState("تُحسب الإحصائيات من بياناتك المحفوظة على هذا الجهاز.") }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.SettingsScreen(context: Context) {
    val prefs = remember { context.getSharedPreferences("dayflow_settings", Context.MODE_PRIVATE) }
    var weekStart by remember { mutableStateOf(prefs.getBoolean("week_saturday", true)) }
    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { GlassCard { Text("DAYFLOW", color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("إصدار 1.0.0", color = Muted); Text("صُنع بعناية بواسطة DSTWR", color = Muted, fontSize = 12.sp) } }
        item { GlassCard { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("بداية الأسبوع", color = Color.White); Text("السبت بدلاً من الأحد", color = Muted, fontSize = 11.sp) }; Switch(weekStart, { weekStart = it; prefs.edit().putBoolean("week_saturday", it).apply() }) } } }
        item { GlassCard {
            Text("إشعارات التطبيق", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("تحكم في صلاحيات التنبيهات من إعدادات Android.", color = Muted, fontSize = 11.sp)
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("إدارة الإشعارات", color = Gold) }
        } }
        item { EmptyState("بياناتك محفوظة محلياً على جهازك. لا يحتاج التطبيق إلى حساب أو خادم.") }
    }
}

private fun scheduleReminder(context: Context, id: Long, title: String, details: String): Boolean {
    val dateText = details.take(16)
    val trigger = try {
        java.time.LocalDateTime.parse(dateText, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: Exception) { return false }
    if (trigger <= System.currentTimeMillis()) return false
    return ReminderScheduler.schedule(context, id.toInt(), title, details.drop(16).trim().ifBlank { "حان موعد تذكيرك" }, trigger)
}

@Composable
private fun HeroLandscape() {
    Box(Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(24.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Brush.linearGradient(listOf(Color(0xFF4C3474), Color(0xFF171329), Color(0xFF0D0B18)), Offset.Zero, Offset(w, h)))
            drawCircle(Color(0x66E7C78A), radius = 25.dp.toPx(), center = Offset(w * 0.78f, h * 0.28f))
            val far = Path().apply {
                moveTo(0f, h * 0.78f); lineTo(w * 0.20f, h * 0.42f); lineTo(w * 0.36f, h * 0.68f)
                lineTo(w * 0.53f, h * 0.34f); lineTo(w * 0.76f, h * 0.73f); lineTo(w, h * 0.46f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(far, Brush.linearGradient(listOf(Color(0xFF75609A), Color(0xFF29213F)), Offset.Zero, Offset(0f, h)))
            val near = Path().apply {
                moveTo(0f, h * 0.78f); lineTo(w * 0.27f, h * 0.62f); lineTo(w * 0.48f, h * 0.39f)
                lineTo(w * 0.70f, h * 0.78f); lineTo(w, h * 0.58f); lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(near, Brush.linearGradient(listOf(Color(0xFF33264E), Color(0xFF100D1D)), Offset(0f, h * 0.35f), Offset(0f, h)))
            val summit = Offset(w * 0.48f, h * 0.39f)
            drawCircle(Gold, radius = 3.5.dp.toPx(), center = Offset(summit.x, summit.y - 13.dp.toPx()))
            drawLine(Gold, Offset(summit.x, summit.y - 8.dp.toPx()), Offset(summit.x, summit.y + 2.dp.toPx()), strokeWidth = 2.dp.toPx())
            drawLine(Gold, Offset(summit.x, summit.y - 4.dp.toPx()), Offset(summit.x - 5.dp.toPx(), summit.y + 1.dp.toPx()), strokeWidth = 1.5.dp.toPx())
            drawLine(Gold, Offset(summit.x, summit.y - 4.dp.toPx()), Offset(summit.x + 5.dp.toPx(), summit.y + 1.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text("خطوة صغيرة، يوم مختلف", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("رتّب أولوياتك وابدأ من هنا", color = Color(0xFFE4D9F3), fontSize = 11.sp)
        }
    }
}
