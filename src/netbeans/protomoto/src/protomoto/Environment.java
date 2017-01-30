package protomoto;

public interface Environment {
    Evaluator createEvaluator(Cell receiver, Cell ast);

    Cell createInteger(int i);
}
