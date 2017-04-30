package lgp.core.evolution.instructions

import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.Module

typealias RegisterIndex = Int

/**
 * An instruction in an LGP program.
 *
 * Instructions can be implemented in any way but they must operate
 * on a set of registers, with one register as output and a set of
 * registers as operands.
 *
 * The registers the instruction operates on are specified by an index.
 *
 * @param T The type of data the instruction operates on.
 */
abstract class Instruction<T> : Module {
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
    abstract var operation: Operation<T>

    /**
     * Executes the instruction on the given set of registers.
     *
     * @param registers A set of registers.
     */
    abstract fun execute(registers: RegisterSet<T>)

    /**
     * Clones an instruction instance.
     *
     * When copying an instruction, it is expected that a deep copy
     * is performed, to prevent modifying the state of other instructions.
     */
    abstract fun copy(): Instruction<T>
}