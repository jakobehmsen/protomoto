package protomoto.ast;

public class ASTInteger implements ASTCell {
    private int i;
    
    public ASTInteger(int i) {
        this.i = i;
    }

    @Override
    public <T> T accept(ASTCellVisitor<T> visitor) {
        return visitor.visitInteger(i);
    }
}
