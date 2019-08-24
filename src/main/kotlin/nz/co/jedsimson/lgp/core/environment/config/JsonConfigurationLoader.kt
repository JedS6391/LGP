package nz.co.jedsimson.lgp.core.environment.config

import com.google.gson.Gson
import nz.co.jedsimson.lgp.core.environment.ComponentLoadException
import java.io.FileReader
import java.io.IOException
import nz.co.jedsimson.lgp.core.environment.ComponentLoaderBuilder
import nz.co.jedsimson.lgp.core.environment.MemoizedComponentProvider
import nz.co.jedsimson.lgp.core.modules.ModuleInformation

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
 * This caching has two side-effects:
 *
 *   - Calls to [JsonConfigurationLoader.load] are deterministic in the context of evolution.
 *   - Any changes to configuration during evolution will not be propagated through the system.
 *
 * @constructor Creates an instance of [JsonConfigurationLoader].
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
    private val configurationProvider = MemoizedComponentProvider("Configuration") {
        this.gson.fromJson(FileReader(this.filename), Configuration().javaClass)
    }

    /**
     * A [ComponentLoaderBuilder] implementation for building a [JsonConfigurationLoader] instance.
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
        fun filename(name: String) = apply {
            this.filename = name
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
        return this.configurationProvider.component
    }

    override val information = ModuleInformation(
        description = "A loader than can load configuration from a JSON file."
    )
}