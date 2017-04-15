package lgp.core.evolution.fitness

import lgp.core.environment.Environment
import lgp.core.evolution.population.Program

data class Evaluation(val fitness: Double)

class FitnessEvaluator<T> {
    fun evaluate(program: Program<T>, environment: Environment<T>): Evaluation {
        // Build a fitness context for this program
        val context = FitnessContext<T>(
                program = program,
                fitnessCases = environment.dataset.instances,
                fitnessFunction = environment.fitnessFunction
        )

        return Evaluation(context.fitness())
    }
}