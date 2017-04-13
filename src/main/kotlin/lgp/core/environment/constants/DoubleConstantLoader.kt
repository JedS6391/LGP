package lgp.core.environment.constants

import lgp.core.environment.ComponentLoaderBuilder
import lgp.core.modules.ModuleInformation

class FloatConstantLoader constructor(constants: List<String>)
    : GenericConstantLoader<Double>(constants, String::toDouble) {

    // Give this loader a custom description since it is provided as part of the core package.
    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "A loader than can parse specified constants into floats."
    }
}
