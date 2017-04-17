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
     * The types of the registers used by the LGP environment. Custom types can be used meaning that
     * custom operations/operations can be defined for those custom register types.
     *
     * **NOTE:** The register type dictates the types used throughout the entire environment.
     */
    var registerType = "java.lang.Double"

    /**
     * The location of a data set to be loaded by an appropriate [lgp.core.environment.dataset.DatasetLoader].
     */
    // TODO: Default to example directory or something along those lines.
    var datasetFilename = ""


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
}