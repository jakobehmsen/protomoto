package protomoto.cell;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import protomoto.cell.ArrayCell;
import protomoto.cell.BehaviorDescriptor;
import protomoto.cell.BehaviorCell;
import protomoto.cell.IntegerCell;
import protomoto.cell.StringCell;
import protomoto.cell.DefaultCell;
import protomoto.cell.Cell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.TableSwitchGenerator;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import protomoto.emit.ASTMapper;
import protomoto.emit.ASTMappers;
import protomoto.runtime.Evaluator;
import protomoto.runtime.Frame;
import protomoto.runtime.Instruction;
import protomoto.emit.InstructionEmitter;
import protomoto.emit.InstructionEmitters;
import protomoto.emit.InstructionMapper;
import protomoto.runtime.Instructions;
import protomoto.emit.MetaFrame;
import protomoto.runtime.EvaluatorInterface;
import protomoto.runtime.Hotspot;
import protomoto.runtime.Hotspot0;
import protomoto.runtime.HotspotStrategy;
import protomoto.runtime.Jitter;
import protomoto.runtime.SingleClassLoader;

public class Environment {
    private DefaultCell anyProto;
    private DefaultCell integerProto;
    private DefaultCell behaviorProto;
    private DefaultCell arrayProto;
    private DefaultCell stringProto;
    private DefaultCell frameProto;
    private DefaultCell nil;
    private Hashtable<String, Integer> stringToSymbolCode = new Hashtable<>();
    
    public Environment() {
        anyProto = new DefaultCell(null);
        anyProto.newTag();
        integerProto = new DefaultCell(anyProto);
        integerProto.newTag();
        behaviorProto = new DefaultCell(anyProto);
        behaviorProto.newTag();
        arrayProto = new DefaultCell(anyProto);
        arrayProto.newTag();
        stringProto = new DefaultCell(anyProto);
        stringProto.newTag();
        frameProto = new DefaultCell(anyProto);
        frameProto.newTag();
        nil = new DefaultCell(anyProto);
        nil.newTag();
        
        anyProto.put(getSymbolCode("Integer"), integerProto);
        anyProto.put(getSymbolCode("Behavior"), behaviorProto);
        anyProto.put(getSymbolCode("Array"), arrayProto);
        anyProto.put(getSymbolCode("String"), stringProto);
        anyProto.put(getSymbolCode("Frame"), frameProto);
        anyProto.put(getSymbolCode("Nil"), nil);
        
        int primitiveErrorOccurredSymbolCode = getSymbolCode("primitiveErrorOccurred");
        frameProto.put(primitiveErrorOccurredSymbolCode, new BehaviorCell(frameProto, new Instruction[] {
            Instructions.load(1),
            Instructions.finish(1)
        }, 0));
    }
    
    public interface HotspotCacheMissHandler {
        Cell handleAndEvaluate(Environment environment, Cell self, Cell[] args);
    }
    
