package protomoto;

public interface Cell {
    Cell resolveProto(Environment environment);
    BehaviorProtoCell resolveBehavior(Environment environment, int symbolCode);
    BehaviorProtoCell resolveEvaluateBehavior();
}
