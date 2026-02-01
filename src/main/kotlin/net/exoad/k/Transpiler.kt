package net.exoad.k

class Transpiler : NodeVisitor<Unit> {
    val sb = StringBuilder()
    private val includes = mutableSetOf<String>()

    val sharedHContent: String by lazy {
        CLang.build {
            comment("Generated at: ${java.time.Instant.now()}")
            ppHeaderGuard("__COMPILER_BUNDLE_K_SHARED_H__")
            ppInclude("stdint.h")
            ppDefine(NameMangler["true_k"], "1")
            ppDefine(NameMangler["false_k"], "0")
            ppDefine(NameMangler["persistent_k"], "static")
            typedef(NameMangler["int_t"], "int32_t")
            typedef(NameMangler["float_t"], "float")
            typedef(NameMangler["double_t"], "double")
            typedef(NameMangler["char_t"], "int8_t")
            typedef(NameMangler["short_t"], "int16_t")
            typedef(NameMangler["long_t"], "int64_t")
            typedef(NameMangler["bool_t"], "uint8_t")
            typedef(NameMangler["ubyte_t"], "uint8_t")
            typedef(NameMangler["uint_t"], "uint32_t")
            typedef(NameMangler["ulong_t"], "uint64_t")
            typedef(NameMangler["unit_t"], "void")
            appendRaw("\n")
            ppIfDef("__GNUC__")
            ppDefine(NameMangler["immutable_k"], "const")
            ppDefine(NameMangler["mutable_k"], CLang.ppAttribute("unused"))
            ppElse()
            ppDefine(NameMangler["immutable_k"], "const")
            ppDefine(NameMangler["mutable_k"], "")
            ppEndIf()
            ppEndIf()
        }
    }

    override fun visitModule(module: Module) {
        throw NotImplementedError("Not implemented yet")
    }

    private fun isCompileTimeConstant(expr: Expr): Boolean {
        return when (expr) {
            is Literal<*> -> true
            is UnaryOp -> isCompileTimeConstant(expr.expr)
            is BinaryOp -> isCompileTimeConstant(expr.left) && isCompileTimeConstant(
                expr.right
            )
            else -> false
        }
    }

    private fun collectIncludes(node: Node) {
        when (node) {
            is Module -> node.statements.forEach { collectIncludes(it) }
            is ExprStmt -> collectIncludes(node.expr)
            is VarDecl -> node.init?.let { collectIncludes(it) }
            is TypeAlias -> Unit
            is IfStmt -> {
                collectIncludes(node.condition)
                node.thenBranch.forEach { collectIncludes(it) }
                node.elseBranch?.forEach { collectIncludes(it) }
            }
            is FunctionDecl -> node.body.forEach { collectIncludes(it) }
            is Call -> {
                val callee = node.callee
                if (callee is Identifier && callee.name.startsWith("@")) {
                    val raw = callee.name.substring(1)
                    val idx = raw.indexOf('_')
                    if (idx >= 0) {
                        val header = raw.substring(0, idx)
                        includes.add(header)
                    }
                }
                node.typeArgs.forEach { collectIncludes(it) }
                node.args.forEach { collectIncludes(it) }
            }
            is BinaryOp -> {
                collectIncludes(node.left)
                collectIncludes(node.right)
            }
            is Variable -> Unit
            is Identifier -> Unit
            is Literal<*> -> Unit
            else -> Unit
        }
    }

    override fun visitExprStmt(exprStmt: ExprStmt) {
        exprStmt.expr.accept(this)
        sb.append(";")
    }

    override fun visitBreakStmt(breakStmt: BreakStmt) {
        sb.append("break;")
    }

    override fun visitContinueStmt(continueStmt: ContinueStmt) {
        sb.append("continue;")
    }

    override fun visitVarDecl(varDecl: VarDecl) {
        if (varDecl.isMutable) {
            sb.append("${NameMangler["mutable_k"]} ")
        } else {
            sb.append("${NameMangler["immutable_k"]} ")
        }
        varDecl.type.accept(this)
        sb.append(" ${varDecl.name}=")
        varDecl.init!!.accept(this)
        sb.append(";")
    }

