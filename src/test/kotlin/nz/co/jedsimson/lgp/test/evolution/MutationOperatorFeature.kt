package nz.co.jedsimson.lgp.test.evolution

import com.nhaarman.mockitokotlin2.*
import nz.co.jedsimson.lgp.core.environment.EnvironmentDefinition
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.EffectiveCalculationRegisterResolver
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.EffectiveCalculationRegisterResolvers
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro.MacroMutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro.MacroMutationStrategies
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro.MacroMutationStrategyFactory
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.ConstantMutationFunctions
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.MicroMutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.MicroMutationStrategies
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.micro.MicroMutationStrategyFactory
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategy
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategyFactory
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.modules.ModuleFactory
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import nz.co.jedsimson.lgp.core.program.instructions.InstructionGenerator
import nz.co.jedsimson.lgp.core.program.instructions.RegisterIndex
import nz.co.jedsimson.lgp.core.program.registers.RandomRegisterGenerator
import nz.co.jedsimson.lgp.core.program.registers.Register
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import nz.co.jedsimson.lgp.core.program.registers.RegisterType
import nz.co.jedsimson.lgp.test.mocks.MockEnvironment
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import kotlin.random.Random

object MutationOperatorFeature : Spek({

    Feature("Macro mutations") {

        // Initialisation
        Scenario("Macro mutation operator with invalid insertion and deletion rate") {
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockConfiguration = Configuration().apply {
                minimumProgramLength = 1
                maximumProgramLength = 10
            }
            var macroMutationOperator: MacroMutationOperator<Double, Outputs.Single<Double>>? = null
            var exception: Exception? = null

            When("A macro mutation operator is initialised with an insertion and deletion rate that is less than 1.0") {
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)

                try {
                    macroMutationOperator = MacroMutationOperator(mockEnvironment, 0.5, 0.4)
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The macro mutation operator is not created") {
                assert(macroMutationOperator == null) { "Macro mutation operator was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type ${exception!!::class.java.simpleName}" }
            }

            When("A macro mutation operator is initialised with an insertion and deletion rate that is greater than 1.0") {
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)
                whenever(mockEnvironment.configuration).thenReturn(mockConfiguration)

                try {
                    macroMutationOperator = MacroMutationOperator(mockEnvironment, 0.5, 0.6)
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The macro mutation operator is not created") {
                assert(macroMutationOperator == null) { "Macro mutation operator was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException) { "Exception was not the correct type" }
            }

        }

        // High level operator execution (i.e. does it delegate to the strategy correctly)
        Scenario("Macro mutation operator delegates to a mutation strategy factory") {
            var macroMutationOperator: MacroMutationOperator<Double, Outputs.Single<Double>>? = null
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockMutationStrategyFactory = mock<MutationStrategyFactory<Double, Outputs.Single<Double>>>()
            val mockMutationStrategy = mock<MutationStrategy<Double, Outputs.Single<Double>>>()
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()

            Given("A macro mutation operator") {
                whenever(mockMutationStrategyFactory.getStrategyForIndividual(mockProgram)).thenReturn(mockMutationStrategy)

                macroMutationOperator = MacroMutationOperator(mockEnvironment, 0.5, 0.5, mockMutationStrategyFactory)
            }

            When("The macro mutation operator is applied to a program") {
                macroMutationOperator!!.mutate(mockProgram)
            }

            Then("The mutation strategy is retrieved and executed correctly") {
                verify(mockProgram, times(1)).findEffectiveProgram()
                verify(mockMutationStrategyFactory, times(1)).getStrategyForIndividual(mockProgram)
                verify(mockMutationStrategy, times(1)).mutate(mockProgram)
            }
        }

        // Strategy factory (i.e. should return the appropriate strategy)
        Scenario("Macro mutation strategy factory provides the insertion strategy when program length is less than the maximum and the mutation type is insertion") {
            var macroMutationStrategyFactory: MacroMutationStrategyFactory<Double, Outputs.Single<Double>>? = null
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockInstructions = (0 until 5).map { mock<Instruction<Double>>() }.toMutableList()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockModuleFactory = mock<ModuleFactory<Double, Outputs.Single<Double>>>()
            val insertionRate = 0.5
            val deletionRate = 0.5

            Given("A macro mutation strategy factory") {
                val configuration = Configuration().apply {
                    minimumProgramLength = 1
                    maximumProgramLength = 10
                }

                whenever(mockRandom.nextDouble()).thenReturn(insertionRate - 0.1)
                whenever(mockModuleFactory.resolveModuleFromType(CoreModuleType.InstructionGenerator)).thenReturn(mock<InstructionGenerator<Double, Outputs.Single<Double>>>())
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.moduleFactory).thenReturn(mockModuleFactory)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)

                macroMutationStrategyFactory = MacroMutationStrategyFactory(mockEnvironment, insertionRate, deletionRate)
            }

            When("The mutation strategy is requested") {
                mutationStrategy = macroMutationStrategyFactory!!.getStrategyForIndividual(mockProgram)
            }

            Then("The mutation strategy is the macro mutation insertion strategy") {
                assert(mutationStrategy != null) { "Mutation strategy was null" }
                assert(mutationStrategy is MacroMutationStrategies.MacroMutationInsertionStrategy<Double, Outputs.Single<Double>>) { "Mutation strategy was not the correct type" }
            }
        }

        Scenario("Macro mutation strategy factory provides the insertion strategy when program length is equal to the minimum and the mutation type is deletion") {
            var macroMutationStrategyFactory: MacroMutationStrategyFactory<Double, Outputs.Single<Double>>? = null
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockInstructions = (0 until 1).map { mock<Instruction<Double>>() }.toMutableList()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockModuleFactory = mock<ModuleFactory<Double, Outputs.Single<Double>>>()
            val insertionRate = 0.5
            val deletionRate = 0.5

            Given("A macro mutation strategy factory") {
                val configuration = Configuration().apply {
                    minimumProgramLength = 1
                    maximumProgramLength = 10
                }

                whenever(mockRandom.nextDouble()).thenReturn(insertionRate + 0.1)
                whenever(mockModuleFactory.resolveModuleFromType(CoreModuleType.InstructionGenerator)).thenReturn(mock<InstructionGenerator<Double, Outputs.Single<Double>>>())
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.moduleFactory).thenReturn(mockModuleFactory)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)

                macroMutationStrategyFactory = MacroMutationStrategyFactory(mockEnvironment, insertionRate, deletionRate)
            }

            When("The mutation strategy is requested") {
                mutationStrategy = macroMutationStrategyFactory!!.getStrategyForIndividual(mockProgram)
            }

            Then("The mutation strategy is the macro mutation insertion strategy") {
                assert(mutationStrategy != null) { "Mutation strategy was null" }
                assert(mutationStrategy is MacroMutationStrategies.MacroMutationInsertionStrategy<Double, Outputs.Single<Double>>) { "Mutation strategy was not the correct type" }
            }
        }

        Scenario("Macro mutation strategy factory provides the insertion strategy when program length is greater than the minimum and the mutation type is deletion") {
            var macroMutationStrategyFactory: MacroMutationStrategyFactory<Double, Outputs.Single<Double>>? = null
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockInstructions = (0 until 5).map { mock<Instruction<Double>>() }.toMutableList()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockModuleFactory = mock<ModuleFactory<Double, Outputs.Single<Double>>>()
            val insertionRate = 0.5
            val deletionRate = 0.5

            Given("A macro mutation strategy factory") {
                val configuration = Configuration().apply {
                    minimumProgramLength = 1
                    maximumProgramLength = 10
                }

                whenever(mockRandom.nextDouble()).thenReturn(insertionRate + 0.1)
                whenever(mockModuleFactory.resolveModuleFromType(CoreModuleType.InstructionGenerator)).thenReturn(mock<InstructionGenerator<Double, Outputs.Single<Double>>>())
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.moduleFactory).thenReturn(mockModuleFactory)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)

                macroMutationStrategyFactory = MacroMutationStrategyFactory(mockEnvironment, insertionRate, deletionRate)
            }

            When("The mutation strategy is requested") {
                mutationStrategy = macroMutationStrategyFactory!!.getStrategyForIndividual(mockProgram)
            }

            Then("The mutation strategy is the macro mutation deletion strategy") {
                assert(mutationStrategy != null) { "Mutation strategy was null" }
                assert(mutationStrategy is MacroMutationStrategies.MacroMutationDeletionStrategy<Double, Outputs.Single<Double>>) { "Mutation strategy was not the correct type" }
            }
        }

        Scenario("Macro mutation strategy factory provides the insertion strategy when program length is equal to the maximum and the mutation type is insertion") {
            var macroMutationStrategyFactory: MacroMutationStrategyFactory<Double, Outputs.Single<Double>>? = null
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockInstructions = (0 until 10).map { mock<Instruction<Double>>() }.toMutableList()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockModuleFactory = mock<ModuleFactory<Double, Outputs.Single<Double>>>()
            val mockInstructionGenerator = mock<InstructionGenerator<Double, Outputs.Single<Double>>>()
            val insertionRate = 0.5
            val deletionRate = 0.5

            Given("A macro mutation strategy factory") {
                val configuration = Configuration().apply {
                    minimumProgramLength = 1
                    maximumProgramLength = 10
                }

                whenever(mockRandom.nextDouble()).thenReturn(insertionRate - 0.1)
                whenever(mockModuleFactory.resolveModuleFromType(CoreModuleType.InstructionGenerator)).thenReturn(mockInstructionGenerator)
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.moduleFactory).thenReturn(mockModuleFactory)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)

                macroMutationStrategyFactory = MacroMutationStrategyFactory(mockEnvironment, insertionRate, deletionRate)
            }

            When("The mutation strategy is requested") {
                mutationStrategy = macroMutationStrategyFactory!!.getStrategyForIndividual(mockProgram)
            }

            Then("The mutation strategy is the macro mutation deletion strategy") {
                assert(mutationStrategy != null) { "Mutation strategy was null" }
                assert(mutationStrategy is MacroMutationStrategies.MacroMutationDeletionStrategy<Double, Outputs.Single<Double>>) { "Mutation strategy was not the correct type" }
            }
        }

        // Strategy implementation - insertion
        Scenario("Insertion macro mutation strategy does not insert an instruction if there are no effective registers") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val initialProgramLength = 10
            val mutationPoint = 5
            val effectiveRegisters = listOf<RegisterIndex>()
            val mockInstructions = (0 until initialProgramLength).map { mock<Instruction<Double>>() }.toMutableList()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockModuleFactory = mock<ModuleFactory<Double, Outputs.Single<Double>>>()
            val mockInstructionGenerator = mock<InstructionGenerator<Double, Outputs.Single<Double>>>()
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null

            Given("An insertion macro mutation strategy") {
                whenever(mockRandom.nextInt(any())).thenReturn(mutationPoint)
                whenever(mockModuleFactory.resolveModuleFromType(CoreModuleType.InstructionGenerator)).thenReturn(mockInstructionGenerator)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.moduleFactory).thenReturn(mockModuleFactory)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)
                val mockEffectiveCalculationRegisterResolver = { _: Program<Double, Outputs.Single<Double>>, _: Int ->
                    effectiveRegisters
                }

                mutationStrategy = MacroMutationStrategies.MacroMutationInsertionStrategy(mockEnvironment, mockEffectiveCalculationRegisterResolver)
            }

            When("The mutation strategy is executed") {
                mutationStrategy!!.mutate(mockProgram)
            }

            Then("No mutation is performed") {
                verify(mockInstructionGenerator, times(0)).generateInstruction()

                assert(mockProgram.instructions.size == initialProgramLength) { "Program length was modified but should have remained the same" }
            }
        }

        Scenario("Insertion macro mutation strategy inserts a new random instruction when there is a single effective register") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val initialProgramLength = 10
            val mutationPoint = 5
            val effectiveRegisters = listOf<RegisterIndex>(0)
            val originalMockInstructions = (0 until initialProgramLength).map { mock<Instruction<Double>>() }.toMutableList()
            val newMockInstruction = mock<Instruction<Double>>()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockModuleFactory = mock<ModuleFactory<Double, Outputs.Single<Double>>>()
            val mockInstructionGenerator = mock<InstructionGenerator<Double, Outputs.Single<Double>>>()
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null

            Given("An insertion macro mutation strategy") {
                whenever(mockRandom.nextInt(any())).thenReturn(mutationPoint)
                whenever(mockModuleFactory.resolveModuleFromType(CoreModuleType.InstructionGenerator)).thenReturn(mockInstructionGenerator)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.moduleFactory).thenReturn(mockModuleFactory)
                whenever(mockInstructionGenerator.generateInstruction()).thenReturn(newMockInstruction)
                // Make a copy of the original instruction list
                whenever(mockProgram.instructions).thenReturn(originalMockInstructions.toMutableList())
                val mockEffectiveCalculationRegisterResolver = { _: Program<Double, Outputs.Single<Double>>, _: Int ->
                    effectiveRegisters
                }

                mutationStrategy = MacroMutationStrategies.MacroMutationInsertionStrategy(mockEnvironment, mockEffectiveCalculationRegisterResolver)
            }

            When("The mutation strategy is executed") {
                mutationStrategy!!.mutate(mockProgram)
            }

            Then("A new random instruction is inserted") {
                verify(mockInstructionGenerator, times(1)).generateInstruction()
                assert(mockProgram.instructions.size == initialProgramLength + 1) { "Program length was not modified but should have increased" }
                assert(mockProgram.instructions != originalMockInstructions) { "Original instructions and current instructions are the same" }
            }

            And("The new instruction was inserted at the mutation point") {
                assert(mockProgram.instructions[mutationPoint] == newMockInstruction) { "Instruction at the mutation point was not expected" }
                assert(mockProgram.instructions.filterIndexed { index, _ -> index != mutationPoint } == originalMockInstructions) { "Instruction list does not match original when excluding new instruction" }
                // Only one effective register in this scenario, so we can just compare directly
                verify(newMockInstruction).destination = effectiveRegisters.first()
            }
        }

        Scenario("Insertion macro mutation strategy inserts a new random instruction when there are multiple effective registers") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val initialProgramLength = 10
            val mutationPoint = 5
            val effectiveRegisters = listOf<RegisterIndex>(0, 1, 2)
            val expectedDestinationRegister = effectiveRegisters[1]
            val originalMockInstructions = (0 until initialProgramLength).map { mock<Instruction<Double>>() }.toMutableList()
            val newMockInstruction = mock<Instruction<Double>>()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockModuleFactory = mock<ModuleFactory<Double, Outputs.Single<Double>>>()
            val mockInstructionGenerator = mock<InstructionGenerator<Double, Outputs.Single<Double>>>()
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null

            Given("An insertion macro mutation strategy") {
                whenever(mockRandom.nextInt(any())).thenReturn(mutationPoint)
                // This will ensure that the second register is picked, as the formula is (0.5 * 10).toInt() = 1
                whenever(mockRandom.nextDouble()).thenReturn(0.5)
                whenever(mockModuleFactory.resolveModuleFromType(CoreModuleType.InstructionGenerator)).thenReturn(mockInstructionGenerator)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.moduleFactory).thenReturn(mockModuleFactory)
                whenever(mockInstructionGenerator.generateInstruction()).thenReturn(newMockInstruction)
                // Make a copy of the original instruction list
                whenever(mockProgram.instructions).thenReturn(originalMockInstructions.toMutableList())
                val mockEffectiveCalculationRegisterResolver = { _: Program<Double, Outputs.Single<Double>>, _: Int ->
                    effectiveRegisters
                }

                mutationStrategy = MacroMutationStrategies.MacroMutationInsertionStrategy(mockEnvironment, mockEffectiveCalculationRegisterResolver)
            }

            When("The mutation strategy is executed") {
                mutationStrategy!!.mutate(mockProgram)
            }

            Then("A new random instruction is inserted") {
                verify(mockInstructionGenerator, times(1)).generateInstruction()
                assert(mockProgram.instructions.size == initialProgramLength + 1) { "Program length was not modified but should have increased" }
                assert(mockProgram.instructions != originalMockInstructions) { "Original instructions and current instructions are the same" }
            }

            And("The new instruction was inserted at the mutation point") {
                assert(mockProgram.instructions[mutationPoint] == newMockInstruction) { "Instruction at the mutation point was not expected" }
                assert(mockProgram.instructions.filterIndexed { index, _ -> index != mutationPoint } == originalMockInstructions) { "Instruction list does not match original when excluding new instruction" }
                verify(newMockInstruction).destination = expectedDestinationRegister
            }
        }

        // Strategy implementation - deletion
        Scenario("Deletion macro mutation strategy does not delete any instruction if there are no effective instructions") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val initialProgramLength = 10
            val mockInstructions = (0 until initialProgramLength).map { mock<Instruction<Double>>() }.toMutableList()
            val effectiveInstructions = mutableListOf<Instruction<Double>>()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null

            Given("A deletion macro mutation strategy") {
                whenever(mockRandom.nextDouble()).thenReturn(0.5)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)
                whenever(mockProgram.effectiveInstructions).thenReturn(effectiveInstructions)

                mutationStrategy = MacroMutationStrategies.MacroMutationDeletionStrategy(mockEnvironment)
            }

            When("The mutation strategy is executed") {
                mutationStrategy!!.mutate(mockProgram)
            }

            Then("No mutation is performed") {
                assert(mockProgram.instructions.size == initialProgramLength) { "Program length was modified but should have remained the same" }
            }
        }

        Scenario("Deletion macro mutation strategy deletes a random instruction when there are effective instructions") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val initialProgramLength = 10
            val mockInstructions = (0 until initialProgramLength).map { mock<Instruction<Double>>() }.toMutableList()
            val originalMockInstructions = mockInstructions.toMutableList()
            val effectiveInstructions = mockInstructions.filterIndexed { idx, _ -> idx % 2 == 0 }.toMutableList()
            val expectedDeletedInstructionIndex = 2
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null

            Given("A deletion macro mutation strategy") {
                whenever(mockRandom.nextDouble()).thenReturn(0.5)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)
                whenever(mockProgram.effectiveInstructions).thenReturn(effectiveInstructions)

                mutationStrategy = MacroMutationStrategies.MacroMutationDeletionStrategy(mockEnvironment)
            }

            When("The mutation strategy is executed") {
                mutationStrategy!!.mutate(mockProgram)
            }

            Then("A random instruction is deleted") {
                assert(mockProgram.instructions.size == initialProgramLength - 1) { "Program length was modified but should have remained the same" }

                originalMockInstructions.remove(effectiveInstructions[expectedDeletedInstructionIndex])

                assert(originalMockInstructions == mockProgram.instructions) { "Program instructions does not match expected" }
            }
        }
    }

    Feature("Micro mutations") {

        // Initialisation
        Scenario("Micro mutation operator with invalid register mutation rate and operator mutation rate") {
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            var microMutationOperator: MicroMutationOperator<Double, Outputs.Single<Double>>? = null
            var exception: Exception? = null

            When("A micro mutation operator is initialised with an register mutation rate and operator mutation rate that is greater than 1.0") {
                whenever(mockEnvironment.randomState).thenReturn(Random.Default)

                try {
                    microMutationOperator = MicroMutationOperator(mockEnvironment, 0.6, 0.6, ConstantMutationFunctions.identity())
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The micro mutation operator is not created") {
                assert(microMutationOperator == null) { "Micro mutation operator was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalArgumentException ) { "Exception was not the correct type (${exception!!::class.java.simpleName})" }
            }
        }

        // High level operator execution (i.e. does it delegate to the strategy correctly)
        Scenario("Micro mutation operator delegates to a mutation strategy factory") {
            var microMutationOperator: MicroMutationOperator<Double, Outputs.Single<Double>>? = null
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockMutationStrategyFactory = mock<MutationStrategyFactory<Double, Outputs.Single<Double>>>()
            val mockMutationStrategy = mock<MutationStrategy<Double, Outputs.Single<Double>>>()
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()

            Given("A micro mutation operator") {
                whenever(mockMutationStrategyFactory.getStrategyForIndividual(mockProgram)).thenReturn(mockMutationStrategy)

                microMutationOperator = MicroMutationOperator(mockEnvironment, 0.5, 0.5, ConstantMutationFunctions.identity(), mockMutationStrategyFactory)
            }

            When("The micro mutation operator is applied to a program with no instructions") {
                whenever(mockProgram.effectiveInstructions).thenReturn(mutableListOf())

                microMutationOperator!!.mutate(mockProgram)
            }

            Then("The mutation strategy is not retrieved or executed") {
                verify(mockProgram, times(1)).findEffectiveProgram()
                verify(mockMutationStrategyFactory, times(0)).getStrategyForIndividual(mockProgram)
                verify(mockMutationStrategy, times(0)).mutate(mockProgram)
            }

            When("The micro mutation operator is applied to a program with at least one effective instruction") {
                whenever(mockProgram.effectiveInstructions).thenReturn(mutableListOf(mock()))

                microMutationOperator!!.mutate(mockProgram)
            }

            Then("The mutation strategy is retrieved and executed correctly") {
                // We expect 2 times here as one is from the previous invocation
                verify(mockProgram, times(2)).findEffectiveProgram()
                verify(mockMutationStrategyFactory, times(1)).getStrategyForIndividual(mockProgram)
                verify(mockMutationStrategy, times(1)).mutate(mockProgram)
            }
        }

        // Strategy factory (i.e. should return the appropriate strategy)
        Scenario("Micro mutation strategy factory provides the register mutation strategy when random number is less than the register mutation rate") {
            var microMutationStrategyFactory: MicroMutationStrategyFactory<Double, Outputs.Single<Double>>? = null
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockRegisterSet = mock<RegisterSet<Double>>()
            val registerMutationRate = 0.3
            val operationMutationRate = 0.3

            Given("A micro mutation strategy factory") {
                val configuration = Configuration().apply {
                    constantsRate = 0.5
                }

                whenever(mockRandom.nextDouble()).thenReturn(registerMutationRate - 0.1)
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockProgram.registers).thenReturn(mockRegisterSet)

                microMutationStrategyFactory = MicroMutationStrategyFactory(mockEnvironment, registerMutationRate, operationMutationRate, ConstantMutationFunctions.identity())
            }

            When("The mutation strategy is requested") {
                mutationStrategy = microMutationStrategyFactory!!.getStrategyForIndividual(mockProgram)
            }

            Then("The mutation strategy is the micro mutation register strategy") {
                assert(mutationStrategy != null) { "Mutation strategy was null" }
                assert(mutationStrategy is MicroMutationStrategies.RegisterMicroMutationStrategy<Double, Outputs.Single<Double>>) { "Mutation strategy was not the correct type" }
            }
        }

        Scenario("Micro mutation strategy factory provides the operator mutation strategy when random number is between the register mutation rate + operator mutation rate") {
            var microMutationStrategyFactory: MicroMutationStrategyFactory<Double, Outputs.Single<Double>>? = null
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockRegisterSet = mock<RegisterSet<Double>>()
            val registerMutationRate = 0.3
            val operationMutationRate = 0.3

            Given("A micro mutation strategy factory") {
                val configuration = Configuration().apply {
                    constantsRate = 0.5
                }

                whenever(mockRandom.nextDouble()).thenReturn((registerMutationRate + operationMutationRate) - 0.1)
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockProgram.registers).thenReturn(mockRegisterSet)

                microMutationStrategyFactory = MicroMutationStrategyFactory(mockEnvironment, registerMutationRate, operationMutationRate, ConstantMutationFunctions.identity())
            }

            When("The mutation strategy is requested") {
                mutationStrategy = microMutationStrategyFactory!!.getStrategyForIndividual(mockProgram)
            }

            Then("The mutation strategy is the micro mutation register strategy") {
                assert(mutationStrategy != null) { "Mutation strategy was null" }
                assert(mutationStrategy is MicroMutationStrategies.OperatorMicroMutationStrategy<Double, Outputs.Single<Double>>) { "Mutation strategy was not the correct type" }
            }
        }

        Scenario("Micro mutation strategy factory provides the constant mutation strategy when random number is greater than the register mutation rate + operator mutation rate") {
            var microMutationStrategyFactory: MicroMutationStrategyFactory<Double, Outputs.Single<Double>>? = null
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockRegisterSet = mock<RegisterSet<Double>>()
            val registerMutationRate = 0.3
            val operationMutationRate = 0.3

            Given("A micro mutation strategy factory") {
                val configuration = Configuration().apply {
                    constantsRate = 0.5
                }

                whenever(mockRandom.nextDouble()).thenReturn((registerMutationRate + operationMutationRate) + 0.1)
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockProgram.registers).thenReturn(mockRegisterSet)

                microMutationStrategyFactory = MicroMutationStrategyFactory(mockEnvironment, registerMutationRate, operationMutationRate, ConstantMutationFunctions.identity())
            }

            When("The mutation strategy is requested") {
                mutationStrategy = microMutationStrategyFactory!!.getStrategyForIndividual(mockProgram)
            }

            Then("The mutation strategy is the micro mutation constant strategy") {
                assert(mutationStrategy != null) { "Mutation strategy was null" }
                assert(mutationStrategy is MicroMutationStrategies.ConstantMicroMutationStrategy<Double, Outputs.Single<Double>>) { "Mutation strategy was not the correct type" }
            }
        }

        // Strategy implementation - register
        Scenario("Register micro mutation strategy modifies the destination register of a random instruction when there are effective registers") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockInstructions = (0 until 10).map { mock<Instruction<Double>>() }.toMutableList()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockRegisterSet = mock<RegisterSet<Double>>()
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val originalDestinationRegister = 0
            val expectedDestinationRegister = 3

            Given("A register micro mutation strategy") {
                val configuration = Configuration().apply {
                    constantsRate = 0.5
                }

                // This will cause the 2nd instruction to be chosen for mutation and the 1st register (i.e. destination) to be chosen for mutation
                whenever(mockRandom.nextDouble()).thenReturn(0.1)
                whenever(mockInstructions[1].destination).thenReturn(originalDestinationRegister)
                whenever(mockInstructions[1].operands).thenReturn(mutableListOf(1, 2))
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)
                whenever(mockProgram.effectiveInstructions).thenReturn(mockInstructions)
                val mockEffectiveCalculationRegisterResolver = { _: Program<Double, Outputs.Single<Double>>, _: Int ->
                    // This is the register that should replace the original destination register
                    listOf(expectedDestinationRegister)
                }

                mutationStrategy = MicroMutationStrategies.RegisterMicroMutationStrategy(
                    mockEnvironment,
                    // TODO: Mock the register generator
                    RandomRegisterGenerator(mockRandom, mockRegisterSet),
                    mockEffectiveCalculationRegisterResolver
                )
            }

            When("The mutation strategy is executed") {
                mutationStrategy!!.mutate(mockProgram)
            }

            Then("A mutation is performed") {
                verify(mockInstructions[1]).destination = expectedDestinationRegister
            }
        }

        Scenario("Register micro mutation strategy does not modify the destination register of a random instruction when there are no effective registers") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockInstructions = (0 until 10).map { mock<Instruction<Double>>() }.toMutableList()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockRegisterSet = mock<RegisterSet<Double>>()
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val originalDestinationRegister = 0

            Given("A register micro mutation strategy") {
                val configuration = Configuration().apply {
                    constantsRate = 0.5
                }

                // This will cause the 2nd instruction to be chosen for mutation and the 1st register (i.e. destination) to be chosen for mutation
                whenever(mockRandom.nextDouble()).thenReturn(0.1)
                whenever(mockInstructions[1].destination).thenReturn(originalDestinationRegister)
                whenever(mockInstructions[1].operands).thenReturn(mutableListOf(1, 2))
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)
                whenever(mockProgram.effectiveInstructions).thenReturn(mockInstructions)
                val mockEffectiveCalculationRegisterResolver = { _: Program<Double, Outputs.Single<Double>>, _: Int ->
                    listOf<RegisterIndex>()
                }

                mutationStrategy = MicroMutationStrategies.RegisterMicroMutationStrategy(
                    mockEnvironment,
                    // TODO: Mock the register generator
                    RandomRegisterGenerator(mockRandom, mockRegisterSet),
                    mockEffectiveCalculationRegisterResolver
                )
            }

            When("The mutation strategy is executed") {
                mutationStrategy!!.mutate(mockProgram)
            }

            Then("A mutation is performed") {
                verify(mockInstructions[1]).destination = originalDestinationRegister
            }
        }

        Scenario("Register micro mutation strategy replaces one of the operand registers of a random instruction with a constant register when less than the constant rate") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockInstructions = (0 until 10).map { mock<Instruction<Double>>() }.toMutableList()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockRegisterSet = mock<RegisterSet<Double>>()
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val originalDestinationRegister = 0

            Given("A register micro mutation strategy") {
                val configuration = Configuration().apply {
                    constantsRate = 0.5
                }

                // This will cause the 5th instruction to be chosen for mutation and the 2st register (i.e. 1st operand) to be chosen for mutation
                whenever(mockRandom.nextDouble()).thenReturn(0.4)
                whenever(mockRandom.nextInt(any())).thenReturn(3)
                whenever(mockInstructions[4].destination).thenReturn(originalDestinationRegister)
                whenever(mockInstructions[4].operands).thenReturn(mutableListOf(1, 2))
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)
                whenever(mockProgram.effectiveInstructions).thenReturn(mockInstructions)
                whenever(mockRegisterSet.count).thenReturn(4)
                whenever(mockRegisterSet.register(3)).thenReturn(Register(1.0, 3))
                whenever(mockRegisterSet.registerType(3)).thenReturn(RegisterType.Constant)
                val mockEffectiveCalculationRegisterResolver = { _: Program<Double, Outputs.Single<Double>>, _: Int ->
                    listOf<RegisterIndex>()
                }

                mutationStrategy = MicroMutationStrategies.RegisterMicroMutationStrategy(
                    mockEnvironment,
                    // TODO: Mock the register generator
                    RandomRegisterGenerator(mockRandom, mockRegisterSet),
                    mockEffectiveCalculationRegisterResolver
                )
            }

            When("The mutation strategy is executed") {
                mutationStrategy!!.mutate(mockProgram)
            }

            Then("A mutation is performed") {
                assert(mockInstructions[4].operands == listOf(3, 2)) { "Mutated instruction operands do not match expected" }
            }
        }

        Scenario("Register micro mutation strategy replaces one of the operand registers of a random instruction with an input register when greater than the constant rate") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockInstructions = (0 until 10).map { mock<Instruction<Double>>() }.toMutableList()
            val mockEnvironment = mock<EnvironmentDefinition<Double, Outputs.Single<Double>>>()
            val mockRandom = mock<Random>()
            val mockRegisterSet = mock<RegisterSet<Double>>()
            var mutationStrategy: MutationStrategy<Double, Outputs.Single<Double>>? = null
            val originalDestinationRegister = 0

            Given("A register micro mutation strategy") {
                val configuration = Configuration().apply {
                    constantsRate = 0.1
                }

                // This will cause the 5th instruction to be chosen for mutation and the 2st register (i.e. 1st operand) to be chosen for mutation
                whenever(mockRandom.nextDouble()).thenReturn(0.4)
                whenever(mockRandom.nextInt(any())).thenReturn(3)
                whenever(mockInstructions[4].destination).thenReturn(originalDestinationRegister)
                whenever(mockInstructions[4].operands).thenReturn(mutableListOf(1, 2))
                whenever(mockEnvironment.randomState).thenReturn(mockRandom)
                whenever(mockEnvironment.configuration).thenReturn(configuration)
                whenever(mockProgram.instructions).thenReturn(mockInstructions)
                whenever(mockProgram.effectiveInstructions).thenReturn(mockInstructions)
                whenever(mockRegisterSet.count).thenReturn(4)
                whenever(mockRegisterSet.register(3)).thenReturn(Register(1.0, 3))
                whenever(mockRegisterSet.registerType(3)).thenReturn(RegisterType.Input)
                val mockEffectiveCalculationRegisterResolver = { _: Program<Double, Outputs.Single<Double>>, _: Int ->
                    listOf<RegisterIndex>()
                }

                mutationStrategy = MicroMutationStrategies.RegisterMicroMutationStrategy(
                    mockEnvironment,
                    // TODO: Mock the register generator
                    RandomRegisterGenerator(mockRandom, mockRegisterSet),
                    mockEffectiveCalculationRegisterResolver
                )
            }

            When("The mutation strategy is executed") {
                mutationStrategy!!.mutate(mockProgram)
            }

            Then("A mutation is performed") {
                assert(mockInstructions[4].operands == listOf(3, 2)) { "Mutated instruction operands do not match expected" }
            }
        }

        // Strategy implementation - operator

        // Strategy implementation - constant
    }

    Feature("Effective calculation register resolution") {

        Scenario("Base resolver throws when provided an invalid stop point") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            var exception: Exception? = null

            When("The base resolver is executed with a stop point less than zero") {
                try {
                    EffectiveCalculationRegisterResolvers.baseResolver(mockProgram, -1)
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("An invalid argument exception is given") {
                assert(exception != null) { "Exception was null" }
                assert(exception!! is IllegalArgumentException) { "Exception was not the correct type" }
            }
        }

        Scenario("Base resolver only finds the output register as effective when the stop point is zero and there are no instructions") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val outputRegisters = listOf(0)
            var registerIndices: List<RegisterIndex>? = null

            When("The base resolver is executed with a stop point of zero") {
                whenever(mockProgram.outputRegisterIndices).thenReturn(outputRegisters)

                registerIndices = EffectiveCalculationRegisterResolvers.baseResolver(mockProgram, 0)
            }

            Then("Only the output register is found as an effective calculation registers") {
                assert(registerIndices != null) { "Register indices was null" }
                assert(registerIndices!!.size == 1) { "Incorrect number of register indices" }
                assert(registerIndices!![0] == outputRegisters[0]) { "Resolved register index does not match expected" }
            }
        }

        Scenario("Base resolver finds an instructions registers as effective") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockInstruction = mock<Instruction<Double>>()
            val mockRegisterSet = mock<RegisterSet<Double>>()
            val outputRegisters = listOf(0)
            var registerIndices: List<RegisterIndex>? = null

            When("The base resolver is executed with a stop point of zero") {
                whenever(mockRegisterSet.registerType(1)).thenReturn(RegisterType.Input)
                whenever(mockRegisterSet.registerType(2)).thenReturn(RegisterType.Calculation)
                whenever(mockInstruction.destination).thenReturn(0)
                whenever(mockInstruction.operands).thenReturn(mutableListOf(1, 2))
                whenever(mockProgram.instructions).thenReturn(mutableListOf(mockInstruction))
                whenever(mockProgram.outputRegisterIndices).thenReturn(outputRegisters)
                whenever(mockProgram.registers).thenReturn(mockRegisterSet)

                registerIndices = EffectiveCalculationRegisterResolvers.baseResolver(mockProgram, 0)
            }

            Then("The first instructions registers are found as effective") {
                assert(registerIndices != null) { "Register indices was null" }
                assert(registerIndices!!.size == 2) { "Incorrect number of register indices" }
                assert(registerIndices!! == listOf(1, 2)) { "Resolved register indices do not match expected" }
            }
        }

        Scenario("Base resolver finds multiple instructions registers as effective") {
            val mockProgram = mock<Program<Double, Outputs.Single<Double>>>()
            val mockInstruction1 = mock<Instruction<Double>>()
            val mockInstruction2 = mock<Instruction<Double>>()
            val mockRegisterSet = mock<RegisterSet<Double>>()
            val outputRegisters = listOf(0)
            var registerIndices: List<RegisterIndex>? = null

            When("The base resolver is executed with a stop point of zero") {
                whenever(mockRegisterSet.registerType(1)).thenReturn(RegisterType.Input)
                whenever(mockRegisterSet.registerType(2)).thenReturn(RegisterType.Calculation)
                whenever(mockRegisterSet.registerType(0)).thenReturn(RegisterType.Calculation)
                whenever(mockRegisterSet.registerType(4)).thenReturn(RegisterType.Constant)
                whenever(mockInstruction1.destination).thenReturn(0)
                whenever(mockInstruction1.operands).thenReturn(mutableListOf(1, 2))
                whenever(mockInstruction2.destination).thenReturn(1)
                whenever(mockInstruction2.operands).thenReturn(mutableListOf(0, 4))
                whenever(mockProgram.instructions).thenReturn(mutableListOf(mockInstruction2, mockInstruction1))
                whenever(mockProgram.outputRegisterIndices).thenReturn(outputRegisters)
                whenever(mockProgram.registers).thenReturn(mockRegisterSet)

                registerIndices = EffectiveCalculationRegisterResolvers.baseResolver(mockProgram, 0)
            }

            Then("All of the instructions registers are found as effective") {
                assert(registerIndices != null) { "Register indices was null" }
                assert(registerIndices!!.size == 2) { "Incorrect number of register indices" }
                assert(registerIndices!! == listOf(2, 0)) { "Resolved register indices do not match expected (${registerIndices!!})" }
            }
        }
    }
})
