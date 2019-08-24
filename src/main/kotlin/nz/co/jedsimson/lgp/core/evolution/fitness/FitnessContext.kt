package nz.co.jedsimson.lgp.core.evolution.fitness

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.dataset.Sample
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.program.Output

/**
 * A case to evaluate a programs fitness on.
 *
 * @param TData The type of data the features represent.
 * @param TTarget The type of target.
 * @property features A sampling of features from a data set.
 * @property target The target for this cases set of features.
 */
data class FitnessCase<TData, TTarget : Target<TData>>(val features: Sample<TData>, val target: TTarget)

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
    protected val environment: EnvironmentFacade<TData, TOutput, TTarget>
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
 * A base implementation of [FitnessContext].
 */
abstract class BaseFitnessContext<TData, TOutput : Output<TData>, TTarget : Target<TData>>(
    environment: EnvironmentFacade<TData, TOutput, TTarget>
) : FitnessContext<TData, TOutput, TTarget>(environment) {

    private val fitnessFunction by lazy {
        this.environment.fitnessFunctionProvider()
    }

    /**
     * Evaluates the fitness by performing the following steps:
     *
     * 1. Finds the effective program
     * 2. Writes each fitness case to the programs register set
     * 3. Executes the program and collects the output from each fitness case
     * 4. Executes the fitness function from the given environment
     */
    override fun fitness(program: Program<TData, TOutput>, fitnessCases: List<FitnessCase<TData, TTarget>>): Double {
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

            // ... and gather a result from the program.
            program.output()
        }

        // Copy the fitness to the program for later accesses
        program.fitness = fitnessFunction(outputs, fitnessCases)

        return program.fitness
    }
}