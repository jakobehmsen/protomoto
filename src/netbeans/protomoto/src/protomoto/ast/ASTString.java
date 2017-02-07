package protomoto.ast;

public class ASTString implements ASTCell {
    private String string;

    public ASTString(String string) {
        this.string = string;
    }

    @Override
    public <T> T accept(ASTCellVisitor<T> visitor) {
        return visitor.visitString(string);
    }
}
