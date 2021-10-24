# Package nz.co.jedsimson.lgp.core.modules

Provides the main components for defining an interacting with modules in the system.
  
  - Interfaces to define a module in the system, as well as metadata about modules:
    - `Module`
    - `ModuleInformation`
  - Definitions for managing collections of `Module`s and creating instances of a `Module`.
    - `ModuleContainer`
    - `ModuleFactory`
    
Modules are core to the system, as any number of them can be registered and then used in different parts 
of the system. This extensible design allows control of all the aspects of the system. 

The goal of the `nz.co.jedsimson.lgp.core` package is to provide a base set of modules for general purpose
usage of the system for evolving programs tailored to solve particular problems, and offering extension points
through the modules API.