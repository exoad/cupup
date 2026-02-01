package net.exoad.k

object CLang {
    fun ppAttribute(attribute: String): String {
        return "__attribute__(($attribute))"
    }

    class CBuilder(private val sb: StringBuilder) {
        fun appendRaw(text: String): StringBuilder {
            return sb.append(text)
        }

        fun ppHeaderGuard(name: String): StringBuilder {
            return sb.appendLine("#ifndef $name\n#define $name")
        }

        fun ppInclude(header: String): StringBuilder {
            return sb.appendLine("#include<$header>")
        }

        fun typedef(
            alias: String,
            type: String,
            includeNewline: Boolean = false
        ): StringBuilder {
            return if (includeNewline) sb.appendLine("typedef $type $alias;")
            else sb.append("typedef $type $alias;")
        }

        fun ppDefine(name: String, value: String = ""): StringBuilder {
            return sb.appendLine("#define $name $value")
        }

        fun ppIfDef(name: String): StringBuilder {
            return sb.appendLine("#ifdef $name")
        }

        fun ppElse(): StringBuilder {
            return sb.appendLine("#else")
        }

        fun ppEndIf(): StringBuilder {
            return sb.appendLine("#endif")
        }

        fun comment(text: String): StringBuilder {
            return sb.appendLine("//$text")
        }
    }

    fun build(block: CBuilder.() -> Unit): String {
        val sb = StringBuilder()
        CBuilder(sb).block()
        return sb.toString()
    }
}
