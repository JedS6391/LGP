package lgp.core.environment.config

import com.google.gson.annotations.SerializedName
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

    /*
     * Each instances attributes are loaded into a programs register set at execution time.
     *
     * [inputAttributesLowIndex] refers to the lower bound of the range of attributes to load.
     * [inputAttributesHighIndex] refers to the upper bound of the range of attributes to load.
     * [classAttributeIndex] refers to the index of an attribute which acts as the class attribute.
     */

    /**
     * A lower bound of attributes from the data set to load into the registers.
     */
    var inputAttributesLowIndex = 0

    /**
     * An upper bound of attributes from the data set to load into the registers.
     */
    var inputAttributesHighIndex = 0

    /**
     * An index of an attribute from the data set that will act as a class attribute.
     */
    var classAttributeIndex = 0

    var crossoverRate = 0.5

    var microMutationRate = 0.5

    var macroMutationRate = 0.5

    var generations = 50

    var numOffspring = 50
}