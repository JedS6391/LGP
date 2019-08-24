package nz.co.jedsimson.lgp.core.program.instructions

import nz.co.jedsimson.lgp.core.program.registers.RegisterSet
import nz.co.jedsimson.lgp.core.modules.Module
import nz.co.jedsimson.lgp.core.program.Program
import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex

/**
 * An instruction in a [Program].
 *
 * Instructions can be implemented in any way but they must operate on a set of registers,
 * with one register as output and a set of registers as operands.
 *
 * The registers the instruction operates on are specified by an index.
 *
 * @param TData The type of data the instruction operates on.
 */
abstract class Instruction<TData> : Module {
    /**
     * Index of a register where the result of the instructions execution is stored.
     */
    abstract var destination: RegisterIndex

    /**
     * Collection of indices where the operands for the instructions operation are stored.
     */
    abstract var operands: MutableList<RegisterIndex>

    /**
     * An operation that applies a function to a set of operands.
     */
    abstract var operation: Operation<TData>

    /**
     * Executes the instruction on the given set of registers.
     *
     * @param registers A set of registers.
     */
    abstract fun execute(registers: RegisterSet<TData>)

    /**
     * Clones an instruction instance.
     *
     * When copying an instruction, it is expected that a deep copy
     * is performed, to prevent modifying the state of other instructions.
     */
    abstract fun copy(): Instruction<TData>
}