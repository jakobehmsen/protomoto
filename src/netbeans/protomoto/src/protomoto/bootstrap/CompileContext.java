package protomoto.bootstrap;

import java.util.HashSet;

public class CompileContext {
    private HashSet<String> locals = new HashSet<>();
    
    public void declare(String name) {
        locals.add(name);
    }
    
    public boolean isDeclared(String name) {
        return locals.contains(name);
    }
}
