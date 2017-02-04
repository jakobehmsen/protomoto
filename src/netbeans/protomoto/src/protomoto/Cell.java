package protomoto;

public interface Cell {
    Cell resolveProto(Environment environment);
    BehaviorCell resolveBehavior(Environment environment, int symbolCode);
    BehaviorCell resolveEvaluateBehavior();
    void put(int symbolCode, Cell c);
    Cell get(int symbolCode);
}
