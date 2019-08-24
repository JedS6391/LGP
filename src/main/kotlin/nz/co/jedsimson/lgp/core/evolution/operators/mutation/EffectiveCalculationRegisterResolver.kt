package nz.co.jedsimson.lgp.core.evolution.operators.mutation

import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex
import nz.co.jedsimson.lgp.core.program.registers.RegisterType

/**
 * A function that will search through a [Program] to determine which calculation registers are effective at the given stop point.
 */
typealias EffectiveCalculationRegisterResolver<TProgram, TOutput> = (Program<TProgram, TOutput>, Int) -> List<RegisterIndex>

/**
 * A collection of [EffectiveCalculationRegisterResolver]s.
 */
internal object EffectiveCalculationRegisterResolvers {

    /**
     * Finds the effective calculation registers for the instruction that corresponds to the given [stopPoint].
     *
     * @param program A [Program] to search for effective calculation registers in.
     * @param stopPoint The point to stop the search at.
     * @throws [IllegalArgumentException] when stop point is less than zero.
     */
    fun <TProgram, TOutput : Output<TProgram>> baseResolver(
        program: Program<TProgram, TOutput>,
        stopPoint: Int
    ): List<RegisterIndex> {

        if (stopPoint < 0) {
            throw IllegalArgumentException("Stop point must be greater than or equal to zero.")
        }

        val effectiveRegisters = program.outputRegisterIndices.toMutableList()

        // Only instructions up until to the stop point should be searched.
        (program.instructions.lastIndex downTo stopPoint).forEach { i ->
            val instruction = program.instructions[i]

            if (instruction.destination in effectiveRegisters) {
                effectiveRegisters.remove(instruction.destination)

                instruction.operands.forEach { operand ->
                    val type = program.registers.registerType(operand)
                    val isCalculation = type == RegisterType.Calculation || type == RegisterType.Input

                    if (operand !in effectiveRegisters && isCalculation) {
                        effectiveRegisters.add(operand)
                    }
                }
            }
        }

        return effectiveRegisters
    }

}

