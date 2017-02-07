package protomoto;

import java.util.Arrays;

public class ArrayCell extends AbstractCell {
    private final Cell[] items;

    public ArrayCell(Cell[] items) {
        this.items = items;
    }
    
    @Override
    public BehaviorCell resolveEvaluateBehavior() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Cell resolveProto(Environment environment) {
        return environment.getArrayProto();
    }

    public Cell get(int index) {
        return items[index];
    }

    public void set(int index, Cell cell) {
        items[index] = cell;
    }

    @Override
    public String toString() {
        return Arrays.toString(items);
    }

    public int length() {
        return items.length;
    }
}
