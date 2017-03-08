package protomoto.bootstrap.lang;

import protomoto.cell.Cell;

public interface Compiler {
    default Cell compile() {
        return compile(new CompileContext());
    }
    
    default String modifyId(String id) {return id;}
    
    Cell compile(CompileContext ctx);
}