    public static Object newHotspot(HotspotStrategy hotspotStrategy, int symbolCode, int arity, Map<Integer, Object> hotspotCache) {
        Class<?> hotspotClass = hotspotStrategy.getHotspotInterface(arity);
        ClassNode hotspotClassNode = new ClassNode();
        hotspotClassNode.version = Opcodes.V1_8;
        hotspotClassNode.access = Opcodes.ACC_PUBLIC;
        hotspotClassNode.name="Hotspot_" + symbolCode + "_" + arity;
        hotspotClassNode.signature="L" + hotspotClassNode.name + ";";
        hotspotClassNode.superName="java/lang/Object";
        hotspotClassNode.interfaces.add(Type.getType(hotspotClass).getInternalName());
        
        hotspotClassNode.fields.add(new FieldNode(
            Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC, 
            "hotspotCacheMissHandler", 
            Type.getDescriptor(HotspotCacheMissHandler.class), 
            null, 
            null
        ));
        
        hotspotCache.keySet().forEach(type -> {
            String behaviorFieldName = "behavior" + type;
            hotspotClassNode.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE|Opcodes.ACC_STATIC, 
                behaviorFieldName, 
                Type.getDescriptor(hotspotClass), 
                null, 
                null
            ));
        });
        
        MethodNode constructor = new MethodNode(
            Opcodes.ACC_PUBLIC, 
            "<init>", 
            Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{}), 
            null, 
            null);
        GeneratorAdapter initAdapter = new GeneratorAdapter(Opcodes.ACC_PUBLIC, new Method(constructor.name, constructor.desc), constructor);
        initAdapter.loadThis();
        initAdapter.invokeConstructor(Type.getType(Object.class), new Method("<init>", "()V"));
        initAdapter.visitInsn(Opcodes.RETURN);
        hotspotClassNode.methods.add(constructor);
        
        String hotspotClassNodeSignature = hotspotClassNode.signature;

        Type[] evalParameterTypes = new Type[2 + arity];
        evalParameterTypes[0] = Type.getType(Environment.class);
        evalParameterTypes[1] = Type.getType(Cell.class);
        for(int i = 2; i < evalParameterTypes.length; i++) {
            evalParameterTypes[i] = Type.getType(Cell.class);
        }

        MethodNode evalMethodNode = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "evaluate",
            Type.getMethodDescriptor(Type.getType(Cell.class), evalParameterTypes),
            null, 
            null);
        GeneratorAdapter evalAdapter = new GeneratorAdapter(
            Opcodes.ACC_PUBLIC,
            new Method("evaluate", Type.getType(Cell.class), evalParameterTypes), 
            evalMethodNode);
        
        evalAdapter.loadArg(1); // Load self
        evalAdapter.getField(Type.getType(Cell.class), "tag", Type.INT_TYPE);
        
        evalAdapter.tableSwitch(hotspotCache.keySet().stream().mapToInt(x -> x).toArray(), new TableSwitchGenerator() {
            @Override
            public void generateCase(int key, Label end) {
                // Load behavior from static field access
                String behaviorFieldName = "behavior" + key;
                evalAdapter.getStatic(Type.getType(hotspotClassNodeSignature), behaviorFieldName, Type.getType(hotspotClass));
                
                evalAdapter.invokeInterface(
                    Type.getType(hotspotClass), 
                    new Method("evaluate", Type.getType(Cell.class), evalParameterTypes));
            }

            @Override
            public void generateDefault() {
                // Cache miss, find behavior, add to cache, generate a new hotspot, and replace hotspot usage
                
                /*
                Cell[] args = new Cell[arity];
                // Populate args
                return hotspotCacheMissHandler.handleAndEvaluate(environment, self, args);
                */
                
                int argsLocal = evalAdapter.newLocal(Type.getType("[L" + Cell.class.getName().replace(".", "/") + ";"));
                evalAdapter.push(arity);
                evalAdapter.newArray(Type.getType(Cell.class));
                for(int i = 0; i < arity; i++) {
                    int argIndex = i + 2;
                    evalAdapter.dup();
                    evalAdapter.push(i);
                    evalAdapter.loadArg(argIndex);
                    evalAdapter.box(Type.getType(Cell.class));
                    evalAdapter.arrayStore(Type.getType(Cell.class));
                }
                evalAdapter.storeLocal(argsLocal);
                
                evalAdapter.getStatic(Type.getType(hotspotClassNodeSignature), "hotspotCacheMissHandler", Type.getType(HotspotCacheMissHandler.class));
                evalAdapter.loadArg(0); // Load environment
                evalAdapter.loadArg(1); // Load self
                evalAdapter.loadLocal(argsLocal);
                evalAdapter.invokeInterface(
                    Type.getType(HotspotCacheMissHandler.class), 
                    new Method("handleAndEvaluate", Type.getType(Cell.class), new Type[]{Type.getType(Environment.class), Type.getType(Cell.class), Type.getType("[L" + Cell.class.getName().replace(".", "/") + ";")}));
                evalAdapter.returnValue();
            }
        });
        
        hotspotClassNode.methods.add(evalMethodNode);
        
        try {
            Class<?> hotspotClassGenerated = new SingleClassLoader(hotspotClassNode).loadClass(hotspotClassNode.name);
            Field hotspotCacheMissHandlerField = hotspotClassGenerated.getDeclaredField("hotspotCacheMissHandler");
            hotspotCacheMissHandlerField.setAccessible(true);
            hotspotCacheMissHandlerField.set(null, new HotspotCacheMissHandler() {
                @Override
                public Cell handleAndEvaluate(Environment environment, Cell self, Cell[] args) {
                    BehaviorCell behavior = self.resolveBehavior(environment, symbolCode);
                    
                    Hotspot hotspot = behavior.getHotspot(environment, arity);
                    hotspotCache.put(self.tag, hotspot);
                    
                    Class<?>[] parameterTypes = new Class<?>[2 + hotspot.getArity()];
                    Object[] reflectionArgs = new Object[2 + hotspot.getArity()];
                    parameterTypes[0] = Environment.class;
                    reflectionArgs[0] = environment;
                    parameterTypes[1] = Cell.class;
                    reflectionArgs[1] = self;
                    for(int i = 2; i < parameterTypes.length; i++) {
                        parameterTypes[i] = Cell.class;
                        reflectionArgs[i] = args[i];
                    }
                    
                    try {
                        java.lang.reflect.Method evalMethod = hotspot.getClass().getMethod("evaluate", parameterTypes);
                        return (Cell) evalMethod.invoke(hotspot, reflectionArgs);
                    } catch (NoSuchMethodException ex) {
                        Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (SecurityException ex) {
                        Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvocationTargetException ex) {
                        Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    return null;
                }
            });
        
            hotspotCache.forEach((type, object) -> {
                String behaviorFieldName = "behavior" + type;
                try {
                    Field behaviorField = hotspotClassGenerated.getDeclaredField(behaviorFieldName);
                    behaviorField.setAccessible(true);
                    behaviorField.set(null, object);
                } catch (NoSuchFieldException ex) {
                    Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            
            return hotspotClassGenerated.newInstance();
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    private HotspotStrategy hotspotStrategy = new HotspotStrategy() {
        @Override
        public Class<?> getHotspotInterface(int arity) {
            ClassNode hotspotInteraceClassNode = new ClassNode();
            
            if(arity == 0) {
                return Hotspot0.class;
            }
            
            return null;
        }

        @Override
        public Object newHotspot(int symbolCode, int arity) {
            /*ClassNode hotspotClassNode = new ClassNode();
        
            hotspotClassNode = new ClassNode();
            hotspotClassNode.version = Opcodes.V1_8;
            hotspotClassNode.access = Opcodes.ACC_PUBLIC;
            hotspotClassNode.name="Hotspot_" + symbolCode + "_" + arity;
            hotspotClassNode.signature="L" + hotspotClassNode.name + ";";
            hotspotClassNode.superName="java/lang/Object";
            hotspotClassNode.interfaces.add(Type.getType(hotspotStrategy.getHotspotInterface(0)).getInternalName());
            
            Type[] evalParameterTypes = new Type[2 + arity];
            evalParameterTypes[0] = Type.getType(Environment.class);
            evalParameterTypes[1] = Type.getType(Cell.class);
            for(int i = 0; i < evalParameterTypes.length; i++) {
                evalParameterTypes[i] = Type.getType(Cell.class);
            }
            
            MethodNode evalMethodNode = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "evaluate",
                Type.getMethodDescriptor(Type.getType(Cell.class), evalParameterTypes),
                null, 
                null);
            GeneratorAdapter evalAdapter = new GeneratorAdapter(
                    Opcodes.ACC_PUBLIC,
                    new Method("evaluate", Type.getType(Cell.class), evalParameterTypes), 
                    evalMethodNode);*/
            
            Hashtable<Integer, Object> hotspotCache = new Hashtable<>();
            return Environment.newHotspot(this, symbolCode, arity, hotspotCache);
            
            /*if(arity == 0) {
                return new Hotspot0() {
                    @Override
                    public Cell evaluate(Environment environment, Cell self) {
                        BehaviorCell behavior = self.resolveBehavior(environment, symbolCode);
                        // Could the Environment, this HotspotStrategy relates to, be used here instead?
                        // I.e.: Hotspot0 behaviorHotspot = (Hotspot0) behavior.getHotspot(Environment.this, arity);
                        // Then one less argument is necessary for hotspots? Or...?
                        Hotspot0 behaviorHotspot = (Hotspot0) behavior.getHotspot(environment, arity);
                        return behaviorHotspot.evaluate(environment, self);
                        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                };
            }
            
            return null;*/
        }
    };
    
    public HotspotStrategy getHotspotStrategy() {
        return hotspotStrategy;
    }

    public EvaluatorInterface createEvaluator(Cell ast) {
        Evaluator evaluator = new Evaluator(this);
        MetaFrame metaFrame = new MetaFrame();
        ArrayList<String> errors = new ArrayList<>();
        Instruction[] instructions = getInstructions(metaFrame, ast, InstructionEmitters.single(Instructions.finish(0)), errors);
        if(errors.size() > 0)
            throw new IllegalArgumentException("Compile errors:\n" + errors.stream().collect(Collectors.joining("\n")));
        BehaviorCell behavior = new BehaviorCell(frameProto, instructions, metaFrame.variableCount());
        Frame frame = behavior.createSendFrame(evaluator, null, 0, new Cell[]{anyProto});
        evaluator.setFrame(frame);
        
        /*ClassNode classNode = new ClassNode();
        classNode.version = Opcodes.V1_8;
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.signature="LGenerated;";
        classNode.name="Generated";
        classNode.superName="java/lang/Object";
        classNode.interfaces.add(Type.getType(hotspotStrategy.getHotspotInterface(0)).getInternalName());*/
        /*MethodNode methodNode = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "evaluate",
                Type.getMethodDescriptor(Type.getType(Cell.class), new Type[]{Type.getType(Environment.class), Type.getType(Cell.class)}),
                null, 
                null);
        GeneratorAdapter generator = new GeneratorAdapter(
                Opcodes.ACC_PUBLIC,
                new Method("evaluate", Type.getType(Cell.class), new Type[]{Type.getType(Environment.class), Type.getType(Cell.class)}), 
                methodNode);*/
        
        Jitter jitter = new Jitter(hotspotStrategy);
        
        jitter.emit(instructions);
        
        /*Printer printer = new Textifier();
        methodNode.accept(new TraceMethodVisitor(printer));
        printer.getText().forEach(line -> System.out.print(line));*/
        //classNode.methods.add(methodNode);
        
        try {
            Class<Hotspot0> c = (Class<Hotspot0>) jitter.compileClass();
            Hotspot0 hotspot = c.getConstructor(HotspotStrategy.class).newInstance(hotspotStrategy);
            //java.lang.reflect.Method evalMethod = c.getMethod("eval", Environment.class, Cell.class);
            
            return new EvaluatorInterface() {
                private Cell response;
                private boolean isFinished = false;

                @Override
                public boolean isFinished() {
                    return isFinished;
                }

                @Override
                public void proceed() {
                    response = hotspot.evaluate(Environment.this, ast);
                    isFinished = true;
                    /*try {
                        response = (Cell) evalMethod.invoke(null, Environment.this, getAnyProto());
                        isFinished = true;
                    } catch (IllegalAccessException ex) {
                        Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvocationTargetException ex) {
                        Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                    }*/
                }

                @Override
                public int getReturnCode() {
                    return 0;
                }

                @Override
                public Cell getResponse() {
                    return response;
                }
            };
            
            /*Cell result = (Cell) evalMethod.invoke(null, this, getAnyProto());
            c.toString();*/
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
        
        //return evaluator;
    }

    public DefaultCell getAnyProto() {
        return anyProto;
    }

    public DefaultCell getIntegerProto() {
        return integerProto;
    }

    public IntegerCell createInteger(int value) {
        IntegerCell integerCell = new IntegerCell(value);
        integerCell.setTag(integerProto.getTag());
        return integerCell;
    }

    public DefaultCell getBehaviorProto() {
        return behaviorProto;
    }

    public final int getSymbolCode(String string) {
        return stringToSymbolCode.computeIfAbsent(string, k -> stringToSymbolCode.size());
    }

    public DefaultCell getArrayProto() {
        return arrayProto;
    }

    public DefaultCell getStringProto() {
        return stringProto;
    }

    public DefaultCell getNil() {
        return nil;
    }

    public BehaviorDescriptor createBehavior(String[] parameters, Cell ast, List<String> errors) {
        MetaFrame metaFrame = new MetaFrame();
        for (String parameter: parameters) {
            metaFrame.declareVar(parameter);
        }
        Instruction[] instructions = getInstructions(metaFrame, ast, InstructionEmitters.single(Instructions.respond()), errors);
        
        return instructions != null ? new BehaviorDescriptor(instructions, metaFrame.variableCount() - parameters.length) : null;
    }

    public ArrayCell createArray(int length) {
        Cell[] items = new Cell[length];
        Arrays.fill(items, 0, length, nil);
        ArrayCell arrayCell = new ArrayCell(items);
        arrayCell.setTag(arrayProto.getTag());
        return arrayCell;
    }

    public StringCell createString(String string) {
        StringCell stringCell = new StringCell(string);
        stringCell.setTag(stringProto.getTag());
        return stringCell;
    }
    
    public Instruction[] getInstructions(MetaFrame metaFrame, Cell ast, InstructionEmitter endEmitter, List<String> errors) {
        Hashtable<Cell, ASTMapper> mappers = new Hashtable<>();
        
        mappers.put(createString("consti"), ASTMappers.<IntegerCell>constExpression(i -> Instructions.pushi(i.getIntValue())));
        mappers.put(createString("consts"), ASTMappers.<StringCell>constExpression(s -> Instructions.pushs(s.string)));
        
        mappers.put(createString("addi"), ASTMappers.binaryExpression(Instructions.addi()));
        mappers.put(createString("subi"), ASTMappers.binaryExpression(Instructions.subi()));
        mappers.put(createString("muli"), ASTMappers.binaryExpression(Instructions.muli()));
        mappers.put(createString("divi"), ASTMappers.binaryExpression(Instructions.divi()));
        
        mappers.put(createString("environment"), ASTMappers.nnaryExpression(Instructions.environment(), 0));
        mappers.put(createString("this_frame"), ASTMappers.nnaryExpression(Instructions.thisFrame(), 0));
        
        mappers.put(createString("array_new"), ASTMappers.nnaryExpression(Instructions.arrayNew(), 1));
        mappers.put(createString("self"), ASTMappers.nnaryExpression(Instructions.load(0), 0));
        
        mappers.put(createString("send"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                Cell receiver = ast.get(1);
                StringCell name = (StringCell) ast.get(2);
                int symbolCode = getSymbolCode(name.string);
                int arity = ast.length() - 3;
                
                emitters.add(InstructionEmitters.single(Instructions.preSend(symbolCode, arity)));
                
                mapExpression.accept(receiver);
                
                for(int i = 3; i < ast.length(); i++) {
                    Cell argument = ast.get(i);
                    mapExpression.accept(argument);
                }
                
                emitters.add(InstructionEmitters.single(Instructions.send(symbolCode, arity)));
                
                if(!asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.pop()));
                }
            }
        });
        
        mappers.put(createString("clone"), ASTMappers.nnaryExpression(Instructions.cloneCell(), 1));
        mappers.put(createString("set_slot"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                Cell receiver = ast.get(1);
                StringCell name = (StringCell) ast.get(2);
                int symbolCode = getSymbolCode(name.string);
                Cell value = ast.get(3);
                
                mapExpression.accept(receiver);
                emitters.add(InstructionEmitters.single(Instructions.setSlotPre(symbolCode)));
                mapExpression.accept(value);
                
                if(asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.dupX2()));
                }
                
                emitters.add(InstructionEmitters.single(Instructions.setSlot(symbolCode)));
            }
        });
        mappers.put(createString("get_slot"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                Cell receiver = ast.get(1);
                StringCell name = (StringCell) ast.get(2);
                int symbolCode = getSymbolCode(name.string);
                
                mapExpression.accept(receiver);
                
                emitters.add(InstructionEmitters.single(Instructions.getSlot(symbolCode)));
                
                if(!asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.pop()));
                }
            }
        });
        mappers.put(createString("array_set"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                Cell receiver = ast.get(1);
                Cell index = ast.get(2);
                Cell value = ast.get(3);
                
                mapExpression.accept(receiver);
                mapExpression.accept(index);
                mapExpression.accept(value);
                
                if(asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.dupX2()));
                }
                
                emitters.add(InstructionEmitters.single(Instructions.arraySet()));
            }
        });
        mappers.put(createString("array_get"), ASTMappers.nnaryExpression(Instructions.arraySet(), 2));
        mappers.put(createString("array_length"), ASTMappers.nnaryExpression(Instructions.arrayLength(), 1));
        mappers.put(createString("behavior"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                if(asExpression) {
                    Cell frameProto = ast.get(1);
                    mapExpression.accept(frameProto);
                    ArrayCell parametersCell = (ArrayCell)ast.get(2);
                    String[] parameters = new String[parametersCell.length()];
                    
                    for(int i = 0; i < parametersCell.length(); i++) {
                        parameters[i] = ((StringCell)parametersCell.get(i)).string;
                    }
                    
                    Cell body = ast.get(3);

                    BehaviorDescriptor behavior = createBehavior(parameters, body, errors);
                    
                    emitters.add(InstructionEmitters.single(Instructions.pushb(behavior)));
                }
            }
        });
        
        mappers.put(createString("var"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                StringCell name = (StringCell) ast.get(1);
                Cell value = (Cell) ast.get(2);
                
                mapExpression.accept(value);
                
                if(asExpression)
                    emitters.add(InstructionEmitters.single(Instructions.dup()));
                
                emitters.add(new InstructionEmitter() {
                    int index;
                    
                    @Override
                    public void prepare(MetaFrame metaFrame) {
                        if(metaFrame.indexOf(name.string) != -1) {
                            errorCollector.accept("'" + name.string + "' is already declared.");
                            return;
                        }
                        
                        metaFrame.declareVar(name.string);
                    }

                    @Override
                    public void emit(MetaFrame metaFrame, List<Instruction> instructions) {
                        int index = metaFrame.indexOf(name.string);
                        instructions.add(Instructions.store(index));
                    }
                });
            }
        });
        
        mappers.put(createString("set"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                StringCell name = (StringCell) ast.get(1);
                Cell value = (Cell) ast.get(2);
                
                mapExpression.accept(value);
                
                if(asExpression)
                    emitters.add(InstructionEmitters.single(Instructions.dup()));
                
                emitters.add(new InstructionEmitter() {
                    int index;
                    
                    @Override
                    public void prepare(MetaFrame metaFrame) {
                        if(metaFrame.indexOf(name.string) == -1) {
                            errorCollector.accept("'" + name.string + "' is undeclared.");
                        }
                    }

                    @Override
                    public void emit(MetaFrame metaFrame, List<Instruction> instructions) {
                        int index = metaFrame.indexOf(name.string);
                        instructions.add(Instructions.store(index));
                    }
                });
            }
        });
        
        mappers.put(createString("get"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                StringCell name = (StringCell) ast.get(1);
                
                emitters.add(new InstructionEmitter() {
                    @Override
                    public void prepare(MetaFrame metaFrame) {
                        if(metaFrame.indexOf(name.string) == -1) {
                            errorCollector.accept("'" + name.string + "' is undeclared.");
                        }
                    }

                    @Override
                    public void emit(MetaFrame metaFrame, List<Instruction> instructions) {
                        int index = metaFrame.indexOf(name.string);
                        instructions.add(Instructions.load(index));
                    }
                });
            }
        });
        
        mappers.put(createString("push"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                Cell target = ast.get(1);
                
                // Result of target will be on top of stack for each of statement
                mapExpression.accept(target);
                
                for(int i = 2; i < ast.length(); i++) {
                    Cell statement = ast.get(i);
                    mapStatement.accept(statement);
                }
                
                // Result of target remains top of stack at this point
                if(!asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.pop()));
                }
            }
        });
        
        mappers.put(createString("peek"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> mapExpression, Consumer<Cell> mapStatement, Consumer<String> errorCollector) {
                // Result of target remains top of stack at this point
                if(asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.peek()));
                }
            }
        });
        
        return InstructionMapper.fromAST(metaFrame, ast, mappers, endEmitter, errors);
    }

    public Cell getFrameProto() {
        return frameProto;
    }
}
