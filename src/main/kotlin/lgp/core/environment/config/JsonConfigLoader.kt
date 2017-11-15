package lgp.core.environment.config

import com.google.gson.Gson
import lgp.core.environment.ComponentLoaderBuilder
import lgp.core.modules.ModuleInformation
import java.io.FileReader
import java.io.IOException

/**
 * An implementation of [ConfigLoader] that loads configuration from a JSON file.
 *
 * A builder is provided that should be primarily used when creating an instance through
 * the Java API.
 *
 * **NOTE:** This implementation caches the results of loading configuration, because the loader
 * is immutable - that is, once it is created it can not be altered to load configuration from
 * an alternate location.
 *
 * This means that the we can cache the result of loading configuration
 * because we don't want to get changes in configuration when it is loaded in different places
 * throughout an LGP run (i.e. the result of calling [JsonConfigLoader.load] should be deterministic
 * in the context of an LGP run.
 *
 * @property filename JSON file to load configuration information from.
 */
class JsonConfigLoader constructor(private val filename: String) : ConfigLoader {
    /**
     * Creates an instance of [JsonConfigLoader] using the given builder.
     *
     * @property builder An instance of [JsonConfigLoader.Builder].
     */
    private constructor(builder: Builder) : this(builder.filename)

    // Using Google's JSON parsing library.
    private val gson: Gson = Gson()

    // We cache the result of loading config so that on the first access we load
    // the configuration from disk, but subsequent loads return the cached copy.
    private val memoizedConfig by lazy {
        this.gson.fromJson(FileReader(this.filename), Config().javaClass)
    }

    /**
     * A custom [ComponentLoaderBuilder] implementation for building a [JsonConfigLoader] instance.
     *
     * The builder allows for a filename to be specified, which references
     * a JSON file to load configuration data from.
     */
    class Builder : ComponentLoaderBuilder<JsonConfigLoader> {

        /**
         * The filename of a JSON file.
         */
        lateinit var filename: String

        /**
         * Sets the filename of the JSON file to load configuration from.
         *
         * @param name The name of a JSON file.
         * @return A builder for the filename given.
         */
        fun filename(name: String): Builder {
            this.filename = name

            return this
        }

        /**
         * Builds an instance of [JsonConfigLoader] with the information
         * given to the builder.
         *
         * @throws [UninitializedPropertyAccessException] When a required property of the builder has not been set.
         * @return A [JsonConfigLoader] with the information given to the builder.
         */
        override fun build(): JsonConfigLoader {
            return JsonConfigLoader(this)
        }
    }

    /**
     * Loads an instance of [Config] by parsing the JSON file associated
     * with this loader.
     *
     * @throws IOException When the filename given to the loader is not valid.
     * @return A [Config] object that represents the contents of the JSON file.
     */
    override fun load(): Config {
        return this.memoizedConfig
    }

    override val information = ModuleInformation(
        description = "A loader than can load configuration from a JSON file."
    )
}