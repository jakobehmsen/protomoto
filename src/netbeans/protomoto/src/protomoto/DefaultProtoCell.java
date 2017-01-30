package protomoto;

public class DefaultProtoCell extends AbstractProtoCell {
    private AbstractProtoCell proto;
    
    public DefaultProtoCell(AbstractProtoCell proto) {
        this.proto = proto;
    }

    @Override
    protected AbstractProtoCell resolveProto(ProtoEnvironment environment) {
        return proto;
    }

    @Override
    public BehaviorProtoCell resolveEvaluateBehavior() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
