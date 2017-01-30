package protomoto;

public class BehaviorProtoCell extends AbstractProtoCell {
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
    protected AbstractProtoCell resolveProto(ProtoEnvironment environment) {
        return environment.getBehaviorProto();
    }
    
    public ProtoFrame createSendFrame(ProtoEvaluator evaluator, ProtoFrame sender, AbstractProtoCell self, int arity) {
        ProtoFrame frame = new ProtoFrame(evaluator, sender, instructions);
        
        frame.push(self);
        frame.allocate(variableCount);
        
        if(arity > 0) {
            sender.popInto(arity, frame);
        }
        
        return frame;
    }
}
