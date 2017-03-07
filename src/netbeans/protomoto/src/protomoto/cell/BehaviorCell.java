package protomoto.cell;

import protomoto.runtime.Evaluator;
import protomoto.runtime.Frame;
import protomoto.runtime.Instruction;
import protomoto.cell.AbstractCell;
import protomoto.cell.Cell;

public class BehaviorCell extends AbstractCell {
    private Cell frameProto;
    private Instruction[] instructions;
    private int variableCount;

    public BehaviorCell(Cell frameProto, Instruction[] instructions, int variableCount) {
        this.frameProto = frameProto;
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
        Frame frame = new Frame(frameProto, evaluator, sender, instructions);
        
        frame.push(selfAndArguments[0]);
        
        if(arity > 0) {
            frame.pushFrom(1, arity, selfAndArguments);
        }
        
        frame.allocate(variableCount);
        
        return frame;
    }

    @Override
    public Cell cloneCell() {
        return new BehaviorCell(frameProto, instructions, variableCount);
    }
}
