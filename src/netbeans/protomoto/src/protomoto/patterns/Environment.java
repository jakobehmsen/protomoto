package protomoto.patterns;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Environment {
    private Hashtable<String, List<PatternAction>> definitions;
    
    public void put(String name, Pattern pattern, Action action) {
        List<PatternAction> patternActions = definitions.computeIfAbsent(name, k -> new ArrayList<>());
        patternActions.add(new PatternAction(pattern, action));
    }

    public Pattern match(String name) {
        return null;
    }
    
    
}
