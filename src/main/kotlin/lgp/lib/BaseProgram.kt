package lgp.lib

import lgp.core.evolution.instructions.BaseArity
import lgp.core.evolution.instructions.BranchOperation
import lgp.core.evolution.instructions.Instruction
import lgp.core.evolution.instructions.RegisterIndex
import lgp.core.evolution.population.Program
import lgp.core.evolution.registers.RegisterSet
import lgp.core.evolution.registers.RegisterType
import lgp.core.evolution.registers.copy
import lgp.core.modules.ModuleInformation

/**
 * @suppress
 */
class BaseProgram<T>(instructions: List<Instruction<T>>, registerSet: RegisterSet<T>, val sentinelTrueValue: T)
    // TODO: Configurable output register
    // By Default we are choosing the first calculation register.
    : Program<T>(instructions.toMutableList(), registerSet, outputRegisterIndex = registerSet.calculationRegisters.start) {

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
            sb.append(instruction.toString())
            sb.append('\n')
        }

        return sb.toString()
    }

    override val information = ModuleInformation(
        description = "A simple program that executes instructions sequentially."
    )
}

class BaseProgramSimplifier<T> {

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