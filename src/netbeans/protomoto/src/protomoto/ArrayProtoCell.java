package protomoto;

import java.util.Arrays;

public class ArrayProtoCell extends AbstractProtoCell implements ArrayCell {
    public final AbstractProtoCell[] items;

    public ArrayProtoCell(AbstractProtoCell[] items) {
        this.items = items;
    }
    
    @Override
    public BehaviorProtoCell resolveEvaluateBehavior() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected AbstractProtoCell resolveProto(ProtoEnvironment environment) {
        return environment.getArrayProto();
    }

    @Override
    public Cell get(int index) {
        return items[index];
    }

    @Override
    public void set(int index, Cell cell) {
        items[index] = (AbstractProtoCell) cell;
    }

    @Override
    public String toString() {
        return Arrays.toString(items);
    }
}
