package protomoto;

import java.util.Hashtable;

public abstract class AbstractProtoCell implements Cell {
    private Hashtable<Integer, AbstractProtoCell> behavior = new Hashtable<>();
    
    public abstract BehaviorProtoCell resolveEvaluateBehavior();
    
    public void put(int symbolCode, AbstractProtoCell c) {
        behavior.put(symbolCode, c);
    }
    
    public BehaviorProtoCell resolveBehavior(ProtoEnvironment environment, int symbolCode) {
        AbstractProtoCell c = behavior.get(symbolCode);
        if(c != null)
            return c.resolveEvaluateBehavior();
        AbstractProtoCell proto = resolveProto(environment);
        if(proto != null)
            return proto.resolveBehavior(environment, symbolCode);
        return null; // How to handle?
    }
    
    protected abstract AbstractProtoCell resolveProto(ProtoEnvironment environment);
}
