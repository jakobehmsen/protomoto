package protomoto.cell;

import protomoto.runtime.Instruction;
import protomoto.cell.BehaviorCell;
import protomoto.cell.Cell;

public class BehaviorDescriptor {
    private Instruction[] instructions;
    private int arity;
    private String[] parameters;

    public BehaviorDescriptor(Instruction[] instructions, int arity, String[] parameters) {
        this.instructions = instructions;
        this.arity = arity;
        this.parameters = parameters;
    }

    public BehaviorCell createBehavior(Cell frameProto) {
        return new BehaviorCell(frameProto, instructions, arity, parameters);
    }
}
