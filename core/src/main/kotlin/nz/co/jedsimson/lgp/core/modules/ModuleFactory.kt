package nz.co.jedsimson.lgp.core.modules

import nz.co.jedsimson.lgp.core.environment.EnvironmentFacade
import nz.co.jedsimson.lgp.core.environment.dataset.Target
import nz.co.jedsimson.lgp.core.program.Output
import java.lang.ClassCastException

/**
 * Exception thrown when no [Module] is registered for a requested [RegisteredModuleType].
 */
class MissingModuleException(message: String) : Exception(message)

/**
 * Exception thrown when a [Module] is cast as a type that is not valid for it.
 */
class ModuleCastException(message: String) : Exception(message)

/**
 * Represents the different modules that are able to be registered with an environment.
 *
 * Any module that is able to be registered with the [EnvironmentFacade] as a *registered component*
 * should have a module type defined for it using this interface.
 *
 * @see [CoreModuleType] for an example implementation.
 */
interface RegisteredModuleType

/**
 * A mapping for core modules to a module type value.
 */
enum class CoreModuleType : RegisteredModuleType {

    /**
     * An [InstructionGenerator] implementation.
     */
    InstructionGenerator,

    /**
     * A [ProgramGenerator] implementation.
     */
    ProgramGenerator,

    /**
     * A [SelectionOperator] implementation.
     */
    SelectionOperator,

    /**
     * A [RecombinationOperator] implementation.
     */
    RecombinationOperator,

    /**
     * A [MacroMutationOperator] implementation.
     */
    MacroMutationOperator,

    /**
     * A [MicroMutationOperator] implementation.
     */
    MicroMutationOperator,

    /**
     * A [FitnessContext] implementation.
     */
    FitnessContext
}

/**
 * Facilitates access to modules that have been registered in a [ModuleContainer].
 *
 * The main goal is to separate the building of a module container (a public operation)
 * and retrieving instances of a module.
 *
 * @property container A [ModuleContainer] that this factory manages.
 * @constructor Creates a new [ModuleFactory] with the given [ModuleContainer].
 */
abstract class ModuleFactory<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    internal val container: ModuleContainer<TProgram, TOutput, TTarget>
) {

    /**
     * Provides an instance of the [Module] registered for the given [type].
     *
     * The module will be loaded and cast to the type given if possible to cast to that type. This method is able to
     * perform safe casts due to Kotlin's reified generics. If the cast can't be done safely, a [ModuleCastException]
     * will be thrown and contains details about the failed cast. Due to the use of reified generics, callers of this
     * function can be assured that a [ClassCastException] won't occur.
     *
     * **NOTE:** This function cannot be used from Java; use the [instanceUnsafe] function instead.
     *
     * @param type The type of of module to get an instance of.
     * @param TModule The type to cast the module as.
     * @return An instance of the module registered for the given module type cast safely as [TModule].
     * @throws MissingModuleException When no builder has been registered for the type of module requested.
     * @throws ModuleCastException When the requested module can't be cast to the type given.
     */
    inline fun <reified TModule : Module> instance(type: RegisteredModuleType): TModule {
        try {
            return resolveModuleFromType(type) as TModule
        }
        catch (classCastException: ClassCastException) {
            throw ModuleCastException("Unable to cast $type module as ${TModule::class.java.simpleName}.")
        }
    }

    /**
     * Provides an instance of the module [type] given (for Java interoperability).
     *
     * **TL;DR:** Java users -- beware! This function implements [instance] for Java callers, but unlike the Kotlin
     * equivalent, provides no guarantee that a [ClassCastException] will not occur.
     *
     * This method exists to aid Java interoperability: the [instance] method cannot be used from Java
     * as it is both inlined and makes use of reified generics. With this method, Java users can query for
     * [RegisteredModuleType] instances.
     *
     * The reason it is unsafe is that there is no guarantee that the cast will be correct: if the type requested
     * can be cast to [Module] then the cast will succeed and a [ClassCastException] will be given where the call is
     * made (i.e. the function will return with no error until an assignment type is checked). With the normal (and
     * safe) [instance] function, the invalid cast can be caught before the function returns and dealt with appropriately.
     *
     * @param type The type of of module to get an instance of.
     * @return An instance of the module registered for the given module type cast as [TModule].
     * @throws MissingModuleException When no builder has been registered for the type of module requested.
     */
    @Suppress("UNCHECKED_CAST")
    fun <TModule : Module> instanceUnsafe(type: RegisteredModuleType): TModule {
        return resolveModuleFromType(type) as TModule
    }

    /**
     * Resolves the raw [Module] registered for the given [type].
     *
     * @param type The type of of module to get an instance of.
     * @return An instance of the module registered for the given module type.
     * @throws MissingModuleException When no builder has been registered for the type of module requested.
     */
    @PublishedApi
    internal abstract fun resolveModuleFromType(type: RegisteredModuleType): Module
}

/**
 * An implementation of [ModuleFactory] that will cache [Module] instances after the first instantiation.
 */
internal class CachingModuleFactory<TProgram, TOutput : Output<TProgram>, TTarget : Target<TProgram>>(
    container: ModuleContainer<TProgram, TOutput, TTarget>
) : ModuleFactory<TProgram, TOutput, TTarget>(container) {

    // All instances are provided as singletons
    private val instanceCache = mutableMapOf<RegisteredModuleType, Module>()

    override fun resolveModuleFromType(type: RegisteredModuleType): Module {
        if (type in instanceCache) {
            return instanceCache[type]!!
        }
        
        val moduleBuilder = this.container.modules[type]
                ?: throw MissingModuleException("No module builder registered for $type.")

        val module = moduleBuilder(this.container.environment)

        instanceCache[type] = module

        return module
    }

}