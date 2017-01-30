package protomoto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InstructionMapper {
    public static Instruction[] fromAST(MetaFrame metaFrame, Cell ast, Map<Cell, ASTMapper> mappers, InstructionEmitter endEmitter) {
        ArrayList<InstructionEmitter> emitters = new ArrayList<>();
        
        fromAST(ast, emitters, true, mappers);
        emitters.add(endEmitter);
        
        emitters.forEach(e -> e.prepare(metaFrame));
        
        ArrayList<Instruction> instructions = new ArrayList<>();
        
        emitters.forEach(e -> e.emit(metaFrame, instructions));
        
        return instructions.toArray(new Instruction[instructions.size()]);
    }
    
    public static void fromAST(Cell ast, List<InstructionEmitter> emitters, boolean asExpression, Map<Cell, ASTMapper> mappers) {
        if(ast instanceof ArrayCell) {
            ArrayCell arrayAst = (ArrayCell)ast;
            if(arrayAst.items.length > 0 && arrayAst.items[0] instanceof StringCell) {
                Cell astType = arrayAst.items[0];
                ASTMapper mapper = mappers.get(astType);
                
                mapper.translate(arrayAst, emitters, asExpression, child -> fromAST(child, emitters, true, mappers));
            } else {
                // What if empty?
                
                for(int i = 0; i < arrayAst.items.length; i++) {
                    boolean childAsExpression = i == arrayAst.items.length - 1;
                    fromAST(arrayAst.items[i], emitters, childAsExpression, mappers);
                }
            }
        } else {
            // How to handle leafs?
        }
    }
}
