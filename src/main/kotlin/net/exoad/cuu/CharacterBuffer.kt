package net.exoad.cuu

class CharacterBuffer(content: String) {
    private val chars = (content + '\u0000').toCharArray()
    var position = 0
    var line = 1
    var column = 1
    val current: Char get() = chars[position]
    val isAtEnd: Boolean get() = chars[position] == '\u0000'

    fun advance(): Char {
        val char = chars[position]
        if (char != '\u0000') {
            position++
            if (char == '\n') {
                line++
                column = 1
            } else {
                column++
            }
        }
        return char
    }

    fun peek(offset: Int = 0): Char {
        return chars[position + offset]
    }
}
