package protomoto;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ASTMappers {
    public static <T extends Cell> ASTMapper constExpression(Function<T, Instruction> constInstructionFunc) {
        return new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                if(asExpression) {
                    Instruction constInstruction = constInstructionFunc.apply((T) ast.get(1));
                
                    emitters.add(InstructionEmitters.single(constInstruction));
                }
            }
        };
    }
    
    public static ASTMapper binaryExpression(Instruction instruction) {
        return new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                Cell lhs = (Cell) ast.get(1);
                Cell rhs = (Cell) ast.get(2);
                
                mapExpression.accept(lhs);
                mapExpression.accept(rhs);
                
                emitters.add(InstructionEmitters.single(instruction));
                
                if(!asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.pop()));
                }
            }
        };
    }
    
    public static ASTMapper nnaryExpression(Instruction instruction, int arity) {
        return new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                for(int i = ast.length() - arity; i < ast.length(); i++) {
                    Cell cell = (Cell) ast.get(i);
                    mapExpression.accept(cell);
                }
                
                emitters.add(InstructionEmitters.single(instruction));
                
                if(!asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.pop()));
                }
            }
        };
    }
}
