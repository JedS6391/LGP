package lgp.core.evolution.instructions

/**
 * The arity of an operation, specifying how many arguments it can take.
 *
 * @param number An integer that represents how many arguments the arity describes.
 */
enum class Arity(val number: Int) {
    Unary(1),
    Binary(2);
}