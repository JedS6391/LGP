package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.environment.RegisteredModuleType
import lgp.core.evolution.fitness.Evaluation
import lgp.core.evolution.fitness.FitnessEvaluator
import java.util.*
import kotlin.streams.toList

class Population<T>(val environment: Environment<T>) {

    private val select: SelectionOperator<T> = this.environment.registeredModule(
            RegisteredModuleType.SelectionOperator
    )

    private val combine: RecombinationOperator<T> = this.environment.registeredModule(
            RegisteredModuleType.RecombinationOperator
    )

    private val microMutate: MutationOperator<T> = this.environment.registeredModule(
            RegisteredModuleType.MicroMutationOperator
    )

    private val macroMutate: MutationOperator<T> = this.environment.registeredModule(
            RegisteredModuleType.MacroMutationOperator
    )

    private val fitnessEvaluator: FitnessEvaluator<T> = FitnessEvaluator()

    lateinit var individuals: MutableList<Program<T>>

     private fun initialise() {
        val programGenerator: ProgramGenerator<T> = this.environment.registeredModule(RegisteredModuleType.ProgramGenerator)

        this.individuals = programGenerator.next()
                                           .take(this.environment.config.populationSize)
                                           .toMutableList()
    }

    fun evolve() {
        val rg = Random()

        // Roughly follows Algorithm 2.1 in Linear Genetic Programming (Brameier. M, Banzhaf W.)
        // 1. Initialise a population of random programs
        this.initialise()

        // Determine the initial fitness of the individuals in the population
        val initialEvaluations = this.individuals.map { individual ->
            this.fitnessEvaluator.evaluate(individual, this.environment)
        }

        var best = initialEvaluations.sortedBy(Evaluation<T>::fitness).first()
        var gen = 0

        while (gen++ < this.environment.config.generations) {
            // Randomly select 2 * n individuals from the population without replacement (n = population size)
            val intermediatePopulation = (0..(2 * this.individuals.size - 1)).map { rg.choice(this.individuals) }

            // 3. Perform two fitness tournaments of size n.
            // We change this step to allow for different selection operators. All we care about
            // is that we use the selection operator and we get copies of some individuals in the
            // population.
            // 4. Make temporary copies of the two tournament winners. (Selection operator should handle this)
            val children = this.select.select(intermediatePopulation)

            // 5. Modify the two winners by one or more variation operators for certain probabilities
            (0..children.size - 1 step 2).map { idx ->
                val mother = children[idx]
                val father = children[idx + 1]

                // Combine mother and father with some prob.
                if (rg.nextGaussian() < this.environment.config.crossoverRate) {
                    this.combine.combine(mother, father)
                }

                // Mutate mother or father (or both) with some prob.
                if (rg.nextGaussian() < this.environment.config.microMutationRate) {
                    this.microMutate.mutate(mother)
                } else if (rg.nextGaussian() < this.environment.config.macroMutationRate) {
                    this.macroMutate.mutate(mother)
                }

                if (rg.nextGaussian() < this.environment.config.microMutationRate) {
                    this.microMutate.mutate(father)
                } else if (rg.nextGaussian() < this.environment.config.macroMutationRate) {
                    this.macroMutate.mutate(father)
                }
            }

            // 6. Evaluate the fitness of the children
            // 7. If the currently best-fit individual is replaced by one of the children validate the
            //    new best program using unknown data.
            // TODO: Do validation step
            val evaluations = children.map { individual ->
                this.fitnessEvaluator.evaluate(individual, this.environment)
            }

            val bestChild = evaluations.sortedBy(Evaluation<T>::fitness).first()

            best = if (bestChild.fitness < best.fitness) bestChild else best

            // The children are copies of individuals in the population, so add the copies
            // to the population.
            this.individuals.addAll(children)

            this.computeStatistics(evaluations, best)
        }

        println("Best Program:")
        println(best.individual)

        best.individual.findEffectiveProgram()

        println("Effective Program:")
        for (instruction in best.individual.effectiveInstructions) {
            println(instruction)
        }
    }

    private fun computeStatistics(evaluations: List<Evaluation<T>>, best: Evaluation<T>) {
        val averageFitness = (evaluations.map(Evaluation<T>::fitness).sum() / evaluations.size.toDouble())
        val bestFitness = best.fitness

        println("avg. fitness = $averageFitness, best fitness = $bestFitness")
    }

}

/**
 * Return a random element from the given list.
 */
fun <T> Random.choice(list: List<T>): T {
    return list[(this.nextDouble() * list.size).toInt()]
}