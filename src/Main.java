import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

 class CPUSimulator {

    // Memory and Register File
    private static final int MEMORY_SIZE = 1024; // 1 KB memory
    private int[] memory = new int[MEMORY_SIZE];
    private int[] registers = new int[32]; // 32 general-purpose registers
    private int pc = 0; // Program Counter

    // Instruction Fields
    private int opcode, rd, funct3, rs1, rs2, funct7, imm;

    // Opcode Constants
    private static final int OP_LUI = 0x37;
    private static final int OP_AUIPC = 0x17;
    private static final int OP_JAL = 0x6F;
    private static final int OP_JALR = 0x67;
    private static final int OP_BRANCH = 0x63;
    private static final int OP_LOAD = 0x03;
    private static final int OP_STORE = 0x23;
    private static final int OP_IMM = 0x13;
    private static final int OP_REG = 0x33;
    private static final int OP_SYSTEM = 0x73;

    // Instruction Memory (Program)
    private HashMap<Integer, Integer> instructionMemory = new HashMap<>();

    // GUI Components
    private JFrame frame;
    private JTextArea registerArea;
    private JTextArea instructionArea;
    private JTextArea fetchedInstructionArea;
    private JTextField speedField;
    private JButton startButton;
    private JButton nextButton;

    public CPUSimulator() {
        setupGUI();
    }

    private void setupGUI() {
        frame = new JFrame("CPU Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);

        JPanel panel = new JPanel(new BorderLayout());

        registerArea = new JTextArea(20, 30);
        registerArea.setEditable(false);
        JScrollPane registerScrollPane = new JScrollPane(registerArea);

        instructionArea = new JTextArea(10, 50);
        instructionArea.setEditable(false);
        JScrollPane instructionScrollPane = new JScrollPane(instructionArea);

        fetchedInstructionArea = new JTextArea(5, 50);
        fetchedInstructionArea.setEditable(false);
        JScrollPane fetchedScrollPane = new JScrollPane(fetchedInstructionArea);

        JPanel controlPanel = new JPanel(new FlowLayout());
        speedField = new JTextField(10);
        startButton = new JButton("Start");
        nextButton = new JButton("Next");
        controlPanel.add(new JLabel("Speed (instr/sec):"));
        controlPanel.add(speedField);
        controlPanel.add(startButton);
        controlPanel.add(nextButton);

        panel.add(registerScrollPane, BorderLayout.WEST);
        panel.add(instructionScrollPane, BorderLayout.CENTER);
        panel.add(fetchedScrollPane, BorderLayout.SOUTH);
        panel.add(controlPanel, BorderLayout.NORTH);

        frame.add(panel);
        frame.setVisible(true);

        nextButton.addActionListener(e -> executeNextInstruction(0));
        startButton.addActionListener(e -> {
            String speedText = speedField.getText();
            double speed = 0;
            if (!speedText.isEmpty()) {
                try {
                    speed = Double.parseDouble(speedText);
                } catch (NumberFormatException ignored) {
                }
            }
            if (speed > 0) {
                double finalSpeed = speed;
                new Thread(() -> executeNextInstruction(finalSpeed)).start();
            }
        });
    }

    private void updateGUI(int changedRegister, int instruction) {
        StringBuilder registerContent = new StringBuilder("Register File:\n");
        for (int i = 0; i < registers.length; i++) {
            if (i == changedRegister) {
                registerContent.append(String.format("x%d: %08x <---\n", i, registers[i]));
            } else {
                registerContent.append(String.format("x%d: %08x\n", i, registers[i]));
            }
        }
        registerArea.setText(registerContent.toString());

        StringBuilder instructionContent = new StringBuilder("Instruction Fields:\n");
        instructionContent.append(String.format("Opcode: %02x\n", opcode));
        instructionContent.append(String.format("rd: %d\n", rd));
        instructionContent.append(String.format("funct3: %d\n", funct3));
        instructionContent.append(String.format("rs1: %d\n", rs1));
        instructionContent.append(String.format("rs2: %d\n", rs2));
        instructionContent.append(String.format("funct7: %02x\n", funct7));
        instructionContent.append(String.format("imm: %08x\n", imm));
        instructionArea.setText(instructionContent.toString());

        fetchedInstructionArea.setText(String.format("Currently Executed Instruction:\nPC: %08x\nInstruction: %08x\n", pc, instruction));
    }

    // Load Program into Instruction Memory
    public void loadProgram(int[] instructions) {
        for (int i = 0; i < instructions.length; i++) {
            instructionMemory.put(i * 4, instructions[i]); // Instructions are word-aligned
        }
    }

    // Fetch Stage
    private int fetch() {
        return instructionMemory.getOrDefault(pc, 0);
    }

    // Decode Stage
    private void decode(int instruction) {
        opcode = instruction & 0x7F;
        rd = (instruction >> 7) & 0x1F;
        funct3 = (instruction >> 12) & 0x7;
        rs1 = (instruction >> 15) & 0x1F;
        rs2 = (instruction >> 20) & 0x1F;
        funct7 = (instruction >> 25) & 0x7F;
        imm = extractImmediate(instruction, opcode);
    }

    private int extractImmediate(int instruction, int opcode) {
        switch (opcode) {
            case OP_LUI:
            case OP_AUIPC:
                return instruction & 0xFFFFF000;
            case OP_JAL:
                return ((instruction >> 12) & 0xFF) | ((instruction >> 20) & 0x1) << 11 |
                        ((instruction >> 21) & 0x3FF) << 1 | ((instruction >> 31) & 0x1) << 20;
            case OP_BRANCH:
                return ((instruction >> 8) & 0xF) << 1 | ((instruction >> 25) & 0x3F) << 5 |
                        ((instruction >> 7) & 0x1) << 11 | ((instruction >> 31) & 0x1) << 12;
            case OP_LOAD:
            case OP_STORE:
            case OP_IMM:
                return (instruction >> 20);
            default:
                return 0;
        }
    }

    // Execute Stage
    private int execute() {
        switch (opcode) {
            case OP_LUI:
                return imm;
            case OP_AUIPC:
                return pc + imm;
            case OP_JAL:
                return pc + 4;
            case OP_JALR:
                return (registers[rs1] + imm) & ~1;
            case OP_BRANCH:
                return handleBranch();
            case OP_LOAD:
            case OP_STORE:
            case OP_IMM:
            case OP_REG:
                return handleALU();
            default:
                return 0;
        }
    }

    private int handleBranch() {
        int comparison = 0;
        switch (funct3) {
            case 0x0: // BEQ
                comparison = (registers[rs1] == registers[rs2]) ? imm : 0;
                break;
            case 0x1: // BNE
                comparison = (registers[rs1] != registers[rs2]) ? imm : 0;
                break;
            case 0x4: // BLT
                comparison = (registers[rs1] < registers[rs2]) ? imm : 0;
                break;
            case 0x5: // BGE
                comparison = (registers[rs1] >= registers[rs2]) ? imm : 0;
                break;
        }
        return comparison;
    }

    private int handleALU() {
        switch (funct3) {
            case 0x0: // ADD or SUB
                return funct7 == 0 ? registers[rs1] + imm : registers[rs1] - imm;
            case 0x1: // SLL
                return registers[rs1] << rs2;
            case 0x2: // SLT
                return registers[rs1] < imm ? 1 : 0;
            case 0x4: // XOR
                return registers[rs1] ^ imm;
            case 0x5: // SRL or SRA
                return funct7 == 0 ? registers[rs1] >>> rs2 : registers[rs1] >> rs2;
            case 0x6: // OR
                return registers[rs1] | imm;
            case 0x7: // AND
                return registers[rs1] & imm;
            default:
                return 0;
        }
    }

    // Memory Access Stage
    private int memoryAccess(int aluResult) {
        if (opcode == OP_LOAD) {
            return memory[aluResult];
        } else if (opcode == OP_STORE) {
            memory[aluResult] = registers[rs2];
        }
        return aluResult;
    }

    // Write Back Stage
    private void writeBack(int result) {
        if (opcode != OP_STORE) {
            registers[rd] = result;
        }
    }

    // Simulate Instruction Execution
    private void executeNextInstruction(double speed) {
        if (!instructionMemory.containsKey(pc)) {
            JOptionPane.showMessageDialog(frame, "Simulation Complete.");
            return;
        }

        int instruction = fetch();
        decode(instruction);
        int aluResult = execute();
        int memResult = memoryAccess(aluResult);
        writeBack(memResult);

        updateGUI(rd, instruction);

        pc += 4;

        if (speed > 0) {
            try {
                Thread.sleep((long) (1000 / speed));
                executeNextInstruction(speed);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static void main(String[] args) {
        CPUSimulator simulator = new CPUSimulator();

        // Example program: LUI x1, 0x12345
        int program[] = {
                0x00500093,
                0x001080b3,
                0x40108133,
                0x0020c233,
                0x0020a2b3,
                0x0020e333,
                0x002090b3,
                0x00109133,
                0x401091b3,
                0x12345037,
                0x6789a017,
                0x008006ef,
                0x00408067,
                0x00410063,
                0x00414063,
                0x00418063,
                0x0041c063,
                0x0041e063,
                0x0041f063,
                0x0080a103,
                0x0080af23
        };

        simulator.loadProgram(program);
    }
}
