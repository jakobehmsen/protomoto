package protomoto.bootstrap;

import protomoto.ASTFactory;
import protomoto.Cell;

public interface Compiler {
    default Cell compile() {
        return compile(new CompileContext());
    }
    
    Cell compile(CompileContext ctx);
}
