package net.exoad.cuu

abstract class NodeVisitor<R> {
    abstract fun visitModule(module: Module): R
    abstract fun visitExprStmt(exprStmt: ExprStmt): R
    abstract fun visitVarDecl(variableDecl: VariableDecl): R
    abstract fun visitTypeAlias(typeAlias: TypeAlias): R
    abstract fun <T> visitLiteral(literal: Literal<T>): R

    open fun visitRecordDecl(recordDecl: RecordDecl): R = TODO()

    abstract fun visitBinaryOp(binaryOp: BinaryOp): R
    abstract fun visitIdentifier(identifier: Identifier): R
    abstract fun visitType(type: Type): R
    abstract fun visitIfStmt(ifStmt: IfStmt): R
    abstract fun visitCall(call: Call): R
    abstract fun visitFunctionDecl(functionDecl: FunctionDecl): R
    abstract fun visitCast(cast: Cast): R
    abstract fun visitUnaryOp(unaryOp: UnaryOp): R
    abstract fun visitWhileStmt(whileStmt: WhileStmt): R
    abstract fun visitDeferStmt(deferStmt: DeferStmt): R
    abstract fun visitBreakStmt(breakStmt: BreakStmt): R
    abstract fun visitContinueStmt(continueStmt: ContinueStmt): R
}

enum class ModifierLocaleContext {
    FUNCTION_LOCAL_FUNCTION,
    FUNCTION_LOCAL_VARIABLE,
    FUNCTION_PARAMETER,
    RECORD,
    FUNCTION,
    VARIABLE,
    RECORD_MEMBER_VARIABLE,
    RECORD_MEMBER_FUNCTION;
}

enum class Modifier(val allowedLocales: Set<ModifierLocaleContext>) {
    PUBLIC(ModifierLocaleContext.entries.filter {
        when (it) {
            ModifierLocaleContext.FUNCTION_PARAMETER,
            ModifierLocaleContext.FUNCTION_LOCAL_FUNCTION,
            ModifierLocaleContext.FUNCTION_LOCAL_VARIABLE -> false

            else -> true
        }
    }.toSet()),
    MUTABLE(ModifierLocaleContext.entries.filter {
        when (it) {
            ModifierLocaleContext.FUNCTION_PARAMETER -> false

            else -> true
        }
    }.toSet());

}

fun List<Modifier>.validateForLocale(locale: ModifierLocaleContext) {
    forEach {
        if (!it.allowedLocales.contains(locale)) {
            error("Modifier ${it.name} is not allowed in context $locale")
        }
    }
}

fun List<Modifier>.collectInvalidModifiers(locale: ModifierLocaleContext): List<Modifier> {
    val collect = mutableListOf<Modifier>()
    forEach {
        if (!it.allowedLocales.contains(locale)) {
            collect.add(it)
        }
    }
    return collect
}

abstract class Node {
    abstract fun <R> accept(visitor: NodeVisitor<R>): R
}


sealed class Stmt : Node()

sealed class Expr : Node()

data class Module(
    val canonName: String,
    val statements: List<Stmt>
) : Node() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitModule(this)
    }
}

data class ExprStmt(val expr: Expr) : Stmt() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitExprStmt(this)
    }
}

data class VariableDecl(
    val name: String,
    val type: Type,
    val init: Expr? = null,
    val modifiers: List<Modifier> = emptyList()
) :
    Stmt() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitVarDecl(this)
    }
}

data class TypeAlias(val original: Type, val aliasName: String) : Stmt() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitTypeAlias(this)
    }
}

data class Identifier(val name: String) : Expr() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitIdentifier(this)
    }
}

sealed class Literal<T>(val value: T, val type: Token.Type) : Expr() {
    data class LInt(val intValue: Int) :
        Literal<Int>(intValue, Token.Type.L_INTEGER) {
        override fun <R> accept(visitor: NodeVisitor<R>): R {
            return visitor.visitLiteral(this)
        }
    }

    data class LString(val stringValue: String) :
        Literal<String>(stringValue, Token.Type.L_STRING) {
        override fun <R> accept(visitor: NodeVisitor<R>): R {
            return visitor.visitLiteral(this)
        }
    }

    data class LBool(val boolValue: Boolean) :
        Literal<Boolean>(
            boolValue,
            if (boolValue) Token.Type.K_TRUE else Token.Type.K_FALSE
        ) {
        override fun <R> accept(visitor: NodeVisitor<R>): R {
            return visitor.visitLiteral(this)
        }
    }

    data class LFloat(val floatValue: Double) :
        Literal<Double>(floatValue, Token.Type.L_FLOAT) {
        override fun <R> accept(visitor: NodeVisitor<R>): R {
            return visitor.visitLiteral(this)
        }
    }

    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitLiteral(this)
    }
}


data class BinaryOp(val op: String, val left: Expr, val right: Expr) : Expr() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitBinaryOp(this)
    }
}

data class RecordDecl(
    val identifier: Type,
    val variableMembers: List<VariableDecl>,
    val functionMembers: List<FunctionDecl>,
    val modifiers: List<Modifier> = emptyList()
) : Stmt() {
    init {
        modifiers.validateForLocale(ModifierLocaleContext.RECORD)
        variableMembers.forEach {
            modifiers.validateForLocale(ModifierLocaleContext.RECORD_MEMBER_VARIABLE)
        }
        functionMembers.forEach {
            modifiers.validateForLocale(ModifierLocaleContext.RECORD_MEMBER_FUNCTION)
        }
    }

    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitRecordDecl(this)
    }
}

data class UnaryOp(
    val op: String,
    val expr: Expr,
    val isPrefix: Boolean = true
) : Expr() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitUnaryOp(this)
    }
}

data class Cast(val expr: Expr, val target: Type) : Expr() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitCast(this)
    }
}


data class IfStmt(
    val condition: Expr,
    val thenBranch: List<Stmt>,
    val elseBranch: List<Stmt>?
) : Stmt() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitIfStmt(this)
    }
}

data class WhileStmt(
    val condition: Expr,
    val body: List<Stmt>
) : Stmt() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitWhileStmt(this)
    }
}

data class DeferStmt(
    val body: List<Stmt>
) : Stmt() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitDeferStmt(this)
    }
}

class BreakStmt : Stmt() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitBreakStmt(this)
    }
}

class ContinueStmt : Stmt() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitContinueStmt(this)
    }
}

data class FunctionDecl(
    val name: String,
    val modifiers: List<Modifier> = emptyList(),
    val generics: List<String> = emptyList(),
    val returnType: Type,
    val parameters: List<VariableDecl>,
    val body: List<Stmt>
) : Stmt() {
    init {
        modifiers.validateForLocale(ModifierLocaleContext.FUNCTION)
        parameters.forEach {
            modifiers.validateForLocale(ModifierLocaleContext.FUNCTION_PARAMETER)
        }
    }

    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitFunctionDecl(this)
    }
}

data class Call(
    val callee: Expr,
    val typeArgs: List<Type> = emptyList(),
    val args: List<Expr>
) : Expr() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitCall(this)
    }
}

sealed class Type : Node() {
    data class Builtin(val name: String) : Type() {
        override fun <R> accept(visitor: NodeVisitor<R>): R {
            return visitor.visitType(this)
        }
    }

    data class Named(val name: String) : Type() {
        override fun <R> accept(visitor: NodeVisitor<R>): R {
            return visitor.visitType(this)
        }
    }

    data class Generic(val base: Type, val args: List<Type>) : Type() {
        override fun <R> accept(visitor: NodeVisitor<R>): R {
            return visitor.visitType(this)
        }
    }
}
