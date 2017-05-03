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

// TODO: Move code relating to modules to another file (or even to the modules package?).

/**
 * Exception thrown when no [Module] is registered for a requested [RegisteredModuleType].
 */
class MissingModuleException(message: String) : Exception(message)

/**
 * Exception thrown when a [Module] is cast as a type that is not valid for it.
 */
class ModuleCastException(message: String) : Exception(message)

/**
 * Represents the different modules that are able to be registered with an environment.
 *
 * Any module that is able to be registered with the [Environment] as a *registered component*
 * should have a module type defined for it using this interface.
 *
 * @see [CoreModuleType] for an example implementation.
 */
interface RegisteredModuleType

/**
 * A mapping for core modules to a module type value.
 */
enum class CoreModuleType : RegisteredModuleType {

    /**
     * An [InstructionGenerator] implementation.
     */
    InstructionGenerator,

    /**
     * A [ProgramGenerator] implementation.
     */
    ProgramGenerator,

    /**
     * A [SelectionOperator] implementation.
     */
    SelectionOperator,

    /**
     * A [RecombinationOperator] implementation.
     */
    RecombinationOperator,

    /**
     * A [MacroMutationOperator] implementation.
     */
    MacroMutationOperator,

    /**
     * A [MicroMutationOperator] implementation.
     */
    MicroMutationOperator
}

/**
 * A container that provides modules that need to be registered with an environment.
 *
 * @property modules A mapping of modules that can be registered to a function that constructs that module.
 */
data class ModuleContainer(val modules: MutableMap<RegisteredModuleType, () -> Module>) {

    // All instances are provided as singletons
    val instanceCache = mutableMapOf<RegisteredModuleType, Module>()

    /**
     * Provides an instance of the module type given.
     *
     * The module will be loaded and cast to the type given if possible to cast to that type.
     *
     * @param type The type of of module to get an instance of.
     * @param TModule The type to cast the module as.
     * @return An instance of the module registered for the given module type.
     * @throws MissingModuleException When no builder has been registered for the type of module requested.
     * @throws ModuleCastException When the requested module can't be cast to the type given.
     */
    inline fun <reified TModule : Module> instance(type: RegisteredModuleType): TModule {
        if (type in instanceCache)
            return instanceCache[type] as TModule

        // If no module builder exists (i.e. it is null) then we can assume that
        // no module builder has been registered for this module type, despite it
        // being requested by from somewhere.
        val moduleBuilder = this.modules[type]
                ?: throw MissingModuleException("No module builder registered for $type.")

        // At this stage, we at least know that the module builder is valid so we can go ahead and execute it.
        // However, we need to check that the module can actually be cast as the type requested.
        // Doing this means that we don't have to do unchecked casts anywhere and gives protection against
        // invalid casts from calling code.
        val module = moduleBuilder() as? TModule
                ?: throw ModuleCastException("Unable to cast $type module as ${TModule::class.java.simpleName}.")

        // Cache this instance for later usages since it is valid when cast to the type given.
        instanceCache[type] = module

        return module
    }

}

/**
 * A central repository for core components made available to the LGP system.
 *
 * An environment should be built by providing the correct components. The environment will
 * maintain these components so that can be accessed by the LGP system from wherever they are needed.
 *
 * The components required by the environment are split into three categories:
 *
 * 1. Construction components: Given to an environment at construction time.
 * 2. Initialisation components: Resolved internally given valid construction components.
 * 3. Registered components: Resolved manually by registering with an environment after construction.
 *
 * After an environment is built and all components are resolved, it can be used to initiate the core
 * evolution process of LGP.
 */
open class Environment<T> {

    // Dependencies that we require at construction time and are used during initialisation
    // but aren't needed after that.
    private val configLoader: ConfigLoader
    private val constantLoader: ConstantLoader<T>
    private val datasetLoader: DatasetLoader<T>
    private val operationLoader: OperationLoader<T>
    private val defaultValueProvider: DefaultValueProvider<T>

    /**
     * A function that can measure the fitness of a program.
     *
     * This property should be provided at construction time.
     */
    val fitnessFunction: FitnessFunction<T>

    // Dependencies that come from the loaders given to the environment and are not necessarily
    // needed until the environment is initialised.

    /**
     * Contains the various configuration options available to the LGP system.
     */
    lateinit var config: Config

    /**
     * A set of constants loaded using the [ConstantLoader] given at construction time.
     */
    lateinit var constants: List<T>

    /**
     * A dataset loaded using the [DatasetLoader] given at construction time.
     */
    lateinit var dataset: Dataset<T>

    /**
     * A set of operations loaded using the [OperationLoader] given at construction time.
     */
    lateinit var operations: List<Operation<T>>

    /**
     * A set of registers that is built at initialisation time.
     */
    lateinit var registerSet: RegisterSet<T>

    /**
     * A container for the various registered component modules that the environment maintains.
     */
    var container: ModuleContainer

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
    constructor(configLoader: ConfigLoader,
                constantLoader: ConstantLoader<T>,
                datasetLoader: DatasetLoader<T>,
                operationLoader: OperationLoader<T>,
                defaultValueProvider: DefaultValueProvider<T>,
                fitnessFunction: FitnessFunction<T>) {

        this.configLoader = configLoader
        this.constantLoader = constantLoader
        this.datasetLoader = datasetLoader
        this.operationLoader = operationLoader
        this.defaultValueProvider = defaultValueProvider
        this.fitnessFunction = fitnessFunction

        // Empty module container to begin
        this.container = ModuleContainer(modules = mutableMapOf())

        // Kick off initialisation
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

    /**
     * Registers the modules given by a container.
     *
     * @param container A container that specifies modules to be registered.
     */
    fun registerModules(container: ModuleContainer) {
        this.container = container
    }

    /**
     * Register a module builder with a particular module type.
     *
     * @param type The type of module to associate this builder with.
     * @param builder A function that can create the module.
     */
    fun <T : Module> registerModule(type: RegisteredModuleType, builder: () -> T) {
        this.container.modules[type] = builder
    }

    /**
     * Fetches an instance of the module registered for a particular module type.
     *
     * The environment assumes that a module type will have a builder registered to
     * if it is being requested. If this fails, then a [MissingModuleException] will
     * be thrown, indicating that an instance of a particular module type was requested
     * but could not fulfilled.
     *
     * Usually, the type parameter needn't be explicitly given, as it can be inferred
     * from the type of value the result is being assigned to.
     *
     * @param type The type of registered module to fetch.
     * @param TModule The type the module will be cast as.
     * @return An instance of the module registered for the given module type.
     * @throws MissingModuleException When no builder has been registered for the type of module requested.
     */
    inline fun <reified TModule : Module> registeredModule(type: RegisteredModuleType): TModule {
        return this.container.instance<TModule>(type)
    }

}