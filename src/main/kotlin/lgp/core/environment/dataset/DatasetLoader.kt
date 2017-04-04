package lgp.core.environment.dataset

import lgp.core.environment.ComponentLoader

/**
 * Loads a [Dataset] in some way defined by the implementor.
 *
 * @param TData Type of attributes in the data set.
 */
interface DatasetLoader<out TData> : ComponentLoader<Dataset<TData>>