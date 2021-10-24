package nz.co.jedsimson.lgp.core.modules

import java.lang.StringBuilder

/**
 * Provides information about a [Module] (e.g. what it does/what it is for).
 *
 * @property description A brief description of the [Module].
 * @constructor Creates a new instance of [ModuleInformation] with the given [description]
 */
data class ModuleInformation(val description: String) {

    /**
     * Creates a new instance of [ModuleInformation] with a description derived from the given metadata.
     *
     * @param name The name of the module this information relates to.
     * @param metadata A mapping of metadata about the module this information describes.
     */
    constructor(name: String, metadata: Map<String, String>) : this(deriveDescription(name, metadata))
}

private fun deriveDescription(name: String, metadata: Map<String, String>): String {
    val sb = StringBuilder()

    sb.appendln(name)
    sb.appendln()

    metadata.forEach { (k, v) ->
        sb.appendln("$k: $v")
    }

    return sb.toString()
}