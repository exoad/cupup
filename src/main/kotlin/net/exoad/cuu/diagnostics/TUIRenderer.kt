package net.exoad.cuu.diagnostics

/**
 * Simple TUI symbols for the K compiler
 */
object TUISymbols {
    const val ARROW = "-->"
    const val CARET = "^"
    const val CHECK = "✓"
    const val CROSS = "✗"
    const val WARN = "⚠"
    const val INFO = "ℹ"
    const val BULLET = "•"
}

/**
 * ANSI color codes for TUI
 */
object TUIColors {
    const val RESET = "\u001B[0m"
    const val BOLD = "\u001B[1m"

    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val CYAN = "\u001B[36m"
    const val MAGENTA = "\u001B[35m"

    const val BRIGHT_RED = "\u001B[91m"
    const val BRIGHT_GREEN = "\u001B[92m"
    const val BRIGHT_YELLOW = "\u001B[93m"
    const val BRIGHT_BLUE = "\u001B[94m"
    const val BRIGHT_CYAN = "\u001B[96m"
    const val BRIGHT_MAGENTA = "\u001B[95m"

    fun isColorSupported(): Boolean {
        val term = System.getenv("TERM")
        val noColor = System.getenv("NO_COLOR")
        return noColor == null && (term != null && term != "dumb")
    }
}

/**
 * Simple diagnostic renderer inspired by Rust's compiler output
 */
class TUIDiagnosticRenderer(
    private val sourceFile: String,
    sourceContent: String,
    private val useColors: Boolean = TUIColors.isColorSupported()
) {
    private val lines = sourceContent.lines()

    private fun color(text: String, colorCode: String): String {
        return if (useColors) "$colorCode$text${TUIColors.RESET}" else text
    }

    private fun bold(text: String): String {
        return if (useColors) "${TUIColors.BOLD}$text${TUIColors.RESET}" else text
    }

    fun renderDiagnostic(diagnostic: Diagnostic): String {
        val sb = StringBuilder()

        // Level and message
        val (icon, colorCode, levelText) = when (diagnostic.level) {
            DiagnosticLevel.ERROR -> Triple(
                TUISymbols.CROSS,
                TUIColors.BRIGHT_RED,
                "error"
            )
            DiagnosticLevel.WARNING -> Triple(
                TUISymbols.WARN,
                TUIColors.BRIGHT_YELLOW,
                "warning"
            )
            DiagnosticLevel.INFO -> Triple(
                TUISymbols.INFO,
                TUIColors.BRIGHT_CYAN,
                "info"
            )
            DiagnosticLevel.HINT -> Triple(
                TUISymbols.BULLET,
                TUIColors.BRIGHT_BLUE,
                "hint"
            )
        }

        val codePart =
            diagnostic.code?.let { "[${color(it, TUIColors.BRIGHT_MAGENTA)}]" }
                ?: ""
        sb.append(
            "${color(icon, colorCode)} ${
                color(
                    levelText,
                    colorCode
                )
            }$codePart: ${diagnostic.message}\n"
        )

        // Location
        sb.append("${TUISymbols.ARROW} ${sourceFile}:${diagnostic.span.start.line}:${diagnostic.span.start.column}\n")

        // Source context
        renderSourceContext(sb, diagnostic)

        // Hint
        diagnostic.hint?.let {
            sb.append("${color("hint", TUIColors.BRIGHT_CYAN)}: $it\n")
        }

        // Notes
        diagnostic.notes.forEach { note ->
            sb.append("${color("note", TUIColors.BRIGHT_BLUE)}: $note\n")
        }

        return sb.toString()
    }

    private fun renderSourceContext(sb: StringBuilder, diagnostic: Diagnostic) {
        val lineNum = diagnostic.span.start.line
        val column = diagnostic.span.start.column
        val length = diagnostic.span.length.coerceAtLeast(1)

        if (lineNum < 1 || lineNum > lines.size) return

        // Show the line
        sb.append("${lineNum.toString().padStart(4)} | ${lines[lineNum - 1]}\n")

        // Caret line
        val caretLine =
            " ".repeat(4 + 3 + (column - 1)) + TUISymbols.CARET.repeat(length)
        sb.append(caretLine + "\n")
    }

    fun renderAll(diagnostics: List<Diagnostic>): String {
        val buffer = "-".repeat(40)

        return "$buffer\n${
            diagnostics.joinToString("$buffer\n") {
                renderDiagnostic(it)
            }
        }$buffer"
    }

    fun renderSummary(diagnostics: List<Diagnostic>): String {
        val errors = diagnostics.count { it.level == DiagnosticLevel.ERROR }
        val warnings = diagnostics.count { it.level == DiagnosticLevel.WARNING }

        return when {
            errors == 0 && warnings == 0 -> {
                "${color(TUISymbols.CHECK, TUIColors.BRIGHT_GREEN)} ${bold(color("Compilation successful", TUIColors.BRIGHT_GREEN))}"
            }
            else -> {
                val parts = mutableListOf<String>()
                if (errors > 0) {
                    val text = if (errors == 1) "error" else "errors"
                    parts.add(color("$errors $text", TUIColors.BRIGHT_RED))
                }
                if (warnings > 0) {
                    val text = if (warnings == 1) "warning" else "warnings"
                    parts.add(color("$warnings $text", TUIColors.BRIGHT_YELLOW))
                }
                val summary = "${bold("Compilation failed:")} ${parts.joinToString(", ")}"
                if (errors > 0) {
                    "$summary\n${color("Please fix the above errors.", TUIColors.BRIGHT_YELLOW)}"
                } else {
                    summary
                }
            }
        }
    }
}
