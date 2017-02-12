package protomoto.patterns;

import java.util.Map;
import protomoto.ast.ASTCell;

public interface Pattern {
    boolean matches(Environment environment, ASTCell cell, Map<String, ASTCell> captures);
}
