package be.webweaver.apps.caskandlabel.core

/** Group invite codes: 6 characters, letters without I/O and digits without 0/1 (server: `create_invite`). */
object InviteCode {
    const val LENGTH = 6
    const val LINK_PREFIX = "caskandlabel://join/"

    /** Accepts "K7M2QX", "k7m-2qx" or "caskandlabel://join/K7M2QX"; returns the bare upper-case code. */
    fun parse(text: String): String {
        val last = text.trim().split('/').lastOrNull { it.isNotEmpty() } ?: text
        return last.uppercase().filter { it.isLetterOrDigit() }
    }

    fun isComplete(text: String): Boolean = parse(text).length == LENGTH

    /** "K7M-2QX". */
    fun formatted(code: String): String =
        if (code.length == LENGTH) "${code.take(3)}-${code.takeLast(3)}" else code

    fun link(code: String): String = LINK_PREFIX + code
}
