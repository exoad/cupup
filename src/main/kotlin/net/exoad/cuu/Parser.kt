package net.exoad.cuu

import net.exoad.cuu.diagnostics.DiagnosticCollector
import net.exoad.cuu.diagnostics.SourceSpan
import kotlin.collections.emptyList

class Parser(
    private val tokens: List<Token>,
    private val diagnostics: DiagnosticCollector = DiagnosticCollector()
) {
    private var position = 0
    private var lastTokenPosition: SourcePosition? = null
    private var currentMembership = Membership.MODULE

    private val currentToken: Token
        get() = tokens[position]

    private val isAtEnd: Boolean
        get() = currentToken.type == Token.Type.S_EOF

    private fun skipWhitespace() {
        while (!isAtEnd && currentToken.type == Token.Type.S_NEWLINE) {
            position++
        }
    }

    private fun peek(offset: Int = 0): Token {
        val index = position + offset
        if (index >= tokens.size) {
            return tokens.last()
        }
        return tokens[index]
    }

    private fun advance(count: Int = 1) {
        position += count
    }

    private fun at(type: Token.Type): Boolean {
        return !isAtEnd && peek().type == type
    }

    private fun expect(
        type: Token.Type,
        hint: String = "Try adding the expected token here",
        consumer: ((Token) -> Unit) = { advance() }
    ) {
        if (!at(type)) {
            val current = peek()
            diagnostics.reportError(
                "Expected ${type.rawDiagnosticsRepresentation}, but found ${current.type.rawDiagnosticsRepresentation}",
                SourceSpan.single(current.canonicalLocation),
                hint,
                "P001"
            )
            // Don't throw immediately - allow parser to continue and collect more errors
            // Skip tokens until we find a reasonable recovery point
            skipErrorRecovery(type)
            return
        }
        consumer(peek())
    }

    private fun skipErrorRecovery(expected: Token.Type) {
        var skipCount = 0
        while (!isAtEnd && skipCount < 20) {
            if (at(expected)) {
                advance()
                return
            }
            if (at(Token.Type.S_CLOSE_BRACE) ||
                at(Token.Type.S_SEMICOLON) ||
                at(Token.Type.S_NEWLINE)
            ) {
                return
            }
            advance()
            skipCount++
        }
    }

    private fun expectOptional(type: Token.Type): Boolean {
        if (at(type)) {
            advance()
            return true
        }
        return false
    }

    private fun here(): SourcePosition {
        return peek().canonicalLocation
    }

    private fun rememberMembership(
        membership: Membership,
        runnable: () -> Unit
    ) {
        val old = currentMembership
        currentMembership = membership
        runnable()
        currentMembership = old
    }

    private fun parseModifiers(): List<Modifier> {
        val modifiers = mutableSetOf<Modifier>()
        while (Token.Type.allModifiers.contains(peek().type)) {
            // might cache this here instead of relooping everything
            val x = Modifier.related[peek().type]
            if (x != null) {
                if (modifiers.contains(x)) {
                    diagnostics.reportError(
                        "Duplicate modifier '${
                            peek().type
                        }'",
                        span = SourceSpan.single(here())
                    )
                }
                modifiers.add(x)
                advance()
            } else {
                return modifiers.toList()
            }
        }
        // never return here
        return modifiers.toList()
    }

    fun parseProgram(): Module {
        if (!at(Token.Type.K_MOD)) {
            diagnostics.reportError(
                "Missing module declaration. Instead got ${peek().type.name}",
                SourceSpan.single(SourcePosition(1, 1)),
                "Add 'mod <module_name>' at the top of the file, for example: 'mod mymodule'",
                "P001"
            )
            return Module("error", emptyList())
        }
        advance() // consume mod
        val nameToken = peek()
        expect(Token.Type.IDENTIFIER)
        val moduleName = nameToken.content
        if (!moduleName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
            diagnostics.reportError(
                "Invalid module name: must be a valid C-style identifier",
                SourceSpan.single(nameToken.canonicalLocation),
                "Use a name starting with a letter or underscore, followed by letters, digits, or underscores",
                "P001"
            )
        }
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd) {
            skipWhitespace()
            if (!isAtEnd) {
                statements.add(parseStatement())
            }
        }
        return Module(moduleName, statements)
    }

    private fun parseStatement(): Stmt {
        skipWhitespace()
        var potentialModifiers = emptyList<Modifier>()
        if (Modifier.related[peek().type] != null) {
            potentialModifiers = parseModifiers()
        }
        when {
            at(Token.Type.K_RECORD) -> return parseRecordDecl(potentialModifiers)
            at(Token.Type.K_FX) -> return parseFunctionDecl(potentialModifiers)
            at(Token.Type.K_ALIAS) -> return parseTypeAlias()
            at(Token.Type.K_IF) -> return parseIfStmt()
            at(Token.Type.K_WHILE) -> return parseWhileStmt()
            at(Token.Type.K_DEFER) -> return parseDeferStmt()
            at(Token.Type.K_BREAK) -> {
                advance()
                return BreakStmt()
            }

            at(Token.Type.K_CONTINUE) -> {
                advance()
                return ContinueStmt()
            }

            at(Token.Type.IDENTIFIER) -> {
                val typePos = if (at(Token.Type.K_MUT)) 3 else 2
                if (peek(typePos - 1).type == Token.Type.S_COLON && peek(
                        typePos
                    ).type in setOf(
                        Token.Type.IDENTIFIER,
                        Token.Type.K__INT,
                        Token.Type.K__FLOAT,
                        Token.Type.K__DOUBLE,
                        Token.Type.K__BYTE,
                        Token.Type.K__SHORT,
                        Token.Type.K__LONG,
                        Token.Type.K__BOOL,
                        Token.Type.K__UNIT
                    ) && peek(typePos + 1).type == Token.Type.S_EQUAL
                ) {
                    return parseVarDecl(potentialModifiers)
                }
            }
        }
        val expr = parseExpression()
        skipWhitespace()
        if (diagnostics.hasErrors) {
            while (!isAtEnd && !at(Token.Type.S_NEWLINE) && !at(Token.Type.S_CLOSE_BRACE)) {
                advance()
            }
            skipWhitespace()
        }

        return ExprStmt(expr)
    }

    private fun parseVarDecl(potentialModifiers: List<Modifier>): VariableDecl {
        val nameToken = peek()
        advance()
        expect(
            Token.Type.S_COLON,
            "Variable declarations require a type annotation, e.g., let x: Int = 5"
        )
        val type = parseType()
        expect(
            Token.Type.S_EQUAL,
            "Variable declarations require an initializer, e.g., let x: Int = value"
        )
        val init = parseExpression()
        skipWhitespace()
        return VariableDecl(
            nameToken.content,
            type,
            init,
            membership = currentMembership
        )
    }

    private fun parseExpression(): Expr {
        if (isAtEnd) {
            val current = peek()
            diagnostics.reportError(
                "Unexpected end of file while parsing expression",
                SourceSpan.single(current.canonicalLocation),
                "Add an expression before the end of file",
                "P002"
            )
            return Literal.LInt(0)
        }
        return parseAssignment()
    }

    // Assignment (right-associative)
    private fun parseAssignment(): Expr {
        val left = parseCast()
        when (peek().type) {
            Token.Type.S_EQUAL,
            Token.Type.OP_PLUS_EQ,
            Token.Type.OP_DASH_EQ,
            Token.Type.OP_ASTERISK_EQ,
            Token.Type.OP_FORWARD_SLASH_EQ,
            Token.Type.OP_MOD_EQ,
            Token.Type.OP_SHL_EQ,
            Token.Type.OP_SHR_EQ,
            Token.Type.OP_AMP_EQ,
            Token.Type.OP_PIPE_EQ,
            Token.Type.OP_CARET_EQ -> {
                val op = peek().content
                advance()
                val right = parseAssignment()
                return BinaryOp(op, left, right)
            }

            else -> return left
        }
    }

    // Casts using 'as'
    private fun parseCast(): Expr {
        var expr = parseLogicalOr()
        while (at(Token.Type.K_AS)) {
            expect(Token.Type.K_AS)
            val target = parseType()
            expr = Cast(expr, target)
        }
        return expr
    }

    private fun parseLogicalOr(): Expr {
        var expr = parseLogicalAnd()
        while (peek().type == Token.Type.OP_OR) {
            val op = peek().content
            advance()
            val right = parseLogicalAnd()
            expr = BinaryOp(op, expr, right)
        }
        return expr
    }

    private fun parseLogicalAnd(): Expr {
        var expr = parseBitwiseOr()
        while (peek().type == Token.Type.OP_AND) {
            val op = peek().content
            advance()
            val right = parseBitwiseOr()
            expr = BinaryOp(op, expr, right)
        }
        return expr
    }

    private fun parseBitwiseOr(): Expr {
        var expr = parseBitwiseXor()
        while (peek().type == Token.Type.S_PIPE || peek().type == Token.Type.OP_PIPE) {
            val op = peek().content
            advance()
            val right = parseBitwiseXor()
            expr = BinaryOp(op, expr, right)
        }
        return expr
    }

    private fun parseBitwiseXor(): Expr {
        var expr = parseBitwiseAnd()
        while (peek().type == Token.Type.OP_BITWISE_XOR) {
            val op = peek().content
            advance()
            val right = parseBitwiseAnd()
            expr = BinaryOp(op, expr, right)
        }
        return expr
    }

    private fun parseBitwiseAnd(): Expr {
        var expr = parseEquality()
        while (peek().type == Token.Type.S_AMPERSAND || peek().type == Token.Type.OP_AMPERSAND) {
            val op = peek().content
            advance()
            val right = parseEquality()
            expr = BinaryOp(op, expr, right)
        }
        return expr
    }

    private fun parseEquality(): Expr {
        var expr = parseRelational()
        while (peek().type == Token.Type.OP_EQ || peek().type == Token.Type.OP_NEQ) {
            val op = peek().content
            advance()
            val right = parseRelational()
            expr = BinaryOp(op, expr, right)
        }
        return expr
    }

    private fun parseRelational(): Expr {
        var expr = parseShift()
        while (
            peek().type == Token.Type.S_OPEN_ANGLE ||
            peek().type == Token.Type.S_CLOSE_ANGLE ||
            peek().type == Token.Type.OP_LEQ ||
            peek().type == Token.Type.OP_GEQ
        ) {
            val op = peek().content
            advance()
            val right = parseShift()
            expr = BinaryOp(op, expr, right)
        }
        return expr
    }

    private fun parseShift(): Expr {
        var expr = parseAdditive()
        while (peek().type == Token.Type.OP_SHL || peek().type == Token.Type.OP_SHR) {
            val op = peek().content
            advance()
            val right = parseAdditive()
            expr = BinaryOp(op, expr, right)
        }
        return expr
    }

    private fun parseAdditive(): Expr {
        var expr = parseMultiplicative()
        while (!isAtEnd && (peek().type == Token.Type.S_PLUS || peek().type == Token.Type.S_DASH)) {
            val op = peek().content
            advance()
            if (isAtEnd) {
                val current = peek()
                diagnostics.reportError(
                    "Unexpected end of file after operator '$op'",
                    SourceSpan.single(current.canonicalLocation),
                    "Add an expression after the operator",
                    "P003"
                )
                return expr
            }
            val right = parseMultiplicative()
            expr = BinaryOp(op, expr, right)
        }
        return expr
    }

    private fun parseMultiplicative(): Expr {
        var expr = parseUnary()
        while (!isAtEnd && (peek().type == Token.Type.S_ASTERISK || peek().type == Token.Type.S_FORWARD_SLASH || peek().type == Token.Type.OP_MOD)) {
            val op = peek().content
            advance()
            if (isAtEnd) {
                val current = peek()
                diagnostics.reportError(
                    "Unexpected end of file after operator '$op'",
                    SourceSpan.single(current.canonicalLocation),
                    "Add an expression after the operator",
                    "P003"
                )
                return expr
            }
            val right = parseUnary()
            expr = BinaryOp(op, expr, right)
        }
        return expr
    }

    private fun parseUnary(): Expr {
        if (peek().type == Token.Type.S_PLUS || peek().type == Token.Type.S_DASH || peek().type == Token.Type.S_BANG || peek().type == Token.Type.S_TILDE || peek().type == Token.Type.OP_INC || peek().type == Token.Type.OP_DEC) {
            val op = peek().content
            advance()
            val operand = parseUnary()
            return UnaryOp(op, operand, true)
        }
        return parsePostfix()
    }

    private fun parsePostfix(): Expr {
        var expr = parsePrimary()
        while (peek().type == Token.Type.OP_INC || peek().type == Token.Type.OP_DEC) {
            val op = peek().content
            advance()
            expr = UnaryOp(op, expr, false)
        }
        return expr
    }

    private fun parsePrimary(): Expr {
        val token = peek()
        advance()
        return when (token.type) {
            Token.Type.L_INTEGER -> {
                val raw = token.content
                val intValue = when {
                    raw.startsWith("0x") || raw.startsWith("0X") -> Integer.parseInt(
                        raw.substring(2),
                        16
                    )

                    raw.startsWith("0b") || raw.startsWith("0B") -> Integer.parseInt(
                        raw.substring(2),
                        2
                    )

                    else -> Integer.parseInt(raw)
                }
                Literal.LInt(intValue)
            }

            Token.Type.L_FLOAT -> Literal.LFloat(token.content.toDouble())
            Token.Type.L_STRING -> Literal.LString(token.content)
            Token.Type.K_TRUE -> Literal.LBool(true)
            Token.Type.K_FALSE -> Literal.LBool(false)
            Token.Type.IDENTIFIER,
            Token.Type.K__INT,
            Token.Type.K__FLOAT,
            Token.Type.K__DOUBLE,
            Token.Type.K__BYTE,
            Token.Type.K__SHORT,
            Token.Type.K__LONG,
            Token.Type.K__BOOL,
            Token.Type.K__UNIT -> {
                val ident = Identifier(token.content)
                val typeArgs = if (at(Token.Type.S_OPEN_BRACKET)) {
                    expect(Token.Type.S_OPEN_BRACKET)
                    val types = mutableListOf<Type>()
                    while (!at(Token.Type.S_CLOSE_BRACKET) && !isAtEnd && !diagnostics.hasErrors) {
                        types.add(parseType())
                        if (at(Token.Type.S_COMMA)) {
                            expect(Token.Type.S_COMMA)
                        } else if (!at(Token.Type.S_CLOSE_BRACKET)) {
                            val current = peek()
                            diagnostics.reportError(
                                "Expected ',' or ']', but found ${current.type.rawDiagnosticsRepresentation}",
                                SourceSpan.single(current.canonicalLocation),
                                "Add comma to separate type arguments or close bracket",
                                "P001"
                            )
                            break
                        }
                    }
                    expect(Token.Type.S_CLOSE_BRACKET)
                    types
                } else {
                    emptyList()
                }
                if (at(Token.Type.S_OPEN_PARENTHESIS)) {
                    expect(Token.Type.S_OPEN_PARENTHESIS)
                    val args = mutableListOf<Expr>()
                    while (!at(Token.Type.S_CLOSE_PARENTHESIS) && !isAtEnd && !diagnostics.hasErrors) {
                        args.add(parseExpression())
                        if (!at(Token.Type.S_CLOSE_PARENTHESIS)) {
                            if (at(Token.Type.S_COMMA)) {
                                advance()
                            } else if (isAtEnd || at(Token.Type.S_NEWLINE) || at(
                                    Token.Type.S_CLOSE_BRACE
                                )
                            ) {
                                if (!at(Token.Type.S_CLOSE_PARENTHESIS)) {
                                    val current = peek()
                                    diagnostics.reportError(
                                        "Expected ')', but found ${current.type.rawDiagnosticsRepresentation}",
                                        SourceSpan.single(current.canonicalLocation),
                                        "Add closing parenthesis before this token",
                                        "P001"
                                    )
                                }
                                break
                            } else {
                                val current = peek()
                                diagnostics.reportError(
                                    "Expected ',' or ')', but found ${current.type.rawDiagnosticsRepresentation}",
                                    SourceSpan.single(current.canonicalLocation),
                                    "Add comma to separate arguments or close parenthesis",
                                    "P001"
                                )
                                break
                            }
                        }
                    }
                    if (at(Token.Type.S_CLOSE_PARENTHESIS)) {
                        expect(Token.Type.S_CLOSE_PARENTHESIS)
                    }
                    Call(ident, typeArgs, args)
                } else {
                    ident
                }
            }

            Token.Type.S_OPEN_PARENTHESIS -> {
                val expr = parseExpression()
                if (!at(Token.Type.S_CLOSE_PARENTHESIS)) {
                    val current = peek()
                    diagnostics.reportError(
                        "Expected ')', but found ${current.type.rawDiagnosticsRepresentation}",
                        SourceSpan.single(current.canonicalLocation),
                        "Add closing parenthesis",
                        "P001"
                    )
                } else {
                    expect(Token.Type.S_CLOSE_PARENTHESIS)
                }
                expr
            }

            Token.Type.S_EOF -> {
                diagnostics.reportError(
                    "Unexpected end of file while parsing expression",
                    SourceSpan.single(token.canonicalLocation),
                    "Add an expression here",
                    "P002"
                )
                Literal.LInt(0)
            }

            else -> {
                diagnostics.reportError(
                    "Unexpected token: ${token.type.rawDiagnosticsRepresentation}",
                    SourceSpan.single(token.canonicalLocation),
                    "Expected an expression here",
                    "P004"
                )
                Literal.LInt(0)
            }
        }
    }

    private fun parseVariable(): VariableDecl {
        val nameToken = peek()
        expect(Token.Type.IDENTIFIER)
        val name = nameToken.content
        expect(Token.Type.S_COLON)
        return VariableDecl(name, parseType(), membership = currentMembership)
    }

    private fun parseTypeAlias(): TypeAlias {
        expect(Token.Type.K_ALIAS)
        val original = parseType()
        expect(
            Token.Type.K_AS,
            "Type aliases use 'as' to specify the new name, e.g., alias OldType as NewType"
        )
        val aliasToken = peek()
        expect(Token.Type.IDENTIFIER)
        skipWhitespace()
        return TypeAlias(original, aliasToken.content)
    }

    private fun parseRecordDecl(modifiers: List<Modifier>): RecordDecl {
        val badModifiers =
                modifiers.collectInvalidModifiers(ModifierLocaleContext.RECORD)
        if (badModifiers.isNotEmpty()) {
            badModifiers.forEach {
                diagnostics.reportError(
                    "The modifier '${it.name}' is not allowed on a record",
                    span = SourceSpan.single(here()),
                    code = "P006"
                )
            }
        }
        expect(Token.Type.K_RECORD)
        val name = parseType()
        expect(Token.Type.S_OPEN_BRACE)
        skipWhitespace()
        val functionMembers = mutableListOf<FunctionDecl>()
        val variableMembers = mutableListOf<VariableDecl>()
        rememberMembership(Membership.RECORD) {
            while (peek(0).type != Token.Type.S_CLOSE_BRACE && peek(1).type != Token.Type.S_EOF) {
                if (peek(0).type == Token.Type.K_FX) {
                    functionMembers.add(parseFunctionDecl(modifiers))
                } else {
                    variableMembers.add(parseVariable())
                }
            }
        }
        expect(Token.Type.S_CLOSE_BRACE)
        if (functionMembers.isEmpty() && variableMembers.isEmpty()) {
            diagnostics.reportInfo(
                "Empty record are wasteful, consider using a type alias.",
                span = SourceSpan.single(here())
            )
        }
        return RecordDecl(
            name,
            variableMembers,
            functionMembers,
            modifiers,
            currentMembership
        )
    }

    private fun parseType(): Type {
        val token = peek()
        advance()
        return when (token.type) {
            Token.Type.K__UBYTE,
            Token.Type.K__UINT,
            Token.Type.K__ULONG,
            Token.Type.K__INT,
            Token.Type.K__FLOAT,
            Token.Type.K__DOUBLE,
            Token.Type.K__BYTE,
            Token.Type.K__SHORT,
            Token.Type.K__LONG,
            Token.Type.K__BOOL,
            Token.Type.K__UNIT -> Type.Builtin(token.content)

            Token.Type.IDENTIFIER -> {
                val name = token.content
                if (at(Token.Type.S_OPEN_BRACKET)) {
                    expect(Token.Type.S_OPEN_BRACKET)
                    val args = mutableListOf<Type>()
                    while (!at(Token.Type.S_CLOSE_BRACKET)) {
                        args.add(parseType())
                        if (!at(Token.Type.S_CLOSE_BRACKET)) {
                            expect(
                                Token.Type.S_COMMA,
                                "Generic type arguments must be separated by commas, e.g., List[Int, String]"
                            )
                        }
                    }
                    expect(
                        Token.Type.S_CLOSE_BRACKET,
                        "Generic types must end with ']', e.g., Type[Arg]"
                    )
                    Type.Generic(Type.Named(name), args)
                } else {
                    Type.Named(name)
                }
            }

            else -> {
                diagnostics.reportError(
                    "Expected type, but found ${token.type.rawDiagnosticsRepresentation}",
                    SourceSpan.single(token.canonicalLocation),
                    "Use a valid type name or built-in type like Int, String, etc.",
                    "P005"
                )
                Type.Builtin("_Int")
            }
        }
    }

    private fun parseIfStmt(): IfStmt {
        expect(Token.Type.K_IF)
        if (isAtEnd) {
            val current = peek()
            diagnostics.reportError(
                "Unexpected end of file after 'if'",
                SourceSpan.single(current.canonicalLocation),
                "Add a condition expression after 'if'",
                "P006"
            )
            return IfStmt(Literal.LBool(true), emptyList(), null)
        }
        val condition = parseExpression()
        skipWhitespace()
        expect(
            Token.Type.S_OPEN_BRACE,
            "If statements require a body in braces after the condition, e.g., if cond { ... }"
        )
        val thenBranch = parseBlock()
        expect(
            Token.Type.S_CLOSE_BRACE,
            "If bodies must end with '}', e.g., if cond { ... }"
        )
        var elseBranch: List<Stmt>? = null
        if (expectOptional(Token.Type.K_ELSE)) {
            if (at(Token.Type.K_IF)) {
                elseBranch = listOf(parseIfStmt())
            } else {
                skipWhitespace()
                expect(
                    Token.Type.S_OPEN_BRACE,
                    "Else bodies must be in braces, e.g., else { ... }"
                )
                elseBranch = parseBlock()
                expect(
                    Token.Type.S_CLOSE_BRACE,
                    "Else bodies must end with '}', e.g., else { ... }"
                )
            }
        }
        return IfStmt(condition, thenBranch, elseBranch)
    }

    private fun parseWhileStmt(): WhileStmt {
        expect(Token.Type.K_WHILE)
        if (isAtEnd) {
            val current = peek()
            diagnostics.reportError(
                "Unexpected end of file after 'while'",
                SourceSpan.single(current.canonicalLocation),
                "Add a condition expression after 'while'",
                "P007"
            )
            return WhileStmt(Literal.LBool(true), emptyList())
        }
        val condition = parseExpression()
        skipWhitespace()
        expect(
            Token.Type.S_OPEN_BRACE,
            "While loops require a body in braces after the condition, e.g., while cond { ... }"
        )
        val body = parseBlock()
        expect(
            Token.Type.S_CLOSE_BRACE,
            "While bodies must end with '}', e.g., while cond { ... }"
        )
        return WhileStmt(condition, body)
    }

    private fun parseBlock(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!at(Token.Type.S_CLOSE_BRACE) && !isAtEnd) {
            skipWhitespace()
            if (at(Token.Type.S_CLOSE_BRACE)) {
                break
            }
            statements.add(parseStatement())
        }
        return statements
    }

    private fun parseFunctionDecl(potentialModifiers: List<Modifier>): FunctionDecl {
        expect(Token.Type.K_FX)
        val nameToken = peek()
        expect(Token.Type.IDENTIFIER)
        val name = nameToken.content
        val generics = if (at(Token.Type.S_OPEN_BRACKET)) {
            expect(Token.Type.S_OPEN_BRACKET)
            val gens = mutableListOf<String>()
            while (!at(Token.Type.S_CLOSE_BRACKET)) {
                val genToken = peek()
                expect(Token.Type.IDENTIFIER)
                gens.add(genToken.content)
                if (!at(Token.Type.S_CLOSE_BRACKET)) {
                    expect(Token.Type.S_COMMA)
                }
            }
            expect(Token.Type.S_CLOSE_BRACKET)
            gens
        } else {
            emptyList()
        }
        expect(
            Token.Type.S_COLON,
            "Function declarations require a return type after the name, e.g., fx func: Int { ... }"
        )
        val returnType = parseType()
        expect(
            Token.Type.S_OPEN_PARENTHESIS,
            "Function parameters must be in parentheses after the return type, e.g., (param: Type)"
        )
        val parameters = mutableListOf<VariableDecl>()
        rememberMembership(Membership.FUNCTION_PARAMETER) {
            while (!at(Token.Type.S_CLOSE_PARENTHESIS)) {
                val paramName = peek()
                expect(Token.Type.IDENTIFIER)
                expect(
                    Token.Type.S_COLON,
                    "Parameters require a type annotation, e.g., param: Int"
                )
                val paramType = parseType()
                parameters.add(
                    VariableDecl(
                        paramName.content,
                        paramType,
                        null,
                        membership = currentMembership
                    )
                ) // dummy init
                if (!at(Token.Type.S_CLOSE_PARENTHESIS)) {
                    expect(
                        Token.Type.S_COMMA,
                        "Multiple parameters must be separated by commas, e.g., (a: Int, b: String)"
                    )
                }
            }
        }
        expect(
            Token.Type.S_CLOSE_PARENTHESIS,
            "Parameter list must end with ')', e.g., (param: Type)"
        )
        skipWhitespace()
        lateinit var body: List<Stmt>
        rememberMembership(Membership.FUNCTION_LOCAL) {
            expect(
                Token.Type.S_OPEN_BRACE,
                "Function bodies must start with '{', e.g., { statements }"
            )
            body = parseBlock()
            expect(
                Token.Type.S_CLOSE_BRACE,
                "Function bodies must end with '}', e.g., { statements }"
            )
        }
        return FunctionDecl(
            name,
            emptyList(),
            generics,
            returnType,
            currentMembership,
            parameters,
            body,
        )
    }

    private fun parseDeferStmt(): DeferStmt {
        expect(Token.Type.K_DEFER)
        skipWhitespace()
        expect(
            Token.Type.S_OPEN_BRACE,
            "Defer statements require a body in braces, e.g., defer { ... }"
        )
        val body = parseBlock()
        expect(
            Token.Type.S_CLOSE_BRACE,
            "Defer bodies must end with '}', e.g., defer { ... }"
        )
        return DeferStmt(body)
    }
}