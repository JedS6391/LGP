package nz.co.jedsimson.lgp.core.environment.config

import java.util.ArrayList

/**
 * Represents the validity of a given [Configuration].
 */
sealed class ConfigurationValidity {

    /**
     * A value that can be used to determine whether the [Configuration] is valid or not.
     */
    abstract val isValid: Boolean

    /**
     * A valid configuration.
     */
    class Valid : ConfigurationValidity() {
        override val isValid = true
    }

    /**
     * An invalid configuration.
     *
     * @property reason A reason why the configuration is not valid.
     */
    class Invalid(val reason: String) : ConfigurationValidity() {
        override val isValid = false
    }
}

/**
 * Exception given when an [nz.co.jedsimson.lgp.core.environment.Environment] is initialised with an invalid [Configuration].
 *
 * @property message A message describing why the configuration is invalid.
 */
class InvalidConfigurationException(message: String) : Exception(message)

/**
 * Represents the parameters that can be set for the an LGP environment.
 *
 * An implementation of [ConfigurationLoader] can load [Configuration] instances from a specific source.
 *
 * @constructor Initialises a new [Configuration] instance.
 */
class Configuration {

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
     * [lgp.core.program.instructions.Operation]s to be loaded into an [lgp.core.environment.Environment].
     *
     * **NOTE:** Custom loaders can be provided that can provided a collection of
     * [lgp.core.program.instructions.Operation]s in some custom way, by implementing
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
    @Deprecated("This property is no longer part of the core configuration and has been made an input to the TournamentSelection class.")
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

    /**
     * Provides the ability to pass in the number of runs as a configuration parameter
     * instead of a hard-coded value in the problem definition.
     *
     * The parameter is not used by default anywhere in the system, but consumers
     * can choose to use it.
     */
    var numberOfRuns = 1

    /**
     * Provides a representation of the configuration validity.
     *
     * @return A [ConfigurationValidity] which can be used to determine whether the configuration is valid or not.
     */
    fun isValid(): ConfigurationValidity {
        return when {
            // Need at least one feature in the data set.
            numFeatures <= 0 -> ConfigurationValidity.Invalid("numFeatures: At least one feature variable should be specified.")
            // If no constants are provided then a rate of constants can't be specified.
            constants.isEmpty() && constantsRate > 0.0 -> ConfigurationValidity.Invalid(
                "constants/constantsRate: No constants were provided but a constant rate greater than 0 was given."
            )
            // Constant rate should be positive.
            constantsRate < 0.0 -> ConfigurationValidity.Invalid("constantsRate: Constant rate should be a positive value.")
            // Program lengths should be greater than 0.
            !this.programLengthsAreValid() -> ConfigurationValidity.Invalid(
                "programLengths: All program lengths should be greater than 0."
            )
            // Need at least one operation in order to create programs.
            operations.isEmpty() -> ConfigurationValidity.Invalid("operations: At least one operation is needed in order to create programs.")
            // There is no point in configuring the system with no population or generations.
            populationSize <= 0 || generations <= 0 -> ConfigurationValidity.Invalid(
                "populationSize/generations: A positive population size and number of generations is needed."
            )
            numberOfRuns < 1 -> ConfigurationValidity.Invalid(
                "At least one run needs to be performed."
            )
            // In all other cases, the configuration is valid.
            else -> ConfigurationValidity.Valid()
        }
    }

    private fun programLengthsAreValid(): Boolean {
        return (
            initialMinimumProgramLength > 0 &&
            initialMaximumProgramLength > 0 &&
            minimumProgramLength > 0 &&
            maximumProgramLength > 0
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Configuration) {
            return false
        }

        return (
            this.initialMinimumProgramLength == other.initialMinimumProgramLength &&
            this.initialMaximumProgramLength == other.initialMaximumProgramLength &&
            this.minimumProgramLength == other.minimumProgramLength &&
            this.maximumProgramLength == other.maximumProgramLength &&
            this.operations == other.operations &&
            this.constantsRate == other.constantsRate &&
            this.constants == other.constants &&
            this.numCalculationRegisters == other.numCalculationRegisters &&
            this.populationSize == other.populationSize &&
            this.numFeatures == other.numFeatures &&
            this.crossoverRate == other.crossoverRate &&
            this.microMutationRate == other.microMutationRate &&
            this.macroMutationRate == other.macroMutationRate &&
            this.generations == other.generations &&
            this.numOffspring == other.numOffspring &&
            this.branchInitialisationRate == other.branchInitialisationRate &&
            this.stoppingCriterion == other.stoppingCriterion &&
            this.numberOfRuns == other.numberOfRuns
        )
    }

    override fun toString(): String {
        val sb = StringBuilder()

        sb.appendln("Configuration:")
        sb.appendln("\tinitialMinimumProgramLength = $initialMinimumProgramLength")
        sb.appendln("\tinitialMaximumProgramLength = $initialMaximumProgramLength")
        sb.appendln("\tminimumProgramLength = $minimumProgramLength")
        sb.appendln("\tmaximumProgramLength = $maximumProgramLength")
        sb.appendln("\toperations = $operations")
        sb.appendln("\tconstantsRate = $constantsRate")
        sb.appendln("\tconstants = $constants")
        sb.appendln("\tnumCalculationRegisters = $numCalculationRegisters")
        sb.appendln("\tpopulationSize = $populationSize")
        sb.appendln("\tnumFeatures = $numFeatures")
        sb.appendln("\tcrossoverRate = $crossoverRate")
        sb.appendln("\tmicroMutationRate = $microMutationRate")
        sb.appendln("\tmacroMutationRate = $macroMutationRate")
        sb.appendln("\tgenerations = $generations")
        sb.appendln("\tnumOffspring = $numOffspring")
        sb.appendln("\tbranchInitialisationRate = $branchInitialisationRate")
        sb.appendln("\tstoppingCriterion = $stoppingCriterion")

        return sb.toString()
    }
}