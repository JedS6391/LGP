package lgp.core.environment.operations

import lgp.core.environment.ComponentLoader
import lgp.core.evolution.instructions.Operation

/**
 * Loads a collection of [Operation]s in some way defined by the implementer.
 *
 * @param T Type that the operations perform their function on.
 */
interface OperationLoader<T> : ComponentLoader<List<Operation<T>>>