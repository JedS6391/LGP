package nz.co.jedsimson.lgp.test.evolution

import com.nhaarman.mockitokotlin2.*
import nz.co.jedsimson.lgp.core.environment.EnvironmentDefinition
import nz.co.jedsimson.lgp.core.environment.config.Configuration
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.EffectiveCalculationRegisterResolver
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro.MacroMutationOperator
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro.MacroMutationStrategies
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.macro.MacroMutationStrategyFactory
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategy
import nz.co.jedsimson.lgp.core.evolution.operators.mutation.strategy.MutationStrategyFactory
import nz.co.jedsimson.lgp.core.modules.CoreModuleType
import nz.co.jedsimson.lgp.core.modules.ModuleFactory
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import nz.co.jedsimson.lgp.core.program.instructions.InstructionGenerator
import nz.co.jedsimson.lgp.core.program.instructions.RegisterIndex
import nz.co.jedsimson.lgp.test.mocks.MockEnvironment
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import java.lang.IllegalStateException
import kotlin.random.Random

object MutationOperatorFeature : Spek({

    Feature("Macro mutations") {

        // Initialisation
        Scenario("Macro mutation operator with invalid insertion and deletion rate") {
            var environment: EnvironmentDefinition<Double, Outputs.Single<Double>>? = null
            var macroMutationOperator: MacroMutationOperator<Double, Outputs.Single<Double>>? = null
            var exception: Exception? = null

            When("A macro mutation operator is initialised with an insertion and deletion rate that is less than 1.0") {
                environment = MockEnvironment(
                        mockConfiguration = Configuration(),
                        mockRandomState = Random.Default
                )

                try {
                    macroMutationOperator = MacroMutationOperator(environment!!, 0.5, 0.4)
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The macro mutation operator is not created") {
                assert(macroMutationOperator == null) { "Macro mutation operator was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalStateException) { "Exception was not the correct type" }
            }

            When("A macro mutation operator is initialised with an insertion and deletion rate that is greater than 1.0") {
                environment = MockEnvironment(
                        mockConfiguration = Configuration(),
                        mockRandomState = Random.Default
                )

                try {
                    macroMutationOperator = MacroMutationOperator(environment!!, 0.5, 0.6)
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The macro mutation operator is not created") {
                assert(macroMutationOperator == null) { "Macro mutation operator was not null" }
                assert(exception != null) { "Exception was null" }
                assert(exception is IllegalStateException) { "Exception was not the correct type" }
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
})
