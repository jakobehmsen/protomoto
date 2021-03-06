package protomoto.cell;

import protomoto.cell.Cell;

public class IntegerCell extends AbstractCell {
    private final int value;
    
    public IntegerCell(int value) {
        this.value = value;
    }
    
    @Override
    public Cell resolveProto(Environment environment) {
        return environment.getIntegerProto();
    }

    @Override
    public String toString() {
        return "" + value;
    }

    public int getIntValue() {
        return value;
    }

    @Override
    public BehaviorCell resolveEvaluateBehavior() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Cell cloneCell() {
        return new IntegerCell(value);
    }
}
