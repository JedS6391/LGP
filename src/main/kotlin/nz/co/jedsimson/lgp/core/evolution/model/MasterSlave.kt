package nz.co.jedsimson.lgp.core.evolution.model

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Dataset
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.evolution.fitness.Evaluation
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessEvaluator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.MutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.RecombinationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.selection.SelectionOperator
import nz.co.jedsimson.lgp.core.evolution.pairwise
import nz.co.jedsimson.lgp.core.evolution.pmap
import nz.co.jedsimson.lgp.core.evolution.standardDeviation
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.ProgramGenerator

// TODO: Master-slave and island migration techniques could potentially share logic with steady-state,
// since they are just modifications of that core algorithm.

/**
 * A model for evolution using a steady-state algorithm. The evaluation and mutation processes are
 * parallelised in a master-slave based technique.
 */
class MasterSlave<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>
) : EvolutionModel<TProgram, TOutput, TTarget>(environment) {

    private val moduleFactory = this.environment.moduleFactory

    private val select: SelectionOperator<TProgram, TOutput, TTarget> = this.moduleFactory.instance(
            CoreModuleType.SelectionOperator
    )

    private val combine: RecombinationOperator<TProgram, TOutput, TTarget> = this.moduleFactory.instance(
            CoreModuleType.RecombinationOperator
    )

    private val microMutate: MutationOperator<TProgram, TOutput, TTarget> = this.moduleFactory.instance(
            CoreModuleType.MicroMutationOperator
    )

    private val macroMutate: MutationOperator<TProgram, TOutput, TTarget> = this.moduleFactory.instance(
            CoreModuleType.MacroMutationOperator
    )

    private val fitnessEvaluator: FitnessEvaluator<TProgram, TOutput, TTarget> = FitnessEvaluator(this.environment)

    lateinit var individuals: MutableList<Program<TProgram, TOutput>>

    lateinit var bestProgram: Program<TProgram, TOutput>

    private fun initialise() {
        val programGenerator: ProgramGenerator<TProgram, TOutput, TTarget> = this.moduleFactory.instance(
                CoreModuleType.ProgramGenerator
        )

        this.individuals = programGenerator.next()
                .take(this.environment.configuration.populationSize)
                .toMutableList()
    }

    override fun train(dataset: Dataset<TProgram, TTarget>): EvolutionResult<TProgram, TOutput> {
        val rg = this.environment.randomState

        // Roughly follows Algorithm 2.1 in Linear Genetic Programming (Brameier. M, Banzhaf W.)
        // 1. Initialise a population of random programs
        this.initialise()

        // Determine the initial fitness of the individuals in the population
        val initialEvaluations = this.individuals.pmap { individual ->
            this.fitnessEvaluator.evaluate(individual, dataset)
        }.toList()

        var best = initialEvaluations.sortedBy(Evaluation<TProgram, TOutput>::fitness).first()
        this.bestProgram = best.individual

        val statistics = mutableListOf<EvolutionStatistics>()

        (0 until this.environment.configuration.generations).forEach { gen ->
            // Stop early whenever we can.
            if (best.fitness <= this.environment.configuration.stoppingCriterion) {
                // Make sure to add at least one set of statistics.
                statistics.add(this.statistics(gen, best))

                this.bestProgram = best.individual

                return EvolutionResult(best.individual, this.individuals, statistics)
            }

            val children = this.select.select(this.individuals)

            children.pairwise().map { (mother, father) ->
                // Combine mother and father with some prob.
                if (rg.nextDouble() < this.environment.configuration.crossoverRate) {
                    this.combine.combine(mother, father)
                }

                // Mutate mother or father (or both) with some prob.
                if (rg.nextDouble() < this.environment.configuration.microMutationRate) {
                    this.microMutate.mutate(mother)
                } else if (rg.nextDouble() < this.environment.configuration.macroMutationRate) {
                    this.macroMutate.mutate(mother)
                }

                if (rg.nextDouble() < this.environment.configuration.microMutationRate) {
                    this.microMutate.mutate(father)
                } else if (rg.nextDouble() < this.environment.configuration.macroMutationRate) {
                    this.macroMutate.mutate(father)
                }
            }

            // TODO: Do validation step
            val evaluations = children.pmap { individual ->
                this.fitnessEvaluator.evaluate(individual, dataset)
            }.sortedBy(Evaluation<TProgram, TOutput>::fitness)

            val bestChild = evaluations.first()

            best = if (bestChild.fitness < best.fitness) bestChild else best
            this.bestProgram = best.individual

            // The children are copies of individuals in the population, so add the copies
            // to the population.
            this.individuals.addAll(children)

            statistics.add(this.statistics(gen, best))
        }

        this.bestProgram = best.individual

        return EvolutionResult(best.individual, this.individuals, statistics)
    }

    private fun statistics(generation: Int, best: Evaluation<TProgram, TOutput>): EvolutionStatistics {
        var meanFitness = 0.0
        var meanProgramLength = 0.0
        var meanEffectiveProgramLength = 0.0
        val bestFitness = best.fitness

        this.individuals.forEach { individual ->
            meanFitness += individual.fitness
            meanProgramLength += individual.instructions.size
            meanEffectiveProgramLength += individual.effectiveInstructions.size
        }

        meanFitness /= this.individuals.size
        meanProgramLength /= this.individuals.size
        meanEffectiveProgramLength /= this.individuals.size

        // Use the mean that we've already calculated.
        val standardDeviation = this.individuals.map(
            Program<TProgram, TOutput>::fitness
        ).standardDeviation(meanFitness)

        return EvolutionStatistics(
            data = mapOf(
                "generation" to generation,
                "bestFitness" to bestFitness,
                "meanFitness" to meanFitness,
                "standardDeviationFitness" to standardDeviation,
                "meanProgramLength" to meanProgramLength,
                "meanEffectiveProgramLength" to meanEffectiveProgramLength
            )
        )
    }

    override fun test(dataset: Dataset<TProgram, TTarget>): TestResult<TProgram, TOutput> {
        this.bestProgram.findEffectiveProgram()

        val outputs = dataset.inputs.map { features ->
            // Reset registers.
            this.bestProgram.registers.reset()

            // Load the features
            this.bestProgram.registers.writeInstance(features)

            // Run the program...
            this.bestProgram.execute()

            // ... and gather a result from the programs specified output registers.
            this.bestProgram.output()
        }

        return TestResult(
            predicted = outputs,
            expected = dataset.outputs
        )
    }

    override val information = ModuleInformation("Algorithm 2.1 (LGP Algorithm)")

    override fun copy(): MasterSlave<TProgram, TOutput, TTarget> {
        return MasterSlave(this.environment)
    }

    override fun deepCopy(): EvolutionModel<TProgram, TOutput, TTarget> {
        return MasterSlave(this.environment.copy())
    }
}