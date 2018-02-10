package lgp.lib.misc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import lgp.core.environment.ComponentLoaderBuilder
import lgp.core.environment.config.Config
import lgp.core.environment.config.ConfigLoader
import lgp.core.environment.config.JsonConfigLoader
import lgp.core.modules.ModuleInformation
import java.nio.file.Files
import java.nio.file.Paths

/**
 * An implementation of [ConfigLoader] that loads configuration from a YAML file.
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
 * throughout an LGP run (i.e. the result of calling [YamlConfigLoader.load] should be deterministic
 * in the context of an LGP run.
 *
 * @property filename YAML file to load configuration information from.
 */
class YamlConfigLoader constructor(private val filename: String) : ConfigLoader {

    /**
     * Creates an instance of [YamlConfigLoader] using the given builder.
     *
     * @property builder An instance of [YamlConfigLoader.Builder].
     */
    private constructor(builder: YamlConfigLoader.Builder) : this(builder.filename)

    // Our YAML parser. We enable Kotlin support when setting up the mapper.
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    // We cache the result of loading config so that on the first access we load
    // the configuration from disk, but subsequent loads return the cached copy.
    private val memoizedConfig by lazy {
        Files.newBufferedReader(Paths.get(this.filename)).use { reader ->
            mapper.readValue<Config>(reader)
        }
    }
    /**
     * A custom [ComponentLoaderBuilder] implementation for building a [YamlConfigLoader] instance.
     *
     * The builder allows for a filename to be specified, which references
     * a YAML file to load configuration data from.
     */
    class Builder : ComponentLoaderBuilder<YamlConfigLoader> {

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
         * Builds an instance of [YamlConfigLoader] with the information
         * given to the builder.
         *
         * @throws [UninitializedPropertyAccessException] When a required property of the builder has not been set.
         * @return A [YamlConfigLoader] with the information given to the builder.
         */
        override fun build(): YamlConfigLoader {
            return YamlConfigLoader(this)
        }
    }

    /**
     *
     *
     * @returns
     */
    override fun load(): Config {
        return this.memoizedConfig
    }

    override val information = ModuleInformation(
            description = "A loader than can load configuration from a YAML file."
    )

}