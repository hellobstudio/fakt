package hu.faktapp

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val tasks: List<Task> = TaskRepository.load(app)
    private val store = MarkStore(app)
    private val validIds: Set<String> = tasks.map { it.id }.toSet()

    val marks = mutableStateMapOf<String, Mark>()

    // "Feladatok" képernyő szűrői
    var level by mutableStateOf("kozep")
    var era by mutableStateOf<String?>(null)
    var topic by mutableStateOf<String?>(null)
    var statusFilter by mutableStateOf<Status?>(null)
    var query by mutableStateOf("")

    // "Kiadottak" képernyő szűrői
    var aLevel by mutableStateOf<String?>(null)
    var aTopic by mutableStateOf<String?>(null)
    var aStatus by mutableStateOf<Status?>(null)

    init {
        marks.putAll(store.loadAll())
    }

    fun statusOf(id: String): Status = marks[id]?.status ?: Status.NONE

    fun setMark(id: String, status: Status, note: String) {
        val n = note.trim()
        if (status == Status.NONE && n.isEmpty()) {
            marks.remove(id)
            store.remove(id)
        } else {
            val m = Mark(status, n, System.currentTimeMillis())
            marks[id] = m
            store.put(id, m)
        }
    }

    /** Visszaállítás biztonsági mentésből; a visszaadott szám a beolvasott kijelölések száma. */
    fun importMarks(text: String): Int? {
        val data = Backup.fromJson(text) ?: return null
        var count = 0
        for ((id, m) in data) {
            if (id in validIds) {
                marks[id] = m
                store.put(id, m)
                count++
            }
        }
        return count
    }
}
