package nz.co.jedsimson.lgp.core.environment

import nz.co.jedsimson.lgp.core.environment.config.*
import nz.co.jedsimson.lgp.core.environment.constants.ConstantLoader
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.environment.events.Diagnostics
import nz.co.jedsimson.lgp.core.environment.operations.OperationLoader
import nz.co.jedsimson.lgp.core.evolution.ResultAggregator
import nz.co.jedsimson.lgp.core.evolution.ResultAggregators
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessFunctionProvider
import nz.co.jedsimson.lgp.core.modules.*
import nz.co.jedsimson.lgp.core.program.instructions.Operation
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.registers.ArrayRegisterSet
import kotlin.random.Random

/**
 * A specification for building an [Environment].
 *
 * @property configurationLoader A component that can load configuration information.
 * @property constantLoader A component that can load constants.
 * @property operationLoader A component that can load operations for the LGP system.
 * @property defaultValueProvider A component that provides default values for the registers in the register set.
 * @property fitnessFunctionProvider A function used to evaluate the fitness of LGP programs.
 * @property randomStateSeed Sets the seed of the random number generator. If a value is given, the seed will
 *           be set, and will produce deterministic runs. If null is given, a random seed will be chosen.
 */
data class EnvironmentSpecification<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    val configurationLoader: ConfigurationLoader,
    val constantLoader: ConstantLoader<TProgram>,
    val operationLoader: OperationLoader<TProgram>,
    val defaultValueProvider: DefaultValueProvider<TProgram>,
    val fitnessFunctionProvider: FitnessFunctionProvider<TProgram, TOutput, TTarget>,
    val resultAggregator: ResultAggregator<TProgram>? = null,
    val randomStateSeed: Long? = null
)

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
 *
 * @param specification A specification for an [Environment].
 * @constructor Builds an [Environment] from the given [EnvironmentSpecification].
 */
