package protomoto;

import java.util.List;

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
