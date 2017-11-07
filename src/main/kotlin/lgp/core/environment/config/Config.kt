package lgp.core.environment.config

import java.util.ArrayList

/**
 * Represents the parameters that can be set for the an LGP environment.
 *
 * An implementation of [ConfigLoader] can load [Config]s from a specified source.
 */
class Config {

    /**
     * The minimum length of a program generated during population initialisation.
     */
    var initialMinimumProgramLength = 10

    /**
     * The maximum length of a program generated during population initialisation.
     */
    var initialMaximumProgramLength = 30

    /**
     * The lower bound for program length during the evolution process.
     */
    var minimumProgramLength = 10

    /**
     * The upper bound for program length during the evolution process.
     */
    var maximumProgramLength = 200

    /**
     * A collection of fully-qualified class names specifying the classes of
     * [lgp.core.evolution.instructions.Operation]s to be loaded into an [lgp.core.environment.Environment].
     *
     * **NOTE:** Custom loaders can be provided that can provided a collection of
     * [lgp.core.evolution.instructions.Operation]s in some custom way, by implementing
     * the [lgp.core.environment.operations.OperationLoader] interface.
     */
    var operations: List<String> = ArrayList()

    /**
     * The probability with which instructions in an LGP program will contain a constant value.
     */
    var constantsRate = 0.5

    /**
     * Any values to be used as constants in an LGP program.
     *
     * **NOTE:** Constants are specified as string values to permit the use of custom register
     * types. This means that a custom [lgp.core.environment.constants.ConstantLoader]
     * will need to be implemented to load the list of strings into an appropriate type
     * suitable to be loaded into a register set.
     */
    var constants: List<String> = ArrayList()

    /**
     * How many additional calculation registers should be provided to an LGP program.
     */
    var numCalculationRegisters = 10

    /**
     * How many individuals should be generated in the initial population.
     */
    var populationSize = 100

    /**
     * The number of features in the data set (i.e. the number of input registers that should be made available).
     */
    var numFeatures = 0

    /**
     * The frequency with which crossover should occur.
     */
    var crossoverRate = 0.5

    /**
     * The frequency with which micro-mutations should occur.
     */
    var microMutationRate = 0.5

    /**
     * The frequency with which macro-mutations should occur.
     */
    var macroMutationRate = 0.5

    /**
     * Number of generations to evolve.
     */
    var generations = 50

    /**
     * Number of individuals that should be taken from the population in each generation.
     */
    var numOffspring = 20

    /**
     * How often branches should be included in evolved programs.
     */
    var branchInitialisationRate = 0.0

    /**
     * Determines the threshold for stopping the evolutionary process.
     *
     * When a solution with a fitness of [stoppingCriterion] is found, the search will stop.
     * By default, we don't stop until the fitness is minimised perfectly.
     */
    var stoppingCriterion = 0.0
}