package lgp.core.modules

/**
 * A module in the LGP system.
 *
 * An implementation of [Module] can be used in the appropriate
 * place in the system, meaning that custom modules can be built
 * to provide custom functionality to the system.
 */
interface Module {
    /**
     * Provides information about the module.
     */
    val information: ModuleInformation
}