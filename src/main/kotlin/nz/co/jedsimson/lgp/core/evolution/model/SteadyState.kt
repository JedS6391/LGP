package nz.co.jedsimson.lgp.core.evolution.model

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Dataset
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.events.Diagnostics
import nz.co.jedsimson.lgp.core.evolution.fitness.Evaluation
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessEvaluator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.MutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.recombination.RecombinationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.selection.SelectionOperator
import nz.co.jedsimson.lgp.core.evolution.pairwise
import nz.co.jedsimson.lgp.core.evolution.standardDeviation
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.ProgramGenerator
import kotlin.NoSuchElementException

/**
 * A model for evolution using a steady-state algorithm.
 *
 * For more information, see Algorithm 2.1 (LGP Algorithm) from Linear Genetic Programming
 * (Brameier, M., Banzhaf, W. 2001).
 */
class SteadyState<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    environment: EnvironmentFacade<TProgram, TOutput, TTarget>
) : EvolutionModel<TProgram, TOutput, TTarget>(environment) {

    private val moduleFactory = this.environment.moduleFactory
    private val select: SelectionOperator<TProgram, TOutput, TTarget>
    private val combine: RecombinationOperator<TProgram, TOutput, TTarget>
    private val microMutate: MutationOperator<TProgram, TOutput, TTarget>
    private val macroMutate: MutationOperator<TProgram, TOutput, TTarget>
    private val fitnessEvaluator: FitnessEvaluator<TProgram, TOutput, TTarget>

    lateinit var individuals: MutableList<Program<TProgram, TOutput>>
    lateinit var bestProgram: Program<TProgram, TOutput>

    init {
        this.select = this.moduleFactory.instance(CoreModuleType.SelectionOperator)
        this.combine = this.moduleFactory.instance(CoreModuleType.RecombinationOperator)
        this.microMutate = this.moduleFactory.instance(CoreModuleType.MicroMutationOperator)
        this.macroMutate = this.moduleFactory.instance(CoreModuleType.MacroMutationOperator)
        this.fitnessEvaluator = FitnessEvaluator(this.environment)
    }

    override fun train(dataset: Dataset<TProgram, TTarget>): EvolutionResult<TProgram, TOutput> {
        Diagnostics.trace("SteadyState:train-start")

        val rg = this.environment.randomState

        // Roughly follows Algorithm 2.1 in Linear Genetic Programming (Brameier. M, Banzhaf W.)
        // 1. Initialise a population of random programs
        Diagnostics.traceWithTime("SteadyState:train-initialise") {
            this.initialise()
        }

        // Determine the initial fitness of the individuals in the population
        val initialEvaluations = Diagnostics.traceWithTime("SteadyState:train-initial-evaluations") {
            this.individuals.map { individual ->
                this.fitnessEvaluator.evaluate(individual, dataset)
            }.toList()
        }

        var best = initialEvaluations.minByOrNull(Evaluation<TProgram, TOutput>::fitness)
            ?: throw NoSuchElementException("No individuals in the initial evaluation list.")
        this.bestProgram = best.individual

        val statistics = mutableListOf<EvolutionStatistics>()

        (0 until this.environment.configuration.generations).forEach { generation ->
            Diagnostics.trace("SteadyState:train-generation-$generation")

            // Stop early whenever we can.
            if (best.fitness <= this.environment.configuration.stoppingCriterion) {
                Diagnostics.debug("SteadyState:train-early-exit", mapOf(
                    "best" to best
                ))

                // Make sure to add at least one set of statistics.
                statistics.add(this.statistics(generation, best))

                this.bestProgram = best.individual

                return EvolutionResult(best.individual, this.individuals, statistics)
            }

            val children = Diagnostics.traceWithTime("SteadyState:train-selection") {
                this.select.select(this.individuals)
            }

            children.pairwise().map { (mother, father) ->
                // Combine mother and father with some prob.
                if (rg.nextDouble() < this.environment.configuration.crossoverRate) {
                    Diagnostics.traceWithTime("SteadyState:train-recombination") {
                        this.combine.combine(mother, father)
                    }
                }

                // Mutate mother or father (or both) with some prob.
                if (rg.nextDouble() < this.environment.configuration.microMutationRate) {
                    Diagnostics.traceWithTime("SteadyState:train-micro-mutate-mother") {
                        this.microMutate.mutate(mother)
                    }
                } else if (rg.nextDouble() < this.environment.configuration.macroMutationRate) {
                    Diagnostics.traceWithTime("SteadyState:train-macro-mutate-mother") {
                        this.macroMutate.mutate(mother)
                    }
                }

                if (rg.nextDouble() < this.environment.configuration.microMutationRate) {
                    Diagnostics.traceWithTime("SteadyState:train-micro-mutate-father") {
                        this.microMutate.mutate(father)
                    }
                } else if (rg.nextDouble() < this.environment.configuration.macroMutationRate) {
                    Diagnostics.traceWithTime("SteadyState:train-macro-mutate-father") {
                        this.macroMutate.mutate(father)
                    }
                }
            }

            // TODO: Do validation step
            val evaluations = Diagnostics.traceWithTime("SteadyState:train-children-evaluations") {
                children.map { individual ->
                    this.fitnessEvaluator.evaluate(individual, dataset)
                }.sortedBy(Evaluation<TProgram, TOutput>::fitness)
            }

            val bestChild = evaluations.first()

            best = if (bestChild.fitness < best.fitness) bestChild else best
            this.bestProgram = best.individual

            // The children are copies of individuals in the population, so add the copies
            // to the population.
            this.individuals.addAll(children)

            statistics.add(this.statistics(generation, best))
        }

        this.bestProgram = best.individual

        Diagnostics.trace("SteadyState:train-end")

        return EvolutionResult(best.individual, this.individuals, statistics)
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

    override fun copy(): SteadyState<TProgram, TOutput, TTarget> {
        return SteadyState(this.environment)
    }

    override fun deepCopy(): EvolutionModel<TProgram, TOutput, TTarget> {
        return SteadyState(this.environment.copy())
    }

    private fun initialise() {
        val programGenerator = this.moduleFactory.instance<ProgramGenerator<TProgram, TOutput, TTarget>>(CoreModuleType.ProgramGenerator)

        this.individuals = programGenerator.next()
            .take(this.environment.configuration.populationSize)
            .toMutableList()
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
        val standardDeviation = this.individuals
            .map(Program<TProgram, TOutput>::fitness)
            .standardDeviation(meanFitness)

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
}