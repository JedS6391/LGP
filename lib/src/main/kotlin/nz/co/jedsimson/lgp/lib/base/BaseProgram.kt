package nz.co.jedsimson.lgp.lib.base

import nz.co.jedsimson.lgp.core.program.instructions.BaseArity
import nz.co.jedsimson.lgp.core.program.instructions.Instruction
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.ProgramTranslator
import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import nz.co.jedsimson.lgp.core.program.registers.RegisterType
import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Outputs
import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex
import nz.co.jedsimson.lgp.lib.operations.BranchOperation

/**
 * A collection of built-in functions that can be used to resolve the output of a [BaseProgram] instance.
 */
object BaseProgramOutputResolvers {

    /**
     * Resolves a single output value from a [BaseProgram].
     *
     * The resolver will use the first register index defined in [Program.outputRegisterIndices].
     */
    fun <TProgram> singleOutput(): (BaseProgram<TProgram, Outputs.Single<TProgram>>) -> Outputs.Single<TProgram> {
        return { program ->
            // We always take the first register. If multiple are given, they are simply ignored
            // because we can only return one value as output.
            val output = program.outputRegisterIndices.first()

            Outputs.Single(
                program.registers[output]
            )
        }
    }

    /**
     * Resolves a collection of output values from a [BaseProgram].
     *
     * The resolver will use each register index defined in [Program.outputRegisterIndices].
     */
    fun <TProgram> multipleOutput(): (BaseProgram<TProgram, Outputs.Multiple<TProgram>>) -> Outputs.Multiple<TProgram> {
        return { program ->
            Outputs.Multiple(
                program.outputRegisterIndices.map { output ->
                    program.registers[output]
                }
            )
        }
    }
}

internal enum class BranchResult {
    NotTaken,
    Taken
}

/**
 * A built-in offering of the [Program] abstraction.
 *
 * Instructions of this program are executed in sequence and can be gathered
 * from a single output register.
 *
 * @property sentinelTrueValue A value that should be considered as boolean "true".
 * @property outputResolver A function that can be used to resolve the programs register contents to an [Output].
 */
class BaseProgram<TProgram, TOutput : Output<TProgram>>(
    override var instructions: MutableList<Instruction<TProgram>>,
    override val registers: RegisterSet<TProgram>,
    override val outputRegisterIndices: List<RegisterIndex>,
    val sentinelTrueValue: TProgram,
    val outputResolver: (BaseProgram<TProgram, TOutput>) -> TOutput
) : Program<TProgram, TOutput>() {

    override fun output(): TOutput {
        return this.outputResolver(this)
    }

    override fun execute() {
        // We consider the last branch as taken when beginning execution.
        // This effectively means that the `main` function is considered as a single branch
        // which is always taken.
        var branchResult = BranchResult.Taken

        for (instruction in this.effectiveInstructions) {
            branchResult = this.determineNextBranchResult(instruction, branchResult)
        }
    }

    override fun copy(): BaseProgram<TProgram, TOutput> {
        val copy = BaseProgram(
            instructions = this.instructions.map(Instruction<TProgram>::copy).toMutableList(),
            registers = this.registers.copy(),
            outputRegisterIndices = this.outputRegisterIndices,
            sentinelTrueValue = this.sentinelTrueValue,
            outputResolver = this.outputResolver
        )

        // Make sure to copy fitness information over
        copy.fitness = this.fitness

        return copy
    }

    override fun findEffectiveProgram() {
        val effectiveRegisters = this.outputRegisterIndices.toMutableSet()
        val effectiveInstructions = mutableListOf<Instruction<TProgram>>()

        for ((i, instruction) in instructions.reversed().withIndex()) {
            if (instruction.operation is BranchOperation<TProgram>) {
                if (instruction in effectiveInstructions) {
                    instruction.operands.filter { operand ->
                        operand !in effectiveRegisters &&
                        this.registers.registerType(operand) != RegisterType.Constant
                    }
                    .forEach { operand -> effectiveRegisters.add(operand) }
                }

                continue
            }

            if (instruction.destination in effectiveRegisters) {
                effectiveInstructions.add(0, instruction)

                var j = i - 1
                var branchesMarked = false

                while (j >= 0 && (this.instructions[j].operation is BranchOperation<TProgram>)) {
                    effectiveInstructions.add(0, this.instructions[j])
                    branchesMarked = true
                    j--
                }

                if (!branchesMarked) {
                    effectiveRegisters.remove(instruction.destination)
                }

                for (operand in instruction.operands) {
                    val isConstant = this.registers.registerType(operand) == RegisterType.Constant

                    if (operand !in effectiveRegisters && !isConstant) {
                        effectiveRegisters.add(operand)
                    }
                }
            }
        }

        this.effectiveInstructions = effectiveInstructions
    }

    override fun toString(): String {
        val sb = StringBuilder()

        this.instructions.map { instruction ->
            if (instruction in effectiveInstructions) {
                sb.append("$instruction;")
            } else {
                sb.append("// $instruction;")
            }

            sb.append('\n')
        }

        return sb.toString()
    }

    override val information = ModuleInformation(
        description = "A simple program that executes instructions sequentially."
    )

    private fun determineNextBranchResult(
        currentInstruction: Instruction<TProgram>,
        lastBranchResult: BranchResult
    ): BranchResult {
        return when (lastBranchResult) {
            BranchResult.Taken -> {
                currentInstruction.execute(this.registers)

                val output = this.registers[currentInstruction.destination]

                // We consider the next branch as needing to be taken when:
                //   - The current instruction IS NOT a branching operation (i.e. we are still within the context
                //     of another branch or what equates to the programs `main` function)
                //   - The current instruction IS a branching operation and the execution result represents *true*
                val shouldTakeBranch =
                    currentInstruction.operation !is BranchOperation<TProgram> ||
                            (currentInstruction.operation is BranchOperation<TProgram> && output == this.sentinelTrueValue)

                if (shouldTakeBranch) BranchResult.Taken else BranchResult.NotTaken
            }
            else -> {
                // The last branch was not taken, so we don't execute, we just need to determine
                // whether to consider the next branch as needing to be taken (with similar logic
                // to the branching flow).
                if (currentInstruction.operation !is BranchOperation<TProgram>) {
                    BranchResult.Taken
                } else {
                    BranchResult.NotTaken
                }
            }
        }
    }
}

