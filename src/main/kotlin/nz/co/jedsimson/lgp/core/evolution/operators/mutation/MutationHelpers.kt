package nz.co.jedsimson.lgp.core.evolution.operators.mutation

import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.instructions.RegisterIndex
import nz.co.jedsimson.lgp.core.program.registers.RegisterType

/**
 * Finds the effective calculation registers in a [Program], up until the given stop point.
 *
 * @param stopPoint The point to stop the search at.
 */
internal fun <TProgram, TOutput : Output<TProgram>> Program<TProgram, TOutput>.findEffectiveCalculationRegisters(
    stopPoint: Int
): List<RegisterIndex> {
    val effectiveRegisters = this.outputRegisterIndices.toMutableList()
    // Only instructions up until to the stop point should be searched.
    val instructions = this.instructions.reversed().filterIndexed { idx, _ -> idx < stopPoint }

    instructions.forEach { instruction ->
        if (instruction.destination in effectiveRegisters) {
            effectiveRegisters.remove(instruction.destination)

            instruction.operands.forEach { operand ->
                val type = this.registers.registerType(operand)
                val isCalculation = type == RegisterType.Calculation || type == RegisterType.Input

                if (operand !in effectiveRegisters && isCalculation) {
                    effectiveRegisters.add(operand)
                }
            }
        }
    }

    return effectiveRegisters
}
