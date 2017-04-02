package lgp.core.environment.config

import com.google.gson.Gson
import lgp.core.environment.ComponentLoaderBuilder
import lgp.core.modules.ModuleInformation
import java.io.FileReader
import java.io.IOException

/**
 * Loads [Config]s from a JSON file specified by filename.
 *
 * This loader provides a builder, so that the loader can be built up
 * rather than explicitly providing values required.
 */
class JsonConfigLoader private constructor(val filename: String) : ConfigLoader {

    private constructor(builder: Builder) : this(builder.filename)

    private val gson: Gson = Gson()

    /**
     * A custom [ComponentLoaderBuilder] for this [ComponentLoader].
     *
     * The builder allows for a filename to be specified, which references
     * a JSON file to load configuration data from.
     */
    class Builder : ComponentLoaderBuilder<JsonConfigLoader> {

        lateinit var filename: String

        /**
         * Sets the filename of the JSON file to load configuration from.
         *
         * @param name the name of a JSON file.
         * @return a builder for the filename given.
         */
        fun filename(name: String): Builder {
            this.filename = name

            return this
        }

        /**
         * Builds an instance of [JsonConfigLoader] with the information
         * given to the builder.
         *
         * @throws [UninitializedPropertyAccessException] when a required property of the builder has not been set.
         * @return a [JsonConfigLoader] with the information given to the builder.
         */
        override fun build(): JsonConfigLoader {
            // TODO: Verify that correct configuration information
            // has been given to builder
            return JsonConfigLoader(this)
        }
    }

    /**
     * Loads an instance of [Config] by parsing the JSON file associated
     * with this loader.
     *
     * @throws IOException when the filename given to the loader is not valid.
     * @return A [Config] object that represents the contents of the JSON file.
     */
    override fun load(): Config {
        return this.gson.fromJson(FileReader(this.filename), Config().javaClass)
    }

    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "A loader than can load configuration from a JSON file."
    }
}