package protomoto.ast;

import java.util.List;

public interface ASTCell {
    <T> T accept(ASTCellVisitor<T> visitor);
}
