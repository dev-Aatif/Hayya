package dev.hayya.core.domain.update

/**
 * Lightweight, zero-dependency RFC-8259 JSON parser used by the updater engine
 * so release metadata parsing runs identically on Android and JVM unit tests.
 */
internal object SimpleJsonParser {

    fun parseObject(input: String): Map<String, Any?>? {
        val parser = Parser(input)
        parser.skipWhitespace()
        val value = parser.parseValue() ?: return null
        parser.skipWhitespace()
        @Suppress("UNCHECKED_CAST")
        return value as? Map<String, Any?>
    }

    fun parseArray(input: String): List<Any?>? {
        val parser = Parser(input)
        parser.skipWhitespace()
        val value = parser.parseValue() ?: return null
        parser.skipWhitespace()
        @Suppress("UNCHECKED_CAST")
        return value as? List<Any?>
    }

    private class Parser(private val src: String) {
        private var index = 0

        fun skipWhitespace() {
            while (index < src.length && src[index].isWhitespace()) {
                index++
            }
        }

        fun parseValue(): Any? {
            skipWhitespace()
            if (index >= src.length) return null
            return when (val ch = src[index]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> throw IllegalArgumentException("Unexpected JSON char '$ch' at $index")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek('}')) {
                index++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                when {
                    peek(',') -> index++
                    peek('}') -> {
                        index++
                        return map
                    }
                    else -> throw IllegalArgumentException("Expected ',' or '}' at $index")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = ArrayList<Any?>()
            skipWhitespace()
            if (peek(']')) {
                index++
                return list
            }
            while (true) {
                list.add(parseValue())
                skipWhitespace()
                when {
                    peek(',') -> index++
                    peek(']') -> {
                        index++
                        return list
                    }
                    else -> throw IllegalArgumentException("Expected ',' or ']' at $index")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (index < src.length) {
                val ch = src[index++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (index >= src.length) throw IllegalArgumentException("Unterminated escape at $index")
                        when (val esc = src[index++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (index + 4 > src.length) throw IllegalArgumentException("Invalid unicode escape at $index")
                                val code = src.substring(index, index + 4).toInt(16)
                                sb.append(code.toChar())
                                index += 4
                            }
                            else -> throw IllegalArgumentException("Invalid escape '\\$esc' at $index")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun parseBoolean(): Boolean {
            return if (src.startsWith("true", index)) {
                index += 4
                true
            } else if (src.startsWith("false", index)) {
                index += 5
                false
            } else {
                throw IllegalArgumentException("Invalid boolean at $index")
            }
        }

        private fun parseNull(): Any? {
            if (src.startsWith("null", index)) {
                index += 4
                return null
            }
            throw IllegalArgumentException("Invalid null at $index")
        }

        private fun parseNumber(): Number {
            val start = index
            if (peek('-')) index++
            while (index < src.length && src[index].isDigit()) index++
            var isFloat = false
            if (peek('.')) {
                isFloat = true
                index++
                while (index < src.length && src[index].isDigit()) index++
            }
            if (peek('e') || peek('E')) {
                isFloat = true
                index++
                if (peek('+') || peek('-')) index++
                while (index < src.length && src[index].isDigit()) index++
            }
            val raw = src.substring(start, index)
            return if (isFloat) raw.toDouble() else raw.toLong()
        }

        private fun peek(expected: Char): Boolean = index < src.length && src[index] == expected

        private fun expect(expected: Char) {
            if (index >= src.length || src[index] != expected) {
                throw IllegalArgumentException("Expected '$expected' at $index")
            }
            index++
        }
    }
}
