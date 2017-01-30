package protomoto;

public class DefaultCell extends AbstractCell {
    private Cell proto;
    
    public DefaultCell(Cell proto) {
        this.proto = proto;
    }

    @Override
    public Cell resolveProto(Environment environment) {
        return proto;
    }

    @Override
    public BehaviorProtoCell resolveEvaluateBehavior() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
