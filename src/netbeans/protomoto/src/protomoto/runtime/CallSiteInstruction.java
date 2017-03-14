package protomoto.runtime;

import protomoto.cell.BehaviorCell;

public interface CallSiteInstruction extends Instruction {
    Instruction uncache(int tag);
}
