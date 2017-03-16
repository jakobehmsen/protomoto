package protomoto.runtime;

import java.util.Stack;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import protomoto.cell.Cell;
import protomoto.cell.Environment;
import protomoto.cell.IntegerCell;

public class Jitter {
    //private Stack<CellDescriptor> stack = new Stack<>();
    private GeneratorAdapter adapter;

    public Jitter(GeneratorAdapter adapter) {
        this.adapter = adapter;
    }

    public void pushi(int value) {
        adapter.loadArg(0); // Load environment
        adapter.push(value);
        adapter.invokeVirtual(
            Type.getType(Environment.class), 
            new Method("createInteger", Type.getType(IntegerCell.class), new Type[]{Type.INT_TYPE}));
        
        /*stack.push(new CellDescriptor() {
            @Override
            public void emitBoxing(GeneratorAdapter adapter) {
            }
        });*/
    }

    public void finish(int returnCode) {
        //stack.peek().emitBoxing(adapter);
        adapter.returnValue();
    } 

    public void load(int index) {
        // index=0 means load self
        int offset = 1; // 0=environment, 1=self
        int offsetIndex = offset + index;
        //adapter.loadLocal(offsetIndex);
        adapter.loadArg(offsetIndex);
    }

    public void setSlotPre(int symbolCode) {
        adapter.push(symbolCode);
    }

    public void setSlot(int symbolCode) {
        adapter.invokeVirtual(
            Type.getType(Cell.class), 
            new Method("put", Type.VOID_TYPE, new Type[]{Type.INT_TYPE, Type.getType(Cell.class)}));
    }

    public void dupX1() {
        adapter.dupX1();
    }

    public void dupX2() {
        adapter.dupX2();
    }
}
