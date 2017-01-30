package protomoto;

public interface Evaluator {
    void proceed();
    boolean isFinished();
    Cell getResponse();
}
