package protomoto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Environment {
    private DefaultCell anyProto;
    private DefaultCell integerProto;
    private DefaultCell behaviorProto;
    private DefaultCell arrayProto;
    private DefaultCell stringProto;
    private DefaultCell frameProto;
    private DefaultCell nil;
    private DefaultCell primitive;
    private Hashtable<String, Integer> stringToSymbolCode = new Hashtable<>();
    
    public Environment() {
        anyProto = new DefaultCell(null);
        integerProto = new DefaultCell(anyProto);
        behaviorProto = new DefaultCell(anyProto);
        arrayProto = new DefaultCell(anyProto);
        stringProto = new DefaultCell(anyProto);
        frameProto = new DefaultCell(anyProto);
        nil = new DefaultCell(anyProto);
        primitive = new DefaultCell(anyProto);
        
        anyProto.put(getSymbolCode("Integer"), integerProto);
        anyProto.put(getSymbolCode("Behavior"), behaviorProto);
        anyProto.put(getSymbolCode("Array"), arrayProto);
        anyProto.put(getSymbolCode("String"), stringProto);
        anyProto.put(getSymbolCode("Frame"), frameProto);
        anyProto.put(getSymbolCode("Nil"), nil);
        anyProto.put(getSymbolCode("Primitive"), primitive);
        
        int errorOccurredSymbolCode = getSymbolCode("errorOccurred");
        primitive.put(errorOccurredSymbolCode, new BehaviorCell(new Instruction[] {
            Instructions.load(1),
            Instructions.finish(1)
        }, 0));
    }

    public Evaluator createEvaluator(Cell ast) {
        Evaluator evaluator = new Evaluator(this);
        MetaFrame metaFrame = new MetaFrame();
        ArrayList<String> errors = new ArrayList<>();
        Instruction[] instructions = getInstructions(metaFrame, ast, InstructionEmitters.single(Instructions.finish(0)), errors);
        if(errors.size() > 0)
            throw new IllegalArgumentException("Compile errors:\n" + errors.stream().collect(Collectors.joining("\n")));
        BehaviorCell behavior = new BehaviorCell(instructions, metaFrame.variableCount());
        Frame frame = behavior.createSendFrame(evaluator, null, 0, new Cell[]{anyProto});
        evaluator.setFrame(frame);
        return evaluator;
    }

    public DefaultCell getAnyProto() {
        return anyProto;
    }

    public DefaultCell getIntegerProto() {
        return integerProto;
    }

    public IntegerCell createInteger(int value) {
        return new IntegerCell(value);
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

    public BehaviorCell createBehavior(String[] parameters, Cell ast, List<String> errors) {
        MetaFrame metaFrame = new MetaFrame();
        for (String parameter: parameters) {
            metaFrame.declareVar(parameter);
        }
        Instruction[] instructions = getInstructions(metaFrame, ast, InstructionEmitters.single(Instructions.respond()), errors);
        
        return instructions != null ? new BehaviorCell(instructions, metaFrame.variableCount() - parameters.length) : null;
    }

    public ArrayCell createArray(int length) {
        Cell[] items = new Cell[length];
        Arrays.fill(items, 0, length, nil);
        return new ArrayCell(items);
    }

    public StringCell createString(String string) {
        return new StringCell(string);
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
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild, Consumer<String> errorCollector) {
                Cell receiver = ast.get(1);
                StringCell name = (StringCell) ast.get(2);
                int symbolCode = getSymbolCode(name.string);
                int arity = ast.length() - 3;
                
                translateChild.accept(receiver);
                
                for(int i = 3; i < ast.length(); i++) {
                    Cell argument = ast.get(i);
                    translateChild.accept(argument);
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
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild, Consumer<String> errorCollector) {
                Cell receiver = ast.get(1);
                StringCell name = (StringCell) ast.get(2);
                int symbolCode = getSymbolCode(name.string);
                Cell value = ast.get(3);
                
                translateChild.accept(receiver);
                translateChild.accept(value);
                
                if(asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.dup2()));
                }
                
                emitters.add(InstructionEmitters.single(Instructions.setSlot(symbolCode)));
            }
        });
        mappers.put(createString("get_slot"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild, Consumer<String> errorCollector) {
                Cell receiver = ast.get(1);
                StringCell name = (StringCell) ast.get(2);
                int symbolCode = getSymbolCode(name.string);
                
                translateChild.accept(receiver);
                
                emitters.add(InstructionEmitters.single(Instructions.getSlot(symbolCode)));
                
                if(!asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.pop()));
                }
            }
        });
        mappers.put(createString("array_set"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild, Consumer<String> errorCollector) {
                Cell receiver = ast.get(1);
                Cell index = ast.get(2);
                Cell value = ast.get(3);
                
                translateChild.accept(receiver);
                translateChild.accept(index);
                translateChild.accept(value);
                
                if(asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.dup3()));
                }
                
                emitters.add(InstructionEmitters.single(Instructions.arraySet()));
            }
        });
        mappers.put(createString("array_get"), ASTMappers.nnaryExpression(Instructions.arraySet(), 2));
        mappers.put(createString("array_length"), ASTMappers.nnaryExpression(Instructions.arrayLength(), 1));
        mappers.put(createString("behavior"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild, Consumer<String> errorCollector) {
                if(asExpression) {
                    ArrayCell parametersCell = (ArrayCell)ast.get(1);
                    String[] parameters = new String[parametersCell.length()];
                    
                    for(int i = 0; i < parametersCell.length(); i++) {
                        parameters[i] = ((StringCell)parametersCell.get(i)).string;
                    }
                    
                    Cell body = ast.get(2);

                    BehaviorCell behavior = createBehavior(parameters, body, errors);
                    
                    emitters.add(InstructionEmitters.single(Instructions.pushb(behavior)));
                }
            }
        });
        
        mappers.put(createString("var"), new ASTMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild, Consumer<String> errorCollector) {
                StringCell name = (StringCell) ast.get(1);
                Cell value = (Cell) ast.get(2);
                
                translateChild.accept(value);
                
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
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild, Consumer<String> errorCollector) {
                StringCell name = (StringCell) ast.get(1);
                Cell value = (Cell) ast.get(2);
                
                translateChild.accept(value);
                
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
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild, Consumer<String> errorCollector) {
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
        
        return InstructionMapper.fromAST(metaFrame, ast, mappers, endEmitter, errors);
    }

    public Cell getPrimitive() {
        return primitive;
    }

    public Cell getFrameProto() {
        return frameProto;
    }
}
