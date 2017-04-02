package lgp.core.environment.dataset

import lgp.core.environment.ComponentLoader

interface DatasetLoader<out TData> : ComponentLoader<Dataset<TData>>