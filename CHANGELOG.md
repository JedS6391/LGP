# Change Log

## [v-5.3](https://github.com/JedS6391/LGP/tree/5.3) (2021-10-25)

**Additions:**

- Add logging abstractions. Logging can now be configured using SLF4J to gain deeper introspection into the system.

**Breaking Changes:**

- Move to multi-module project structure. The main repository now includes the `core` and `lib` packages.
- The artifacts published to Maven have changed to reflect the new structure:
  - `nz.co.jedsimson.lgp:LGP` is now `nz.co.jedsimson.lgp:core`
  - `nz.co.jedsimson.lgp:LGP-lib` is now `nz.co.jedsimson.lgp:lib`

**Fixes:**

- Maven publication POM did not declare dependencies in the 5.2 release. This has been resolved.

## [v-5.2](https://github.com/JedS6391/LGP/tree/5.2) (2021-10-23)

**Changes:**

- Migrate from Travis CI to GitHub Actions
- Update to Gradle 7.x
- Migrate build scripts from Groovy to Kotlin
- Re-generate KDoc and JavaDoc

## [v-5.1](https://github.com/JedS6391/LGP/tree/5.1) (2019-09-08)

**Breaking changes:**

- The `TournamentSelection` operator now has an additional (non-optional) property `numberOfOffspring` which supersedes
  the old `numOffspring` `Configuration` property.
- `EventDispatcher` no longer contains methods for register `EventListener` definitions. The registration
  methods have been moved to `EventRegistry`.
  
**Changes:**

- Any components which make use of the experimental co-routine APIs have been marked with the `@ExperimentalCoroutinesApi` attribute.
- A new `DiagnosticEvent` has been exposed which allows introspection into the system. The majority of the built-in
  components have been updated to dispatch `DiagnosticEvent`s for various actions. For example, to register a listener
  for trace events:
  
  ```kotlin
  EventRegistry.register(object : EventListener<DiagnosticEvent.Trace> {
    override fun handle(event: DiagnosticEvent.Trace) {
        // Do something with event
    }
  })
  ```
- Unit tests have been added for the following components (and there are more to come!):
  - Tournament selection operator
  - Recombination operator

**Fixes:**

- Address an incorrect typealias definition for `MultipleOutputFitnessFunction` (08fa90d)
- Refactor `LinearCrossover` operator and address a bug that caused program length to fall below the configured
  minimum program length

## [v-5.0](https://github.com/JedS6391/LGP/tree/5.0) (2019-08-25)

**Breaking changes:**

*Type definitions*

- The majority of the core library has been modified to accept a generic target type parameter, to allow the target
  of the data set to be strongly-typed through the system. Previously, only the program data type and type of program
  output was generic. The main benefit of the introduced target type parameter, is to simplify the usage and
  implementation of fitness functions.

*Modules*

- `ModuleContainer` has been moved to the `nz.co.jedsimson.lgp.core.modules` package. 
- The module resolution functions `instance` and `instanceUnsafe` are no longer the responsibility of `ModuleContainer` 
  and have been relocated to the `ModuleFactory` class. 
  The intended usage is that `ModuleContainer` defines a set of `ModuleBuilder`s, and
  the `ModuleFactory` can be used to resolve instances from a given `ModuleContainer`.
 
*Environment*  
 
- Direct dependency on `Environment` has been removed from all core implementations and is recommended for any custom implementations.
  The public contract is now defined in `EnvironmentFacade` and this should be the contract relied upon.
- `Environment` can no longer be directly used to resolve dependencies as the `registeredModule` and `registerModuleUnsafe`
  functions have been removed. Dependencies should be resolved using `EnvironmentFacade.moduleFactory`.
  
*Evolution operators*

- The `nz.co.jedsimson.lgp.core.evolution` package was restructured significantly. It is now split up as below:

```
    nz.co.jedsimson.lgp.core.evolution.operators
    ├── mutation 
    │   ├── macro
    │   ├── micro
    │   └── strategy
    ├── recombination
    └── selection
```
- `MacroMutationOperator` now accepts an optional `MutationStrategyFactory` which can be used to customise the mutation behaviour.
- `MicroMutationOperator` now accepts an optional `MutationStrategyFactory` which can be used to customise the mutation behaviour.
- When no `MutationStrategyFactory` is given to either `MacroMutationOperator` or `MicroMutationOperator`, they will use the default
  strategy factory which preserves the previous behaviour of these implementations. The main reason behind this change is internal,
  but it can be used externally too.

*Program*

- The properties `instructions`, `registers`, and `outputRegisterIndices` are now abstract properties of `Program`.
- The properties `arity` and `func` (now renamed to `function`) are now abstract properties of `Operation`.

*Registers*

- *RegisterSet* is now an interface and the implementation has been moved to *ArrayRegisterSet*.


*Miscellaneous*

- The built-in Kotlin `Random` is now used instead of `java.util.Random`. 
- Moved `Random` extension functions `choice`, `randInt`, `sample` to the `nz.co.jedsimson.lgp.core.environment` package.
- Move `MutableList` extension function `slice` into the `nz.co.jedsimson.lgp.core.evolution.operators` package.
- Moved `Valid` and `Invalid` `ConfigurationValidity` implementations into the parent class (i.e. no longer access `Valid`, instead use `ConfigurationValidity.Valid`).

**Changes:**

- Introduce `EnvironmentFacade` which defines the public contract for the `Environment`. It is intended that `Environment`
  no longer be used directly, and instead dependencies should rely on the `EnvironmentFacade`. All core implementations
  that previously relied on `Environment` no rely on `EnvironmentFacade`.
  
- Introduce `ModuleFactory` which can be used to resolve `Module` instances. The implementation of module resolution has been
  abstracted to allow `instance` and `instanceUnsafe` to share the same code.
  
- Introduce `ComponentProvider` to simplify the implementation of `ComponentLoader`s that share common component resolution logic.
  A caching `ComponentProvider` is provided in the core package in `MemoizedComponentProvider`. The built-in `ComponentLoader`s
  (`JsonConfigurationLoader`, `CsvDatasetLoader`, `GenericConstantLoader`, and `DefaultOperationLoader`) have been updated to use
  `MemoizedComponentProvider`.

- This release adds a large amount of unit tests for the core library to ensure the implementation is correct and
  make changes going forward more robust. The tests are located in the `nz.co.jedsimson.lgp.test` package and can be
  run with the command `./gradlew test`.
  
- Introduced the `RegisterSet.apply` function, which can be used to modify the value of a given register in the set.

- Handle `RegisterSet` index-out-of-bounds more gracefully. Now a `RegisterReadException` will be given.
  
**Fixes:**

- Fixed an issue where an `Invalid` `ConfigurationValidty` reported itself as valid.
- Improve generation of sequences using both `SequenceGenerator` and `UniformlyDistributedGenerator`, particularly concerning edge cases.
- Better handling of `ClassNotFoundException` when using the `DefaultOperationLoader`.
- Resolved an issue that caused `RandomRegisterGenerator` to re-evaluate the given predicate on each sequence iteration, instead of 
  evaluating once and waiting until that type is generated.

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
