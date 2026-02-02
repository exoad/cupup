package net.exoad.cuu.diagnostics

import kotlin.system.exitProcess

object AnsiColors {
    const val RESET = "\u001B[0m"
    const val BOLD = "\u001B[1m"
    const val DIM = "\u001B[2m"
    const val RED = "\u001B[31m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val CYAN = "\u001B[36m"
    const val WHITE = "\u001B[37m"
    const val BRIGHT_RED = "\u001B[91m"
    const val BRIGHT_YELLOW = "\u001B[93m"
    const val BRIGHT_BLUE = "\u001B[94m"
    const val BRIGHT_CYAN = "\u001B[96m"

    fun isColorSupported(): Boolean {
        val term = System.getenv("TERM")
        return System.getenv("NO_COLOR") == null && (term != null && term != "dumb")
    }
}

class DiagnosticRenderer(
    private val sourceFile: String,
    sourceContent: String,
    private val useColors: Boolean = AnsiColors.isColorSupported()
) {
    private val lines = sourceContent.lines()

    private fun color(text: String, color: String): String {
        return if (useColors) "$color$text${AnsiColors.RESET}" else text
    }

    private fun bold(text: String): String {
        return if (useColors) "${AnsiColors.BOLD}$text${AnsiColors.RESET}" else text
    }

    private fun dim(text: String): String {
        return if (useColors) "${AnsiColors.DIM}$text${AnsiColors.RESET}" else text
    }

    fun render(diagnostic: Diagnostic): String {
        val sb = StringBuilder()
        val levelPrefix = when (diagnostic.level) {
            DiagnosticLevel.ERROR -> color("error", AnsiColors.BRIGHT_RED)
            DiagnosticLevel.WARNING -> color(
                "warning",
                AnsiColors.BRIGHT_YELLOW
            )
            DiagnosticLevel.INFO -> color("info", AnsiColors.BRIGHT_CYAN)
            DiagnosticLevel.HINT -> color("hint", AnsiColors.BRIGHT_BLUE)
        }
        val codeStr = diagnostic.code?.let { "[$it]" } ?: ""
        sb.append("${bold(levelPrefix)}$codeStr: ${bold(diagnostic.message)}\n")
        sb.append("${dim("  -->")} ${bold("${sourceFile}:${diagnostic.span.start.line}:${diagnostic.span.start.column}")}\n")
        renderSourceContext(sb, diagnostic)
        diagnostic.hint?.let {
            sb.append(
                "${dim("  =")} ${
                    color(
                        "hint",
                        AnsiColors.BRIGHT_CYAN
                    )
                }: $it\n"
            )
        }
        diagnostic.notes.forEach { note ->
            sb.append(
                "${dim("  =")} ${
                    color(
                        "note",
                        AnsiColors.BRIGHT_BLUE
                    )
                }: $note\n"
            )
        }

        return sb.toString()
    }

    private fun renderSourceContext(sb: StringBuilder, diagnostic: Diagnostic) {
        val lineNum = diagnostic.span.start.line
        val column = diagnostic.span.start.column
        val length = diagnostic.span.length.coerceAtLeast(1)
        if (lineNum < 1 || lineNum > lines.size) {
            return
        }
        val lineNumWidth = lineNum.toString().length.coerceAtLeast(3)
        val lineNumStr = lineNum.toString().padStart(lineNumWidth)
        sb.append("${dim(" ".repeat(lineNumWidth))} ${dim("|")}\n")
        sb.append("${bold(lineNumStr)} ${dim("|")} ${lines[lineNum - 1]}\n")
        sb.append(
            "${dim(" ".repeat(lineNumWidth))} ${dim("|")} ${
                buildIndicatorLine(
                    column,
                    length,
                    diagnostic.level
                )
            }\n"
        )
    }

    private fun buildIndicatorLine(
        column: Int,
        length: Int,
        level: DiagnosticLevel
    ): String {
        return " ".repeat((column - 1).coerceAtLeast(0)) + color(
            "^".repeat(length.coerceAtLeast(1)), when (level) {
                DiagnosticLevel.ERROR -> AnsiColors.BRIGHT_RED
                DiagnosticLevel.WARNING -> AnsiColors.BRIGHT_YELLOW
                DiagnosticLevel.INFO -> AnsiColors.BRIGHT_CYAN
                DiagnosticLevel.HINT -> AnsiColors.BRIGHT_BLUE
            }
        )
    }

    fun renderAll(diagnostics: List<Diagnostic>): String {
        if (diagnostics.isEmpty()) {
            return ""
        }

        return diagnostics.joinToString("\n") { render(it) }
    }

    fun renderSummary(diagnostics: List<Diagnostic>): String {
        val errors = diagnostics.count { it.level == DiagnosticLevel.ERROR }
        val warnings = diagnostics.count { it.level == DiagnosticLevel.WARNING }
        if (errors == 0 && warnings == 0) {
            return color("Compilation successful", AnsiColors.BRIGHT_CYAN)
        }
        val parts = mutableListOf<String>()
        if (errors > 0) {
            val errorText = if (errors == 1) "error" else "errors"
            parts.add(color("$errors $errorText", AnsiColors.BRIGHT_RED))
        }
        if (warnings > 0) {
            val warningText = if (warnings == 1) "warning" else "warnings"
            parts.add(color("$warnings $warningText", AnsiColors.BRIGHT_YELLOW))
        }

        return "${bold("Compilation failed:")} ${parts.joinToString(", ")}"
    }
}

object DiagnosticDisplay {
    private const val PREFER_BOXED = true

    fun display(
        collector: DiagnosticCollector,
        sourceFile: String,
        sourceContent: String
    ) {
        val diagnostics = collector.getAllDiagnostics()
        if (diagnostics.isEmpty()) {
            return
        }
        if (PREFER_BOXED) {
            val renderer = TUIDiagnosticRenderer(sourceFile, sourceContent)
            println(renderer.renderAll(diagnostics))
            println()
            println(renderer.renderSummary(diagnostics))
        } else {
            val renderer = DiagnosticRenderer(sourceFile, sourceContent)
            println(renderer.renderAll(diagnostics))
            println()
            println(renderer.renderSummary(diagnostics))
        }
    }

    fun displayAndExit(
        collector: DiagnosticCollector,
        sourceFile: String,
        sourceContent: String,
        exitCode: Int = 1
    ): Nothing {
        display(collector, sourceFile, sourceContent)
        exitProcess(exitCode)
    }
}
