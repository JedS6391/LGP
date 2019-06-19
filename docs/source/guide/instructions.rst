Instructions
************

An Instruction is a vital component of an LGP program. They provide the functionality to allow programs to compute values and are executed in a sequential order when evaluating a program.

Overview
========

Instruction
-----------

An ``Instruction`` is composed of an ``Operation`` and information about the operands of the instruction. Whereas an ``Operation`` has a function that it uses to transform a given number of arguments, an ``Instruction`` has a destination register, a set of operand registers and an ``Operation``. The instruction can be executed, which involves applying the operation to the operand registers and storing the result in the destination register.

The system provides a built-in instruction type (``BaseInstruction``) for instructions that have a single output register (the most common case of instruction). Where necessary, one can implement custom instructions that may need custom logic --- for example, gathering sensory data from the environment.

The ``BaseInstruction`` type offers a C-style instruction based representation (suitable for export during the translation process). For example, an instruction that adds two operands together and stores the result in an output register would be output as ``"r[1] = r[1] + r[2]"``.

.. note:: The ``BaseInstruction`` type is found in the ``nz.co.jedsimson.lgp.lib`` package which resides in the `LGP-lib repository <https://github.com/JedS6391/LGP-lib>`_.

Instruction Generator
---------------------

One of the important modules required to perform evolution is the ``InstructionGenerator``. This module is responsible for providing the logic necessary to create instructions that build up valid programs in LGP. Like a tree-based GP approach, there a multiple techniques for creating instructions and thus the implementation used needs to be modular to allow different schemes to be utilised.

The system offers a built-in ``InstructionGenerator`` (``RandomInstructionGenerator``) which is capable of producing a endless, random stream of new ``BaseInstruction`` instances.

.. note:: The ``RandomInstructionGenerator`` type is found in the ``nz.co.jedsimson.lgp.lib`` package which resides in the `LGP-lib repository <https://github.com/JedS6391/LGP-lib>`_.

API
===

See `nz.co.jedsimson.lgp.core.program.instructions <https://lgp.jedsimson.co.nz/api/html/nz.co.jedsimson.lgp.core.program.instructions/index.html>`_ for details on the ``Instruction`` and ``InstructionGenerator`` APIs.

For the API of the built-in modules --- ``BaseInstruction`` and ``RandomInstructionGenerator`` --- refer to `nz.co.jedsimson.lgp.lib <https://github.com/JedS6391/LGP-lib>`_.


