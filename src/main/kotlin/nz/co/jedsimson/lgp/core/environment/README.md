# Package nz.co.jedsimson.lgp.core.environment

Contains core system components that define the environment in which evolution will take place.

## Component loaders

One responsibility of the environment is to group a set of component loaders for the different components that are 
available to the system at run time:

  - `ConfigurationLoader`: Responsible for loading configuration that will effect various aspects of the evolution process.
  - `ConstantLoader`: Responsible for loading constant values into the system.
  - `DatasetLoader`: Handles loading a data set into the system.
  - `OperationLoader`: Handles loading the operations for programs.

## Events

The environment contains definition for creating, dispatching, and receiving events. The main purpose of this sub-system
is to allow introspection into the evolution process by listening for certain notifications as events are raised from
the system.

## Environment Facade

The environment provides a facade which defines a simple interface for modules/components that wish to access the environment.