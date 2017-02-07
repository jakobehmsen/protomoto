package protomoto.patterns;

import java.util.Hashtable;
import java.util.Map;
import protomoto.ast.ASTCell;
import protomoto.ast.ASTList;

public class ListPatterns {
    public static ListPattern capture(String name, ListPattern pattern) {
        return new ListPattern() {
            @Override
            public boolean matches(ListStream items, Map<String, ASTCell> captures, ListPattern next) {
                ListStreamPosition position = items.position();
                if(pattern.matches(items, captures, next)) {
                    ASTCell[] consumed = position.consumed();
                    captures.put(name, new ASTList(consumed));
                    return true;
                }
                
                return false;
            }
        };
    }
    
    public static ListPattern single(Pattern pattern) {
        return new ListPattern() {
            @Override
            public boolean matches(ListStream items, Map<String, ASTCell> captures, ListPattern next) {
                if(items.remaining() > 0 && pattern.matches(items.peek(), captures)) {
                    items.consume();
                    return true;
                }
                
                return false;
            }
        };
    }
    
    public static ListPattern lazy(ListPattern pattern) {
        return new ListPattern() {
            @Override
            public boolean matches(ListStream items, Map<String, ASTCell> captures, ListPattern next) {
                // Next should capture into dummy map
                Hashtable<String, ASTCell> dummyCaptures = new Hashtable<>();
                ListStream dummyItems = items.sublist();
                while(next == null || !next.matches(dummyItems, dummyCaptures, null)) {
                    if(!pattern.matches(items, captures, null)) {
                        break;
                    }
                    
                    dummyItems = items.sublist();
                }
                
                return true;
            }
        };
    }
    
    public static ListPattern sequence(ListPattern... patterns) {
        return new ListPattern() {
            @Override
            public boolean matches(ListStream items, Map<String, ASTCell> captures, ListPattern next) {
                for (int i = 0; i < patterns.length; i++) {
                    ListPattern pattern = patterns[i];
                    ListPattern nextInSequence = i + 1 < patterns.length ? patterns[i + 1] : null; 
                    if (!pattern.matches(items, captures, nextInSequence)) {
                        return false;
                    }
                }
                
                return true;
            }
        };
    }
}
