package protomoto.patterns;

import java.util.Map;
import protomoto.ast.ASTCell;

public interface Action {
    ASTCell process(Reducer reducer, Map<String, ASTCell> captured);
}
