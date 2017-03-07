package protomoto.cell;

public interface Cell {
    Cell resolveProto(Environment environment);
    BehaviorCell resolveBehavior(Environment environment, int symbolCode);
    BehaviorCell resolveEvaluateBehavior();
    void put(int symbolCode, Cell c);
    Cell get(Environment environment, int symbolCode);
    Cell cloneCell();
}
