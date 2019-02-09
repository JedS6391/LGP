# Change Log

## [v-4.2](https://github.com/JedS6391/LGP/tree/4.2) (2019-02-09)

**Breaking changes:**

- The `nz.co.jedsimson.lgp.lib` and `nz.co.jedsimson.lgp.examples` packages have been removed and split into their own modules:
    - [`nz.co.jedsimson.lgp.lib`](https://github.com/JedS6391/LGP-lib)
    - [`nz.co.jedsimson.lgp.examples`](https://github.com/JedS6391/LGP-examples)
    
If you are relying on these then please reference LGP core as well as the modules you require and change the package 
names as required. This should be a relatively minor breaking change (as far as breaking changes go).

## [v-4.1](https://github.com/JedS6391/LGP/tree/4.1) (2019-02-09)

**Changes:**

- `BaseProgramGenerator` now only generates random program instances
- `EffectiveProgramGenerator` can be used to generate effective program instances

**Fixes:**

- The algorithm used for generating effective program instances would previously not always generate effective programs.

**Additions:**

- Added `RandomRegisterGenerator<T>.getRandomInputAndCalculationRegisters()` helper method for getting a given number
  of input and calculation registers (with a random distribution).

## [v-4.0](https://github.com/JedS6391/LGP/tree/4.0) (2019-02-06)

**Breaking changes:**

- Rename top-level package from `lgp` to `nz.co.jedsimson.lgp` to match Maven deployment.

**Enhancements/Additions:**

- Facilitate the deployment of the LGP artefact to Maven central.
- RegisterSet performance improvements (`Array` vs `MutableList`)

*The version has been bumped to 4.0 to reflect these breaking changes.*

## [v-3.1](https://github.com/JedS6391/LGP/tree/3.1) (2018-12-19)

**Changes:**

- Kotlin 1.3.11
- kotlinx-coroutines 1.0.1
- dokka 0.9.17
- Removes IntelliJ project files

**Acknowledgements:**

- [Ulrik Günther](https://github.com/skalarproduktraum) -- updated Kotlin, kotlinx-coroutines, dokka, and tidied up IntelliJ files.

## [v-3.0](https://github.com/JedS6391/LGP/tree/3.0) (2018-11-04)

**Breaking changes:**

- Most of the core components in the system now require an extra type parameter: `TOutput`. This is used to determine what type of output the programs have, enabling programs with single and multiple outputs
- Addition of classes/helpers to aid in handling programs with different numbers of outputs

*The version has been bumped to 3.0 to reflect these breaking changes.*

**Enhancements/Additions:**

- New `ExclusiveOr` bitwise operation
- Allow operations to be serialised with a `toString` method for easier program translation
- New example problem `FullAdder` to demonstrate the use of multiple-output programs

**Bug fixes:**

- Fix an issue where the `Or` operation was incorrectly performing a logical and
- Address a logic error in the Island Migration implementation that could cause problems with certain configurations

**Acknowledgements:**

- [Hongyu Wang](https://github.com/HongyuJerryWang) -- contributions to the implementation of multiple program outputs, full adder example, new operations, and bug fixes.

## [v-2.0](https://github.com/JedS6391/LGP/tree/2.0) (2018-10-11)

**Breaking changes:**

- The `Trainers` object no longer exists
- Related to above, the `Trainer` implementations have moved from `lgp.core.evolution` to `lgp.core.evolution.training`
- A new package `lgp.core.program` has been created and quite a few classes from `lgp.core.evolution` were migrated

The version was bumped to 2.0 to reflect these potentially breaking changes.

**Enhancements:**

- Added the ability to train asynchronously using the built-in `Trainer` implementations
- Update to the Kotlin 1.3 release candidate to make use of the soon to be stable co-routine APIs

## [v-1.2-beta](https://github.com/JedS6391/LGP/tree/v-1.2-beta) (2018-02-10)
[Full Changelog](https://github.com/JedS6391/LGP/compare/v-1.1...v-1.2-beta)

**Implemented enhancements:**

- Add YAML ConfigLoader implementation. [\#28](https://github.com/JedS6391/LGP/pull/28)

## [v-1.1](https://github.com/JedS6391/LGP/tree/v-1.1) (2018-02-09)
**Closed issues:**

- README.md needs small update [\#25](https://github.com/JedS6391/LGP/issues/25)
- Add configurable stopping criterion. [\#19](https://github.com/JedS6391/LGP/issues/19)
- Base Problem Implementation [\#16](https://github.com/JedS6391/LGP/issues/16)
- Resources for examples. [\#11](https://github.com/JedS6391/LGP/issues/11)
- Fitness context modularity. [\#5](https://github.com/JedS6391/LGP/issues/5)

**Merged pull requests:**

- Fix Kotlin usage example command. [\#26](https://github.com/JedS6391/LGP/pull/26)
- Code tidy up + linting. [\#24](https://github.com/JedS6391/LGP/pull/24)
- Add javadoc and further java interoperability documentation. [\#23](https://github.com/JedS6391/LGP/pull/23)
- Improvements to Java interoperability. [\#22](https://github.com/JedS6391/LGP/pull/22)
- Add ability to toggle the determinism of the system. [\#21](https://github.com/JedS6391/LGP/pull/21)
- Add base problem implementation for faster usage with defaults. [\#20](https://github.com/JedS6391/LGP/pull/20)
- Update documentation for latest core implementation. [\#15](https://github.com/JedS6391/LGP/pull/15)
- Refactor data set and model API to allow training and testing of model on different data sets. [\#14](https://github.com/JedS6391/LGP/pull/14)
- Add automatic dataset generation, simply problem definition, and add separate builds for examples/core. [\#13](https://github.com/JedS6391/LGP/pull/13)
- Add more examples. [\#12](https://github.com/JedS6391/LGP/pull/12)
- Further documentation. [\#10](https://github.com/JedS6391/LGP/pull/10)
- Improve evolutionary model implementation, add evolution runners. [\#9](https://github.com/JedS6391/LGP/pull/9)
- Add initial documentation for LGP, and refactor module loading. [\#8](https://github.com/JedS6391/LGP/pull/8)
- Update documentation and some modularity changes. [\#7](https://github.com/JedS6391/LGP/pull/7)
- Implementation of evolutionary search operators. [\#6](https://github.com/JedS6391/LGP/pull/6)
- Fitness function modularity changes. [\#4](https://github.com/JedS6391/LGP/pull/4)
- Update Documentation [\#3](https://github.com/JedS6391/LGP/pull/3)
- Update documentation. [\#2](https://github.com/JedS6391/LGP/pull/2)
- Updated documentation. [\#1](https://github.com/JedS6391/LGP/pull/1)
