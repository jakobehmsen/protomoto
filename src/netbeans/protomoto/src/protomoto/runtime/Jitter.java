package protomoto.runtime;

import java.util.ArrayList;
import java.util.Hashtable;
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

    public Jitter(HotspotStrategy hotspotStrategy, int arity) {
        this.hotspotStrategy = hotspotStrategy;
        
        classNode = new ClassNode();
        classNode.version = Opcodes.V1_8;
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.signature="LGenerated;";
        classNode.name="Generated";
        classNode.superName="java/lang/Object";
        classNode.interfaces.add(Type.getType(hotspotStrategy.getHotspotInterface(arity)).getInternalName());
        classNode.interfaces.add(Type.getType(CallSiteContainer.class).getInternalName());
        
        Type[] parameterTypes = new Type[2 + arity];
        parameterTypes[0] = Type.getType(Environment.class);
        parameterTypes[1] = Type.getType(Cell.class);
        for(int i = 2; i < parameterTypes.length; i++) {
            parameterTypes[i] = Type.getType(Cell.class);
        }
        
        MethodNode evalMethodNode = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "evaluate",
            Type.getMethodDescriptor(Type.getType(Cell.class), parameterTypes),
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
    }
    
    public Class<?> compileClass() {
        try {
            Class<?> c = new SingleClassLoader(hotspotStrategy.getClassLoader(), classNode).loadClass("Generated");
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
        evalAdapter.loadArg(offsetIndex);
    }
    
    private class PropertyGetHotspotField {
        public int symbolCode;
        public int id;

        public PropertyGetHotspotField(int symbolCode, int id) {
            this.symbolCode = symbolCode;
            this.id = id;
        }
    }
    
    private ArrayList<PropertyGetHotspotField> propertyGetHotspotFieldHotspotFields = new ArrayList<>();
    
    private void declarePropertyGetHotspot(int symbolCode) {
        Class<?> hotspotClass = PropertyGetHotspot.class;
        String hotspotFieldName = getPropertyGetHotspotFieldName(symbolCode);
        
        int hotspotId = propertyGetHotspotFieldHotspotFields.size();
        propertyGetHotspotFieldHotspotFields.add(new PropertyGetHotspotField(symbolCode, hotspotId));
        classNode.fields.add(new FieldNode(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, 
            hotspotFieldName, 
            Type.getDescriptor(hotspotClass), 
            null, 
            null
        ));
        
        // TODO: Continue from here
        initAdapter.loadArg(0); // Load hotspotStrategy
        // Load hotspot field id
        initAdapter.loadThis();
        initAdapter.push(hotspotId);
        initAdapter.push(symbolCode);
        initAdapter.invokeInterface(
            Type.getType(HotspotStrategy.class), 
            new Method("bindGet", Type.VOID_TYPE, new Type[]{Type.getType(CallSiteContainer.class), Type.INT_TYPE, Type.INT_TYPE}));
    }
    
    private String getPropertyGetHotspotFieldName(int symbolCode) {
        return "hotspot_get_" + symbolCode;
    }
    
    public void getSlot(int symbolCode) {
        
    }

    public void setSlotPre(int symbolCode) {
        evalAdapter.push(symbolCode);
    }

    public void setSlot(int symbolCode) {
        // How to do this?
        // Different kinds of properties?
        // Some properties hold behavior; some hold references to other cells.
        // Should properties be declared before they are used?
        
        // What about reading slots?
        
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
    
    private Hashtable<String, Integer> varNameToIndex = new Hashtable<>();

    public void declareVar(String name) {
        int index = evalAdapter.newLocal(Type.getType(Cell.class));
        varNameToIndex.put(name, index);
    }

    public void storeVar(String name) {
        int index = varNameToIndex.get(name);
        evalAdapter.storeLocal(index);
    }

    public void loadVar(String name) {
        int index = varNameToIndex.get(name);
        evalAdapter.loadLocal(index);
    }

    public void loadArg(int index) {
        evalAdapter.loadArg(2 + index);
    }
    
    private class MessageSendHotspotField {
        public int symbolCode;
        public int arity;
        public int id;

        public MessageSendHotspotField(int symbolCode, int arity, int id) {
            this.symbolCode = symbolCode;
            this.arity = arity;
            this.id = id;
        }
    }
    
    private ArrayList<MessageSendHotspotField> messageSendHotspotFields = new ArrayList<>();
    
    private void declareMessageSendHotspot(int symbolCode, int arity) {
        Class<?> hotspotClass = hotspotStrategy.getHotspotInterface(arity);
        String hotspotFieldName = getMessageSendHotspotFieldName(symbolCode, arity);
        
        int hotspotId = messageSendHotspotFields.size();
        messageSendHotspotFields.add(new MessageSendHotspotField(symbolCode, arity, hotspotId));
        classNode.fields.add(new FieldNode(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, 
            hotspotFieldName, 
            Type.getDescriptor(hotspotClass), 
            null, 
            null
        ));
        // Can method handles/call site/dynamic invoke be used instead?
        initAdapter.loadArg(0); // Load hotspotStrategy
        // Load hotspot field id
        initAdapter.loadThis();
        initAdapter.push(hotspotId);
        initAdapter.push(symbolCode);
        initAdapter.push(arity);
        initAdapter.invokeInterface(
            Type.getType(HotspotStrategy.class), 
            new Method("bind", Type.VOID_TYPE, new Type[]{Type.getType(CallSiteContainer.class), Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE}));
    }
    
    private String getMessageSendHotspotFieldName(int symbolCode, int arity) {
        return "hotspot_" + symbolCode + "_" + arity;
    }
    
    public void preSend(int symbolCode, int arity) {
        String hotspotFieldName = getMessageSendHotspotFieldName(symbolCode, arity);
        boolean isDeclared = classNode.fields.stream().anyMatch(x -> ((FieldNode)x).name.equals(hotspotFieldName));
        if(!isDeclared) {
            declareMessageSendHotspot(symbolCode, arity);
        }
        
        Class<?> hotspotClass = hotspotStrategy.getHotspotInterface(arity);
        evalAdapter.getStatic(Type.getType(classNode.signature), hotspotFieldName, Type.getType(hotspotClass));
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
        
        MethodNode setGetMethodNode = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "setGet",
            Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{Type.INT_TYPE, Type.getType(Object.class)}),
            null, 
            null);
        GeneratorAdapter setGetAdapter = new GeneratorAdapter(
            Opcodes.ACC_PUBLIC,
            new Method(setGetMethodNode.name, setGetMethodNode.desc), 
            setGetMethodNode);
        classNode.methods.add(setGetAdapter);
        
        setGetAdapter.loadArg(0); // Load hotspotId
        setGetAdapter.tableSwitch(propertyGetHotspotFieldHotspotFields.stream().mapToInt(f -> f.id).toArray(), new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                PropertyGetHotspotField hotspotField = propertyGetHotspotFieldHotspotFields.stream().filter(e -> e.id == key).findFirst().get();
                String hotspotFieldName = getPropertyGetHotspotFieldName(hotspotField.symbolCode);
                Class<?> hotspotClass = PropertyGetHotspot.class;
                setGetAdapter.loadArg(1); // Load hotspotValue
                setGetAdapter.checkCast(Type.getType(hotspotClass));
                setGetAdapter.putStatic(Type.getType(classNode.signature), hotspotFieldName, Type.getType(hotspotClass));
                setGetAdapter.visitInsn(Opcodes.RETURN);
            }

            @Override
            public void generateDefault() {
                setGetAdapter.visitInsn(Opcodes.RETURN);
            }
        });
        
        MethodNode setMethodNode = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "set",
            Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{Type.INT_TYPE, Type.getType(Object.class)}),
            null, 
            null);
        GeneratorAdapter setAdapter = new GeneratorAdapter(
            Opcodes.ACC_PUBLIC,
            new Method(setMethodNode.name, setMethodNode.desc), 
            setMethodNode);
        classNode.methods.add(setMethodNode);
        
        setAdapter.loadArg(0); // Load hotspotId
        setAdapter.tableSwitch(messageSendHotspotFields.stream().mapToInt(f -> f.id).toArray(), new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                MessageSendHotspotField hotspotField = messageSendHotspotFields.stream().filter(e -> e.id == key).findFirst().get();
                String hotspotFieldName = getMessageSendHotspotFieldName(hotspotField.symbolCode, hotspotField.arity);
                Class<?> hotspotClass = hotspotStrategy.getHotspotInterface(hotspotField.arity);
                setAdapter.loadArg(1); // Load hotspotValue
                setAdapter.checkCast(Type.getType(hotspotClass));
                setAdapter.putStatic(Type.getType(classNode.signature), hotspotFieldName, Type.getType(hotspotClass));
                setAdapter.visitInsn(Opcodes.RETURN);
            }

            @Override
            public void generateDefault() {
                setAdapter.visitInsn(Opcodes.RETURN);
            }
        });
    }
}
