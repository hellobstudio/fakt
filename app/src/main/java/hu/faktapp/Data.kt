package hu.faktapp

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/** A feladat kijelölési állapota (színekkel). */
enum class Status(val label: String, val short: String, val strong: Color, val soft: Color) {
    NONE("Nincs kijelölve", "–", Color(0xFF757575), Color(0x00000000)),
    CLASS("Órán megcsináltuk", "Órán", Color(0xFF2E7D32), Color(0xFFDDF1DE)),
    TEST("Dolgozat", "Dolgozat", Color(0xFFC62828), Color(0xFFFADBDB)),
    HOMEWORK("Házi feladat", "Házi", Color(0xFF1565C0), Color(0xFFDBEAFB))
}

/** Témakörök kanonikus sorrendje. */
val TOPICS = listOf("Szöveg", "Prezentáció, grafika", "Weblap", "Táblázat", "Adatbázis", "Programozás")

data class TaskFile(val name: String, val url: String)

data class Task(
    val id: String,
    val level: String,      // "kozep" | "emelt"
    val era: String,        // "digkult" | "informatika"
    val year: Int,
    val month: String,      // "május" | "október" | "február"
    val idegen: Boolean,
    val topics: List<String>,
    val title: String,
    val pdf: String,
    val files: List<TaskFile>,
    val nosrc: Boolean
) {
    val sessionLabel: String get() = "$year $month" + if (idegen) " (idegen)" else ""
    val levelLabel: String get() = if (level == "emelt") "Emelt" else "Közép"
    val eraLabel: String get() = if (era == "digkult") "Dig. kultúra" else "Informatika"

    /** Időrendi rendezéshez: nagyobb = újabb. */
    val sortKey: Int
        get() {
            val m = when (month) {
                "október" -> 3
                "május" -> 2
                else -> 1
            }
            return year * 100 + m * 10 + (if (idegen) 0 else 1)
        }
}

data class Mark(val status: Status, val note: String, val updated: Long)

fun statusFrom(name: String): Status = Status.values().firstOrNull { it.name == name } ?: Status.NONE

object TaskRepository {
    fun load(context: Context): List<Task> {
        val text = context.assets.open("tasks.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        val list = ArrayList<Task>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val tj = o.getJSONArray("topics")
            val topics = (0 until tj.length()).map { tj.getString(it) }
            val fj = o.getJSONArray("files")
            val files = (0 until fj.length()).map {
                val f = fj.getJSONObject(it)
                TaskFile(f.getString("name"), f.getString("url"))
            }
            list.add(
                Task(
                    id = o.getString("id"),
                    level = o.getString("level"),
                    era = o.getString("era"),
                    year = o.getInt("year"),
                    month = o.getString("month"),
                    idegen = o.getBoolean("idegen"),
                    topics = topics,
                    title = o.getString("title"),
                    pdf = o.getString("pdf"),
                    files = files,
                    nosrc = o.optBoolean("nosrc", false)
                )
            )
        }
        return list
    }
}

/** A kijelölések tartós tárolása (SharedPreferences, azonosító -> JSON). */
class MarkStore(context: Context) {
    private val prefs = context.getSharedPreferences("marks", Context.MODE_PRIVATE)

    fun loadAll(): Map<String, Mark> {
        val res = LinkedHashMap<String, Mark>()
        for ((k, v) in prefs.all) {
            if (v is String) {
                try {
                    val o = JSONObject(v)
                    res[k] = Mark(statusFrom(o.optString("s", "NONE")), o.optString("n", ""), o.optLong("t", 0L))
                } catch (e: Exception) {
                    // sérült bejegyzés kihagyása
                }
            }
        }
        return res
    }

    fun put(id: String, m: Mark) {
        val json = JSONObject()
            .put("s", m.status.name)
            .put("n", m.note)
            .put("t", m.updated)
            .toString()
        prefs.edit().putString(id, json).apply()
    }

    fun remove(id: String) {
        prefs.edit().remove(id).apply()
    }
}

object Backup {
    fun toJson(marks: Map<String, Mark>): String {
        val m = JSONObject()
        for ((k, v) in marks) {
            m.put(k, JSONObject().put("s", v.status.name).put("n", v.note).put("t", v.updated))
        }
        return JSONObject().put("app", "Fakt-App").put("version", 1).put("marks", m).toString()
    }

    /** Hibás szövegnél null-t ad vissza. */
    fun fromJson(text: String): Map<String, Mark>? {
        return try {
            val root = JSONObject(text.trim())
            val m = root.getJSONObject("marks")
            val res = LinkedHashMap<String, Mark>()
            val keys = m.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val o = m.getJSONObject(k)
                res[k] = Mark(statusFrom(o.optString("s", "NONE")), o.optString("n", ""), o.optLong("t", 0L))
            }
            res
        } catch (e: Exception) {
            null
        }
    }
}