    override fun visitTypeAlias(typeAlias: TypeAlias) {
        sb.append("typedef ")
        typeAlias.original.accept(this)
        sb.append(" ${typeAlias.aliasName};")
    }

    override fun <T> visitLiteral(literal: Literal<T>) {
        with(sb) {
            when (literal) {
                is Literal.LInt -> append(literal.value)
                is Literal.LString -> append(
                    "\"${
                        literal.value
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\t", "\\t")
                            .replace("\r", "\\r")
                            .replace("\b", "\\b")
                            .replace("\u000C", "\\f")
                    }\""
                )
                is Literal.LBool -> append(
                    if (literal.value) NameMangler["true_k"]
                    else NameMangler["false_k"]
                )
                is Literal.LFloat -> append(literal.value)
            }
        }
    }

    override fun visitVariable(variable: Variable) {
        sb.append(variable.name)
    }

    override fun visitBinaryOp(binaryOp: BinaryOp) {
        sb.append("(")
        binaryOp.left.accept(this)
        sb.append(" ${binaryOp.op} ")
        binaryOp.right.accept(this)
        sb.append(")")
    }

    override fun visitIdentifier(identifier: Identifier) {
        sb.append(identifier.name)
    }

    override fun visitType(type: Type) {
        with(sb) {
            when (type) {
                is Type.Builtin -> append(
                    when (type.name) {
                        "_Int" -> NameMangler["int_t"]
                        "_Float" -> NameMangler["float_t"]
                        "_Double" -> NameMangler["double_t"]
                        "_Byte" -> NameMangler["char_t"]
                        "_Short" -> NameMangler["short_t"]
                        "_Long" -> NameMangler["long_t"]
                        "_Bool" -> NameMangler["bool_t"]
                        "_Unit" -> NameMangler["unit_t"]
                        "_UInt" -> NameMangler["uint_t"]
                        "_UByte" -> NameMangler["ubyte_t"]
                        "_ULong" -> NameMangler["ulong_t"]
                        else -> type.name
                    }
                )
                is Type.Generic -> error("No Impl")
                is Type.Named -> append(type.name)
            }
        }
    }

    override fun visitIfStmt(ifStmt: IfStmt) {
        sb.append("if(")
        ifStmt.condition.accept(this)
        sb.append("){")
        ifStmt.thenBranch.forEach {
            it.accept(this)
        }
        sb.append("}")
        ifStmt.elseBranch?.let { elseStmts ->
            sb.append("else{")
            elseStmts.forEach {
                it.accept(this)
            }
            sb.append("}")
        }
    }

    override fun visitWhileStmt(whileStmt: WhileStmt) {
        sb.append("while(")
        whileStmt.condition.accept(this)
        sb.append("){")
        whileStmt.body.forEach { it.accept(this) }
        sb.append("}")
    }

    override fun visitCall(call: Call) {
        throw NotImplementedError("Not implemented yet")
    }

    override fun visitFunctionDecl(functionDecl: FunctionDecl) {
        throw NotImplementedError("Not implemented yet")
    }

    override fun visitDeferStmt(deferStmt: DeferStmt) {
        // Handled in visitFunctionDecl to control ordering; nothing emitted here
    }

    override fun visitCast(cast: Cast) {
        sb.append("(")
        cast.target.accept(this)
        sb.append(")(")
        cast.expr.accept(this)
        sb.append(")")
    }

    override fun visitUnaryOp(unaryOp: UnaryOp) {
        if (unaryOp.isPrefix) {
            sb.append("(")
            sb.append(unaryOp.op)
            unaryOp.expr.accept(this)
            sb.append(")")
        } else {
            sb.append("(")
            unaryOp.expr.accept(this)
            sb.append(")")
            sb.append(unaryOp.op)
        }
    }

    fun transpile(module: Module): String {
        module.accept(this)
        return sb.toString()
    }
}