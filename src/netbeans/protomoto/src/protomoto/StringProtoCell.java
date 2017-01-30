package protomoto;

import java.util.Objects;

public class StringProtoCell extends AbstractProtoCell {
    public final String string;

    public StringProtoCell(String string) {
        this.string = string;
    }

    @Override
    public BehaviorProtoCell resolveEvaluateBehavior() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected AbstractProtoCell resolveProto(ProtoEnvironment environment) {
        return environment.getStringProto();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StringProtoCell && this.string.equals(((StringProtoCell)obj).string);
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
