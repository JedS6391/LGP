package lgp.core.environment

import lgp.core.environment.config.Config
import lgp.core.environment.config.ConfigLoader
import lgp.core.environment.constants.ConstantLoader
import lgp.core.environment.dataset.Dataset
import lgp.core.environment.dataset.DatasetLoader
import lgp.core.environment.operations.OperationLoader
import lgp.core.evolution.fitness.FitnessFunction
import lgp.core.evolution.instructions.InstructionGenerator
import lgp.core.evolution.instructions.Operation
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.Module

class ModuleRegistrationException : Exception("All module types must have a module implementation registered to them.")

/**
 * Represents the different modules that are able to be registered with an environment.
 *
 * Any module that is a *Register Component* should be specified here.
 */
// TODO: Find a way to make this more automatic.
// TODO: Investigate using sealed class.
enum class RegisteredModuleType {

    /**
     * A module that provides a concrete [lgp.core.evolution.instructions.InstructionGenerator] implementation.
     */
    InstructionGenerator,

    /**
     * A module that provides a concrete [lgp.core.evolution.population.ProgramGenerator] implementation.
     */
    ProgramGenerator,

    SelectionOperator,

    RecombinationOperator,

    MacroMutationOperator,

    MicroMutationOperator
}

/**
 * A container that provides modules that need to be registered with an environment.
 *
 * @property modules A mapping of modules that can be registered to a function that constructs that module.
 */
data class ModuleContainer(val modules: Map<RegisteredModuleType, () -> Module>) {

    // All instances are provided as singletons
    private val instanceCache = mutableMapOf<RegisteredModuleType, Module>()

    /**
     * Provides an instance of the module type given.
     *
     * The module will be loaded and cast to the type given.
     *
     * @param type The type of of module to get an instance of.
     * @param TModule The type to cast the module as.
     */
    fun <TModule> instance(type: RegisteredModuleType): TModule {
        if (type in instanceCache)
            return instanceCache[type] as TModule

        val moduleBuilder = this.modules[type]

        // Because the module building function could potentially be null
        // due to the way maps work, we can't just directly call it so we
        // have to make an ugly call to `invoke`.
        val module = moduleBuilder?.invoke()

        // Cache this instance
        instanceCache[type] = module!!

        return module as TModule
    }

}

/**
 * A central repository for core components of the LGP system.
 *
 * An environment should be built by providing the correct components. The environment will
 * maintain these components so that can be accessed by the LGP system from wherever they are needed.
 *
 * The components required by the environment are split into three categories:
 *
 *     1. Construction components: Given to an environment at construction time.
 *     2. Initialisation components: Resolved internally given valid construction components.
 *     3. Registered components: Resolved manually by registering with an environment after construction.
 *
 * After an environment is built and all components are resolved, it can be used to initiate the core
 * evolution process of LGP.
 */
open class Environment<T> {

    // Dependencies that we require at construction time
    private val configLoader: ConfigLoader
    private val constantLoader: ConstantLoader<T>
    private val datasetLoader: DatasetLoader<T>
    private val operationLoader: OperationLoader<T>
    private val defaultValueProvider: DefaultValueProvider<T>
    val fitnessFunction: FitnessFunction<T>

    // Dependencies that come from the loaders given to the environment and are not necessarily
    // needed until the environment is initialised.
    lateinit var config: Config
    lateinit var constants: List<T>
    lateinit var dataset: Dataset<T>
    lateinit var operations: List<Operation<T>>

    lateinit var registerSet: RegisterSet<T>

    lateinit var container: ModuleContainer

    // TODO: Should a default environment be provided?
    /**
     * Builds an environment with the specified construction components.
     *
     * @param configLoader A component that can load configuration information.
     * @param constantLoader A component that can load constants.
     * @param datasetLoader A component that can load the dataset for an LGP system.
     * @param operationLoader A component that can load operations for the LGP system.
     * @param defaultValueProvider A component that provides default values for the registers in the register set.
     * @param fitnessFunction A function used to evaluate the fitness of LGP programs.
     * @param initialise Whether or not initialised components should be initialised automatically.
     */
    // TODO: Move all components to an object to make constructor smaller.
    // TODO: Allow custom initialisation method for initialisation components.
    // TODO: Default value provider and fitness function could be given in config?
    constructor(configLoader: ConfigLoader, constantLoader: ConstantLoader<T>,
                datasetLoader: DatasetLoader<T>, operationLoader: OperationLoader<T>,
                defaultValueProvider: DefaultValueProvider<T>, fitnessFunction: FitnessFunction<T>,
                initialise: Boolean = true) {

        this.configLoader = configLoader
        this.constantLoader = constantLoader
        this.datasetLoader = datasetLoader
        this.operationLoader = operationLoader
        this.defaultValueProvider = defaultValueProvider
        this.fitnessFunction = fitnessFunction

        // Kick off initialisation
        if (initialise)
            this.initialise()
    }

    private fun initialise() {
        // Load the components each loader is responsible for.
        this.config = this.configLoader.load()
        this.constants = this.constantLoader.load()
        this.dataset = this.datasetLoader.load()
        this.operations = this.operationLoader.load()

        // TODO: Instead of initialising, allow user to register?
        this.initialiseRegisterSet()
    }

    private fun initialiseRegisterSet() {
        // The environment takes care of its own base register set that is not modified by programs.
        // This means that anything that can access the environment has access to a blank register set.
        // TODO: Pass environment to register set and make it a dependency that must be registered.

        this.dataset.classAttribute(this.config.classAttributeIndex)
        this.dataset.inputAttributes(this.config.inputAttributesLowIndex..this.config.inputAttributesHighIndex)

        this.registerSet = RegisterSet(
                inputRegisters = this.dataset.numInputs(),
                calculationRegisters = this.config.numCalculationRegisters,
                constants = this.constants,
                defaultValueProvider = this.defaultValueProvider
        )
    }

    private fun validateModules(container: ModuleContainer): Boolean {
        // Check all module types have an implementation
        return RegisteredModuleType.values().map { type ->
            type in container.modules
        }.all { b -> b}
    }

    /**
     * Registers the modules given by a container.
     *
     * @param container A container that specifies modules to be registered.
     * @throws ModuleRegistrationException When an implementation is not provided for a registered module type.
     */
    fun registerModules(container: ModuleContainer) {
        if (!this.validateModules(container)) {
            throw ModuleRegistrationException()
        }

        this.container = container
    }

    /**
     * Fetches an instance of the module registered for a particular module type.
     *
     * @param type The type of registered module to fetch.
     * @param TModule The type the module will be cast as.
     */
    fun <TModule : Module> registeredModule(type: RegisteredModuleType): TModule {
        return this.container.instance<TModule>(type)
    }

}