package net.exoad.cuu

class Transpiler : NodeVisitor<Unit>() {
    val sb = StringBuilder()
    private val includes = mutableSetOf<String>()

    val sharedHContent: String by lazy {
        CLang.build {
            comment("Generated at: ${java.time.Instant.now()}")
            ppHeaderGuard("__COMPILER_BUNDLE_K_SHARED_H__")
            ppInclude("stdint.h")
            ppDefine(Mangler["true_k"], "1")
            ppDefine(Mangler["false_k"], "0")
            ppDefine(Mangler["persistent_k"], "static")
            typedef(Mangler["int_t"], "int32_t")
            typedef(Mangler["float_t"], "float")
            typedef(Mangler["double_t"], "double")
            typedef(Mangler["char_t"], "int8_t")
            typedef(Mangler["short_t"], "int16_t")
            typedef(Mangler["long_t"], "int64_t")
            typedef(Mangler["bool_t"], "uint8_t")
            typedef(Mangler["ubyte_t"], "uint8_t")
            typedef(Mangler["uint_t"], "uint32_t")
            typedef(Mangler["ulong_t"], "uint64_t")
            typedef(Mangler["unit_t"], "void")
            appendRaw("\n")
            ppIfDef("__GNUC__")
            ppDefine(Mangler["immutable_k"], "const")
            ppDefine(Mangler["mutable_k"], CLang.ppAttribute("unused"))
            ppElse()
            ppDefine(Mangler["immutable_k"], "const")
            ppDefine(Mangler["mutable_k"], "")
            ppEndIf()
            ppEndIf()
        }
    }

    override fun visitModule(module: Module) {
//        throw NotImplementedError("Not implemented yet")
        sb.appendLine(sharedHContent)
        sb.append(CLang.build {
            comment("-- MODULE DECLARE : ${module.canonName}")
        })
        module.statements.forEach {
            it.accept(this)
        }
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
            is VariableDecl -> node.init?.let { collectIncludes(it) }
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

    override fun visitVarDecl(variableDecl: VariableDecl) {
        if (variableDecl.modifiers.contains(Modifier.MUTABLE)) {
            sb.append(Mangler["mutable_k"])
            sb.append(" ")
        } else {
            sb.append(Mangler["immutable_k"])
            sb.append(" ")
        }
        variableDecl.type.accept(this)
        sb.append(" ${variableDecl.name}=")
        variableDecl.init!!.accept(this)
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
                    if (literal.value) Mangler["true_k"]
                    else Mangler["false_k"]
                )

                is Literal.LFloat -> append(literal.value)
            }
        }
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
                        "_Int" -> Mangler["int_t"]
                        "_Float" -> Mangler["float_t"]
                        "_Double" -> Mangler["double_t"]
                        "_Byte" -> Mangler["char_t"]
                        "_Short" -> Mangler["short_t"]
                        "_Long" -> Mangler["long_t"]
                        "_Bool" -> Mangler["bool_t"]
                        "_Unit" -> Mangler["unit_t"]
                        "_UInt" -> Mangler["uint_t"]
                        "_UByte" -> Mangler["ubyte_t"]
                        "_ULong" -> Mangler["ulong_t"]
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
        // ignored
    }

    override fun visitFunctionDecl(functionDecl: FunctionDecl) {
        // ignored
    }

    override fun visitDeferStmt(deferStmt: DeferStmt) {
        // ignored
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