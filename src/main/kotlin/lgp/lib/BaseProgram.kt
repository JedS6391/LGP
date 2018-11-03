package lgp.lib

import lgp.core.evolution.fitness.Output
import lgp.core.evolution.fitness.Outputs
import lgp.core.program.instructions.BaseArity
import lgp.core.program.instructions.BranchOperation
import lgp.core.program.instructions.Instruction
import lgp.core.program.instructions.RegisterIndex
import lgp.core.program.Program
import lgp.core.program.ProgramTranslator
import lgp.core.program.registers.RegisterSet
import lgp.core.program.registers.RegisterType
import lgp.core.modules.ModuleInformation

object BaseProgramOutputResolvers {
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

/**
 * A built-in offering of the ``Program`` interface.
 *
 * Instructions of this program are executed in sequence and can be gathered
 * from a single output register.
 */
class BaseProgram<TProgram, TOutput : Output<TProgram>>(
        instructions: List<Instruction<TProgram>>,
        registerSet: RegisterSet<TProgram>,
        outputRegisterIndices: List<RegisterIndex>,
        val sentinelTrueValue: TProgram,
        val outputResolver: (BaseProgram<TProgram, TOutput>) -> TOutput
) : Program<TProgram, TOutput>(
    instructions.toMutableList(),
    registerSet,
    outputRegisterIndices
) {

    override fun output(): TOutput {
        return this.outputResolver(this)
    }

    override fun execute() {
        var branchResult = true

        for (instruction in this.effectiveInstructions) {
            // Need to take note of the instruction result, as we should skip the
            // next instruction if the previous was a branch instruction.
            branchResult = when {
                branchResult -> {
                    instruction.execute(this.registers)

                    val output = this.registers[instruction.destination]

                    ((instruction.operation !is BranchOperation<TProgram>) ||
                            (instruction.operation is BranchOperation<TProgram>
                                    && output == this.sentinelTrueValue))
                }
                else -> {
                    (instruction.operation !is BranchOperation<TProgram>)
                }
            }
        }
    }

    override fun copy(): BaseProgram<TProgram, TOutput> {
        val copy = BaseProgram(
            instructions = this.instructions.map(Instruction<TProgram>::copy),
            registerSet = this.registers.copy(),
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
                sb.append(instruction.toString() + ";")
            } else {
                sb.append("// " + instruction.toString() + ";")
            }

            sb.append('\n')
        }

        return sb.toString()
    }

    override val information = ModuleInformation(
        description = "A simple program that executes instructions sequentially."
    )
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
            if (instruction.operation.arity === BaseArity.Unary) {
                sb.append(instruction.operation.representation)
                sb.append("(")
                sb.append(this.simplifyOperand(program, instruction.operands[0]))
                sb.append(")")
            } else if (instruction.operation.arity === BaseArity.Binary) {
                sb.append(this.simplifyOperand(program, instruction.operands[0]))
                sb.append(instruction.operation.representation)
                sb.append(this.simplifyOperand(program, instruction.operands[1]))
            } else {
                sb.append(instruction.operation.representation + "(" + this.simplifyOperand(program, instruction.operands[0]))
                for (operand in instruction.operands.drop(1)) {
                    sb.append("," + this.simplifyOperand(program, operand))
                }
                sb.append(")")
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