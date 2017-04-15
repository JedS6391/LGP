package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.evolution.fitness.FitnessEvaluator

class Population<T>(val environment: Environment<T>) {

    private lateinit var individuals: List<Program<T>>

    private fun populate() {
        val programGenerator = this.environment.programGenerator

        this.individuals = programGenerator.next()
                                           .take(this.environment.config.populationSize)
                                           .toList()
    }

    fun evolve() {
        this.populate()

        val evaluator = FitnessEvaluator<T>()

        this.individuals.map { individual ->
            val evaluation = evaluator.evaluate(individual, this.environment)

            println(evaluation.fitness)
        }
    }

}