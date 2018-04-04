package lgp.core.environment.config

import com.google.gson.Gson
import lgp.core.environment.ComponentLoaderBuilder
import lgp.core.modules.ModuleInformation
import java.io.FileReader
import java.io.IOException

/**
 * An implementation of [ConfigurationLoader] that loads configuration from a JSON file.
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
 * throughout an LGP run (i.e. the result of calling [JsonConfigurationLoader.load] should be deterministic
 * in the context of an LGP run.
 *
 * @property filename JSON file to load configuration information from.
 */
class JsonConfigurationLoader constructor(private val filename: String) : ConfigurationLoader {
    /**
     * Creates an instance of [JsonConfigurationLoader] using the given builder.
     *
     * @property builder An instance of [JsonConfigurationLoader.Builder].
     */
    private constructor(builder: Builder) : this(builder.filename)

    // Using Google's JSON parsing library.
    private val gson: Gson = Gson()

    // We cache the result of loading configuration so that on the first access we load
    // the configuration from disk, but subsequent loads return the cached copy.
    private val memoizedConfig by lazy {
        this.gson.fromJson(FileReader(this.filename), Configuration().javaClass)
    }

    /**
     * A custom [ComponentLoaderBuilder] implementation for building a [JsonConfigurationLoader] instance.
     *
     * The builder allows for a filename to be specified, which references
     * a JSON file to load configuration data from.
     */
    class Builder : ComponentLoaderBuilder<JsonConfigurationLoader> {

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
         * Builds an instance of [JsonConfigurationLoader] with the information
         * given to the builder.
         *
         * @throws [UninitializedPropertyAccessException] When a required property of the builder has not been set.
         * @return A [JsonConfigurationLoader] with the information given to the builder.
         */
        override fun build(): JsonConfigurationLoader {
            return JsonConfigurationLoader(this)
        }
    }

    /**
     * Loads an instance of [Configuration] by parsing the JSON file associated
     * with this loader.
     *
     * @throws IOException When the filename given to the loader is not valid.
     * @return A [Configuration] object that represents the contents of the JSON file.
     */
    override fun load(): Configuration {
        return this.memoizedConfig
    }

    override val information = ModuleInformation(
        description = "A loader than can load configuration from a JSON file."
    )
}