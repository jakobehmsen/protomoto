package protomoto;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ASTMappers {
    public static <T extends Cell> ASTMapper constExpression(Function<T, Instruction> constInstructionFunc) {
        return new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild) {
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
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild) {
                Cell lhs = (Cell) ast.get(1);
                Cell rhs = (Cell) ast.get(2);
                
                translateChild.accept(lhs);
                translateChild.accept(rhs);
                
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
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild) {
                for(int i = ast.items.length - arity; i < ast.items.length - arity; i++) {
                    Cell cell = (Cell) ast.get(1);
                    translateChild.accept(cell);
                }
                
                emitters.add(InstructionEmitters.single(instruction));
                
                if(!asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.pop()));
                }
            }
        };
    }
}
