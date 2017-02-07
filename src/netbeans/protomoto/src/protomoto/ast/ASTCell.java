package protomoto.ast;

public interface ASTCell {
    <T> T accept(ASTCellVisitor<T> visitor);
}
