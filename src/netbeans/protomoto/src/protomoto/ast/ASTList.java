package protomoto.ast;

public class ASTList implements ASTCell {
    public ASTCell[] items;

    public ASTList(ASTCell[] items) {
        this.items = items;
    }

    @Override
    public <T> T accept(ASTCellVisitor<T> visitor) {
        return visitor.visitList(items);
    }
}
