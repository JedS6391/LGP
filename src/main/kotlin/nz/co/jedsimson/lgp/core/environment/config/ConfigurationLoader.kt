package nz.co.jedsimson.lgp.core.environment.config

import nz.co.jedsimson.lgp.core.environment.ComponentLoader

/**
 * An extended [ComponentLoader] that is responsible for loading [Configuration] instances.
 *
 * The method in which the configuration is loaded is to be defined through an implementation.
 *
 * @see [Configuration]
 */
interface ConfigurationLoader : ComponentLoader<Configuration>