package lgp.core.environment

import lgp.core.environment.config.ConfigLoader
import lgp.core.environment.config.JsonConfigLoader


public class Environment<T> {

    class Builder<U> : ComponentLoaderBuilder<Environment<U>> {

        lateinit var configLoader: ConfigLoader

        fun configurationLoader(loader: ConfigLoader): Builder<U> {
            this.configLoader = loader

            return this
        }

        override fun build(): Environment<U> {
            TODO("not implemented")
        }
    }
}