package protomoto;

public class BehaviorProtoCell extends AbstractCell {
    private Instruction[] instructions;
    private int variableCount;

    public BehaviorProtoCell(Instruction[] instructions, int variableCount) {
        this.instructions = instructions;
        this.variableCount = variableCount;
    }

    @Override
    public BehaviorProtoCell resolveEvaluateBehavior() {
        return this;
    }

    @Override
    public Cell resolveProto(Environment environment) {
        return environment.getBehaviorProto();
    }
    
    public Frame createSendFrame(Evaluator evaluator, Frame sender, Cell self, int arity) {
        Frame frame = new Frame(evaluator, sender, instructions);
        
        frame.push(self);
        frame.allocate(variableCount);
        
        if(arity > 0) {
            sender.popInto(arity, frame);
        }
        
        return frame;
    }
}
