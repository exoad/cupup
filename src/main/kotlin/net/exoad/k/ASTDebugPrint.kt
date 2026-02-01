package net.exoad.k

class ASTDebugPrint : NodeVisitor<String> {
    private var currentIndent = ""

    override fun visitModule(module: Module): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val statements =
            module.statements.joinToString("\n") { it.accept(this) }
        currentIndent = oldIndent
        return "${currentIndent}Module {\n${currentIndent}  Name: ${module.canonName}\n$statements\n${currentIndent}}"
    }

    override fun visitExprStmt(exprStmt: ExprStmt): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val exprStr = exprStmt.expr.accept(this)
        currentIndent = oldIndent
        return "${currentIndent}ExprStmt {\n$exprStr\n${currentIndent}}"
    }

    override fun visitVarDecl(varDecl: VarDecl): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val typeStr = varDecl.type.accept(this)
        val initStr = varDecl.init?.accept(this) ?: "null"
        currentIndent = oldIndent
        return "${currentIndent}VarDecl {\n${currentIndent}  Name = ${
            varDecl
                .name
        }\n${currentIndent}  Type = $typeStr\n${currentIndent}  Init = " +
                "$initStr\n${currentIndent}  Mutable = ${
                    varDecl
                        .isMutable
                }\n${currentIndent}}"
    }

    override fun visitTypeAlias(typeAlias: TypeAlias): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val originalStr = typeAlias.original.accept(this)
        currentIndent = oldIndent
        return "${currentIndent}TypeAlias {\n${currentIndent}  Original = " +
                "$originalStr\n${currentIndent}  Alias = ${
                    typeAlias
                        .aliasName
                }\n${currentIndent}}"
    }

    override fun <T> visitLiteral(literal: Literal<T>): String {
        return "Literal(Value = \"${
            literal.value.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\b", "\\b")
                .replace("\u000C", "\\f")
        }\", Type = ${literal.type})"
    }

    override fun visitVariable(variable: Variable): String {
        return "Variable(Name = ${variable.name}, Type = ${
            variable.type.accept(
                this
            )
        })"
    }

    override fun visitBinaryOp(binaryOp: BinaryOp): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val leftStr = binaryOp.left.accept(this)
        val rightStr = binaryOp.right.accept(this)
        currentIndent = oldIndent
        return "${currentIndent}BinaryOp(Op = ${binaryOp.op}) " +
                "{\n${currentIndent}  Left = $leftStr\n${currentIndent}  " +
                "Right = $rightStr\n${currentIndent}}"
    }

    override fun visitIdentifier(identifier: Identifier): String {
        return "Identifier(Name = ${identifier.name})"
    }

    override fun visitType(type: Type): String {
        return when (type) {
            is Type.Builtin -> "BuiltinType(Name = ${type.name})"
            is Type.Named -> "NamedType(Name = ${type.name})"
            is Type.Generic -> "GenericType(Base = ${type.base.accept(this)}," +
                    " " +
                    "Args = ${
                        type.args.joinToString(
                            ", "
                        ) { it.accept(this) }
                    })"
        }
    }

    override fun visitIfStmt(ifStmt: IfStmt): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val conditionStr = ifStmt.condition.accept(this)
        val thenStatements =
            ifStmt.thenBranch.joinToString("\n") { it.accept(this) }
        val elseStr = ifStmt.elseBranch?.let { elseStmts ->
            val elseStatements =
                elseStmts.joinToString("\n") { it.accept(this) }
            "\n${currentIndent}else {\n$elseStatements\n${currentIndent}}"
        } ?: ""
        currentIndent = oldIndent
        return "${currentIndent}IfStmt {\n${currentIndent}  Condition = " +
                "$conditionStr\n${currentIndent}  " +
                "Then = \n$thenStatements$elseStr\n${currentIndent}}"
    }

    override fun visitWhileStmt(whileStmt: WhileStmt): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val conditionStr = whileStmt.condition.accept(this)
        val bodyStr = whileStmt.body.joinToString("\n") { it.accept(this) }
        currentIndent = oldIndent
        return "${currentIndent}WhileStmt {\n${currentIndent}  Condition = " +
                "$conditionStr\n${currentIndent}  " +
                "Body = \n$bodyStr\n${currentIndent}}"
    }

    override fun visitDeferStmt(deferStmt: DeferStmt): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val bodyStr = deferStmt.body.joinToString("\n") { it.accept(this) }
        currentIndent = oldIndent
        return "${currentIndent}DeferStmt {\n${currentIndent}  " +
                "Body = \n$bodyStr\n${currentIndent}}"
    }

    override fun visitCall(call: Call): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val calleeStr = call.callee.accept(this)
        val typeArgsStr = call.typeArgs.joinToString(", ") { it.accept(this) }
        val argsStr = call.args.joinToString(", ") { it.accept(this) }
        currentIndent = oldIndent
        return "${currentIndent}Call {\n${currentIndent}  Callee = " +
                "$calleeStr\n${currentIndent}  TypeArgs = " +
                "$typeArgsStr\n${currentIndent}  Args = " +
                "$argsStr\n${currentIndent}}"
    }

    override fun visitFunctionDecl(functionDecl: FunctionDecl): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val genericsStr = functionDecl.generics.joinToString(", ")
        val returnTypeStr = functionDecl.returnType.accept(this)
        val paramsStr = functionDecl.parameters.joinToString(", ") {
            "${it.name}= ${it.type.accept(this)}"
        }
        val bodyStr = functionDecl.body.joinToString("\n") { it.accept(this) }
        currentIndent = oldIndent
        return "${currentIndent}FunctionDecl {\n${currentIndent}  Name = " +
                "${functionDecl.name}\n${currentIndent}  Generics = " +
                "$genericsStr\n${currentIndent}  ReturnType = " +
                "$returnTypeStr\n${currentIndent}  Params = " +
                "$paramsStr\n${currentIndent}  " +
                "Body = \n$bodyStr\n${currentIndent}}"
    }

    override fun visitCast(cast: Cast): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val exprStr = cast.expr.accept(this)
        val typeStr = cast.target.accept(this)
        currentIndent = oldIndent
        return "${currentIndent}Cast {\n${currentIndent}  Expr = " +
                "$exprStr\n${currentIndent}  Target = " +
                "$typeStr\n${currentIndent}}"
    }

    override fun visitUnaryOp(unaryOp: UnaryOp): String {
        val oldIndent = currentIndent
        currentIndent += "  "
        val exprStr = unaryOp.expr.accept(this)
        val pos = if (unaryOp.isPrefix) "prefix" else "postfix"
        currentIndent = oldIndent
        return "${currentIndent}UnaryOp(Op = ${unaryOp.op}, Mode = $pos) " +
                "{\n${currentIndent}  Expr = $exprStr\n${currentIndent}}"
    }

    override fun visitBreakStmt(breakStmt: BreakStmt): String {
        return "BreakStmt"
    }

    override fun visitContinueStmt(continueStmt: ContinueStmt): String {
        return "ContinueStmt"
    }
}