package nz.co.jedsimson.lgp.core.evolution.fitness

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.dataset.Sample
import nz.co.jedsimson.lgp.core.environment.dataset.Targets
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Outputs

/**
 * A case to evaluate a programs fitness on.
 *
 * @param TData The type of data the features represent.
 * @param TTarget The type of target the target represents.
 * @property features A sampling of features from a data set.
 * @property target The target value for this cases set of features.
 */
data class FitnessCase<out TData, out TTarget : Target<TData>>(val features: Sample<TData>, val target: TTarget)

/**
 * Provides a way to map a program to fitness cases using a given fitness function.
 *
 * A context will generally execute the given program for each of the fitness cases
 * given to it through the [fitness] method. How the outputs are aggregated is up to
 * the individual implementation, allowing for the possibility of multiple program outputs,
 * weighted program outputs, etc.
 **
 * @property environment Provides access to the current environment
 */
abstract class FitnessContext<TData, TOutput : Output<TData>, TTarget : Target<TData>>(
    val environment: EnvironmentFacade<TData, TOutput, TTarget>
) : Module {

    /**
     * Returns the fitness as determined by this context.
     *
     * @param program A program to evaluate.
     * @param fitnessCases A collection of fitness cases to evaluate the program on.
     *
     * @returns A double value representing the fitness of the given program for the given fitness cases.
     */
    abstract fun fitness(program: Program<TData, TOutput>, fitnessCases: List<FitnessCase<TData, TTarget>>): Double
}

/**
 * A collection of built-in [FitnessContext] implementations.
 */
object FitnessContexts {

    /**
     * Facilitates fitness evaluation for programs which have a single output.
     *
     * For programs with multiple outputs, [MultipleOutputFitnessContext] should be used.
     *
     * @constructor Creates a new [SingleOutputFitnessContext] with the given [EnvironmentFacade].
     */
    class SingleOutputFitnessContext<TData>(
        environment: EnvironmentFacade<TData, Outputs.Single<TData>, Targets.Single<TData>>
    ) : FitnessContext<TData, Outputs.Single<TData>, Targets.Single<TData>>(environment) {

        private val fitnessFunction by lazy {
            this.environment.fitnessFunctionProvider()
        }

        override fun fitness(program: Program<TData, Outputs.Single<TData>>, fitnessCases: List<FitnessCase<TData, Targets.Single<TData>>>): Double {
            // Make sure the programs effective instructions have been found
            program.findEffectiveProgram()

            // Collect the results of the program for each fitness case.
            val outputs = fitnessCases.map { case ->
                // Make sure the registers are in a default state
                program.registers.reset()

                // Load the case
                program.registers.writeInstance(case.features)

                // Run the program...
                program.execute()

                // ... and gather a result from the programs first specified output register.
                // We will ignore any other output registers, under the assumption that if this
                // fitness context is being used, the other registers don't matter.
                val output = program.outputRegisterIndices.first()

                Outputs.Single(program.registers[output])
            }

            // Copy the fitness to the program for later accesses
            program.fitness = this.fitnessFunction(outputs, fitnessCases)

            return program.fitness
        }

        override val information = ModuleInformation(
                description = "A built-in fitness context for evaluating the fitness of single-output programs."
        )
    }

    /**
     * Facilitates fitness evaluation for programs which have multiple outputs.
     *
     * For programs with a single output, [SingleOutputFitnessContext] should be used.
     */
    class MultipleOutputFitnessContext<TData>(
        environment: EnvironmentFacade<TData, Outputs.Multiple<TData>, Targets.Multiple<TData>>
    ) : FitnessContext<TData, Outputs.Multiple<TData>, Targets.Multiple<TData>>(environment) {

        private val fitnessFunction by lazy {
            this.environment.fitnessFunctionProvider()
        }

        override fun fitness(program: Program<TData, Outputs.Multiple<TData>>, fitnessCases: List<FitnessCase<TData, Targets.Multiple<TData>>>): Double {
            // Make sure the programs effective instructions have been found
            program.findEffectiveProgram()

            // Collect the results of the program for each fitness case.
            val outputs = fitnessCases.map { case ->
                // Make sure the registers are in a default state
                program.registers.reset()

                // Load the case
                program.registers.writeInstance(case.features)

                // Run the program...
                program.execute()

                // ... and gather results from the programs specified output registers.
                val outputs = program.outputRegisterIndices.map { output ->
                    program.registers[output]
                }

                Outputs.Multiple(outputs)
            }

            // Copy the fitness to the program for later accesses
            program.fitness = this.fitnessFunction(outputs, fitnessCases)

            return program.fitness
        }

        override val information = ModuleInformation(
                description = "A built-in fitness context for evaluating the fitness of multiple-output programs."
        )
    }
}