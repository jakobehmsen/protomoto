package protomoto;

public class StringCell extends AbstractCell {
    public final String string;

    public StringCell(String string) {
        this.string = string;
    }

    @Override
    public BehaviorCell resolveEvaluateBehavior() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Cell resolveProto(Environment environment) {
        return environment.getStringProto();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StringCell && this.string.equals(((StringCell)obj).string);
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public String toString() {
        return string;
    }
}
