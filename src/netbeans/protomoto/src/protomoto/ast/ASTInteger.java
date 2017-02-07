package protomoto.ast;

public class ASTInteger implements ASTCell {
    private int i;

    @Override
    public <T> T accept(ASTCellVisitor<T> visitor) {
        return visitor.visitInteger(i);
    }
}
