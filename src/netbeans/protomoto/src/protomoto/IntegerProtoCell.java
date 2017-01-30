package protomoto;

public class IntegerProtoCell extends AbstractProtoCell implements IntegerCell {
    private final int value;
    
    public IntegerProtoCell(int value) {
        this.value = value;
    }
    
    @Override
    protected AbstractProtoCell resolveProto(ProtoEnvironment environment) {
        return environment.getIntegerProto();
    }

    @Override
    public String toString() {
        return "" + value;
    }

    @Override
    public int getIntValue() {
        return value;
    }

    @Override
    public BehaviorProtoCell resolveEvaluateBehavior() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
