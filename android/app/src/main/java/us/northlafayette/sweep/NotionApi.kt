package us.northlafayette.sweep

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NotionException(message: String) : Exception(message)

/**
 * Minimal Notion REST client. Each user supplies their own internal-integration
 * token; the app only ever touches the databases that token was granted.
 */
object NotionApi {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun call(method: String, path: String, token: String, body: JSONObject?): JSONObject {
        val builder = Request.Builder()
            .url("https://api.notion.com/v1/$path")
            .header("Authorization", "Bearer $token")
            .header("Notion-Version", "2022-06-28")
        val reqBody = (body?.toString() ?: "{}").toRequestBody(JSON)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(reqBody)
            "PATCH" -> builder.patch(reqBody)
            else -> throw IllegalArgumentException(method)
        }
        client.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                val msg = try {
                    JSONObject(text).optString("message").ifBlank { "HTTP ${resp.code}" }
                } catch (e: Exception) {
                    "HTTP ${resp.code}"
                }
                throw NotionException(msg)
            }
            return JSONObject(text)
        }
    }

    data class Database(val id: String, val title: String, val properties: JSONObject)

    fun listDatabases(token: String): List<Database> {
        val body = JSONObject()
            .put("filter", JSONObject().put("value", "database").put("property", "object"))
            .put("page_size", 100)
        val res = call("POST", "search", token, body)
        val results = res.optJSONArray("results") ?: JSONArray()
        val out = ArrayList<Database>()
        for (i in 0 until results.length()) {
            val obj = results.getJSONObject(i)
            if (obj.optString("object") != "database") continue
            val title = plainText(obj.optJSONArray("title")).ifBlank { "(untitled)" }
            out.add(Database(obj.getString("id"), title, obj.optJSONObject("properties") ?: JSONObject()))
        }
        return out
    }

    fun plainText(arr: JSONArray?): String {
        if (arr == null) return ""
        val sb = StringBuilder()
        for (i in 0 until arr.length()) sb.append(arr.getJSONObject(i).optString("plain_text"))
        return sb.toString()
    }

    /** Name of the title property in a database schema (there is always exactly one). */
    fun titleProperty(properties: JSONObject): String {
        for (key in properties.keys()) {
            if (properties.optJSONObject(key)?.optString("type") == "title") return key
        }
        return "Name"
    }

    fun propertiesOfType(properties: JSONObject, type: String): List<String> {
        val out = ArrayList<String>()
        for (key in properties.keys()) {
            if (properties.optJSONObject(key)?.optString("type") == type) out.add(key)
        }
        return out.sorted()
    }

    fun createItem(token: String, databaseId: String, titleProp: String, text: String) {
        val body = JSONObject()
            .put("parent", JSONObject().put("database_id", databaseId))
            .put(
                "properties", JSONObject().put(
                    titleProp, JSONObject().put(
                        "title", JSONArray().put(
                            JSONObject().put("text", JSONObject().put("content", text))
                        )
                    )
                )
            )
        call("POST", "pages", token, body)
    }

    data class Task(val id: String, val name: String, val due: String?, val url: String?)

    /**
     * Open tasks (done checkbox unchecked), sorted by due date ascending.
     * When [dueOnOrBefore] (ISO date) is given, only tasks due on/before that day
     * are returned — which also excludes tasks with no due date at all.
     */
    fun openTasks(
        token: String,
        databaseId: String,
        doneProp: String,
        dueProp: String,
        dueOnOrBefore: String?
    ): List<Task> {
        val and = JSONArray().put(
            JSONObject().put("property", doneProp).put("checkbox", JSONObject().put("equals", false))
        )
        if (dueOnOrBefore != null) {
            and.put(
                JSONObject().put("property", dueProp).put("date", JSONObject().put("on_or_before", dueOnOrBefore))
            )
        }
        val body = JSONObject()
            .put("filter", JSONObject().put("and", and))
            .put(
                "sorts",
                JSONArray().put(JSONObject().put("property", dueProp).put("direction", "ascending"))
            )
            .put("page_size", 100)
        val res = call("POST", "databases/$databaseId/query", token, body)
        val results = res.optJSONArray("results") ?: JSONArray()
        val out = ArrayList<Task>()
        for (i in 0 until results.length()) {
            val page = results.getJSONObject(i)
            val props = page.optJSONObject("properties") ?: continue
            var name = ""
            for (key in props.keys()) {
                val p = props.optJSONObject(key) ?: continue
                if (p.optString("type") == "title") {
                    name = plainText(p.optJSONArray("title"))
                    break
                }
            }
            if (name.isBlank()) name = "(untitled)"
            val due = props.optJSONObject(dueProp)?.optJSONObject("date")?.optString("start")
                ?.ifBlank { null }
            out.add(Task(page.getString("id"), name, due, page.optString("url").ifBlank { null }))
        }
        return out
    }

    fun completeTask(token: String, pageId: String, doneProp: String) {
        val body = JSONObject().put(
            "properties",
            JSONObject().put(doneProp, JSONObject().put("checkbox", true))
        )
        call("PATCH", "pages/$pageId", token, body)
    }
}
