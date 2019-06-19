package nz.co.jedsimson.lgp.core.program

import nz.co.jedsimson.lgp.core.modules.Module

/**
 * Module that can be used to translate programs to external representations.
 *
 * This class primarily exists to make it easy to translate programs from a single place,
 * rather than from internally defined logic.
 */
abstract class ProgramTranslator<TProgram, TOutput : Output<TProgram>> : Module {

    /**
     * Translates [program] from some internal representation to a concrete representation.
     *
     * This is useful for taking an evolved LGP program and translating it to an output that
     * can be integrated into some other eco-system (e.g. the C programming language).
     *
     * @param program A program to translate.
     * @returns A translated representation of [program] as a string.
     */
    abstract fun translate(program: Program<TProgram, TOutput>): String
}