/**
 * Utility class that can be used to create a simplified representation of a ``BaseProgram``.
 */
class BaseProgramSimplifier<TProgram, TOutput : Output<TProgram>> {

    /**
     * Simplifies [program] and gives it as a string output.
     */
    fun simplify(program: BaseProgram<TProgram, TOutput>): String {
        val sb = StringBuilder()

        program.effectiveInstructions.map { instruction ->
            if (instruction.operation is BranchOperation<TProgram>) {
                sb.append("if (")
            } else {
                sb.append("r[")
                sb.append(instruction.destination)
                sb.append("] = ")
            }

            // TODO: Sanity check length of registers
            when {
                instruction.operation.arity === BaseArity.Unary -> {
                    sb.append(instruction.operation.representation)
                    sb.append("(")
                    sb.append(this.simplifyOperand(program, instruction.operands[0]))
                    sb.append(")")
                }
                instruction.operation.arity === BaseArity.Binary -> {
                    sb.append(this.simplifyOperand(program, instruction.operands[0]))
                    sb.append(instruction.operation.representation)
                    sb.append(this.simplifyOperand(program, instruction.operands[1]))
                }
                // TODO: Handle more arity if defined in BaseArity.
            }

            if (instruction.operation is BranchOperation<TProgram>)
                sb.append(")")

            sb.append("\n")
        }

        return sb.toString()
    }

    private fun simplifyOperand(program: BaseProgram<TProgram, TOutput>, register: RegisterIndex): String {

        return when (program.registers.registerType(register)) {
            RegisterType.Input -> {
                // Simplify feature registers to f_i.
                "f$register"
            }
            RegisterType.Constant -> {
                // Simplify constant
                program.registers[register].toString()
            }
            else -> "r[$register]"
        }
    }
}

/**
 * A [ProgramTranslator] implementation that converts [BaseProgram] instances to a C-based representation.
 *
 * There are two modes that can be used:
 *
 *     1. `includeMainFunction == true` will include a main function in the export which can
 *        be used to execute the model from the command-line.
 *     2. `includeMainFunction == false` will NOT include a main function, and is more suited for
 *        contexts where the model will be integrated into existing code bases.
 */
class BaseProgramTranslator<TProgram, TOutput : Output<TProgram>>(
    private val includeMainFunction: Boolean
) : ProgramTranslator<TProgram, TOutput>() {

    override val information = ModuleInformation(
        description = "A Program Translator that can translate BaseProgram instances to their equivalent" +
                " representation in the C programming language."
    )

    override fun translate(program: Program<TProgram, TOutput>): String {
        val sb = StringBuilder()

        // Make sure that the registers are set to their initial values.
        program.registers.reset()

        val numInputs = program.registers.inputRegisters.count()
        val numRegisters = program.registers.count
        val programString = program.toString()

        // First, we construct any placeholder information we might need.
        var inputPlaceholders = ""

        for (i in 0 until numInputs) {
            inputPlaceholders += "0.0, // input\n"
        }

        var calculationRegisters = ""

        program.registers.calculationRegisters.map { reg ->
            val contents = program.registers[reg]

            calculationRegisters += "$contents, // calculation\n"
        }

        var constantRegisters = ""

        program.registers.constantRegisters.map { reg ->
            val contents = program.registers[reg]

            constantRegisters += "$contents, // constant\n"
        }

        with (sb) {
            if (includeMainFunction) {
                // Includes and globals needed for the inclusion of a main function
                append("#include <stdio.h>\n")
                append("#include <stdlib.h>\n")
                append("\n")
                append("static int NUM_INPUTS = $numInputs;\n")
                append("\n")
            }

            // The model -- the exact representation is defined by the program instance.
            // Generally, we expect `BaseProgram` instances which export C-based instruction representations.
            append("void gp(double r[$numRegisters]) {\n")
            append("\n")
            append(programString.prependIndent("    "))
            append("\n")
            append("}\n")

            if (includeMainFunction) {
                // Add a main function that parses command-line inputs so that the model can
                // be executed from the command-line.
                append("\n")

                var outputRegisters = ""

                program.outputRegisterIndices.map { register ->
                    outputRegisters += "printf(\"%f\\n\", r[$register]);\n"
                }

                append("""
int main(int argc, char *argv[]) {
    if (argc != NUM_INPUTS + 1) {
        printf("Please specify %d input value(s)...\n", NUM_INPUTS);
        exit(1);
    }
    double r[$numRegisters] = {
${ inputPlaceholders.trim().prependIndent("        ") }
${ calculationRegisters.trim().prependIndent("        ") }
${ constantRegisters.trim().prependIndent("        ") }
    };
    if (NUM_INPUTS == 1) {
        double input = strtod(argv[1], NULL);
        r[0] = input;
    } else if (NUM_INPUTS > 1) {
        for (int i = 0; i < NUM_INPUTS; i++) {
            r[i] = strtod(argv[i + 1], NULL);
        }
    } else {
      printf("Unexpected number of inputs...\n");
      exit(1);
    }
    gp(r);
${ outputRegisters.trim().prependIndent("    ")}
    return 0;
}
                """.trimIndent())
            }
        }

        return sb.toString()
    }
}