package nz.co.jedsimson.lgp.test.program

import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.ProgramGenerator
import nz.co.jedsimson.lgp.core.program.ProgramTranslator
import nz.co.jedsimson.lgp.test.mocks.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

// As program generation relies on instruction generation, these tests cover both.
object ProgramAndInstructionGeneratorFeature : Spek({

    Feature("Program and instruction generation") {
        Scenario("Generating a sequence of single-output programs") {
            var programGenerator: ProgramGenerator<Double, Outputs.Single<Double>>? = null
            var programs: Sequence<Program<Double, Outputs.Single<Double>>>? = null
            var programList: List<Program<Double, Outputs.Single<Double>>>? = null

            Given("A program generator") {
                programGenerator = MockSingleOutputProgramGenerator(MockEnvironment())
            }

            When("A sequence of 2 programs is generated") {
                programs = programGenerator!!.next().take(2)
            }

            Then("The sequence contains 2 programs") {
                assert(programs != null) { "Program sequence was null" }

                programList = programs!!.toList()

                assert(programList!!.size == 2) { "Unexpected number of programs" }
            }

            And("Each program in the sequence is a unique instance") {
                assert(programList!![0] != programList!![1]) { "Programs are not unique instances" }
            }

            And("The programs can be run") {
                for (program in programList!!) {
                    program.execute()

                    val output = program.output()
                }
            }
        }

        Scenario("Generating a sequence of multiple-output programs") {
            var programGenerator: ProgramGenerator<Double, Outputs.Multiple<Double>>? = null
            var programs: Sequence<Program<Double, Outputs.Multiple<Double>>>? = null
            var programList: List<Program<Double, Outputs.Multiple<Double>>>? = null

            Given("A program generator") {
                programGenerator = MockMultipleOutputProgramGenerator(MockEnvironmentMultipleOutputs())
            }

            When("A sequence of 2 programs is generated") {
                programs = programGenerator!!.next().take(2)
            }

            Then("The sequence contains 2 programs") {
                assert(programs != null) { "Program sequence was null" }

                programList = programs!!.toList()

                assert(programList!!.size == 2) { "Unexpected number of programs" }
            }

            And("Each program in the sequence is a unique instance") {
                assert(programList!![0] != programList!![1]) { "Programs are not unique instances" }
            }

            And("The programs can be run") {
                for (program in programList!!) {
                    program.execute()

                    val outputs = program.output()
                }
            }
        }

        Scenario("Generated programs can be translated") {
            var programGenerator: ProgramGenerator<Double, Outputs.Single<Double>>? = null
            var programTranslator: ProgramTranslator<Double, Outputs.Single<Double>>? = null
            var program: Program<Double, Outputs.Single<Double>>? = null
            var translatedProgram: String? = null

            Given("A program generator") {
                programGenerator = MockSingleOutputProgramGenerator(MockEnvironment())
            }

            And("A program translator") {
                programTranslator = MockSingleOutputProgramTranslator()
            }

            And("A program is generated") {
                program = programGenerator!!.next().first()
            }

            When("The program is translated") {
                translatedProgram = programTranslator!!.translate(program!!)
            }

            Then("The program is translated correctly") {
                assert(translatedProgram != null) { "Translated program was null" }
                assert(translatedProgram == "MockSingleOutputProgram") { "Translated program was not correct" }
            }
        }
    }
})