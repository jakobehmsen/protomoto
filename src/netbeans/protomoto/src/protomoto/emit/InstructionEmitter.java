package protomoto.emit;

import java.util.List;
import protomoto.runtime.Instruction;

public interface InstructionEmitter {
    void prepare(MetaFrame metaFrame);
    void emit(MetaFrame metaFrame, List<Instruction> instructions);
}
