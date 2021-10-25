package nz.co.jedsimson.lgp.lib.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import nz.co.jedsimson.lgp.core.environment.ComponentLoaderBuilder
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.environment.config.ConfigurationLoader
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import java.nio.file.Files
import java.nio.file.Paths

/**
 * An implementation of [ConfigurationLoader] that loads configuration from a YAML file.
 *
 * A builder is provided that should be primarily used when creating an instance through
 * the Java API.
 *
 * **NOTE:** This implementation caches the results of loading configuration, because the loader
 * is immutable -- that is, once it is created it can not be altered to load configuration from
 * an alternate location.
 *
 * This means that the we can cache the result of loading configuration
 * because we don't want to get changes in configuration when it is loaded in different places
 * throughout an LGP run (i.e. the result of calling [YamlConfigurationLoader.load] should be deterministic
 * in the context of an LGP run.
 *
 * @property filename YAML file to load configuration information from.
 */
class YamlConfigurationLoader constructor(private val filename: String) : ConfigurationLoader {

    /**
     * Creates an instance of [YamlConfigurationLoader] using the given builder.
     *
     * @property builder An instance of [YamlConfigurationLoader.Builder].
     */
    private constructor(builder: Builder) : this(builder.filename)

    // Our YAML parser. We enable Kotlin support when setting up the mapper.
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    // We cache the result of loading configuration so that on the first access we load
    // the configuration from disk, but subsequent loads return the cached copy.
    private val memoizedConfig by lazy {
        Files.newBufferedReader(Paths.get(this.filename)).use { reader ->
            mapper.readValue<Configuration>(reader)
        }
    }
    /**
     * A custom [ComponentLoaderBuilder] implementation for building a [YamlConfigurationLoader] instance.
     *
     * The builder allows for a filename to be specified, which references
     * a YAML file to load configuration data from.
     */
    class Builder : ComponentLoaderBuilder<YamlConfigurationLoader> {

        /**
         * The filename of a YAML file.
         */
        lateinit var filename: String

        /**
         * Sets the filename of the YAML file to load configuration from.
         *
         * @param name The name of a YAML file.
         * @return A builder for the filename given.
         */
        fun filename(name: String): Builder {
            this.filename = name

            return this
        }

        /**
         * Builds an instance of [YamlConfigurationLoader] with the information
         * given to the builder.
         *
         * @throws [UninitializedPropertyAccessException] When a required property of the builder has not been set.
         * @return A [YamlConfigurationLoader] with the information given to the builder.
         */
        override fun build(): YamlConfigurationLoader {
            return YamlConfigurationLoader(this)
        }
    }

    /**
     * Loads an instance of [Configuration] by parsing the YAML file associated with this loader.
     *
     * @returns A [Configuration] object that represents the contents of the YAML file.
     */
    override fun load(): Configuration {
        return this.memoizedConfig
    }

    override val information = ModuleInformation(
            description = "A loader than can load configuration from a YAML file."
    )

}