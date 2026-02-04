package net.exoad.cuu

object Mangler {
    const val MANGLING_ENABLED: Boolean = false

    private val prefixes = mutableMapOf<String, String>()
    private val pool = generatePrefixes().shuffled()
    private var index = 0

    private fun generateRandomPrefix(): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + '_'
        return "_" + (1..5).map {
            chars.random()
        }.joinToString("")
    }

    private fun generatePrefixes(): List<String> {
        val set = mutableSetOf<String>()
        while (set.size < 256) {
            set.add(generateRandomPrefix())
        }
        return set.toList()
    }

    operator fun get(key: String): String {
        @Suppress("KotlinConstantConditions")
        if (!MANGLING_ENABLED) {
            return key
        }
        return prefixes.getOrPut(key) {
            if (index >= pool.size) throw IllegalStateException("Too many unique names")
            pool[index++]
        }
    }
}
