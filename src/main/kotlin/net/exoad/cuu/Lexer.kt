package net.exoad.cuu

import net.exoad.cuu.diagnostics.DiagnosticCollector
import net.exoad.cuu.diagnostics.SourceSpan

class Lexer(
    private val input: String,
    private val diagnostics: DiagnosticCollector = DiagnosticCollector()
) {
    private val buffer = CharacterBuffer(input)

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (!buffer.isAtEnd) {
            when (buffer.current) {
                '\n' -> {
                    tokens.add(
                        Token.Raw(
                            Token.Type.S_NEWLINE,
                            "\n",
                            buffer.position,
                            SourcePosition(buffer.line, buffer.column)
                        )
                    )
                    buffer.advance()
                }

                in '0'..'9' -> tokens.add(lexNumber())
                '"' -> tokens.add(lexString())
                '\'' -> tokens.add(lexChar())
                in 'a'..'z', in 'A'..'Z', '_', '@' -> tokens.add(lexIdentifier())
                '+' -> {
                    when (buffer.peek(1)) {
                        '+' -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.OP_INC,
                                    "++",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance(); buffer.advance()
                        }

                        '=' -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.OP_PLUS_EQ,
                                    "+=",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance(); buffer.advance()
                        }

                        else -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.S_PLUS,
                                    "+",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance()
                        }
                    }
                }

                '-' -> {
                    when (buffer.peek(1)) {
                        '-' -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.OP_DEC,
                                    "--",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance(); buffer.advance()
                        }

                        '=' -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.OP_DASH_EQ,
                                    "-=",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance(); buffer.advance()
                        }

                        else -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.S_DASH,
                                    "-",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance()
                        }
                    }
                }

                '*' -> {
                    if (buffer.peek(1) == '=') {
                        tokens.add(
                            Token.Raw(
                                Token.Type.OP_ASTERISK_EQ,
                                "*=",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance(); buffer.advance()
                    } else {
                        tokens.add(
                            Token.Raw(
                                Token.Type.S_ASTERISK,
                                "*",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance()
                    }
                }

                '/' -> {
                    if (buffer.peek(1) == '/') {
                        buffer.advance(); buffer.advance()
                        while (!buffer.isAtEnd && buffer.current != '\n') {
                            buffer.advance()
                        }
                    } else if (buffer.peek(1) == '*') {
                        val commentStart =
                                SourcePosition(buffer.line, buffer.column)
                        buffer.advance(); buffer.advance()
                        var foundEnd = false
                        while (!buffer.isAtEnd) {
                            if (buffer.current == '*' && buffer.peek(1) == '/') {
                                buffer.advance(); buffer.advance()
                                foundEnd = true
                                break
                            }
                            buffer.advance()
                        }
                        if (!foundEnd) {
                            diagnostics.reportError(
                                "Unterminated block comment",
                                SourceSpan.single(commentStart),
                                "Add '*/' to close the block comment",
                                "E004"
                            )
                        }
                    } else if (buffer.peek(1) == '=') {
                        tokens.add(
                            Token.Raw(
                                Token.Type.OP_FORWARD_SLASH_EQ,
                                "/=",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance(); buffer.advance()
                    } else {
                        tokens.add(
                            Token.Raw(
                                Token.Type.S_FORWARD_SLASH,
                                "/",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance()
                    }
                }

                '%' -> {
                    if (buffer.peek(1) == '=') {
                        tokens.add(
                            Token.Raw(
                                Token.Type.OP_MOD_EQ,
                                "%=",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance(); buffer.advance()
                    } else {
                        tokens.add(
                            Token.Raw(
                                Token.Type.OP_MOD,
                                "%",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance()
                    }
                }

                '(' -> {
                    tokens.add(
                        Token.Raw(
                            Token.Type.S_OPEN_PARENTHESIS,
                            "(",
                            buffer.position,
                            SourcePosition(buffer.line, buffer.column)
                        )
                    )
                    buffer.advance()
                }

                ')' -> {
                    tokens.add(
                        Token.Raw(
                            Token.Type.S_CLOSE_PARENTHESIS,
                            ")",
                            buffer.position,
                            SourcePosition(buffer.line, buffer.column)
                        )
                    )
                    buffer.advance()
                }

                '{' -> {
                    tokens.add(
                        Token.Raw(
                            Token.Type.S_OPEN_BRACE,
                            "{",
                            buffer.position,
                            SourcePosition(buffer.line, buffer.column)
                        )
                    )
                    buffer.advance()
                }

                '}' -> {
                    tokens.add(
                        Token.Raw(
                            Token.Type.S_CLOSE_BRACE,
                            "}",
                            buffer.position,
                            SourcePosition(buffer.line, buffer.column)
                        )
                    )
                    buffer.advance()
                }

                ':' -> {
                    tokens.add(
                        Token.Raw(
                            Token.Type.S_COLON,
                            ":",
                            buffer.position,
                            SourcePosition(buffer.line, buffer.column)
                        )
                    )
                    buffer.advance()
                }

                '=' -> {
                    if (buffer.peek(1) == '=') {
                        tokens.add(
                            Token.Raw(
                                Token.Type.OP_EQ,
                                "==",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance(); buffer.advance()
                    } else {
                        tokens.add(
                            Token.Raw(
                                Token.Type.S_EQUAL,
                                "=",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance()
                    }
                }

                ',' -> {
                    tokens.add(
                        Token.Raw(
                            Token.Type.S_COMMA,
                            ",",
                            buffer.position,
                            SourcePosition(buffer.line, buffer.column)
                        )
                    )
                    buffer.advance()
                }

                '<' -> {
                    when (buffer.peek(1)) {
                        '=' -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.OP_LEQ,
                                    "<=",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance(); buffer.advance()
                        }

                        '<' -> {
                            if (buffer.peek(2) == '=') {
                                tokens.add(
                                    Token.Raw(
                                        Token.Type.OP_SHL_EQ,
                                        "<<=",
                                        buffer.position,
                                        SourcePosition(
                                            buffer.line,
                                            buffer.column
                                        )
                                    )
                                )
                                buffer.advance(); buffer.advance(); buffer.advance()
                            } else {
                                tokens.add(
                                    Token.Raw(
                                        Token.Type.OP_SHL,
                                        "<<",
                                        buffer.position,
                                        SourcePosition(
                                            buffer.line,
                                            buffer.column
                                        )
                                    )
                                )
                                buffer.advance(); buffer.advance()
                            }
                        }

                        else -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.S_OPEN_ANGLE,
                                    "<",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance()
                        }
                    }
                }

                '>' -> {
                    when (buffer.peek(1)) {
                        '=' -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.OP_GEQ,
                                    ">=",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance(); buffer.advance()
                        }

                        '>' -> {
                            if (buffer.peek(2) == '=') {
                                tokens.add(
                                    Token.Raw(
                                        Token.Type.OP_SHR_EQ,
                                        ">>=",
                                        buffer.position,
                                        SourcePosition(
                                            buffer.line,
                                            buffer.column
                                        )
                                    )
                                )
                                buffer.advance(); buffer.advance(); buffer.advance()
                            } else {
                                tokens.add(
                                    Token.Raw(
                                        Token.Type.OP_SHR,
                                        ">>",
                                        buffer.position,
                                        SourcePosition(
                                            buffer.line,
                                            buffer.column
                                        )
                                    )
                                )
                                buffer.advance(); buffer.advance()
                            }
                        }

                        else -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.S_CLOSE_ANGLE,
                                    ">",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance()
                        }
                    }
                }

                '&' -> {
                    when (buffer.peek(1)) {
                        '&' -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.OP_AND,
                                    "&&",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance(); buffer.advance()
                        }

                        '=' -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.OP_AMP_EQ,
                                    "&=",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance(); buffer.advance()
                        }

                        else -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.S_AMPERSAND,
                                    "&",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance()
                        }
                    }
                }

                '|' -> {
                    when (buffer.peek(1)) {
                        '|' -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.OP_OR,
                                    "||",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance(); buffer.advance()
                        }

                        '=' -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.OP_PIPE_EQ,
                                    "|=",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance(); buffer.advance()
                        }

                        else -> {
                            tokens.add(
                                Token.Raw(
                                    Token.Type.S_PIPE,
                                    "|",
                                    buffer.position,
                                    SourcePosition(
                                        buffer.line,
                                        buffer.column
                                    )
                                )
                            )
                            buffer.advance()
                        }
                    }
                }

                '!' -> {
                    if (buffer.peek(1) == '=') {
                        tokens.add(
                            Token.Raw(
                                Token.Type.OP_NEQ,
                                "!=",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance(); buffer.advance()
                    } else {
                        tokens.add(
                            Token.Raw(
                                Token.Type.S_BANG,
                                "!",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance()
                    }
                }

                '^' -> {
                    if (buffer.peek(1) == '=') {
                        tokens.add(
                            Token.Raw(
                                Token.Type.OP_CARET_EQ,
                                "^=",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance(); buffer.advance()
                    } else {
                        tokens.add(
                            Token.Raw(
                                Token.Type.OP_BITWISE_XOR,
                                "^",
                                buffer.position,
                                SourcePosition(
                                    buffer.line,
                                    buffer.column
                                )
                            )
                        )
                        buffer.advance()
                    }
                }

                '~' -> {
                    tokens.add(
                        Token.Raw(
                            Token.Type.S_TILDE,
                            "~",
                            buffer.position,
                            SourcePosition(
                                buffer.line,
                                buffer.column
                            )
                        )
                    )
                    buffer.advance()
                }

                ' ', '\t', '\r' -> buffer.advance() // skip whitespace
                else -> {
                    buffer.advance()
                }
            }
        }
        tokens.add(
            Token.Raw(
                Token.Type.S_EOF,
                "",
                buffer.position,
                SourcePosition(buffer.line, buffer.column)
            )
        )
        return tokens
    }

    private fun lexNumber(): Token {
        val startPos = buffer.position
        val startLine = buffer.line
        val startCol = buffer.column
        var isHex = false
        var isBinary = false
        var hasDot = false
        if (buffer.current == '0') {
            if (buffer.peek(1) == 'x') {
                isHex = true
                buffer.advance()
                buffer.advance()
            } else if (buffer.peek(1) == 'b') {
                isBinary = true
                buffer.advance()
                buffer.advance()
            }
        }
        while (!buffer.isAtEnd && (
                    buffer.current.isDigit() ||
                    (isHex && buffer.current in 'a'..'f' || buffer.current in 'A'..'F') ||
                    (isBinary && buffer.current in '0'..'1') ||
                    (!isHex && !isBinary && buffer.current == '.' && !hasDot)
                                  )
        ) {
            if (buffer.current == '.') {
                hasDot = true
            }
            buffer.advance()
        }
        return Token.Raw(
            when {
                hasDot -> Token.Type.L_FLOAT
                else -> Token.Type.L_INTEGER
            },
            input.substring(startPos, buffer.position),
            startPos,
            SourcePosition(startLine, startCol)
        )
    }

    private fun lexString(): Token {
        val startPos = buffer.position
        val startLine = buffer.line
        val startCol = buffer.column
        buffer.advance() // skip opening "
        val sb = StringBuilder()
        while (!buffer.isAtEnd && buffer.current != '"') {
            if (buffer.current == '\\') {
                buffer.advance()
                if (!buffer.isAtEnd) {
                    with(sb) {
                        when (buffer.current) {
                            'n' -> append('\n')
                            't' -> append('\t')
                            'r' -> append('\r')
                            'b' -> append('\b')
                            'f' -> append('\u000C')
                            '\\' -> append('\\')
                            '"' -> append('"')
                            else -> append(buffer.current) // unknown escape, just append
                        }
                    }
                    buffer.advance()
                }
            } else {
                sb.append(buffer.current)
                buffer.advance()
            }
        }
        buffer.advance() // skip closing "
        return Token.Raw(
            Token.Type.L_STRING,
            sb.toString(),
            startPos,
            SourcePosition(startLine, startCol)
        )
    }

    private fun lexChar(): Token {
        val startPos = buffer.position
        val startLine = buffer.line
        val startCol = buffer.column
        buffer.advance() // skip opening '
        var ch = '\u0000'
        if (!buffer.isAtEnd) {
            if (buffer.current == '\\') {
                buffer.advance()
                if (!buffer.isAtEnd) {
                    ch = when (buffer.current) {
                        'n' -> '\n'
                        't' -> '\t'
                        'r' -> '\r'
                        'b' -> '\b'
                        'f' -> '\u000C'
                        '\\' -> '\\'
                        '\'' -> '\''
                        '"' -> '"'
                        else -> buffer.current
                    }
                    buffer.advance()
                }
            } else {
                ch = buffer.current
                buffer.advance()
            }
        }

        // skip closing '
        if (!buffer.isAtEnd && buffer.current == '\'') {
            buffer.advance()
        } else if (!buffer.isAtEnd) {
            diagnostics.reportError(
                "Expected closing single quote for character literal",
                SourceSpan.single(SourcePosition(buffer.line, buffer.column)),
                "Add a closing single quote (') here",
                "E003"
            )
        } else {
            diagnostics.reportError(
                "Unterminated character literal",
                SourceSpan.single(SourcePosition(startLine, startCol)),
                "Add a closing single quote (') at the end",
                "E003"
            )
        }

        return Token.Raw(
            Token.Type.L_CHAR,
            ch.toString(),
            startPos,
            SourcePosition(startLine, startCol)
        )
    }

    private fun lexIdentifier(): Token {
        val startPos = buffer.position
        val startLine = buffer.line
        val startCol = buffer.column
        while (!buffer.isAtEnd && (buffer.current.isLetterOrDigit() || buffer.current == '_' || buffer.current == '@')) {
            buffer.advance()
        }
        val str = input.substring(startPos, buffer.position)
        val keywords = Token.Type.entries.filter {
            it.name.startsWith("K_")
        }.associateBy {
            val x = it.name.substring(2).replace("'", "")
            when (it) { // for hardcoding shit
                Token.Type.K__ULONG,
                Token.Type.K__UBYTE,
                Token.Type.K__UINT ->
                    "_${x[1].uppercase()}${x[2].uppercase()}${
                        x.substring(3).lowercase()
                    }"

                Token.Type.K__FLOAT,
                Token.Type.K__SHORT,
                Token.Type.K__INT,
                Token.Type.K__BYTE,
                Token.Type.K__BOOL,
                Token.Type.K__LONG,
                Token.Type.K__DOUBLE,
                Token.Type.K__UNIT ->
                    "_${
                        x[1].uppercase()
                    }${
                        x.substring(2).lowercase()
                    }"

                else -> x.lowercase()
            }
        }
        return Token.Raw(
            keywords[str] ?: Token.Type.IDENTIFIER,
            str,
            startPos,
            SourcePosition(startLine, startCol)
        )
    }
}