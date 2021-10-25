package nz.co.jedsimson.lgp.core.environment.operations

import nz.co.jedsimson.lgp.core.environment.ComponentLoader
import nz.co.jedsimson.lgp.core.program.instructions.Operation

/**
 * Loads a collection of [Operation]s in some way defined by the implementer.
 *
 * @param T Type that the operations perform their function on.
 */
interface OperationLoader<T> : ComponentLoader<List<Operation<T>>>