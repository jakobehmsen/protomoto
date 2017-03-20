package protomoto.cell;

import protomoto.runtime.Evaluator;

public abstract class Cell {
    public int tag;
    
    public final int getTag() {
        return tag;
    }
    
    public final void setTag(int tag) {
        this.tag = tag;
    }
    
    public abstract Cell resolveProto(Environment environment);
    public abstract BehaviorCell resolveBehavior(Environment environment, int symbolCode);
    public abstract BehaviorCell resolveEvaluateBehavior();
    public abstract void put(int symbolCode, Cell c);
    public abstract Cell get(Environment environment, int symbolCode);
    public abstract Cell cloneCell();
}
