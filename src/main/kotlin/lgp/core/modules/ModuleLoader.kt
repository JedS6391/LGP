package lgp.core.modules

/**
 * Provides a way to load [Module]s by their name.
 *
 * Internally, the loader caches the class of any [Module]s that are loaded
 * to prevent having to delegate to the class loader each time.
 */
class ModuleLoader {

    private val moduleCache: MutableMap<String, Class<out Module>> = HashMap()

    /**
     * Fetches the class of the [Module] associated with [name].
     *
     * @param name the class name of the [Module] to load.
     * @throws [InvalidModuleException] when the [name] given does not reference
     *         a valid module.
     * @return The [Class] of the [Module] associated with [name].
     */
    fun loadModule(name: String) : Class<out Module>  {
        // First, check the cache to avoid calling the class loader if possible.
        if (this.moduleCache.containsKey(name)) {
            // We know that the cache contains an entry so we don't need to worry
            // about optionals.
            return this.moduleCache[name] as Class<out Module>
        }

        // Cache miss... need to go through the class loader.
        val clazz = this.javaClass.classLoader.loadClass(name)

        when {
            !this.javaClass.isAssignableFrom(clazz) -> {
                // The name provided doesn't match any modules the class loader knows about.
                throw InvalidModuleException("Provided class $name is not a valid Module.")
            }
            else -> {
                // We know that the class is an implementation of `Module` so the cast is valid.
                this.moduleCache[name] = clazz as Class<out Module>
            }
        }

        return this.moduleCache[name] as Class<out Module>
    }

    /**
     * Fetches an instance of the [Module] associated with [name].
     *
     * @param name the class name of the [Module] to load.
     * @throws [InvalidModuleException] when the [name] given does not reference
     *         a valid module.
     * @returns An instance of [Module].
     */
    fun instanceOf(name: String): Module {
        val clazz: Class<out Module> = this.loadModule(name)

        return clazz.newInstance()
    }
}