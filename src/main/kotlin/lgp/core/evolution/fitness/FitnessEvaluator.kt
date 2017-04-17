package lgp.core.evolution.fitness

import lgp.core.environment.Environment
import lgp.core.evolution.population.Program

data class Evaluation<out T>(val fitness: Double, val result: T)

class FitnessEvaluator<T> {
    fun evaluate(program: Program<T>, environment: Environment<T>): Evaluation<T> {
        // Build a fitness context for this program
        val context = FitnessContext<T>(
                program = program,
                fitnessCases = environment.dataset.instances,
                fitnessFunction = environment.fitnessFunction
        )

        return Evaluation(
                fitness = context.fitness(),
                result = program.registers.read(0) // TODO: This should be a parameter
        )
    }
}