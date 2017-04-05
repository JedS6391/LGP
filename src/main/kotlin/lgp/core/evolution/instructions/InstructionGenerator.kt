package lgp.core.evolution.instructions

import lgp.core.modules.Module

/**
 * A generator of instructions in an LGP system.
 *
 * The functionality is left up to the implementor, but the basic skeleton exposes a simple API,
 * that allows the instruction generator to be iterated over as a sequence of instructors, for example:
 *
 * ```
 * val instructionGenerator = SomeInstructionGenerator()
 *
 * // Iterate through the sequence of instructions
 * for (instruction in instructionGenerator.next()) {
 *     doSomethingWith(instruction)
 * }
 * ```
 *
 * @param T The type that the instructions generated operate on.
 */
// TODO: Should we explicitly expose the Iterable<Instruction<T>> interface and allow the generator itself to be iterated over.
abstract class InstructionGenerator<T> : Module {
    /**
     * Gives a sequence of instructions.
     */
    abstract fun next(): Sequence<Instruction<T>>
}