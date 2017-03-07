package protomoto.bootstrap.lang;

import protomoto.cell.Cell;

public interface Compiler {
    default Cell compile() {
        return compile(new CompileContext());
    }
    
    Cell compile(CompileContext ctx);
}
