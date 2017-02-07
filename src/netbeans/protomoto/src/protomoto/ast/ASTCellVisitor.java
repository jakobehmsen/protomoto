package protomoto.ast;

public interface ASTCellVisitor<T> {
    T visitList(ASTCell[] items);
    T visitString(String string);
    T visitInteger(int i);
}
