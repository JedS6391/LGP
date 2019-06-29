package nz.co.jedsimson.lgp.core.modules

/**
 * A module in the system.
 *
 * An implementation of [Module] can be used in different areas of the system, allowing custom modules to be
 * built which can provide custom functionality to the system.
 */
interface Module {

    /**
     * Provides information about the module.
     */
    val information: ModuleInformation
}

/**
 * Represents the state entered when the load of an invalid module is attempted.
 */
class InvalidModuleException(message: String?) : Exception(message)