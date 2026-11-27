package com.terminalx.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.text.method.ScrollingMovementMethod
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val green = Color.rgb(91, 220, 145)
    private val bg = Color.rgb(10, 14, 18)
    private lateinit var output: TextView
    private lateinit var input: EditText
    private lateinit var shellWriter: OutputStreamWriter
    private var shell: Process? = null
    private val worker = Executors.newSingleThreadExecutor()
    private val history = mutableListOf<String>()
    private var historyIndex = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        buildInterface()
        printLine("TERMINAL X  •  Android Shell")
        printLine("جلسة محلية — بدون صلاحيات root")
        printLine("اكتب help لعرض الأوامر المساعدة.")
        printLine("")
        startShell()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "TERMINAL X"
            textSize = 18f
            typeface = Typeface.MONOSPACE
            setTextColor(green)
        }, LinearLayout.LayoutParams(0, dp(42), 1f))
        header.addView(Button(this).apply {
            text = "مسح"
            isAllCaps = false
            setOnClickListener { output.text = "" }
        })
        root.addView(header)

        output = TextView(this).apply {
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.rgb(220, 230, 220))
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(output)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(this).apply {
            text = "$"
            textSize = 18f
            typeface = Typeface.MONOSPACE
            setTextColor(green)
            setPadding(dp(4), 0, dp(8), 0)
        })
        input = EditText(this).apply {
            singleLine = true
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "اكتب أمراً..."
            setBackgroundColor(Color.rgb(24, 31, 38))
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(dp(10), 0, dp(10), 0)
            setOnEditorActionListener { _, action, event ->
                if (action == EditorInfo.IME_ACTION_SEND ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                    submitCommand()
                    true
                } else false
            }
        }
        row.addView(input, LinearLayout.LayoutParams(0, dp(48), 1f))
        row.addView(Button(this).apply {
            text = "تنفيذ"
            isAllCaps = false
            setOnClickListener { submitCommand() }
        })
        root.addView(row)
        setContentView(root)
    }

    private fun startShell() {
        try {
            val home = getExternalFilesDir(null)?.absolutePath ?: filesDir.absolutePath
            shell = ProcessBuilder("/system/bin/sh")
                .redirectErrorStream(true)
                .start()
            shellWriter = OutputStreamWriter(shell!!.outputStream)
            shellWriter.write("export HOME='$home'\ncd '$home'\n")
            shellWriter.flush()
            worker.execute {
                try {
                    BufferedReader(InputStreamReader(shell!!.inputStream)).use { reader ->
                        while (true) {
                            val line = reader.readLine() ?: break
                            runOnUiThread { printLine(line) }
                        }
                    }
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            printLine("تعذر تشغيل shell: ${e.message}")
        }
    }

    private fun submitCommand() {
        val command = input.text.toString().trim()
        if (command.isEmpty()) return
        input.setText("")
        history.add(command)
        historyIndex = history.size
        printLine("~ $command")
        when (command) {
            "clear" -> { output.text = ""; return }
            "help" -> {
                printLine("أوامر مفيدة:")
                printLine("pwd  ls  cd  mkdir  touch  cat  cp  mv  rm")
                printLine("date  whoami  id  uname  df  ps  env  echo")
                printLine("exit — إنهاء جلسة الطرفية")
                printLine("ملاحظة: الأوامر والحزم تعتمد على ما يوفره Android.")
                printLine("لا يوجد root ولا مدير حزم Linux مضمّن.")
                return
            }
            "exit" -> { shell?.destroy(); printLine("انتهت الجلسة."); return }
        }
        try {
            shellWriter.write(command)
            shellWriter.write("\n")
            shellWriter.flush()
        } catch (e: Exception) {
            printLine("خطأ: ${e.message}")
            startShell()
        }
    }

    private fun printLine(line: String) {
        output.append(line + "\n")
        val layout = output.layout
        if (layout != null) {
            val scroll = output.parent as? ScrollView
            scroll?.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    override fun onDestroy() {
        shell?.destroy()
        worker.shutdownNow()
        super.onDestroy()
    }
}
