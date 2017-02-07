package protomoto.patterns;

import java.util.Hashtable;
import java.util.List;
import protomoto.ast.ASTCell;
import protomoto.ast.ASTList;

public class RuleMap implements Reducer {
    private static class PatternAction {
        public Pattern pattern;
        public Action action;
    }
    
    private List<PatternAction> patternActions;
    
    public void bind(Pattern pattern, Action action) {
        
    }
    
    private ASTCell newList(ASTCell[] items) {
        return new ASTList(items);
    }
    
    @Override
    public ASTCell reduce(ASTCell cell) {
        Hashtable<String, ASTCell> captures = new Hashtable<>();
        
        Action action = patternActions.stream().filter(p -> {
            captures.clear();
            return p.pattern.matches(cell, captures);
        }).findFirst().get().action;
        
        return action.process(this, captures);
    }
}
