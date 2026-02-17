package net.exoad.cuu

/**
 * Semantical tokens representing each part of text that was parsed
 */
sealed class Token(
    val type: Type,
    val content: String,
    val pointerPosition: Int,
    val canonicalLocation: SourcePosition
) {
    enum class Type(val rawDiagnosticsRepresentation: String) {
        L_INTEGER("Integer Literal"),
        L_FLOAT("Float Literal"),
        L_STRING("String Literal"),
        L_CHAR("Character Literal"),
        IDENTIFIER("Identifier"),
        S_EOF("'EOF' (End Of File)"),
        S_NEWLINE("'\\n' (Newline)"),
        S_PLUS("'+' (Plus)"),
        S_DASH("'-' (Minus)"),
        S_ASTERISK("'*' (Multiply)"),
        S_FORWARD_SLASH("'/' (Divide)"),
        OP_MOD("'%' (Modulo)"),
        S_EQUAL("'=' (Assignment)"),
        S_OPEN_PARENTHESIS("'(' (Opening Parenthesis)"),
        S_CLOSE_PARENTHESIS("')' (Closing Parenthesis)"),
        S_OPEN_BRACKET("'[' (Opening Bracket)"),
        S_CLOSE_BRACKET("']' (Closing Bracket)"),
        S_OPEN_BRACE("'{' (Opening Brace)"),
        S_CLOSE_BRACE("'}' (Closing Brace)"),
        S_OPEN_ANGLE("'<' (Opening Angle Bracket)"),
        S_CLOSE_ANGLE("'>' (Closing Angle Bracket)"),
        S_COLON("':' (Colon)"),
        S_SEMICOLON("';' (Semicolon)"),
        S_AMPERSAND("'&' (And)"),
        S_UNDERSCORE("'_' (Underscore)"),
        S_PIPE("'|' (Pipe)"),
        S_AMPERSAT("'@' (At)"),
        S_BANG("'!' (Bang)"),
        S_DOT("'.' (Dot)"),
        S_TILDE("'~' (Tilde)"),
        S_COMMA("',' (Comma)"),
        S_QUESTION_MARK("'?', (Question Mark)"),
        K_IF("'if'"),
        K_ELSE("'else'"),
        K_ALIAS("'alias'"),
        K_TRUE("'true'"),
        K_FALSE("'false'"),
        K_MOD("'mod'"),
        K_AS("'as'"),
        K_MUT("'mut'"),
        K_FX("'fx'"),
        K_WHILE("'while'"),
        K_DEFER("'defer'"),
        K_BREAK("'break'"),
        K_RECORD("'record'"),
        K_CONTINUE("'continue'"),

        // builtin stuffs
        K__INT("'_Int'"),
        K__FLOAT("'_Float'"),
        K__DOUBLE("'_Double'"),
        K__BYTE("'_Byte'"),
        K__SHORT("'_Short'"),
        K__BOOL("'_Bool'"),
        K__UNIT("'_Unit'"),
        K__LONG("'_Long'"),
        K__ULONG("'_ULong'"),
        K__UBYTE("'_UByte'"),
        K__UINT("'_UInt'"),

        // C-style operators (single and multi-char)
        OP_INC("'++' (Increment)"),
        OP_DEC("'--' (Decrement)"),
        OP_SHL("'<<' (Shift Left)"),
        OP_SHR("'>>' (Shift Right)"),
        OP_LEQ("'<=' (Less Than or Equal To)"),
        OP_GEQ("'>=' (Greater Than or Equal To)"),
        OP_NEQ("'!=' (Not Equal To)"),
        OP_EQ("'==' (Equal To)"),
        OP_AND("'&&' (Logical And)"),
        OP_OR("'||' (Logical Or)"),
        OP_BITWISE_XOR("'^' (Bitwise Exclusive Or)"),
        OP_CARET_EQ("'^=' (Xor Assign)"),
        OP_ASTERISK_EQ("'*=' (Mul Assign)"),
        OP_FORWARD_SLASH_EQ("'/=' (Div Assign)"),
        OP_MOD_EQ("'%=' (Mod Assign)"),
        OP_PLUS_EQ("'+=' (Add Assign)"),
        OP_DASH_EQ("'-=' (Sub Assign)"),
        OP_SHL_EQ("'<<=' (Shl Assign)"),
        OP_SHR_EQ("'>>=' (Shr Assign)"),
        OP_AMP_EQ("'&=' (And Assign)"),
        OP_PIPE_EQ("'|=' (Or Assign)"),
        OP_AMPERSAND("'&' (Bitwise And)"),
        OP_PIPE("'|' (Bitwise Or)"),
        OP_NOT("'!' (Logical Not)"),
        OP_COMPL("'~' (Bitwise Complement)"),
        OP_EQ_SINGLE("'=' (Assignment)"),
        ;

        fun diagnosticsName(): String {
            return rawDiagnosticsRepresentation
        }

        companion object {
            val allKeywords = entries.filter { it.name.startsWith("K_") }

            val allModifiers = listOf(
                K_MUT
            )

        }
    }

    class Raw(
        type: Type,
        rawString: String,
        pointerPosition: Int,
        canonicalLocation: SourcePosition
    ) :
        Token(type, rawString, pointerPosition, canonicalLocation)

    class Symbol(
        type: Type,
        symbol: String,
        pointerPosition: Int,
        canonicalLocation: SourcePosition
    ) :
        Token(type, symbol, pointerPosition, canonicalLocation)

    class LinkedSymbols(
        type: Type,
        symbols: Array<String>,
        pointerPosition: Int,
        canonicalLocation: SourcePosition
    ) :
        Token(
            type,
            symbols.joinToString("") { it },
            pointerPosition,
            canonicalLocation
        )

    override fun toString(): String {
        return String.format(
            "%-26s %-32s %5d",
            type.name,
            "'$content'",
            pointerPosition
        )
    }
}
