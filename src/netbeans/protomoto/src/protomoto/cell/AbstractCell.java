package protomoto.cell;

import java.util.Hashtable;
import protomoto.runtime.Evaluator;

public abstract class AbstractCell extends Cell {
    private Hashtable<Integer, Cell> behavior = new Hashtable<>();
    
    private static int nextTag;
    
    public static int nextTag() {
        return nextTag++;
    }

    public void newTag() {
        setTag(nextTag());
    }
    
    @Override
    public void put(int symbolCode, Cell c) {
        // Should a new tag be assigned here?
        // Is the type changed here?
        behavior.put(symbolCode, c);
    }

    @Override
    public Cell get(Environment environment, int symbolCode) {
        Cell c = behavior.get(symbolCode);
        if(c != null)
            return c;
        Cell proto = resolveProto(environment);
        if(proto != null)
            return proto.get(environment, symbolCode);
        return null; // How to handle?
    }
    
    @Override
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
