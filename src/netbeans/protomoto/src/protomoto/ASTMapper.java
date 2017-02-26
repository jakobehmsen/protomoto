package protomoto;

import java.util.List;
import java.util.function.Consumer;

public interface ASTMapper {
    void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector);
}
