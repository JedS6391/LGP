package lgp.core.environment.config

import lgp.core.environment.ComponentLoader

/**
 * An extended [ComponentLoader] that is responsible for loading [Config] instances.
 *
 * The method in which the configuration is loaded is to be defined through an implementation.
 *
 * @see [Config]
 */
interface ConfigLoader : ComponentLoader<Config>