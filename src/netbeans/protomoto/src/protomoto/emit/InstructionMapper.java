package protomoto.emit;

import protomoto.cell.ArrayCell;
import protomoto.cell.StringCell;
import protomoto.cell.Cell;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import protomoto.runtime.Instruction;

public class InstructionMapper {
    public static Instruction[] fromAST(MetaFrame metaFrame, Cell ast, Map<Cell, ASTMapper> mappers, InstructionEmitter endEmitter, List<String> errors) {
        ArrayList<InstructionEmitter> emitters = new ArrayList<>();
        
        fromAST(ast, emitters, true, mappers, error -> errors.add(error));
        emitters.add(endEmitter);
        
        emitters.forEach(e -> e.prepare(metaFrame));
        
        if(errors.size() > 0) {
            return null;
        }
        
        ArrayList<Instruction> instructions = new ArrayList<>();
        
        emitters.forEach(e -> e.emit(metaFrame, instructions));
        
        return instructions.toArray(new Instruction[instructions.size()]);
    }
    
    public static void fromAST(Cell ast, List<InstructionEmitter> emitters, boolean asExpression, Map<Cell, ASTMapper> mappers, Consumer<String> errorCollector) {
        if(ast instanceof ArrayCell) {
            ArrayCell arrayAst = (ArrayCell)ast;
            if(arrayAst.length() > 0 && arrayAst.get(0) instanceof StringCell) {
                Cell astType = arrayAst.get(0);
                ASTMapper mapper = mappers.get(astType);
                
                if(mapper == null)
                    throw new IllegalArgumentException("Cannot find mapper for " + astType);
                
                mapper.translate(arrayAst, emitters, asExpression, 
                    child -> fromAST(child, emitters, true, mappers, errorCollector), 
                    child -> fromAST(child, emitters, false, mappers, errorCollector), 
                    errorCollector);
            } else {
                // What if empty?
                
                for(int i = 0; i < arrayAst.length(); i++) {
                    boolean childAsExpression = i == arrayAst.length() - 1;
                    fromAST(arrayAst.get(i), emitters, childAsExpression, mappers, errorCollector);
                }
            }
        } else {
            throw new IllegalArgumentException("Cannot map leaf " + ast);
        }
    }
}
