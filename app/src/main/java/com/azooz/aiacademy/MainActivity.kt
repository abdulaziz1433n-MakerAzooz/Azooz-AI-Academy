package com.azooz.aiacademy

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("academy", MODE_PRIVATE) }
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var language: Spinner
    private lateinit var level: Spinner
    private lateinit var question: EditText
    private lateinit var answer: TextView
    private lateinit var send: Button
    private lateinit var endpoint: EditText
    private val languages = arrayOf("HTML", "CSS", "JavaScript", "Python", "Kotlin", "Swift", "Luau (Roblox)", "Java", "C++", "C#", "Dart", "PHP", "SQL", "Rust", "Go", "TypeScript", "لغة أخرى")
    private val levels = arrayOf("مبتدئ", "متوسط", "متقدم")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(17, 46, 85)
        window.navigationBarColor = Color.rgb(17, 46, 85)
        val scroller = ScrollView(this).apply { setBackgroundColor(Color.rgb(242, 247, 253)) }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22.dp(), 24.dp(), 22.dp(), 24.dp())
        }
        scroller.addView(body)
        setContentView(scroller)

        fun heading(value: String, size: Float) = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(Color.rgb(17, 46, 85))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 8.dp(), 0, 12.dp())
        }
        fun field(hintText: String) = EditText(this).apply {
            hint = hintText
            textSize = 15f
            setTextColor(Color.rgb(17, 46, 85))
            setSingleLine(false)
            gravity = Gravity.TOP or Gravity.RIGHT
            background = rounded(Color.WHITE)
            setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
        }

        body.addView(heading("Azooz AI Academy 🎓", 27f))
        body.addView(heading("اختر لغة البرمجة والمستوى ثم ابدأ التعلم", 15f))
        body.addView(heading("لغة البرمجة", 17f))
        language = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages)
        }
        body.addView(language)
        body.addView(heading("المستوى", 17f))
        level = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, levels)
        }
        body.addView(level)
        language.setSelection(prefs.getInt("language", 0).coerceIn(languages.indices))
        level.setSelection(prefs.getInt("level", 0).coerceIn(levels.indices))

        body.addView(heading("عنوان خادم AI (HTTPS)", 16f))
        endpoint = field("https://your-worker.workers.dev/chat").apply {
            setSingleLine(true)
            setText(prefs.getString("endpoint", ""))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            textDirection = View.TEXT_DIRECTION_LTR
        }
        body.addView(endpoint)
        body.addView(heading("سؤالك أو إجابتك", 17f))
        question = field("اكتب سؤالك أو كودك هنا...").apply { minLines = 4 }
        body.addView(question)

        send = Button(this).apply {
            text = "إرسال إلى المدرس الذكي"
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(30, 104, 201))
        }
        body.addView(send, LinearLayout.LayoutParams(-1, 55.dp()).apply { topMargin = 14.dp() })

        val quick = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun action(title: String, prompt: String) {
            val b = Button(this).apply {
                text = title
                textSize = 12f
                setOnClickListener {
                    question.setText(prompt)
                    ask()
                }
            }
            quick.addView(b, LinearLayout.LayoutParams(0, -2, 1f))
        }
        action("درس", "اشرح لي الدرس التالي تدريجيًا مع مثال صغير.")
        action("تمرين", "أعطني تمرينًا واحدًا مناسبًا لمستواي دون الحل.")
        action("اختبار", "اختبرني بسؤال واحد، ولا تكشف الحل قبل إجابتي.")
        body.addView(quick)
        body.addView(heading("رد المدرس", 18f))

        answer = TextView(this).apply {
            text = prefs.getString("lastReply", "مرحبًا! اختر لغة وابدأ أول درس.")
            textSize = 16f
            setTextColor(Color.rgb(28, 49, 78))
            gravity = Gravity.RIGHT
            background = rounded(Color.WHITE)
            setPadding(16.dp(), 18.dp(), 16.dp(), 18.dp())
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        }
        body.addView(answer, LinearLayout.LayoutParams(-1, -2))
        send.setOnClickListener { ask() }
    }

    private fun ask() {
        val url = endpoint.text.toString().trim().trimEnd('/')
        val message = question.text.toString().trim()
        if (message.isBlank()) {
            question.error = "اكتب سؤالك أولًا"
            return
        }
        if (!url.startsWith("https://") || !url.endsWith("/chat")) {
            endpoint.error = "أدخل رابط HTTPS للخادم وينتهي بـ /chat"
            return
        }
        val selectedLanguage = languages[language.selectedItemPosition]
        val selectedLevel = levels[level.selectedItemPosition]
        prefs.edit().putString("endpoint", url)
            .putInt("language", language.selectedItemPosition)
            .putInt("level", level.selectedItemPosition).apply()
        send.isEnabled = false
        answer.text = "جارٍ التواصل مع المدرس..."
        executor.execute {
            var conn: HttpURLConnection? = null
            val result = try {
                conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 45000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val payload = JSONObject().put("language", selectedLanguage)
                    .put("level", selectedLevel)
                    .put("message", message)
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                val obj = JSONObject(raw)
                if (code in 200..299) obj.optString("reply", "لم يصل رد")
                else "خطأ من الخادم ($code): ${obj.optString("error", "تعذر إكمال الطلب")}"
            } catch (e: Exception) {
                "تعذر الاتصال: ${e.localizedMessage ?: "تحقق من الإنترنت ورابط الخادم"}"
            } finally {
                conn?.disconnect()
            }
            runOnUiThread {
                answer.text = result
                prefs.edit().putString("lastReply", result).apply()
                send.isEnabled = true
            }
        }
    }

    private fun rounded(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = 22.dp().toFloat()
    }
    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
