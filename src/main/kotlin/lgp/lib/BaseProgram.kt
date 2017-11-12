package lgp.lib

import lgp.core.evolution.instructions.BaseArity
import lgp.core.evolution.instructions.BranchOperation
import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.instructions.RegisterIndex
import lgp.core.evolution.population.Program
import lgp.core.evolution.population.ProgramTranslator
import lgp.core.evolution.registers.RegisterSet
import lgp.core.evolution.registers.RegisterType
import lgp.core.evolution.registers.copy
import lgp.core.modules.ModuleInformation

/**
 * A built-in offering of the ``Program`` interface.
 *
 * Instructions of this program are executed in sequence and can be gathered
 * from a single output register.
 */
class BaseProgram<T>(
        instructions: List<Instruction<T>>,
        registerSet: RegisterSet<T>,
        outputRegisterIndex: RegisterIndex,
        val sentinelTrueValue: T
) : Program<T>(
        instructions.toMutableList(),
        registerSet,
        outputRegisterIndex = outputRegisterIndex
) {

    override fun execute() {
        var branchResult = true

        for (instruction in this.effectiveInstructions) {
            // Need to take note of the instruction result, as we should skip the
            // next instruction if the previous was a branch instruction.
            when {
                branchResult -> {
                    instruction.execute(this.registers)

                    val output = this.registers.read(instruction.destination)

                    branchResult = ((instruction.operation !is BranchOperation<T>) ||
                                    (instruction.operation is BranchOperation<T>
                                         && output == this.sentinelTrueValue))
                }
                else -> {
                    branchResult = (instruction.operation !is BranchOperation<T>)
                }
            }
        }
    }

    override fun copy(): BaseProgram<T> {
        val copy =  BaseProgram(
                instructions = this.instructions.map(Instruction<T>::copy),
                registerSet = this.registers.copy(),
                outputRegisterIndex = this.outputRegisterIndex,
                sentinelTrueValue = this.sentinelTrueValue
        )

        // Make sure to copy fitness information over
        copy.fitness = this.fitness

        return copy
    }

    override fun findEffectiveProgram() {
        val effectiveRegisters = mutableSetOf(this.outputRegisterIndex)
        val effectiveInstructions = mutableListOf<Instruction<T>>()

        for ((i, instruction) in instructions.reversed().withIndex()) {
            val instr = instruction

            if (instr.operation is BranchOperation<T>) {
                if (instr in effectiveInstructions) {
                    instr.operands.filter { operand ->
                        operand !in effectiveRegisters &&
                        this.registers.registerType(operand) != RegisterType.Constant
                    }
                    .forEach { operand -> effectiveRegisters.add(operand) }
                }

                continue
            }

            if (instr.destination in effectiveRegisters) {
                effectiveInstructions.add(0, instr)

                var j = i - 1
                var branchesMarked = false

                while (j >= 0 && (this.instructions[j].operation is BranchOperation<T>)) {
                    effectiveInstructions.add(0, this.instructions[j])
                    branchesMarked = true
                    j--
                }

                if (!branchesMarked) {
                    effectiveRegisters.remove(instr.destination)
                }

                for (operand in instr.operands) {
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
class BaseProgramSimplifier<T> {

    /**
     * Simplifies [program] and gives it as a string output.
     */
    fun simplify(program: BaseProgram<T>): String {
        val sb = StringBuilder()

        program.effectiveInstructions.map { instruction ->
            if (instruction.operation is BranchOperation<T>) {
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
            }

            if (instruction.operation is BranchOperation<T>)
                sb.append(")")

            sb.append("\n")
        }

        return sb.toString()
    }

    fun simplifyOperand(program: BaseProgram<T>, register: RegisterIndex): String {

        return when (program.registers.registerType(register)) {
            RegisterType.Input -> {
                // Simplify feature registers to f_i.
                "f$register"
            }
            RegisterType.Constant -> {
                // Simplify constant
                program.registers.read(register).toString()
            }
            else -> "r[${register.toString()}]"
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
class BaseProgramTranslator<T>(val includeMainFunction: Boolean) : ProgramTranslator<T>() {
    override val information = ModuleInformation(
        description = "A Program Translator that can translate BaseProgram instances to their equivalent" +
                      " representation in the C programming language."
    )

    override fun translate(program: Program<T>): String {
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
            val contents = program.registers.read(reg)

            calculationRegisters += "$contents, // calculation\n"
        }

        var constantRegisters = ""

        program.registers.constantRegisters.map { reg ->
            val contents = program.registers.read(reg)

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

    printf("%f\n", r[${program.outputRegisterIndex}]);

    return 0;
}
                """.trimIndent())

            }
        }

        return sb.toString()
    }

}