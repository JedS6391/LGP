package lgp.core.evolution.instructions

import lgp.core.evolution.registers.Register
import lgp.core.evolution.registers.RegisterSet
import lgp.core.modules.Module

typealias RegisterIndex = Int

abstract class Instruction<T> : Module {
    abstract var destination: RegisterIndex

    abstract var operands: MutableList<RegisterIndex>

    abstract var operation: Operation<T>

    abstract fun execute(registers: RegisterSet<T>)

    abstract fun copy(): Instruction<T>
}