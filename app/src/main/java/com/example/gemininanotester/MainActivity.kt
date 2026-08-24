package com.example.gemininanotester

import android.os.Bundle
import android.os.Build
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.genai.prompt.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var resultText: TextView

    private val generativeModel by lazy {
        Generation.getClient()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Gemini Nano Tester"
            textSize = 26f
            setPadding(0, 0, 0, 30)
        }

        val device = TextView(this).apply {
            text = """
                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                Android: ${Build.VERSION.RELEASE}
                API: ${Build.VERSION.SDK_INT}
            """.trimIndent()
            textSize = 16f
        }

        val checkButton = Button(this).apply {
            text = "CHECK NANO AVAILABILITY"
        }

        val testButton = Button(this).apply {
            text = "RUN REAL NANO TEST"
        }

        resultText = TextView(this).apply {
            text = "Press the button to begin."
            textSize = 17f
            setPadding(0, 30, 0, 0)
        }

        layout.addView(title)
        layout.addView(device)
        layout.addView(checkButton)
        layout.addView(testButton)
        layout.addView(resultText)

        setContentView(layout)

        checkButton.setOnClickListener {
            checkNano()
        }

        testButton.setOnClickListener {
            runNanoTest()
        }
    }

    private fun checkNano() {
        resultText.text = "Checking Gemini Nano..."

        lifecycleScope.launch {
            try {
                val status = generativeModel.checkStatus()

                val statusText = when (status) {
                    FeatureStatus.AVAILABLE ->
                        "AVAILABLE ✅\n\nNano is ready for inference."

                    FeatureStatus.DOWNLOADABLE ->
                        "DOWNLOADABLE 🟡\n\nNano is supported but the model is not downloaded yet."

                    FeatureStatus.DOWNLOADING ->
                        "DOWNLOADING 🟡\n\nNano is currently being downloaded."

                    FeatureStatus.UNAVAILABLE ->
                        "UNAVAILABLE ❌\n\nNano is not currently available."

                    else ->
                        "UNKNOWN\n\nStatus code: $status"
                }

                var modelName = ""

                if (status == FeatureStatus.AVAILABLE) {
                    try {
                        modelName =
                            "\n\nBase model:\n${generativeModel.getBaseModelName()}"
                    } catch (e: Exception) {
                        modelName =
                            "\n\nCould not retrieve base model name:\n${e.message}"
                    }
                }

                resultText.text = statusText + modelName

            } catch (e: Exception) {
                resultText.text =
                    "CHECK FAILED ❌\n\n" +
                    "${e.javaClass.simpleName}\n\n" +
                    "${e.message}"
            }
        }
    }

    private fun runNanoTest() {
        resultText.text = "Running Gemini Nano inference..."

        lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(
                    "Reply with exactly: GEMINI NANO LOCAL TEST PASS"
                )

                resultText.text =
                    "INFERENCE SUCCESS ✅\n\n" +
                    "Response:\n${response.text}"

            } catch (e: Exception) {
                resultText.text =
                    "INFERENCE FAILED ❌\n\n" +
                    "${e.javaClass.simpleName}\n\n" +
                    "${e.message}"
            }
        }
    }

    override fun onDestroy() {
        generativeModel.close()
        super.onDestroy()
    }
}
