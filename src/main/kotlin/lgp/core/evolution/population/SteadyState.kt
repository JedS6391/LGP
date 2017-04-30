package lgp.core.evolution.population

import lgp.core.environment.Environment
import lgp.core.environment.RegisteredModuleType
import lgp.core.evolution.fitness.Evaluation
import lgp.core.evolution.fitness.FitnessEvaluator
import lgp.core.modules.ModuleInformation
import java.util.*
import kotlin.streams.toList

class SteadyState<T>(environment: Environment<T>) : EvolutionModel<T>(environment) {

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

    override fun initialise() {
        val programGenerator: ProgramGenerator<T> = this.environment.registeredModule(RegisteredModuleType.ProgramGenerator)

        this.individuals = programGenerator.next()
                                           .take(this.environment.config.populationSize)
                                           .toMutableList()
    }

    override fun evolve() {
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
            val children = this.select.select(this.individuals)

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

    override val information = ModuleInformation("Algorithm 2.1 (LGP Algorithm)")
}