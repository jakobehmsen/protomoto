package protomoto.patterns;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import protomoto.ast.ASTCell;

public class RuleMap implements Reducer {
    private List<PatternAction> patternActions = new ArrayList<>();
    
    public void bind(Pattern pattern, Action action) {
        patternActions.add(new PatternAction(pattern, action));
    }
    
    @Override
    public ASTCell reduce(ASTCell cell) {
        while(true) {
            ASTCell c = cell;
            Hashtable<String, ASTCell> captures = new Hashtable<>();
            Optional<PatternAction> patternAction = patternActions.stream().filter(p -> {
                captures.clear();
                return p.pattern.matches(null, c, captures);
            }).findFirst();
            
            if(!patternAction.isPresent())
                break;
            
            cell = patternAction.get().action.process(this, captures);
        }
        
        return cell;
    }
}
