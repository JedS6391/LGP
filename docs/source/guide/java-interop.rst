Java Interoperability
*********************

For the most part, using the system's API from the context of a Java program will be very similar to that of a Kotlin program. However, because the API is written in and designed for Kotlin, there are certain situations where the Java API is somewhat of a second-class citizen.

Documentation (Javadoc)
=======================

Generally, one can get by referencing the `dokka API documentation <https://jeds6391.github.io/LGP/>`_ when using Java. For the cases where a more Java-centric view is required, the system has `Javadoc <https://jeds6391.github.io/LGP/api/javadoc/index.html>`_ available too.

Peculiarities
=============

Because of the API being design first and foremost for interaction within Kotlin, there are a few pain points that have been aggregated and documented here as a reference:

**Unsafe Implementations**
    Due to Java's deficiency when it comes to reified generics, the ``Environment::registeredModule`` and ``ModuleContainer::instance`` functions require workarounds.

    The workaround implementations (``Environment::registeredModuleUnsafe`` and ``ModuleContainer::instanceUnsafe``) cannot guarantee type safety in the same way their Kotlin counterparts can, and thus should be used with caution. The documentation for both methods provides further detail on the exact problems that arise.

**Lambda Functions**
    Wherever the Kotlin API requires a lambda function, the Java equivalent can make use of Java lambda functions.

    The API however will specify Kotlin types (e.g. ``kotlin.jvm.functions.Function1``) as opposed to the Java equivalent. This should not impact usage in any way.

**Optional Parameters**
    Any API functions which expose optional parameters in Kotlin will require that a parameter is given explicitly in Java, due to the lack of optional parameters in Java.

    The best thing to do is to simply find out what the default parameter used is and make use of that value.
