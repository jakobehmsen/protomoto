package protomoto.patterns;

import java.util.Map;
import protomoto.ast.ASTCell;

public interface Pattern {
    boolean matches(ASTCell cell, Map<String, ASTCell> captures);
}
