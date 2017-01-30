package protomoto;

public interface ArrayCell extends Cell {
    Cell get(int index);
    void set(int index, Cell cell);
}
