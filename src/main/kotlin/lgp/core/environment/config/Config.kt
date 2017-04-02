package lgp.core.environment.config

import java.util.ArrayList

/**
 * Represents the parameters that can be set for the [lgp.core.environment.Environment].
 *
 * An implementation of [ConfigLoader] can load [Config]s from a specified source.
 */
class Config {

    // A newly generated program will have length L where
    // initialMinimumProgramLength <= L <= initialMaximumProgramLength.
    var initialMinimumProgramLength = 10
    var initialMaximumProgramLength = 30

    // A program will always have length L where
    // minimumProgramLength <= L <= maximumProgramLength.
    var minimumProgramLength = 10
    var maximumProgramLength = 200

    // The types of the registers. Custom types can be used meaning that
    // custom operations/operations can be defined for those custom register
    // types. Note that the register type dictates the types used for all
    // operations and operations.
    var registerType = "java.lang.Double"

    // The operations that can be chosen from when generating
    // operations. The list should contain a collection of class
    // names which will be loaded at runtime using reflection.
    // NOTE: If no operations are defined in a format parsed by the config
    // loader then the operation pool will be empty.
    var operations: List<String> = ArrayList()

    // How often should constants be used and what those constants are
    var constantsRate = 0.5

    // Constants are stored as simple strings but can be parsed where necessary.
    // NOTE: If no constants are defined in format parsed by the config loader
    // then the constant pool will be empty.
    var constants: List<String> = ArrayList()

    // Constant loading
    var constantLoaderModule = "core.java.environment.loaders.constants.FloatConstantLoader"
    var constantRegisterLoaderModule = "core.java.environment.loaders.registers.ConstantRegisterLoader"

    var engineModule = "lib.java.base.BaseEngine"
    var programModule = "lib.java.base.BaseProgram"
    var instructionGeneratorModule = "lib.java.base.BaseInstructionGenerator"

    var numCalculationRegisters = 10

    var populationSize = 100
}