# Package nz.co.jedsimson.lgp.core.evolution

Contains core system components that perform the evolution process.

## Fitness

Defines modules for evaluating fitness as well as the generic definition of a fitness function for programs in the system. 
A fitness function is the main driving force for determining the *best* program in a population of individuals.

A set of built-in fitness functions are also provided from the core library.

## Model

Provides an abstraction for the primary model of evolution. The main LGP algorithms are implemented in three variants:

  - `SteadyState`: The classic LGP algorithm.
  - `MasterSlave`: The classic LGP algorithm, but performs evaluation in parallel.
  - `IslandMigration`: Runs independent instances of the classic LGP algorithm on subsets of an overall population
       and uses an island migration technique to share individuals between populations. 

## Operators

Contains interfaces and implementations for the primary evolutionary operators available to the system:

  - Mutation: Defines how individuals from the population will be mutated to form new individuals. 
  - Recombination: Defines how two individuals from the population will be combined.
  - Selection: Defines how individuals will be selected from the population before being subjected to mutation and recombination.

## Training

Provides a set of interfaces and implementations for performing aggregate evolution (training). It is often desirable to build
and evaluate a model multiple times in succession/parallel, which the training abstractions allow.