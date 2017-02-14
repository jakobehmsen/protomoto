package protomoto;

public class BehaviorCell extends AbstractCell {
    private Instruction[] instructions;
    private int variableCount;

    public BehaviorCell(Instruction[] instructions, int variableCount) {
        this.instructions = instructions;
        this.variableCount = variableCount;
    }

    @Override
    public BehaviorCell resolveEvaluateBehavior() {
        return this;
    }

    @Override
    public Cell resolveProto(Environment environment) {
        return environment.getBehaviorProto();
    }
    
    public Frame createSendFrame(Evaluator evaluator, Frame sender, int arity, Cell[] selfAndArguments) {
        Frame frame = new Frame(evaluator, sender, instructions);
        
        frame.push(selfAndArguments[0]);
        
        if(arity > 0) {
            frame.pushFrom(1, arity, selfAndArguments);
        }
        
        frame.allocate(variableCount);
        
        return frame;
    }

    @Override
    public Cell cloneCell() {
        return new BehaviorCell(instructions, variableCount);
    }
}
