package nz.co.jedsimson.lgp.core.evolution.operators.mutation

import nz.co.jedsimson.lgp.core.program.Output
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.instructions.RegisterIndex
import nz.co.jedsimson.lgp.core.program.registers.RegisterType

/**
 * A function that can determine which calculation registers in the given [Program] are effective, up until the given stop point.
 */
typealias EffectiveCalculationRegisterResolver<TProgram, TOutput> = (Program<TProgram, TOutput>, Int) -> List<RegisterIndex>

/**
 * A collection of [EffectiveCalculationRegisterResolver]s.
 */
internal object EffectiveCalculationRegisterResolvers {

    /**
     * Finds the effective calculation registers in a [Program], up until the given [stopPoint].
     *
     * @param program A [Program] to search for effective calculation registers in.
     * @param stopPoint The point to stop the search at.
     */
    fun <TProgram, TOutput : Output<TProgram>> baseResolver(
            program: Program<TProgram, TOutput>,
            stopPoint: Int
    ): List<RegisterIndex> {
        val effectiveRegisters = program.outputRegisterIndices.toMutableList()
        // Only instructions up until to the stop point should be searched.
        val instructions = program.instructions.reversed().filterIndexed { idx, _ -> idx < stopPoint }

        instructions.forEach { instruction ->
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

