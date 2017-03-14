package protomoto.runtime;

public class CallSiteBinding {
    private int index;
    private Instruction[] instructions;
    
    public CallSiteBinding(int index, Instruction[] instructions) {
        this.index = index;
        this.instructions = instructions;
    }
    
    public void uncache(int tag) {
        instructions[index] = ((CallSiteInstruction)instructions[index]).uncache(tag);
    }
}
