package net.exoad.cuu.diagnostics

import net.exoad.cuu.SourcePosition

enum class DiagnosticLevel {
    ERROR,
    WARNING,
    INFO,
    HINT
}

data class SourceSpan(
    val start: SourcePosition,
    val end: SourcePosition,
    val length: Int = 1
) {
    companion object {
        fun single(pos: SourcePosition) = SourceSpan(pos, pos, 1)
        fun range(start: SourcePosition, end: SourcePosition): SourceSpan {
            val length = if (start.line == end.line) {
                end.column - start.column + 1
            } else {
                1
            }
            return SourceSpan(start, end, length)
        }
    }
}

data class Diagnostic(
    val level: DiagnosticLevel,
    val message: String,
    val span: SourceSpan,
    val hint: String? = null,
    val code: String? = null,
    val notes: List<String> = emptyList()
) {
    companion object {
        fun error(
            message: String,
            span: SourceSpan,
            hint: String? = null,
            code: String? = null
        ): Diagnostic {
            return Diagnostic(DiagnosticLevel.ERROR, message, span, hint, code)
        }

        fun warning(
            message: String,
            span: SourceSpan,
            hint: String? = null,
            code: String? = null
        ): Diagnostic {
            return Diagnostic(
                DiagnosticLevel.WARNING,
                message,
                span,
                hint,
                code
            )
        }

        fun info(
            message: String,
            span: SourceSpan,
            hint: String? = null
        ): Diagnostic {
            return Diagnostic(DiagnosticLevel.INFO, message, span, hint)
        }

        fun hint(message: String, span: SourceSpan): Diagnostic {
            return Diagnostic(DiagnosticLevel.HINT, message, span)
        }
    }
}

class DiagnosticException(diagnostic: Diagnostic) :
    Exception(diagnostic.message)

/**
 * Collects and manages diagnostic messages during compilation
 */
class DiagnosticCollector {
    private val diagnostics = mutableListOf<Diagnostic>()
    var hasErrors = false
        private set

    private val maxErrors = 10  // Stop collecting errors after 10 to prevent cascading
    private var errorCount = 0

    fun report(diagnostic: Diagnostic) {
        // Don't report more than max errors to avoid cascading error spam
        if (diagnostic.level == DiagnosticLevel.ERROR && errorCount >= maxErrors) {
            return
        }

        diagnostics.add(diagnostic)
        if (diagnostic.level == DiagnosticLevel.ERROR) {
            hasErrors = true
            errorCount++
        }
    }

    fun reportError(
        message: String,
        span: SourceSpan,
        hint: String? = null,
        code: String? = null
    ) {
        report(Diagnostic.error(message, span, hint, code))
    }

    fun reportWarning(
        message: String,
        span: SourceSpan,
        hint: String? = null,
        code: String? = null
    ) {
        report(Diagnostic.warning(message, span, hint, code))
    }

    fun reportInfo(message: String, span: SourceSpan, hint: String? = null) {
        report(Diagnostic.info(message, span, hint))
    }

    fun getAllDiagnostics(): List<Diagnostic> {
        return diagnostics.toList()
    }

    fun getErrors(): List<Diagnostic> {
        return diagnostics.filter { it.level == DiagnosticLevel.ERROR }
    }

    fun getWarnings(): List<Diagnostic> {
        return diagnostics.filter { it.level == DiagnosticLevel.WARNING }
    }

    fun clear() {
        diagnostics.clear()
        hasErrors = false
    }

    fun throwIfErrors() {
        if (hasErrors) {
            throw DiagnosticException(getErrors().first())
        }
    }
}
