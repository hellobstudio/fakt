@file:OptIn(ExperimentalMaterial3Api::class)

package hu.faktapp

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ListEntry
data class HeaderEntry(val title: String, val marked: Int, val total: Int) : ListEntry
data class TaskEntry(val task: Task, val topic: String) : ListEntry

fun formatDate(millis: Long): String =
    SimpleDateFormat("yyyy.MM.dd.", Locale.getDefault()).format(Date(millis))

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Megosztás"))
}

/** Vízszintesen görgethető szűrőchip-sor. A null érték a "Mind" opció. */
@Composable
fun <T> ChipRow(options: List<Pair<String, T?>>, selected: T?, onSelect: (T?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { opt ->
            FilterChip(
                selected = selected == opt.second,
                onClick = { onSelect(opt.second) },
                label = { Text(opt.first) }
            )
        }
    }
}

@Composable
fun StatusBadge(status: Status) {
    Box(
        modifier = Modifier
            .background(status.strong, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(status.short, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String, color: Color = Color(0xFF37474F)) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFECEFF1))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(subtitle, color = Color(0xFF607D8B), fontSize = 12.sp)
    }
}

/** Egy táblázatsor: [év / időszak] [cím + részletek] [állapotjelvény]. */
@Composable
fun TaskRow(task: Task, mark: Mark?, subtitle: String, onClick: () -> Unit) {
    val status = mark?.status ?: Status.NONE
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(status.soft)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(88.dp)) {
                Text(task.year.toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    task.month + if (task.idegen) " (id.)" else "",
                    fontSize = 12.sp,
                    color = Color(0xFF546E7A)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF607D8B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (status != Status.NONE) {
                Spacer(Modifier.width(8.dp))
                StatusBadge(status)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0x1F000000))
        )
    }
}

/** Részletek + kijelölés párbeszédablak. Státuszgombra kattintva azonnal ment és bezár. */
@Composable
fun TaskDialog(task: Task, mark: Mark?, onSave: (Status, String) -> Unit, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var note by remember(task.id) { mutableStateOf(mark?.note ?: "") }
    val current = mark?.status ?: Status.NONE

    fun open(url: String) {
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            // nincs böngésző / hibás link: nem csinálunk semmit
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "${task.levelLabel} · ${task.eraLabel} · ${task.sessionLabel}\n${task.topics.joinToString(" + ")}",
                    fontSize = 13.sp,
                    color = Color(0xFF546E7A)
                )
                Spacer(Modifier.height(12.dp))

                for (s in listOf(Status.CLASS, Status.TEST, Status.HOMEWORK)) {
                    Button(
                        onClick = { onSave(s, note) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = s.strong)
                    ) {
                        Text((if (current == s) "✓ " else "") + s.label)
                    }
                }
                OutlinedButton(
                    onClick = { onSave(Status.NONE, note) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(if (current == Status.NONE) "✓ Nincs kijelölve" else "Kijelölés törlése")
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Jegyzet (pl. osztály, határidő)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                if (mark != null && mark.updated > 0L) {
                    Text(
                        "Utolsó módosítás: ${formatDate(mark.updated)}",
                        fontSize = 12.sp,
                        color = Color(0xFF607D8B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { open(task.pdf) }) {
                    Text("Feladatlap megnyitása (PDF)")
                }
                for (f in task.files) {
                    TextButton(onClick = { open(f.url) }) {
                        Text("Forrásfájl: ${f.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (task.nosrc) {
                    Text(
                        "Ehhez a feladathoz nincs forrásfájl.",
                        fontSize = 12.sp,
                        color = Color(0xFF607D8B)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(current, note) }) { Text("Jegyzet mentése") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bezár") }
        }
    )
}
