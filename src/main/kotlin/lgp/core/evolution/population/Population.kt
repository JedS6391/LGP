package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.environment.RegisteredModuleType
import lgp.core.evolution.fitness.FitnessEvaluator
import kotlin.streams.toList

class Population<T>(val environment: Environment<T>) {

    private lateinit var individuals: List<Program<T>>

    private fun populate() {
        val programGenerator: ProgramGenerator<T> = this.environment.registeredModule(RegisteredModuleType.ProgramGenerator)

        this.individuals = programGenerator.next()
                                           .take(this.environment.config.populationSize)
                                           .toList()
    }

    fun evolve() {
        this.populate()

        val evaluator = FitnessEvaluator<T>()

        val evaluations = this.individuals.parallelStream().map { individual ->
            evaluator.evaluate(individual, this.environment)
        }.toList()

        evaluations.map(::println)
    }

}