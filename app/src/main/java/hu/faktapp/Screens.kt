@file:OptIn(ExperimentalMaterial3Api::class)

package hu.faktapp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ERA_OPTIONS: List<Pair<String, String?>> = listOf(
    "Minden típus" to null,
    "Digitális kultúra" to "digkult",
    "Informatika (régi)" to "informatika"
)

private val STATUS_OPTIONS: List<Pair<String, Status?>> = listOf(
    "Mind" to null,
    "Nincs kijelölve" to Status.NONE,
    "Órán" to Status.CLASS,
    "Dolgozat" to Status.TEST,
    "Házi" to Status.HOMEWORK
)

private val ASSIGNED_STATUS_OPTIONS: List<Pair<String, Status?>> = listOf(
    "Mind" to null,
    "Órán" to Status.CLASS,
    "Dolgozat" to Status.TEST,
    "Házi" to Status.HOMEWORK
)

private fun topicOptions(topics: List<String>): List<Pair<String, String?>> {
    val list = ArrayList<Pair<String, String?>>()
    list.add("Minden témakör" to null)
    for (t in topics) list.add(t to t)
    return list
}

/* ------------------------------------------------------------------ */
/* 1. képernyő: Feladatok                                              */
/* ------------------------------------------------------------------ */

@Composable
fun FeladatokScreen(vm: MainViewModel, onOpen: (Task) -> Unit) {
    var searching by rememberSaveable { mutableStateOf(false) }

    val topicsHere = TOPICS.filter { tp -> vm.tasks.any { it.level == vm.level && tp in it.topics } }
    val q = vm.query.trim()

    val filtered = vm.tasks.filter { t ->
        t.level == vm.level &&
            (vm.era == null || t.era == vm.era) &&
            (vm.statusFilter == null || vm.statusOf(t.id) == vm.statusFilter) &&
            (q.isEmpty() || t.title.contains(q, ignoreCase = true) || t.sessionLabel.contains(q, ignoreCase = true))
    }

    val entries = ArrayList<ListEntry>()
    for (tp in topicsHere) {
        if (vm.topic != null && vm.topic != tp) continue
        val rows = filtered.filter { tp in it.topics }.sortedByDescending { it.sortKey }
        if (rows.isEmpty()) continue
        entries.add(HeaderEntry(tp, rows.count { vm.statusOf(it.id) != Status.NONE }, rows.size))
        for (t in rows) entries.add(TaskEntry(t, tp))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                if (searching) {
                    TextField(
                        value = vm.query,
                        onValueChange = { vm.query = it },
                        placeholder = { Text("Keresés cím vagy év szerint") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                } else {
                    Text("Fakt-App", fontWeight = FontWeight.Bold)
                }
            },
            actions = {
                IconButton(onClick = {
                    if (searching) vm.query = ""
                    searching = !searching
                }) {
                    Icon(
                        if (searching) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Keresés"
                    )
                }
            }
        )

        TabRow(selectedTabIndex = if (vm.level == "kozep") 0 else 1) {
            Tab(
                selected = vm.level == "kozep",
                onClick = { vm.level = "kozep" },
                text = { Text("KÖZÉPSZINT") }
            )
            Tab(
                selected = vm.level == "emelt",
                onClick = { vm.level = "emelt" },
                text = { Text("EMELT SZINT") }
            )
        }

        Spacer(Modifier.height(6.dp))
        ChipRow<String>(topicOptions(topicsHere), vm.topic) { vm.topic = it }
        ChipRow<String>(ERA_OPTIONS, vm.era) { vm.era = it }
        ChipRow<Status>(STATUS_OPTIONS, vm.statusFilter) { vm.statusFilter = it }
        Spacer(Modifier.height(6.dp))

        if (entries.isEmpty()) {
            Text(
                "Nincs a szűrőknek megfelelő feladat.",
                modifier = Modifier.padding(16.dp),
                color = Color(0xFF607D8B)
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries, key = { e ->
                when (e) {
                    is HeaderEntry -> "h_" + e.title
                    is TaskEntry -> e.task.id + "|" + e.topic
                }
            }) { e ->
                when (e) {
                    is HeaderEntry -> SectionHeader(
                        title = e.title,
                        subtitle = "${e.marked} / ${e.total} kijelölve"
                    )
                    is TaskEntry -> {
                        val mark = vm.marks[e.task.id]
                        val parts = ArrayList<String>()
                        if (vm.era == null) parts.add(e.task.eraLabel)
                        if (e.task.topics.size > 1) parts.add("közös: " + e.task.topics.joinToString(" + "))
                        if (e.task.nosrc) parts.add("nincs forrásfájl")
                        if (mark != null && mark.note.isNotEmpty()) parts.add("✎ " + mark.note)
                        TaskRow(e.task, mark, parts.joinToString(" · ")) { onOpen(e.task) }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 2. képernyő: Kiadott feladatok                                      */
/* ------------------------------------------------------------------ */

@Composable
fun KiadottakScreen(vm: MainViewModel, onOpen: (Task) -> Unit) {
    val context = LocalContext.current

    val assigned = vm.tasks.filter { t ->
        val st = vm.statusOf(t.id)
        st != Status.NONE &&
            (vm.aLevel == null || t.level == vm.aLevel) &&
            (vm.aTopic == null || (vm.aTopic as String) in t.topics) &&
            (vm.aStatus == null || st == vm.aStatus)
    }

    val groups = ArrayList<Pair<Status, List<Task>>>()
    for (s in listOf(Status.CLASS, Status.TEST, Status.HOMEWORK)) {
        val list = assigned
            .filter { vm.statusOf(it.id) == s }
            .sortedByDescending { vm.marks[it.id]?.updated ?: 0L }
        if (list.isNotEmpty()) groups.add(s to list)
    }

    fun buildShareText(): String {
        val sb = StringBuilder()
        sb.append("Kiadott feladatok (Fakt-App)\n")
        for ((s, list) in groups) {
            sb.append("\n== ").append(s.label).append(" (").append(list.size).append(") ==\n")
            for (t in list) {
                sb.append("- ").append(t.sessionLabel).append(" · ").append(t.levelLabel)
                    .append(" · ").append(t.topics.joinToString(" + "))
                    .append(": ").append(t.title)
                val note = vm.marks[t.id]?.note ?: ""
                if (note.isNotEmpty()) sb.append(" [").append(note).append("]")
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Kiadott feladatok", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = {
                    if (assigned.isEmpty()) {
                        Toast.makeText(context, "Nincs megosztható feladat.", Toast.LENGTH_SHORT).show()
                    } else {
                        shareText(context, buildShareText())
                    }
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Lista megosztása")
                }
            }
        )

        ChipRow<String>(
            listOf("Közép + emelt" to null, "Középszint" to "kozep", "Emelt szint" to "emelt"),
            vm.aLevel
        ) { vm.aLevel = it }
        ChipRow<String>(topicOptions(TOPICS), vm.aTopic) { vm.aTopic = it }
        ChipRow<Status>(ASSIGNED_STATUS_OPTIONS, vm.aStatus) { vm.aStatus = it }
        Spacer(Modifier.height(6.dp))

        Text(
            "Összesen ${assigned.size} feladat",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            fontSize = 13.sp,
            color = Color(0xFF607D8B)
        )

        if (assigned.isEmpty()) {
            Text(
                "Még nincs kijelölt feladat ezekkel a szűrőkkel. A Feladatok fülön kattints egy feladatra a kijelöléshez.",
                modifier = Modifier.padding(16.dp),
                color = Color(0xFF607D8B)
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            for ((s, list) in groups) {
                item(key = "gh_" + s.name) {
                    SectionHeader(title = s.label, subtitle = "${list.size} db", color = s.strong)
                }
                items(list, key = { s.name + "|" + it.id }) { t ->
                    val mark = vm.marks[t.id]
                    val parts = ArrayList<String>()
                    parts.add(t.levelLabel + " · " + t.topics.joinToString(" + "))
                    if (mark != null && mark.updated > 0L) parts.add(formatDate(mark.updated))
                    if (mark != null && mark.note.isNotEmpty()) parts.add("✎ " + mark.note)
                    TaskRow(t, mark, parts.joinToString(" · ")) { onOpen(t) }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 3. képernyő: Áttekintés + biztonsági mentés                         */
/* ------------------------------------------------------------------ */

@Composable
fun AttekintesScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var showImport by rememberSaveable { mutableStateOf(false) }
    var importText by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Áttekintés", fontWeight = FontWeight.Bold) })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for ((levelKey, levelName) in listOf("kozep" to "Középszint", "emelt" to "Emelt szint")) {
                val levelTasks = vm.tasks.filter { it.level == levelKey }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(levelName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "${levelTasks.size} feladat összesen",
                            fontSize = 13.sp,
                            color = Color(0xFF607D8B)
                        )
                        Spacer(Modifier.height(8.dp))
                        for (s in listOf(Status.CLASS, Status.TEST, Status.HOMEWORK)) {
                            val n = levelTasks.count { vm.statusOf(it.id) == s }
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(s)
                                Text("  ${s.label}: $n", fontSize = 14.sp)
                            }
                        }
                        val free = levelTasks.count { vm.statusOf(it.id) == Status.NONE }
                        Text(
                            "Még ki nem osztott: $free",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(Modifier.height(10.dp))
                        Text("Témakörönként (kijelölt / összes)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        for (tp in TOPICS) {
                            val inTopic = levelTasks.filter { tp in it.topics }
                            if (inTopic.isEmpty()) continue
                            val marked = inTopic.count { vm.statusOf(it.id) != Status.NONE }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                Text(tp, modifier = Modifier.weight(1f), fontSize = 14.sp)
                                Text("$marked / ${inTopic.size}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Biztonsági mentés", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        "A kijelöléseid a telefonon tárolódnak. Új telefonra vagy újratelepítés előtt mentsd ki őket szövegként (pl. e-mailbe vagy Keepbe), és ott illeszd be vissza.",
                        fontSize = 13.sp,
                        color = Color(0xFF607D8B),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Button(
                        onClick = {
                            if (vm.marks.isEmpty()) {
                                Toast.makeText(context, "Még nincs mit menteni.", Toast.LENGTH_SHORT).show()
                            } else {
                                shareText(context, Backup.toJson(vm.marks.toMap()))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Kijelölések mentése (megosztás)") }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { showImport = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Visszaállítás beillesztett szövegből") }
                }
            }

            Text(
                "Feladatok: ${vm.tasks.size} db · forrás: informatika.fazekas.hu",
                fontSize = 12.sp,
                color = Color(0xFF90A4AE),
                modifier = Modifier.padding(4.dp)
            )
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Visszaállítás") },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Illeszd be a mentett szöveget") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = vm.importMarks(importText)
                    if (n == null) {
                        Toast.makeText(context, "Hibás szöveg, nem sikerült beolvasni.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "$n kijelölés visszaállítva.", Toast.LENGTH_LONG).show()
                        importText = ""
                        showImport = false
                    }
                }) { Text("Visszaállítás") }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }) { Text("Mégse") }
            }
        )
    }
}
