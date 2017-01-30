package protomoto;

import java.util.List;

public interface InstructionEmitter {
    void prepare(MetaFrame metaFrame);
    void emit(MetaFrame metaFrame, List<Instruction> instructions);
}
