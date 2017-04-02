package lgp.core.modules

/**
 * Represents the state entered when the load of an invalid module is attempted.
 */
class InvalidModuleException(message: String?) : Exception(message) {}