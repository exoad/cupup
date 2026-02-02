package net.exoad.cuu

import net.exoad.cuu.diagnostics.*

/**
 * Performs lint analysis on tokens to detect code quality issues
 */
class LintAnalyzer(
    private val tokens: List<Token>,
    private val diagnostics: DiagnosticCollector
) {
    fun analyze() {
        checkUnnecessaryParentheses()
        checkTrailingWhitespace()
        checkConsecutiveNewlines()
    }

    /**
     * Detects unnecessary parentheses in expressions
     */
    private fun checkUnnecessaryParentheses() {
        for (i in tokens.indices) {
            val token = tokens[i]
            if (token.type == Token.Type.S_OPEN_PARENTHESIS) {
                // Check for double parentheses like ((expr))
                if (i + 1 < tokens.size && tokens[i + 1].type == Token.Type.S_OPEN_PARENTHESIS) {
                    val matchingClose = findMatchingParenthesis(i + 1)
                    if (matchingClose != -1 && matchingClose + 1 < tokens.size) {
                        if (tokens[matchingClose + 1].type == Token.Type.S_CLOSE_PARENTHESIS) {
                            diagnostics.reportWarning(
                                "Unnecessary nested parentheses",
                                SourceSpan.single(tokens[i + 1].canonicalLocation),
                                "Consider removing the inner parentheses",
                                "W001"
                            )
                        }
                    }
                }

                // Check for single-item parentheses like (identifier) or (literal)
                if (i + 2 < tokens.size) {
                    val next = tokens[i + 1]
                    val afterNext = tokens[i + 2]

                    if (afterNext.type == Token.Type.S_CLOSE_PARENTHESIS) {
                        when (next.type) {
                            Token.Type.IDENTIFIER,
                            Token.Type.L_INTEGER,
                            Token.Type.L_FLOAT,
                            Token.Type.K_TRUE,
                            Token.Type.K_FALSE -> {
                                // Check if this is not a function call or cast
                                val before = if (i > 0) tokens[i - 1] else null
                                val after =
                                    if (i + 3 < tokens.size) tokens[i + 3] else null

                                val isAfterFunctionOrType = before != null &&
                                        (before.type == Token.Type.IDENTIFIER ||
                                                before.type.name.startsWith("K__"))

                                val isBeforeOperator = after != null &&
                                        (after.type.name.startsWith("OP_") ||
                                                after.type.name.startsWith("S_") &&
                                                after.type !in setOf(
                                            Token.Type.S_NEWLINE,
                                            Token.Type.S_COMMA,
                                            Token.Type.S_CLOSE_BRACE
                                        ))

                                if (!isAfterFunctionOrType && !isBeforeOperator) {
                                    diagnostics.reportInfo(
                                        "Unnecessary parentheses around simple expression",
                                        SourceSpan.single(token.canonicalLocation),
                                        "These parentheses can be safely removed"
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    /**
     * Find the matching closing parenthesis for an opening parenthesis
     */
    private fun findMatchingParenthesis(openIndex: Int): Int {
        var depth = 1
        for (i in openIndex + 1 until tokens.size) {
            when (tokens[i].type) {
                Token.Type.S_OPEN_PARENTHESIS -> depth++
                Token.Type.S_CLOSE_PARENTHESIS -> {
                    depth--
                    if (depth == 0) return i
                }
                else -> {}
            }
        }
        return -1
    }

    /**
     * Check for trailing whitespace (conceptual - tokens don't preserve it)
     */
    private fun checkTrailingWhitespace() {
        // This would require access to the original source with whitespace preserved
        // For now, we skip this check since tokens don't preserve whitespace
    }

    /**
     * Check for excessive consecutive newlines
     */
    private fun checkConsecutiveNewlines() {
        var consecutiveNewlines = 0
        var firstNewlinePos: SourcePosition? = null

        for (token in tokens) {
            if (token.type == Token.Type.S_NEWLINE) {
                if (consecutiveNewlines == 0) {
                    firstNewlinePos = token.canonicalLocation
                }
                consecutiveNewlines++

                if (consecutiveNewlines > 2) {
                    firstNewlinePos?.let {
                        diagnostics.reportInfo(
                            "Multiple consecutive blank lines",
                            SourceSpan.single(it),
                            "Consider using at most one blank line for readability"
                        )
                    }
                    // Reset to avoid repeated warnings
                    consecutiveNewlines = 0
                    firstNewlinePos = null
                }
            } else {
                consecutiveNewlines = 0
                firstNewlinePos = null
            }
        }
    }
}
