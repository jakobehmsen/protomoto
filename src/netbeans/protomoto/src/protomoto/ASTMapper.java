package protomoto;

import java.util.List;
import java.util.function.Consumer;

public interface ASTMapper {

    void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild, Consumer<String> errorCollector);
    
}
