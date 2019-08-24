package nz.co.jedsimson.lgp.test.mocks

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.environment.dataset.Targets
import nz.co.jedsimson.lgp.core.evolution.ResultAggregator
import nz.co.jedsimson.lgp.core.evolution.fitness.FitnessFunctionProvider
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.modules.ModuleContainer
import nz.co.jedsimson.lgp.core.modules.ModuleFactory
import nz.co.jedsimson.lgp.core.modules.RegisteredModuleType
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.instructions.Operation
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import kotlin.random.Random

class MockEnvironment(
    private val mockConfiguration: Configuration? = null,
    private val mockRandomState: Random? = null
) : EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>> {
    override val randomState: Random
        get() = mockRandomState ?: TODO("not implemented")
    override val fitnessFunctionProvider: FitnessFunctionProvider<Double, Outputs.Single<Double>, Targets.Single<Double>>
        get() = TODO("not implemented")
    override val configuration: Configuration
        get() = mockConfiguration ?: TODO("not implemented")
    override val constants: List<Double>
        get() = TODO("not implemented")
    override val operations: List<Operation<Double>>
        get() = TODO("not implemented")
    override val resultAggregator: ResultAggregator<Double>
        get() = TODO("not implemented")
    override val moduleFactory: ModuleFactory<Double, Outputs.Single<Double>, Targets.Single<Double>>
        get() = TODO("not implemented")
    override val registerSet: RegisterSet<Double>
        get() = TODO("not implemented")

    override fun registerModules(container: ModuleContainer<Double, Outputs.Single<Double>, Targets.Single<Double>>) {
        TODO("not implemented")
    }

    override fun registerModule(type: RegisteredModuleType, builder: (EnvironmentFacade<Double, Outputs.Single<Double>, Targets.Single<Double>>) -> Module) {
        TODO("not implemented")
    }

    override fun copy(): MockEnvironment {
        TODO("not implemented")
    }
}

class MockEnvironmentMultipleOutputs : EnvironmentFacade<Double, Outputs.Multiple<Double>, Targets.Multiple<Double>> {
    override val randomState: Random
        get() = TODO("not implemented")
    override val fitnessFunctionProvider: FitnessFunctionProvider<Double, Outputs.Multiple<Double>, Targets.Multiple<Double>>
        get() = TODO("not implemented")
    override val configuration: Configuration
        get() = TODO("not implemented")
    override val constants: List<Double>
        get() = TODO("not implemented")
    override val operations: List<Operation<Double>>
        get() = TODO("not implemented")
    override val resultAggregator: ResultAggregator<Double>
        get() = TODO("not implemented")
    override val moduleFactory: ModuleFactory<Double, Outputs.Multiple<Double>, Targets.Multiple<Double>>
        get() = TODO("not implemented")
    override val registerSet: RegisterSet<Double>
        get() = TODO("not implemented")

    override fun registerModules(container: ModuleContainer<Double, Outputs.Multiple<Double>, Targets.Multiple<Double>>) {
        TODO("not implemented")
    }

    override fun registerModule(type: RegisteredModuleType, builder: (EnvironmentFacade<Double, Outputs.Multiple<Double>, Targets.Multiple<Double>>) -> Module) {
        TODO("not implemented")
    }

    override fun copy(): MockEnvironmentMultipleOutputs {
        TODO("not implemented")
    }
}