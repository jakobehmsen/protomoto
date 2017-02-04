package protomoto;

import java.util.Hashtable;

public abstract class AbstractCell implements Cell {
    private Hashtable<Integer, Cell> behavior = new Hashtable<>();
    
    @Override
    public void put(int symbolCode, Cell c) {
        behavior.put(symbolCode, c);
    }

    @Override
    public Cell get(int symbolCode) {
        return behavior.get(symbolCode);
    }
    
    public BehaviorCell resolveBehavior(Environment environment, int symbolCode) {
        Cell c = behavior.get(symbolCode);
        if(c != null)
            return c.resolveEvaluateBehavior();
        Cell proto = resolveProto(environment);
        if(proto != null)
            return proto.resolveBehavior(environment, symbolCode);
        return null; // How to handle?
    }
}
