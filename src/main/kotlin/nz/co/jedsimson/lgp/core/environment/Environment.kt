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
import nz.co.jedsimson.lgp.core.modules.ModuleContainer
import nz.co.jedsimson.lgp.core.modules.ModuleFactory
import nz.co.jedsimson.lgp.core.modules.RegisteredModuleType
import nz.co.jedsimson.lgp.core.program.Output
import java.util.Random

interface EnvironmentDefinition<TProgram, TOutput : Output<TProgram>>  {

    /**
     * Provides access to the environments random state.
     *
     * This allows the entire framework to share the random state (as a singleton) to allow deterministic runs.
     */
    val randomState: Random

    /**
     * A provider for a function that can measure the fitness of a program.
     *
     * This property should be provided at construction time.
     */
    val fitnessFunctionProvider: FitnessFunctionProvider<TProgram, TOutput>

    /**
     * Contains the various configuration options available to the LGP system.
     */
    val configuration: Configuration

    /**
     * A set of constants loaded using the [ConstantLoader] given at construction time.
     */
    val constants: List<TProgram>

    /**
     * A set of operations loaded using the [OperationLoader] given at construction time.
     */
    val operations: List<Operation<TProgram>>

    /**
     * Provides mechanisms for collecting results during the lifetime of an environment.
     */
    val resultAggregator: ResultAggregator<TProgram>

    /**
     * Provides access to modules that are registered in the environment.
     */
    val moduleFactory: ModuleFactory<TProgram, TOutput>

    /**
     * Registers the modules given by a container.
     *
     * @param container A container that specifies modules to be registered.
     */
    fun registerModules(container: ModuleContainer<TProgram, TOutput>)

    /**
     * Register a module builder with a particular module type.
     *
     * @param type The type of module to associate this builder with.
     * @param builder A function that can create the module.
     */
    fun registerModule(type: RegisteredModuleType, builder: (EnvironmentDefinition<TProgram, TOutput>) -> Module)

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
    fun copy(): EnvironmentDefinition<TProgram, TOutput>
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
class Environment<TProgram, TOutput : Output<TProgram>> : EnvironmentDefinition<TProgram, TOutput> {

    // These dependencies are generally initialised when the environment is constructed.
    // Access to these is moderated through the EnvironmentDefinition facade.
    private val configurationLoader: ConfigurationLoader
    private val constantLoader: ConstantLoader<TProgram>
    private val operationLoader: OperationLoader<TProgram>
    private val defaultValueProvider: DefaultValueProvider<TProgram>
    private val randomStateSeed: Long?
    private lateinit var registerSet: RegisterSet<TProgram>
    private var container: ModuleContainer<TProgram, TOutput>

    // The public environment interface
    // Some dependencies are initialised late as they are created during the environment construction.
    override val randomState: Random
    override val fitnessFunctionProvider: FitnessFunctionProvider<TProgram, TOutput>
    override lateinit var configuration: Configuration
    override lateinit var constants: List<TProgram>
    override lateinit var operations: List<Operation<TProgram>>
    override val resultAggregator: ResultAggregator<TProgram>
    override var moduleFactory: ModuleFactory<TProgram, TOutput>

    /**
     * Builds an environment with the specified construction components.
     *
     * @param configurationLoader A component that can load configuration information.
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
        configurationLoader: ConfigurationLoader,
        constantLoader: ConstantLoader<TProgram>,
        operationLoader: OperationLoader<TProgram>,
        defaultValueProvider: DefaultValueProvider<TProgram>,
        fitnessFunctionProvider: FitnessFunctionProvider<TProgram, TOutput>,
        resultAggregator: ResultAggregator<TProgram>? = null,
        randomStateSeed: Long? = null
    ) {

        this.configurationLoader = configurationLoader
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
        this.moduleFactory = ModuleFactory(this.container)

        // Kick off initialisation
        this.initialise()
    }

    private fun initialise() {
        // Load the components each loader is responsible for.
        this.configuration = this.configurationLoader.load()
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

    override fun registerModules(container: ModuleContainer<TProgram, TOutput>) {
        this.container = container
        this.moduleFactory = ModuleFactory(this.container)

        // Update the containers environment dependency.
        this.container.environment = this
    }

    override fun registerModule(type: RegisteredModuleType, builder: (EnvironmentDefinition<TProgram, TOutput>) -> Module) {
        this.container.modules[type] = builder
    }

    override fun copy(): Environment<TProgram, TOutput> {
        // Construct a copy with the correct construction/initialised components.
        val copy = Environment(
                this.configurationLoader,
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
        copy.moduleFactory.instanceCache.clear()

        return copy
    }
}
