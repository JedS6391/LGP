package lgp.core.environment.config

import lgp.core.environment.ComponentLoader

/**
 * Loads a [Config] in some way defined by the implementor.
 */
interface ConfigLoader : ComponentLoader<Config>