class Environment<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    private val specification: EnvironmentSpecification<TProgram, TOutput, TTarget>
)
    : EnvironmentFacade<TProgram, TOutput, TTarget> {

    // These dependencies are generally initialised when the environment is constructed.
    // Access to these are moderated through the EnvironmentFacade.
    private val configurationLoader: ConfigurationLoader
    private val constantLoader: ConstantLoader<TProgram>
    private val operationLoader: OperationLoader<TProgram>
    private val defaultValueProvider: DefaultValueProvider<TProgram>
    private val randomStateSeed: Long?
    private var container: ModuleContainer<TProgram, TOutput, TTarget>

    // The public environment interface
    // Some dependencies are initialised late as they are created during the environment construction.
    override val randomState: Random
    override val fitnessFunctionProvider: FitnessFunctionProvider<TProgram, TOutput, TTarget>
    override lateinit var configuration: Configuration
    override lateinit var constants: List<TProgram>
    override lateinit var operations: List<Operation<TProgram>>
    override lateinit var registerSet: RegisterSet<TProgram>
    override val resultAggregator: ResultAggregator<TProgram>
    override lateinit var moduleFactory: ModuleFactory<TProgram, TOutput, TTarget>

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
    @Deprecated("This constructor is deprecated.", ReplaceWith("Environment(specification)", "nz.co.jedsimson.lgp.core.environment"), DeprecationLevel.WARNING)
    constructor(
        configurationLoader: ConfigurationLoader,
        constantLoader: ConstantLoader<TProgram>,
        operationLoader: OperationLoader<TProgram>,
        defaultValueProvider: DefaultValueProvider<TProgram>,
        fitnessFunctionProvider: FitnessFunctionProvider<TProgram, TOutput, TTarget>,
        resultAggregator: ResultAggregator<TProgram>? = null,
        randomStateSeed: Long? = null
    ) : this(EnvironmentSpecification(
        configurationLoader = configurationLoader,
        constantLoader = constantLoader,
        operationLoader = operationLoader,
        defaultValueProvider = defaultValueProvider,
        fitnessFunctionProvider = fitnessFunctionProvider,
        resultAggregator = resultAggregator,
        randomStateSeed = randomStateSeed
    ))

    init {
        Diagnostics.trace("Environment:construction-start")

        this.configurationLoader = this.specification.configurationLoader
        this.constantLoader = this.specification.constantLoader
        this.operationLoader = this.specification.operationLoader
        this.defaultValueProvider = this.specification.defaultValueProvider
        this.fitnessFunctionProvider = this.specification.fitnessFunctionProvider
        this.randomStateSeed = this.specification.randomStateSeed
        // If no result aggregator is provided then use the default aggregator which doesn't collect results.
        this.resultAggregator = this.specification.resultAggregator ?: ResultAggregators.DefaultResultAggregator()

        // Determine whether we need to seed the RNG or not.
        when (this.randomStateSeed) {
            is Long -> this.randomState = Random(this.randomStateSeed)
            else    -> this.randomState = Random.Default
        }

        // Empty module container to begin
        this.container = ModuleContainer(modules = mutableMapOf())
        this.moduleFactory = CachingModuleFactory(this.container)

        // Kick off initialisation
        Diagnostics.traceWithTime("Environment:initialise") {
            this.initialise()
        }

        Diagnostics.trace("Environment:construction-end")
    }

    private fun initialise() {
        // Load the components each loader is responsible for.
        this.configuration = Diagnostics.traceWithTime("Environment:initialise-configuration") {
            this.configurationLoader.load()
        }

        this.constants = Diagnostics.traceWithTime("Environment:initialise-constants") {
            this.constantLoader.load()
        }

        this.operations = Diagnostics.traceWithTime("Environment:initialise-operations") {
            this.operationLoader.load()
        }

        // Early exit if the configuration provided is invalid
        when (val configValidity = this.configuration.isValid()) {
            is ConfigurationValidity.Invalid -> throw InvalidConfigurationException(configValidity.reason)
            else -> { /* No-op */ }
        }

        // TODO: Instead of initialising, allow user to register?
        Diagnostics.traceWithTime("Environment:initialise-register-set") {
            this.initialiseRegisterSet()
        }

        // Make sure the modules have access to this environment.
        this.container.environment = this
    }

    private fun initialiseRegisterSet() {
        // The environment takes care of its own base register set that is not modified by programs.
        // This means that anything that can access the environment has access to a blank register set.
        // TODO: Pass environment to register set and make it a dependency that must be registered.

        this.registerSet = ArrayRegisterSet(
            inputRegisters = this.configuration.numFeatures,
            calculationRegisters = this.configuration.numCalculationRegisters,
            constants = this.constants,
            defaultValueProvider = this.defaultValueProvider
        )
    }

    override fun registerModules(container: ModuleContainer<TProgram, TOutput, TTarget>) {
        this.container = container
        this.moduleFactory = CachingModuleFactory(this.container)

        // Update the containers environment dependency.
        this.container.environment = this
    }

    override fun registerModule(type: RegisteredModuleType, builder: (EnvironmentFacade<TProgram, TOutput, TTarget>) -> Module) {
        this.container.modules[type] = builder
    }

    /**
     * It should be noted that because an environment instance has its own RNG associated with it,
     * when making a copy, it is required that the copied environment have its own RNG too.
     * To fulfil this requirement, when an environment is copied, it will initialise a new RNG that is
     * seeded with a seed given from the RNG of the environment instance performing the copy (confusing -- yes!).
     *
     * The main reason behind this complication is to ensure that there are no contention issues when multiple
     * environment instances are operating in a multi-threaded context (e.g. through a [DistributedTrainer]).
     *
     * Furthermore, any modules that are registered with the environment being copied, will be updated so
     * that the reference the correct environment instance (i.e. the copy). This ensures that while the
     * module registrations themselves are shared between copies, when a module is accessed, it gets initialised
     * correctly.
     */
    override fun copy(): Environment<TProgram, TOutput, TTarget> {
        // Construct a copy with the correct construction/initialised components.
        val copy = Environment(
            EnvironmentSpecification(
                this.configurationLoader,
                this.constantLoader,
                this.operationLoader,
                this.defaultValueProvider,
                this.fitnessFunctionProvider,
                this.resultAggregator,
                this.randomState.nextLong()
            )
        )

        // Now, the tricky part. We have to ensure that the containers modules
        // have a reference to the copied environment, and not the old environment.
        val container = ModuleContainer(this.container.modules)
        container.environment = copy

        copy.registerModules(container)

        // We also need to clear any cached modules, just in case there are any references
        // to the previous environment laying around. Generally, any environment instances
        // would be copied before modules are accessed -- but it doesn't hurt to be cautious!
        copy.moduleFactory = CachingModuleFactory(container)

        return copy
    }
}
