package net.exoad.k

interface NodeVisitor<R> {
    fun visitModule(module: Module): R
    fun visitExprStmt(exprStmt: ExprStmt): R
    fun visitVarDecl(varDecl: VarDecl): R
    fun visitTypeAlias(typeAlias: TypeAlias): R
    fun <T> visitLiteral(literal: Literal<T>): R
    fun visitVariable(variable: Variable): R
    fun visitBinaryOp(binaryOp: BinaryOp): R
    fun visitIdentifier(identifier: Identifier): R
    fun visitType(type: Type): R
    fun visitIfStmt(ifStmt: IfStmt): R
    fun visitCall(call: Call): R
    fun visitFunctionDecl(functionDecl: FunctionDecl): R
    fun visitCast(cast: Cast): R
    fun visitUnaryOp(unaryOp: UnaryOp): R
    fun visitWhileStmt(whileStmt: WhileStmt): R
    fun visitDeferStmt(deferStmt: DeferStmt): R
    fun visitBreakStmt(breakStmt: BreakStmt): R
    fun visitContinueStmt(continueStmt: ContinueStmt): R
}

sealed class Node {
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

data class VarDecl(
    val name: String,
    val type: Type,
    val init: Expr? = null,
    val isMutable: Boolean = false
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


data class Variable(val name: String, val type: Type) : Expr() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitVariable(this)
    }
}

data class BinaryOp(val op: String, val left: Expr, val right: Expr) : Expr() {
    override fun <R> accept(visitor: NodeVisitor<R>): R {
        return visitor.visitBinaryOp(this)
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
    val generics: List<String> = emptyList(),
    val returnType: Type,
    val parameters: List<VarDecl>,
    val body: List<Stmt>
) : Stmt() {
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
