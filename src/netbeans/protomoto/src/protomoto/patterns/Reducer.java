package protomoto.patterns;

import protomoto.ast.ASTCell;

public interface Reducer {
    ASTCell reduce(ASTCell cell);
}
