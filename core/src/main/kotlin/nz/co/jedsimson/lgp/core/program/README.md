# Package nz.co.jedsimson.lgp.core.program

Contains the main interfaces and implementations for a program that is evolved by the Linear Genetic Programming algorithms.

LGP describes a program as a sequence of instructions that operate on a set of registers. This definition is modelled
in this package with the following primitives:

  - `Operation`: An action on a set of arguments.
  - `Instruction`: An instruction in a program. Each instructions performs a specific operation.
  - `Register`: A register that has an index and some value.
  - `RegisterSet`: A set of registers. The set is split up into different sections for different types of registers.
  - `Program`: A composition of a sequence of instructions and a register set that can be executed to produce an output.
  
There are a few other components that are used to build these main primitives, but are not detailed here.
