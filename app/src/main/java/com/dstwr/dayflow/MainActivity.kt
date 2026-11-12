package com.dstwr.dayflow

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        setContent { DayflowApp(this) }
    }
}

private enum class Section(val title: String, val symbol: String) {
    HOME("الرئيسية", "⌂"), TASKS("المهام", "✓"), NOTES("الملاحظات", "▤"),
    REMINDERS("التذكيرات", "◷"), HABITS("العادات", "◇")
}

private data class Entry(val id: Long, val title: String, val detail: String = "", val done: Boolean = false)

private class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("dayflow_local", Context.MODE_PRIVATE)
    fun read(key: String): List<Entry> {
        val result = mutableListOf<Entry>()
        val array = try { JSONArray(prefs.getString(key, "[]")) } catch (_: Exception) { JSONArray() }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            result.add(Entry(item.optLong("id"), item.optString("title"), item.optString("detail"), item.optBoolean("done")))
        }
        return result
    }
    fun write(key: String, entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().put("id", entry.id).put("title", entry.title).put("detail", entry.detail).put("done", entry.done))
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
    val habits = remember { mutableStateListOf<Entry>().apply { addAll(store.read("habits")) } }
    var section by remember { mutableStateOf(Section.HOME) }
    var showEditor by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf("") }
    var draftDetail by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<Entry?>(null) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, secondary = Gold, background = Night, surface = Panel)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF21133D), Night)))) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("DAYFLOW", color = Gold, fontSize = 12.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                        Text(section.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("DSTWR", color = Muted, fontSize = 11.sp)
                }
                when (section) {
                    Section.HOME -> HomeScreen(tasks, notes, reminders) { section = it }
                    Section.TASKS -> EntryScreen("مهامك", "أنجز ما يهمك، خطوة بخطوة", tasks, "لا توجد مهام بعد", onDelete = { store.write("tasks", tasks) }, onToggle = { entry -> val i = tasks.indexOfFirst { it.id == entry.id }; if (i >= 0) { tasks[i] = tasks[i].copy(done = !tasks[i].done); store.write("tasks", tasks) } }) { selectedEntry = it; draftTitle = it.title; draftDetail = it.detail; showEditor = true }
                    Section.NOTES -> EntryScreen("مساحة أفكارك", "دوّن أفكارك وارجع إليها متى شئت", notes, "دفتر ملاحظاتك فارغ", onDelete = { store.write("notes", notes) }) { selectedEntry = it; draftTitle = it.title; draftDetail = it.detail; showEditor = true }
                    Section.REMINDERS -> EntryScreen("تذكيراتك", "احتفظ بالأمور المهمة أمام عينيك", reminders, "لا توجد تذكيرات بعد", onDelete = { store.write("reminders", reminders) }) { selectedEntry = it; draftTitle = it.title; draftDetail = it.detail; showEditor = true }
                    Section.HABITS -> HabitScreen(habits, onAdd = { title -> habits.add(0, Entry(System.currentTimeMillis(), title)); store.write("habits", habits) }, onToggle = { entry -> val i = habits.indexOfFirst { it.id == entry.id }; if (i >= 0) { habits[i] = habits[i].copy(done = !habits[i].done); store.write("habits", habits) } }, onDelete = { entry -> habits.remove(entry); store.write("habits", habits) })
                }
                Spacer(Modifier.weight(1f))
                NavigationBar(containerColor = Color(0xF0120E20), contentColor = Muted) {
                    Section.values().forEach { item ->
                        NavigationBarItem(
                            selected = section == item,
                            onClick = { section = item },
                            icon = { Text(item.symbol, fontSize = 19.sp) },
                            label = { Text(item.title, fontSize = 9.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Gold, selectedTextColor = Gold, indicatorColor = Color(0x553D2B62), unselectedIconColor = Muted, unselectedTextColor = Muted)
                        )
                    }
                }
            }
            if (section == Section.TASKS || section == Section.NOTES || section == Section.REMINDERS) {
                FloatingActionButton(
                    onClick = { selectedEntry = null; draftTitle = ""; draftDetail = ""; showEditor = true },
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
                    EditorField(draftDetail, { draftDetail = it }, "التفاصيل (اختياري)")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val title = draftTitle.trim()
                    if (title.isNotEmpty()) {
                        val list = when (section) { Section.TASKS -> tasks; Section.NOTES -> notes; else -> reminders }
                        if (selectedEntry == null) list.add(0, Entry(System.currentTimeMillis(), title, draftDetail.trim()))
                        else {
                            val index = list.indexOfFirst { it.id == selectedEntry!!.id }
                            if (index >= 0) list[index] = selectedEntry!!.copy(title = title, detail = draftDetail.trim())
                        }
                        val key = when (section) { Section.TASKS -> "tasks"; Section.NOTES -> "notes"; else -> "reminders" }
                        store.write(key, list)
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
        items(tasks.take(3), key = { it.id }) { entry ->
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
private fun androidx.compose.foundation.layout.ColumnScope.EntryScreen(title: String, subtitle: String, entries: MutableList<Entry>, empty: String, onDelete: () -> Unit, onToggle: ((Entry) -> Unit)? = null, edit: (Entry) -> Unit) {
    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(subtitle, color = Muted, fontSize = 13.sp) }
        if (entries.isEmpty()) item { EmptyState(empty) }
        items(entries.toList(), key = { it.id }) { entry ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { edit(entry) }) {
                        Text(entry.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (entry.detail.isNotBlank()) Text(entry.detail, color = Muted, fontSize = 12.sp)
                    }
                    if (onToggle != null) Text(if (entry.done) "✓" else "○", color = if (entry.done) Gold else Violet, fontSize = 22.sp, modifier = Modifier.clickable { onToggle(entry) }.padding(8.dp))
                    Text("تحرير", color = Gold, fontSize = 12.sp, modifier = Modifier.clickable { edit(entry) }.padding(8.dp))
                    Text("×", color = Color(0xFFFF8F9B), fontSize = 20.sp, modifier = Modifier.clickable { entries.remove(entry); onDelete() }.padding(8.dp))
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
