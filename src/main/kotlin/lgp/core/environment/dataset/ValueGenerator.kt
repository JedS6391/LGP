package lgp.core.environment.dataset

import kotlin.coroutines.experimental.buildSequence

class SequenceGenerator {
    fun generate(start: Double, end: Double, step: Double, inclusive: Boolean = false): Sequence<Double> = buildSequence {
        var x = start

        while (x <= end) {
            yield(x)

            x += step
        }

        if (x - step < end && inclusive)
            yield(end)
    }
}