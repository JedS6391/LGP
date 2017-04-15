package lgp.core.evolution.fitness

import lgp.core.environment.dataset.Instance
import lgp.core.evolution.population.Program

// Maybe wrap with a data class instead?
typealias FitnessCase<T> = Instance<T>
typealias FitnessFunction<T> = (Program<T>, List<FitnessCase<T>>) -> Double

class FitnessContext<T>(
        private val fitnessCases: List<FitnessCase<T>>,

        private val program: Program<T>,

        // TODO: Is this function type enough?
        private val fitnessFunction: FitnessFunction<T>
)
{

    fun fitness(): Double {
        return this.fitnessFunction(this.program, this.fitnessCases)
    }
}
