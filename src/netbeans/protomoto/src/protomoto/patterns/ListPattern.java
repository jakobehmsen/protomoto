package protomoto.patterns;

import java.util.Map;
import protomoto.ast.ASTCell;

public interface ListPattern {
    boolean matches(ListStream items, Map<String, ASTCell> captures, ListPattern next);
}
