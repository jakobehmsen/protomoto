package protomoto.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.TableSwitchGenerator;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import protomoto.cell.Cell;
import protomoto.cell.Environment;
import protomoto.cell.Environment.CallSiteContainer;
import protomoto.cell.IntegerCell;
import protomoto.cell.StringCell;

public class Jitter {
    //private Stack<CellDescriptor> stack = new Stack<>();
    private HotspotStrategy hotspotStrategy;
    private ClassNode classNode;
    private GeneratorAdapter evalAdapter;
    private GeneratorAdapter initAdapter;
    private GeneratorAdapter setAdapter;

    public Jitter(HotspotStrategy hotspotStrategy) {
        this.hotspotStrategy = hotspotStrategy;
        
        classNode = new ClassNode();
        classNode.version = Opcodes.V1_8;
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.signature="LGenerated;";
        classNode.name="Generated";
        classNode.superName="java/lang/Object";
        classNode.interfaces.add(Type.getType(hotspotStrategy.getHotspotInterface(0)).getInternalName());
        classNode.interfaces.add(Type.getType(CallSiteContainer.class).getInternalName());
        
        MethodNode evalMethodNode = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "evaluate",
            Type.getMethodDescriptor(Type.getType(Cell.class), new Type[]{Type.getType(Environment.class), Type.getType(Cell.class)}),
            null, 
            null);
        evalAdapter = new GeneratorAdapter(
            Opcodes.ACC_PUBLIC,
            new Method(evalMethodNode.name, evalMethodNode.desc),
            evalMethodNode);
        classNode.methods.add(evalMethodNode);
        
