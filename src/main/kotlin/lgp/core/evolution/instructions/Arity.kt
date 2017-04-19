package lgp.core.evolution.instructions

interface Arity {
    val number: Int
}

/**
 * The arity of an operation, specifying how many arguments it can take.
 *
 * @param number An integer that represents how many arguments the arity describes.
 */
enum class BaseArity(override val number: Int) : Arity {
    Unary(1),
    Binary(2);
}