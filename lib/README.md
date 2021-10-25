# LGP Library

> *A set of implementations for core LGP framework components.*

[![license][license-image]][license-url]
[![build][build-image]][build-url]
[![maven][maven-image]][maven-url]

## About

This package contains a set of implementations for core [LGP framework](https://github.com/JedS6391/LGP) components.

---

### Operations
  
**Arithmetic** 

A collection of arithmetic operators for double types, including:

- `Addition`
- `Subtraction`
- `Multiplication`
- `Division`
- `Exponent`

**Bitwise**
 
 A collection of bitwise operators (that work on double types), including:
 
- `Not`
- `And`
- `Or`
- `ExclusiveOr`

**Conditional**

A collection of conditional operators that can alter the control flow of a program. Includes:

- `IfGreater`
- `IfLessThanOrEqualTo`

**Uncategorised**

- `Identity`
- `Sine`

---

### Generators

**`EffectiveProgramGenerator`**

A program generator that will only generate individuals with an effective set of instructions. This can be useful to improve the generated population as it limits the initial amount of *dead code*.

**`RandomProgramGenerator`**

Generates completely random programs from a set of operations and registers. This generator relies of `RandomInstructionGenerator` for generating instructions.


**`RandomInstructionGenerator`**

Generates random instructions from a given set of parameters.

---

### Configuration
 
 **`YamlConfigurationLoader`**

Provides the ability to read configuration from YAML files which may be preferred in some cases.
 
 ---
 
### Base

This package is for implementations of the main elements used by the LGP framework. The core package doesn't provide any implementation for instructions and programs as those may be dependent on the individual use case. These base implementations are meant to cover the general case and should apply in a wide range of scenarios.

**`BaseInstruction`**

An instruction implementation that has the following properties:

- Consists of a single operation that is applied to set of operand registers and stores the result in a single destination register
- Provides a C-style instruction formatting representation (e.g. `r[1] = r[1] + r[2]`
 
**`BaseProgram`**

A program implementation that is made up of a sequence of instructions (generally expected to be `BaseInstructions`). This implementation supports:

- Branching instructions
- Single output instructions
- Multiple program outputs (depending on the output type and output resolver specified)

[license-image]: https://img.shields.io/github/license/mashape/apistatus.svg?style=flat
[license-url]: https://github.com/JedS6391/LGP/blob/master/LICENSE
[build-image]: https://img.shields.io/github/workflow/status/JedS6391/LGP/Release
[build-url]: https://github.com/JedS6391/LGP/actions/workflows/release.yml
[maven-image]: https://img.shields.io/maven-central/v/nz.co.jedsimson.lgp/LGP-lib.svg?label=lib&style=flat
[maven-url]: https://search.maven.org/search?q=g:%22nz.co.jedsimson.lgp%22%20AND%20a:%22LGP-lib%22