package nz.co.jedsimson.lgp.core.environment.dataset

import nz.co.jedsimson.lgp.core.environment.ComponentLoader

/**
 * An extended [ComponentLoader] that loads a [Dataset].
 *
 * The source of the data set is controllable by any implementor, all that is required is that the
 * raw data is gathered into a [Dataset] instance, while satisfying the type constraint [TData] for
 * individual features in the data set.
 *
 * @param TData Type of features in the data set.
 * @param TTarget Type of targets in the data set.
 * @see [Dataset]
 */
interface DatasetLoader<TData, TTarget : Target<TData>> : ComponentLoader<Dataset<TData, TTarget>>