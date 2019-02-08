package nz.co.jedsimson.lgp.core.environment

import nz.co.jedsimson.lgp.core.environment.config.*
import nz.co.jedsimson.lgp.core.environment.constants.ConstantLoader
import nz.co.jedsimson.lgp.core.environment.operations.OperationLoader
import nz.co.jedsimson.lgp.core.evolution.ResultAggregator
import nz.co.jedsimson.lgp.core.evolution.ResultAggregators
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessFunctionProvider
import nz.co.jedsimson.lgp.core.program.instructions.Operation
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.program.Output
import java.util.Random

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
    MicroMutationOperator,

    /**
     * A [FitnessContext] implementation.
     */
    FitnessContext
}

/**
 * A container that provides modules that need to be registered with an environment.
 *
 * @property modules A mapping of modules that can be registered to a function that constructs that module.
 */
data class ModuleContainer<T, TOutput : Output<T>>(
    val modules: MutableMap<RegisteredModuleType, (Environment<T, TOutput>) -> Module>
) {

    lateinit var environment: Environment<T, TOutput>

    // All instances are provided as singletons
    val instanceCache = mutableMapOf<RegisteredModuleType, Module>()

    /**
     * Provides an instance of the module [type] given.
     *
     * The module will be loaded and cast to the type given if possible to cast to that type. This method is able to
     * perform safe casts due to Kotlin's reified generics. If the cast can't be done safely, a [ModuleCastException]
     * will be thrown and contains details about the failed cast. Due to the use of reified generics, callers of this
     * function can be assured that a [ClassCastException] won't occur.
     *
     * **NOTE:** This function cannot be used from Java; use the [instanceUnsafe] function instead.
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
        val module = moduleBuilder(this.environment) as? TModule
                ?: throw ModuleCastException("Unable to cast $type module as ${TModule::class.java.simpleName}.")

        // Cache this instance for later usages since it is valid when cast to the type given.
        instanceCache[type] = module

        return module
    }

    /**
     * Provides an instance of the module [type] given (for Java interoperability).
     *
     * **TL;DR:** Java users -- beware! This function implements [instance] for Java callers, but unlike the Kotlin
     * equivalent, provides no guarantee that a [ClassCastException] will not occur.
     *
     * This method exists to aid Java interoperability: the [instance] method cannot be used from Java
     * as it is both inlined and makes use of reified generics. With this method, Java users can query for
     * [RegisteredModuleType] instances.
     *
     * The reason it is unsafe is that there is no guarantee that the cast will be correct: if the type requested
     * can be cast to [Module] then the cast will succeed and a [ClassCastException] will be given where the call is
     * made (i.e. the function will return with no error until an assignment type is checked). With the normal (and
     * safe) [instance] function, the invalid cast can be caught before the function returns and dealt with appropriately.
     *
     * @param type The type of of module to get an instance of.
     * @param TModule The type to cast the module as.
     * @return An instance of the module registered for the given module type.
     * @throws MissingModuleException When no builder has been registered for the type of module requested.
     * @throws ModuleCastException When the requested module can't be cast to the type given.
     */
    fun <TModule : Module> instanceUnsafe(type: RegisteredModuleType): TModule {
        @Suppress("UNCHECKED_CAST")
        if (type in instanceCache)
            return instanceCache[type] as TModule

        val moduleBuilder = this.modules[type]
                ?: throw MissingModuleException("No module builder registered for $type.")

        @Suppress("UNCHECKED_CAST")
        val module = moduleBuilder(this.environment) as? TModule
                ?: throw ModuleCastException("Unable to cast $type module to given type.")

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
open class Environment<TProgram, TOutput : Output<TProgram>> {

    // Dependencies that we require at construction time and are used during initialisation
    // but aren't needed after that.
    private val configLoader: ConfigurationLoader
    private val constantLoader: ConstantLoader<TProgram>
    private val operationLoader: OperationLoader<TProgram>
    private val defaultValueProvider: DefaultValueProvider<TProgram>
    private val randomStateSeed: Long?
    var randomState: Random

    /**
     * A provider for a function that can measure the fitness of a program.
     *
     * This property should be provided at construction time.
     */
    val fitnessFunctionProvider: FitnessFunctionProvider<TProgram, TOutput>

    // Dependencies that come from the loaders given to the environment and are not necessarily
    // needed until the environment is initialised.

    /**
     * Contains the various configuration options available to the LGP system.
     */
    lateinit var configuration: Configuration

    /**
     * A set of constants loaded using the [ConstantLoader] given at construction time.
     */
    lateinit var constants: List<TProgram>

    /**
     * A set of operations loaded using the [OperationLoader] given at construction time.
     */
    lateinit var operations: List<Operation<TProgram>>

    /**
     * A set of registers that is built at initialisation time.
     */
    lateinit var registerSet: RegisterSet<TProgram>

    /**
     * A container for the various registered component modules that the environment maintains.
     */
    var container: ModuleContainer<TProgram, TOutput>

    val resultAggregator: ResultAggregator<TProgram>

    /**
     * Builds an environment with the specified construction components.
     *
     * @param configLoader A component that can load configuration information.
     * @param constantLoader A component that can load constants.
     * @param operationLoader A component that can load operations for the LGP system.
     * @param defaultValueProvider A component that provides default values for the registers in the register set.
     * @param fitnessFunctionProvider A function used to evaluate the fitness of LGP programs.
     * @param randomStateSeed Sets the seed of the random number generator. If a value is given, the seed will
     *        be set, and will produce deterministic runs. If null is given, a random seed will be chosen.
     */
    // TODO: Move all components to an object to make constructor smaller.
    // TODO: Allow custom initialisation method for initialisation components.
    // TODO: Default value provider and fitness function could be given in configuration?
    constructor(
            configLoader: ConfigurationLoader,
            constantLoader: ConstantLoader<TProgram>,
            operationLoader: OperationLoader<TProgram>,
            defaultValueProvider: DefaultValueProvider<TProgram>,
            fitnessFunctionProvider: FitnessFunctionProvider<TProgram, TOutput>,
            resultAggregator: ResultAggregator<TProgram>? = null,
            randomStateSeed: Long? = null
    ) {

        this.configLoader = configLoader
        this.constantLoader = constantLoader
        this.operationLoader = operationLoader
        this.defaultValueProvider = defaultValueProvider
        this.fitnessFunctionProvider = fitnessFunctionProvider
        this.randomStateSeed = randomStateSeed
        // If no result aggregator is provided then use the default aggregator which doesn't collect results.
        this.resultAggregator = resultAggregator ?: ResultAggregators.DefaultResultAggregator()

        // Determine whether we need to seed the RNG or not.
        when (this.randomStateSeed) {
            is Long -> this.randomState = Random(this.randomStateSeed)
            else    -> this.randomState = Random()
        }

        // Empty module container to begin
        this.container = ModuleContainer(modules = mutableMapOf())

        // Kick off initialisation
        this.initialise()
    }

    private fun initialise() {
        // Load the components each loader is responsible for.
        this.configuration = this.configLoader.load()
        this.constants = this.constantLoader.load()
        this.operations = this.operationLoader.load()

        // Early exit if the configuration provided is invalid
        val configValidity = this.configuration.isValid()

        when (configValidity) {
            is Invalid -> throw InvalidConfigurationException(configValidity.reason)
            else -> { /* No-op */ }
        }

        // TODO: Instead of initialising, allow user to register?
        this.initialiseRegisterSet()

        // Make sure the modules have access to this environment.
        this.container.environment = this
    }

    private fun initialiseRegisterSet() {
        // The environment takes care of its own base register set that is not modified by programs.
        // This means that anything that can access the environment has access to a blank register set.
        // TODO: Pass environment to register set and make it a dependency that must be registered.

        this.registerSet = RegisterSet(
                inputRegisters = this.configuration.numFeatures,
                calculationRegisters = this.configuration.numCalculationRegisters,
                constants = this.constants,
                defaultValueProvider = this.defaultValueProvider
        )
    }

    /**
     * Registers the modules given by a container.
     *
     * @param container A container that specifies modules to be registered.
     */
    fun registerModules(container: ModuleContainer<TProgram, TOutput>) {
        this.container = container

        // Update the containers environment dependency.
        this.container.environment = this
    }

    /**
     * Register a module builder with a particular module type.
     *
     * @param type The type of module to associate this builder with.
     * @param builder A function that can create the module.
     */
    fun registerModule(type: RegisteredModuleType, builder: (Environment<TProgram, TOutput>) -> Module) {
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
        return this.container.instance(type)
    }

    /**
     * Similar to [registeredModule], but may give a [ClassCastException] at the call site.
     *
     * The internals of the module retrieval system cannot guarantee that any casting will be
     * done safely, and thus this method should be used with caution. The reason for its existence
     * is to make life easier for those using the API through Java.
     *
     * @param type The type of registered module to fetch.
     * @param TModule The type the module will be cast as.
     * @return An instance of the module registered for the given module type.
     * @throws MissingModuleException When no builder has been registered for the type of module requested.
     */
    fun <TModule : Module> registeredModuleUnsafe(type: RegisteredModuleType): TModule {
        return this.container.instanceUnsafe(type)
    }

    /**
     * Produces a clone of the current environment.
     *
     * It should be noted that because an environment instance has its own RNG associated with it,
     * when making a copy, it is required that the copied environment have its own RNG too.
     * To fulfil this requirement, when an environment is copied, it will initialise a new RNG that is
     * seeded with a seed given from the RNG of the environment instance performing the copy (confusing -- yes!).
     *
     * The main reason behind this complication is to ensure that there are no contention issues when multiple
     * environment instances are operating in a multi-threaded context (e.g. through a [lgp.core.evolution.Trainers.DistributedTrainer]).
     *
     * Furthermore, any modules that are registered with the environment being copied, will be updated so
     * that the reference the correct environment instance (i.e. the copy). This ensures that while the
     * module registrations themselves are shared between copies, when a module is accessed, it gets initialised
     * correctly.
     *
     * @return A new [Environment] instance that is a copy of that the method is called on.
     */
    fun copy(): Environment<TProgram, TOutput> {
        // Construct a copy with the correct construction/initialised components.
        val copy = Environment(
                this.configLoader,
                this.constantLoader,
                this.operationLoader,
                this.defaultValueProvider,
                this.fitnessFunctionProvider,
                this.resultAggregator,
                this.randomState.nextLong()
        )

        // Now, the tricky part. We have to ensure that the containers modules
        // have a reference to the copied environment, and not the old environment.
        val container = ModuleContainer(this.container.modules)
        container.environment = copy

        copy.registerModules(container)

        // We also need to clear any cached modules, just in case there are any references
        // to the previous environment laying around. Generally, any environment instances
        // would be copied before modules are accessed -- but it doesn't hurt to be cautious!
        copy.container.instanceCache.clear()

        return copy
    }
}
