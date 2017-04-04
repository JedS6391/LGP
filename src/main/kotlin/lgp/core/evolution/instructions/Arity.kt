package lgp.core.evolution.instructions

/**
 * The arity of an operation, specifying how many arguments it can take.
 *
 * @param arity An integer that represents how many arguments the arity describes.
 */
enum class Arity(val arity: Int) {
    Unary(1),
    Binary(2);
}