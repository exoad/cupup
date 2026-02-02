package net.exoad.cuu.diagnostics

import net.exoad.cuu.SourcePosition
import net.exoad.cuu.Token
import kotlin.collections.iterator

/**
 * Example extensions for the diagnostic system.
 * This file shows how to add new error types and lint rules.
 */

// Example 1: Adding semantic analysis errors
class SemanticAnalyzer(
    private val diagnostics: DiagnosticCollector
) {
    fun analyzeUndefinedVariable(name: String, position: SourcePosition) {
        diagnostics.reportError(
            "Undefined variable: '$name'",
            SourceSpan.single(position),
            "Declare the variable before using it, or check for typos",
            "S001"
        )
    }

    fun analyzeTypeMismatch(
        expected: String,
        actual: String,
        position: SourcePosition
    ) {
        diagnostics.reportError(
            "Type mismatch: expected '$expected', but got '$actual'",
            SourceSpan.single(position),
            "Convert the value to the expected type or change the variable type",
            "S002"
        )
    }

    fun analyzeDivisionByZero(position: SourcePosition) {
        diagnostics.reportWarning(
            "Division by zero will cause runtime error",
            SourceSpan.single(position),
            "Add a check to ensure the divisor is not zero",
            "S003"
        )
    }

    fun analyzeUnusedVariable(name: String, position: SourcePosition) {
        diagnostics.reportWarning(
            "Unused variable: '$name'",
            SourceSpan.single(position),
            "Remove this variable or prefix with '_' to indicate intentionally unused",
            "S004"
        )
    }
}

// Example 2: Extending LintAnalyzer with more rules
fun checkNamingConventions(
    tokens: List<Token>,
    diagnostics: DiagnosticCollector
) {
    for (token in tokens) {
        if (token.type == Token.Type.IDENTIFIER) {
            val name = token.content

            // Check for snake_case (conventional in K)
            if (name.contains(Regex("[A-Z]")) && !name.startsWith("_")) {
                diagnostics.reportInfo(
                    "Consider using snake_case for variable names",
                    SourceSpan.single(token.canonicalLocation),
                    "Convert '$name' to snake_case: '${toSnakeCase(name)}'"
                )
            }

            // Check for single-letter names (except i, j, k in loops)
            if (name.length == 1 && name !in setOf(
                    "i",
                    "j",
                    "k",
                    "x",
                    "y",
                    "z"
                )
            ) {
                diagnostics.reportInfo(
                    "Single-letter variable name may be unclear",
                    SourceSpan.single(token.canonicalLocation),
                    "Consider using a more descriptive name"
                )
            }
        }
    }
}

private fun toSnakeCase(name: String): String {
    return name.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}

// Example 3: Custom diagnostic display for specific error types
class CustomDiagnosticRenderer(
    private val baseRenderer: DiagnosticRenderer
) {
    fun renderWithSuggestions(
        diagnostic: Diagnostic,
        suggestions: List<String>
    ): String {
        val base = baseRenderer.render(diagnostic)
        if (suggestions.isEmpty()) return base

        val suggestionsText = suggestions.joinToString("\n") { "    - $it" }
        return "$base\n${AnsiColors.DIM}  Suggestions:${AnsiColors.RESET}\n$suggestionsText\n"
    }
}

// Example 4: Diagnostic filtering and configuration
class DiagnosticConfig(
    val showInfoMessages: Boolean = true,
    val showWarnings: Boolean = true,
    val treatWarningsAsErrors: Boolean = false,
    val suppressedCodes: Set<String> = emptySet()
) {
    fun shouldShow(diagnostic: Diagnostic): Boolean {
        // Suppress by code
        if (diagnostic.code in suppressedCodes) return false

        // Filter by level
        return when (diagnostic.level) {
            DiagnosticLevel.ERROR -> true
            DiagnosticLevel.WARNING -> showWarnings
            DiagnosticLevel.INFO, DiagnosticLevel.HINT -> showInfoMessages
        }
    }

    fun adjustLevel(diagnostic: Diagnostic): Diagnostic {
        if (treatWarningsAsErrors && diagnostic.level == DiagnosticLevel.WARNING) {
            return diagnostic.copy(level = DiagnosticLevel.ERROR)
        }
        return diagnostic
    }
}

class BatchDiagnosticReporter(
    private val diagnostics: DiagnosticCollector
) {
    private val similarErrors =
        mutableMapOf<String, MutableList<SourcePosition>>()

    fun reportSimilar(message: String, position: SourcePosition) {
        similarErrors.getOrPut(message) { mutableListOf() }.add(position)
    }

    fun flush() {
        for ((message, positions) in similarErrors) {
            if (positions.size == 1) {
                diagnostics.reportError(
                    message,
                    SourceSpan.single(positions[0]),
                    code = "BATCH"
                )
            } else {
                diagnostics.reportError(
                    "$message (occurs ${positions.size} times)",
                    SourceSpan.single(positions[0]),
                    "This error occurs at ${positions.size} locations. Fix the first occurrence.",
                    "BATCH"
                )
                positions.drop(1).take(3).forEach { pos ->
                    diagnostics.reportInfo(
                        "Also occurs here",
                        SourceSpan.single(pos)
                    )
                }
                if (positions.size > 4) {
                    diagnostics.reportInfo(
                        "... and ${positions.size - 4} more locations",
                        SourceSpan.single(positions[0])
                    )
                }
            }
        }
        similarErrors.clear()
    }
}

data class JsonDiagnostic(
    val level: String,
    val message: String,
    val file: String,
    val line: Int,
    val column: Int,
    val code: String?,
    val hint: String?
)

fun Diagnostic.toJson(file: String): JsonDiagnostic {
    return JsonDiagnostic(
        level = level.name.lowercase(),
        message = message,
        file = file,
        line = span.start.line,
        column = span.start.column,
        code = code,
        hint = hint
    )
}
