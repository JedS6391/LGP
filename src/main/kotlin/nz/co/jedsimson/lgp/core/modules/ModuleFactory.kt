package nz.co.jedsimson.lgp.core.modules

import nz.co.jedsimson.lgp.core.program.Output

/**
 * Facilitates access to modules that have been registered in a [ModuleContainer].
 *
 * The main goal is to separate the building of a module container (a public operation)
 * and retrieving instances of a module.
 */
class ModuleFactory<TProgram, TOutput : Output<TProgram>> internal constructor(
    val container: ModuleContainer<TProgram, TOutput>
) {

    // All instances are provided as singletons
    val instanceCache = mutableMapOf<RegisteredModuleType, Module>()

    /**
     * Provides an instance of the module [type] given.
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
     * @return An instance of the module registered for the given module type.
     * @throws MissingModuleException When no builder has been registered for the type of module requested.
     * @throws ModuleCastException When the requested module can't be cast to the type given.
     */
    inline fun <reified TModule : Module> instance(type: RegisteredModuleType): TModule {
        if (type in instanceCache)
            return instanceCache[type] as TModule

        // If no module builder exists (i.e. it is null) then we can assume that
        // no module builder has been registered for this module type, despite it
        // being requested by from somewhere.
        val moduleBuilder = this.container.modules[type]
                ?: throw MissingModuleException("No module builder registered for $type.")

        // At this stage, we at least know that the module builder is valid so we can go ahead and execute it.
        // However, we need to check that the module can actually be cast as the type requested.
        // Doing this means that we don't have to do unchecked casts anywhere and gives protection against
        // invalid casts from calling code.
        val module = moduleBuilder(this.container.environment) as? TModule
                ?: throw ModuleCastException("Unable to cast $type module as ${TModule::class.java.simpleName}.")

        // Cache this instance for later usages since it is valid when cast to the type given.
        instanceCache[type] = module

        return module
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
     * @param TModule The type to cast the module as.
     * @return An instance of the module registered for the given module type.
     * @throws MissingModuleException When no builder has been registered for the type of module requested.
     * @throws ModuleCastException When the requested module can't be cast to the type given.
     */
    fun <TModule : Module> instanceUnsafe(type: RegisteredModuleType): TModule {
        @Suppress("UNCHECKED_CAST")
        if (type in instanceCache)
            return instanceCache[type] as TModule

        val moduleBuilder = this.container.modules[type]
                ?: throw MissingModuleException("No module builder registered for $type.")

        @Suppress("UNCHECKED_CAST")
        val module = moduleBuilder(this.container.environment) as? TModule
                ?: throw ModuleCastException("Unable to cast $type module to given type.")

        instanceCache[type] = module

        return module
    }
}