# RISC-V RV32I Emulator

This project is a Java-based emulator for the RISC-V RV32I instruction set. The emulator supports a subset of the RV32I instruction set, focusing on base integer instructions except for `fence`. This project allows you to execute RISC-V instructions and examine register states.

For learning purposes only.

- **Supported Instructions**:
    - **Arithmetic**: `addi`, `add`, `sub`, `and`, `or`, `xor`, `sll`, `srl`, `sra`
    - **Control**: `jal`, `jalr`, `beq`, `bne`, `blt`, `bge`, `bltu`, `bgeu`
    - **Memory Access**: `lw`, `sw`
    - **Immediate Load**: `lui`, `auipc`

- **Not Supported**: `fence` instruction and other advanced instructions like `mul`, `div`, `amo` (Atomic memory operations).

- **Simulation**:
    - Executes RISC-V instructions from a given binary program.
    - Displays register states and memory accesses after execution.
    - Can step through instructions one by one.
    - **Execution Cycle**: Five-staged (unpipelined), setting a path for future development towards cycle-agnostic pipelined and superscalar emulators.

- **Memory Management**:
    - Supports basic memory operations (load and store).
    - Accesses memory through direct address manipulation.

## License
This project is licensed under the MIT License.