package net.exoad.cuu

import net.exoad.cuu.diagnostics.*
import java.io.File
import kotlin.system.exitProcess

data class SourcePosition(val line: Int, val column: Int)

private fun color(
    text: String,
    colorCode: String,
    useColors: Boolean = TUIColors.isColorSupported()
): String {
    return if (useColors) "$colorCode$text${TUIColors.RESET}" else text
}

private fun bold(
    text: String,
    useColors: Boolean = TUIColors.isColorSupported()
): String {
    return if (useColors) "${TUIColors.BOLD}$text${TUIColors.RESET}" else text
}

fun main(args: Array<String>) {
    val dumpLexerTokens = args.contains("--lexerDump")
    val inputFile = "input.cuu"
    val outputFile = "output.c"
    try {
        println(
            "${
                color(
                    "Starting",
                    TUIColors.BRIGHT_BLUE
                )
            } ${bold("compilation of $inputFile...")}"
        )
        val startTime = System.currentTimeMillis()
        val sourceContent = File(inputFile).readText()
        val diagnostics = DiagnosticCollector()
        val tokens = Lexer(sourceContent, diagnostics).tokenize()
        if (dumpLexerTokens) {
            File("lexer_dumped.txt").writeText(buildString {
                tokens.forEach {
                    if (it.type != Token.Type.S_NEWLINE) {
                        append(it.type.name)
                        append(" ")
                    } else {
                        appendLine()
                    }
                }
            })
        }
        LintAnalyzer(tokens, diagnostics).analyze()
        if (diagnostics.hasErrors) {
            DiagnosticDisplay.displayAndExit(
                diagnostics,
                inputFile,
                sourceContent
            )
        }
        val program = Parser(tokens, diagnostics).parseProgram()
        if (diagnostics.hasErrors) {
            DiagnosticDisplay.displayAndExit(
                diagnostics,
                inputFile,
                sourceContent
            )
        }
        val warnings = diagnostics.getWarnings()
        if (warnings.isNotEmpty()) {
            println(
                DiagnosticRenderer(inputFile, sourceContent).renderAll(warnings)
            )
            println()
        }
        File("ast_debug.txt").writeText(ASTDebugPrint().visitModule(program))
        val transpiler = Transpiler()
        program.accept(transpiler)
        File(outputFile).writeText(transpiler.sb.toString())
        println(
            "${
                color(
                    TUISymbols.CHECK,
                    TUIColors.BRIGHT_GREEN
                )
            } ${bold("Generated $outputFile")}"
        )
        println(
            "${
                color(
                    TUISymbols.CHECK,
                    TUIColors.BRIGHT_GREEN
                )
            } ${bold("Compilation completed successfully in ${System.currentTimeMillis() - startTime}ms")}"
        )
    } catch (e: DiagnosticException) {
        exitProcess(1)
    } catch (e: Exception) {
        println("Internal compiler error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}
