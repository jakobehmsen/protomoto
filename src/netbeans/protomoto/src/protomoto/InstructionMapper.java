package protomoto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class InstructionMapper {
    public interface ASTArrayMapper {
        void translate(ArrayProtoCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<AbstractProtoCell> translateChild);
    }
    
    public interface ASTLeafMapper {
        void translate(AbstractProtoCell ast, List<InstructionEmitter> emitters);
    }
    
    public static Instruction[] fromAST(MetaFrame metaFrame, AbstractProtoCell ast, Map<AbstractProtoCell, ASTArrayMapper> mappers, ASTLeafMapper leafMapper, InstructionEmitter endEmitter) {
        ArrayList<InstructionEmitter> emitters = new ArrayList<>();
        
        fromAST(ast, emitters, true, mappers, leafMapper);
        emitters.add(endEmitter);
        
        emitters.forEach(e -> e.prepare(metaFrame));
        
        ArrayList<Instruction> instructions = new ArrayList<>();
        
        emitters.forEach(e -> e.emit(metaFrame, instructions));
        
        return instructions.toArray(new Instruction[instructions.size()]);
    }
    
    public static void fromAST(AbstractProtoCell ast, List<InstructionEmitter> emitters, boolean asExpression, Map<AbstractProtoCell, ASTArrayMapper> mappers, ASTLeafMapper leafMapper) {
        if(ast instanceof ArrayProtoCell) {
            ArrayProtoCell arrayAst = (ArrayProtoCell)ast;
            if(arrayAst.items.length > 0 && arrayAst.items[0] instanceof StringProtoCell) {
                AbstractProtoCell astType = arrayAst.items[0];
                ASTArrayMapper mapper = mappers.get(astType);
                
                mapper.translate(arrayAst, emitters, asExpression, child -> fromAST(child, emitters, true, mappers, leafMapper));
            } else {
                // What if empty?
                
                for(int i = 0; i < arrayAst.items.length; i++) {
                    boolean childAsExpression = i == arrayAst.items.length - 1;
                    fromAST(arrayAst.items[i], emitters, childAsExpression, mappers, leafMapper);
                }
            }
        } else {
            leafMapper.translate(ast, emitters);
        }
    }
}
