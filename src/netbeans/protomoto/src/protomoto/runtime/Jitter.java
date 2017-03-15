package protomoto.runtime;

import java.util.Stack;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import protomoto.cell.Cell;
import protomoto.cell.Environment;
import protomoto.cell.IntegerCell;

public class Jitter {
    private Stack<CellDescriptor> stack = new Stack<>();
    private GeneratorAdapter adapter;

    public Jitter(GeneratorAdapter adapter) {
        this.adapter = adapter;
    }

    public void pushi(int value) {
        adapter.push(value);
        stack.push(new CellDescriptor() {
            @Override
            public void emitBoxing(GeneratorAdapter adapter) {
                //environment.createInteger(value)
                adapter.loadArg(0); // Load environment
                adapter.swap();
                adapter.invokeVirtual(Type.getType(Environment.class), 
                    new Method("createInteger", Type.getType(IntegerCell.class), new Type[]{Type.INT_TYPE}));
            }
        });
    }

    public void finish(int returnCode) {
        stack.peek().emitBoxing(adapter);
        adapter.returnValue();
    } 
}
