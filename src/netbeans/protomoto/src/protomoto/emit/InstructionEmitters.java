package protomoto.emit;

import java.util.List;
import protomoto.runtime.Instruction;

public class InstructionEmitters {
    public static InstructionEmitter single(Instruction instruction) {
        return new InstructionEmitter() {
            @Override
            public void prepare(MetaFrame metaFrame) { }

            @Override
            public void emit(MetaFrame metaFrame, List<Instruction> instructions) {
                instructions.add(instruction);
            }
        };
    }
}
