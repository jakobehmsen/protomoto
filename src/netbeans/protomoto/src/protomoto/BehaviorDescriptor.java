package protomoto;

public class BehaviorDescriptor {
    private Instruction[] instructions;
    private int arity;

    public BehaviorDescriptor(Instruction[] instructions, int arity) {
        this.instructions = instructions;
        this.arity = arity;
    }

    public BehaviorCell createBehavior(Cell frameProto) {
        return new BehaviorCell(frameProto, instructions, arity);
    }
}
