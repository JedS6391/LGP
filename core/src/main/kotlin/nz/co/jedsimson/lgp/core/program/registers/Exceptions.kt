package nz.co.jedsimson.lgp.core.program.registers

/**
 * Thrown when a write operation is attempted on a [RegisterType.Constant] register.
 *
 * @param message A message accompanying the exception.
 */
class RegisterAccessException(message: String) : Exception(message)

/**
 * Thrown when the number of values being written to a particular register range,
 * does not match the size of the range.
 *
 * @param message A message accompanying the exception.
 */
class RegisterWriteRangeException(message: String) : Exception(message)

/**
 * Thrown when a [RegisterSet] cannot be initialised.
 *
 * This could be due to invalid configuration or some other error.
 *
 * @param message A message accompanying the exception.
 */
class RegisterSetInitialisationException(message: String) : Exception(message)

/**
 * Thrown when the read of a [Register] from a [RegisterSet] fails.
 *
 * This is mainly when an index is requested that is outside of the bounds of the register set.
 */
class RegisterReadException(message: String) : Exception(message)