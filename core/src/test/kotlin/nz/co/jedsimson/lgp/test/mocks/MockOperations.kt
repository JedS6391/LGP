package nz.co.jedsimson.lgp.test.mocks

import nz.co.jedsimson.lgp.core.modules.ModuleInformation
import nz.co.jedsimson.lgp.core.program.instructions.BinaryOperation
import nz.co.jedsimson.lgp.core.program.instructions.UnaryOperation
import nz.co.jedsimson.lgp.core.program.registers.RegisterIndex

class Identity : UnaryOperation<Double>({ arguments -> arguments.get(0) }) {
    override val representation: String
        get() = TODO("not implemented")

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        TODO("not implemented")
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")
}

class Zero : BinaryOperation<Double>({ arguments -> 0.0 }) {
    override val representation: String
        get() = TODO("not implemented")

    override fun toString(operands: List<RegisterIndex>, destination: RegisterIndex): String {
        TODO("not implemented")
    }

    override val information: ModuleInformation
        get() = TODO("not implemented")
}