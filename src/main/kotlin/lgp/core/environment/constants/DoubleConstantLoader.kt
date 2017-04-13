package lgp.core.environment.constants

import lgp.core.modules.ModuleInformation

/**
 * An implementation of [ConstantLoader] that loads constants as a collection of [Double]s.
 *
 * We get around having to implement a lot of this ourselves by subclassing [GenericConstantLoader]
 * with a parsing function suitable for converting the raw strings to doubles.
 *
 * @param constants A collection of raw constants.
 * @see [GenericConstantLoader]
 */
class DoubleConstantLoader constructor(constants: List<String>)
    : GenericConstantLoader<Double>(constants, String::toDouble) {

    // Give this loader a custom description since it is provided as part of the core package.
    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "A loader than can parse specified constants into floats."
    }
}
