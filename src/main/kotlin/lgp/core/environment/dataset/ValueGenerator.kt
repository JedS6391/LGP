package lgp.core.environment.dataset

import java.util.*
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

class UniformlyDistributedGenerator {
    fun generate(n: Int, start: Double, end: Double): Sequence<Double> = buildSequence {
        val random = Random()

        (0..n).map {
            val r = random.nextDouble()

            // Scaled to range
            yield(r * (end - start) + start)
        }
    }
}