        MethodNode constructorNode = new MethodNode(
            Opcodes.ACC_PUBLIC, 
            "<init>", 
            Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{Type.getType(HotspotStrategy.class)}), 
            null, 
            null);
        classNode.methods.add(constructorNode);
        initAdapter = new GeneratorAdapter(Opcodes.ACC_PUBLIC, new Method(constructorNode.name, constructorNode.desc), constructorNode);
        initAdapter.loadThis();
        initAdapter.invokeConstructor(Type.getType(Object.class), new Method("<init>", "()V"));
        
        MethodNode setMethodNode = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "set",
            Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{Type.INT_TYPE, Type.getType(Object.class)}),
            null, 
            null);
        setAdapter = new GeneratorAdapter(
            Opcodes.ACC_PUBLIC,
            new Method(setMethodNode.name, setMethodNode.desc), 
            setMethodNode);
        classNode.methods.add(setMethodNode);
    }
    
    public Class<?> compileClass() {
        try {
            Class<?> c = new SingleClassLoader(classNode).loadClass("Generated");
            return c;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Jitter.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void pushi(int value) {
        evalAdapter.loadArg(0); // Load environment
        evalAdapter.push(value);
        evalAdapter.invokeVirtual(
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
        evalAdapter.returnValue();
    } 

    public void load(int index) {
        // index=0 means load self
        int offset = 1; // 0=environment, 1=self
        int offsetIndex = offset + index;
        //adapter.loadLocal(offsetIndex);
        evalAdapter.loadArg(offsetIndex);
    }

    public void setSlotPre(int symbolCode) {
        evalAdapter.push(symbolCode);
    }

    public void setSlot(int symbolCode) {
        evalAdapter.invokeVirtual(
            Type.getType(Cell.class), 
            new Method("put", Type.VOID_TYPE, new Type[]{Type.INT_TYPE, Type.getType(Cell.class)}));
    }

    public void dupX1() {
        evalAdapter.dupX1();
    }

    public void dupX2() {
        evalAdapter.dupX2();
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    public void emit(Instruction[] instructions) {
        for (Instruction instruction : instructions) {
            instruction.emit(this);
        }
        end();
    }

    public void pushs(String string) {
        evalAdapter.loadArg(0); // Load environment
        evalAdapter.push(string);
        evalAdapter.invokeVirtual(
            Type.getType(Environment.class), 
            new Method("createString", Type.getType(StringCell.class), new Type[]{Type.getType(String.class)}));
    }

    public void respond() {
        evalAdapter.returnValue();
    }

    public void pop() {
        evalAdapter.pop();
    }
    
    private class HotspotField {
        public int symbolCode;
        public int arity;
        public int id;

        public HotspotField(int symbolCode, int arity, int id) {
            this.symbolCode = symbolCode;
            this.arity = arity;
            this.id = id;
        }
    }
    
    private ArrayList<HotspotField> hotspotFields = new ArrayList<>();
    //private Hashtable<String, Integer> hotspotFieldNameToId = new Hashtable<>();
    
    private void declareHotspot(int symbolCode, int arity) {
        Class<?> hotspotClass = hotspotStrategy.getHotspotInterface(arity);
        String hotspotFieldName = getHotspotFieldName(symbolCode, arity);
        
        int hotspotId = hotspotFields.size();
        hotspotFields.add(new HotspotField(symbolCode, arity, hotspotId));
        classNode.fields.add(new FieldNode(
            Opcodes.ACC_PRIVATE, 
            hotspotFieldName, 
            Type.getDescriptor(hotspotClass), 
            null, 
            null
        ));
       //initAdapter.loadThis();
        initAdapter.loadArg(0); // Load hotspotStrategy
        // Load hotspot field id
        initAdapter.loadThis();
        initAdapter.push(hotspotId);
        initAdapter.push(symbolCode);
        initAdapter.push(arity);
        initAdapter.invokeInterface(
            Type.getType(HotspotStrategy.class), 
            new Method("bind", Type.VOID_TYPE, new Type[]{Type.getType(CallSiteContainer.class), Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE}));
        /*// Instead of calling newHotspot, bind should be called including the hotspot field id
        initAdapter.invokeInterface(
            Type.getType(HotspotStrategy.class), 
            new Method("newHotspot", Type.getType(Object.class), new Type[]{Type.INT_TYPE, Type.INT_TYPE}));*/
        /*initAdapter.checkCast(Type.getType(hotspotClass));
        // Don't put field, because it is done implicitly
        initAdapter.putField(Type.getType(classNode.signature), hotspotFieldName, Type.getType(hotspotClass));*/
    }
    
    private String getHotspotFieldName(int symbolCode, int arity) {
        return "hotspot_" + symbolCode + "_" + arity;
    }
    
    public void preSend(int symbolCode, int arity) {
        String hotspotFieldName = getHotspotFieldName(symbolCode, arity);
        boolean isDeclared = classNode.fields.stream().anyMatch(x -> ((FieldNode)x).name.equals(hotspotFieldName));
        if(!isDeclared) {
            declareHotspot(symbolCode, arity);
        }
        
        evalAdapter.loadThis();
        Class<?> hotspotClass = hotspotStrategy.getHotspotInterface(arity);
        // Could this be a static field?
        evalAdapter.getField(Type.getType(classNode.signature), hotspotFieldName, Type.getType(hotspotClass));
        evalAdapter.loadArg(0); // Load environment
    }

    public void send(int symbolCode, int arity) {
        Class<?> hotspotClass = hotspotStrategy.getHotspotInterface(arity);
        Type[] parameterTypes = new Type[2 + arity];
        parameterTypes[0] = Type.getType(Environment.class); // environment
        parameterTypes[1] = Type.getType(Cell.class); // self
        for(int i = 2; i < parameterTypes.length; i++) {
            parameterTypes[i] = Type.getType(Cell.class);
        }
        evalAdapter.invokeInterface(
            Type.getType(hotspotClass), 
            new Method("evaluate", Type.getType(Cell.class), parameterTypes));
    }
    
    public void end() {
        initAdapter.visitInsn(Opcodes.RETURN);
        
        setAdapter.loadArg(0); // Load hotspotId
        setAdapter.tableSwitch(hotspotFields.stream().mapToInt(f -> f.id).toArray(), new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                HotspotField hotspotField = hotspotFields.stream().filter(e -> e.id == key).findFirst().get();
                String hotspotFieldName = getHotspotFieldName(hotspotField.symbolCode, hotspotField.arity);
                Class<?> hotspotClass = hotspotStrategy.getHotspotInterface(hotspotField.arity);
                setAdapter.loadThis();
                setAdapter.loadArg(1); // Load hotspotValue
                setAdapter.checkCast(Type.getType(hotspotClass));
                setAdapter.putField(Type.getType(classNode.signature), hotspotFieldName, Type.getType(hotspotClass));
                setAdapter.visitInsn(Opcodes.RETURN);
            }

            @Override
            public void generateDefault() {
                setAdapter.visitInsn(Opcodes.RETURN);
            }
        });
    }
}
