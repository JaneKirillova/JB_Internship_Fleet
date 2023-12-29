package org.example

import javafx.scene.control.Label
import tornadofx.*
import java.io.File
import kotlin.concurrent.thread

class ScriptRunnerApp : App(MainView::class)

class MainView : View("Script Runner") {
    private val scriptPath = "script_file.kts"

    private val editor = textarea {
        isWrapText = true
    }

    private val output = textarea {
        isWrapText = true
        isEditable = false
    }

    private var isRunning = false

    override val root = vbox {
        label("Script Editor")
        add(editor)

        label("Output")
        add(output)

        button("Run Script") {
            action { runScript() }
        }
        label("Script Status:").apply {
            id = "script_status"
            style { textFill = c("green") }
        }
        label("Last Exit Code:").apply {
            id = "last_exit_code"
            style { textFill = c("red") }
        }
    }

    private fun runScript() {
        if (isRunning) {
            output.appendText("Previous script still running. Please wait...\n")
            return
        }
        output.text = ""
        val scriptContent = editor.text
        File(scriptPath).writeText(scriptContent)
        thread {
            executeScript()
        }
    }

    private fun executeScript() {
        isRunning = true
        runLater {
            val label = root.children.filter{it is Label}.find { it.id == "script_status" } as Label
            label.text = "Script is running"
        }

        try {
            val processBuilder = ProcessBuilder("kotlinc", "-script", scriptPath)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach {
                    runLater { output.appendText("$it\n") }
                }
            }

            val exitCode = process.waitFor()
            runLater {
                val label = root.children.filter{it is Label}.find { it.id == "last_exit_code" } as Label
                label.text = "Last Exit Code: $exitCode"
                label.style {
                    textFill = if (exitCode != 0) c("red") else c("green")
                }

                val label2 = root.children.filter{it is Label}.find { it.id == "script_status" } as Label
                label2.text = "Script not running"
            }

        } catch (e: Exception) {
            runLater { output.appendText("An error occurred: ${e.message}\n") }
        } finally {
            isRunning = false
            File(scriptPath).delete()
        }
    }
}

fun main() {
    launch<ScriptRunnerApp>()
}






