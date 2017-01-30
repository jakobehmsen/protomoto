package protomoto;

import java.util.Hashtable;
import java.util.List;
import java.util.function.Consumer;

public class Environment {
    private DefaultCell anyProto;
    private DefaultCell integerProto;
    private DefaultCell behaviorProto;
    private DefaultCell arrayProto;
    private DefaultCell stringProto;
    private Hashtable<String, Integer> stringToSymbolCode = new Hashtable<>();
    
    public Environment() {
        anyProto = new DefaultCell(null);
        integerProto = new DefaultCell(anyProto);
        behaviorProto = new DefaultCell(anyProto);
        arrayProto = new DefaultCell(anyProto);
        stringProto = new DefaultCell(anyProto);
    }

    public Evaluator createEvaluator(Cell receiver, Cell ast) {
        Evaluator evaluator = new Evaluator(this);
        MetaFrame metaFrame = new MetaFrame();
        Instruction[] instructions = getInstructions(metaFrame, ast, InstructionEmitters.single(Instructions.finish()));
        BehaviorProtoCell behavior = new BehaviorProtoCell(instructions, metaFrame.variableCount());
        Frame frame = behavior.createSendFrame(evaluator, null, anyProto, 0);
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

    public int getSymbolCode(String string) {
        return stringToSymbolCode.computeIfAbsent(string, k -> stringToSymbolCode.size());
    }

    public DefaultCell getArrayProto() {
        return arrayProto;
    }

    public DefaultCell getStringProto() {
        return stringProto;
    }

    public BehaviorProtoCell createBehavior(Cell ast) {
        MetaFrame metaFrame = new MetaFrame();
        Instruction[] instructions = getInstructions(metaFrame, ast, InstructionEmitters.single(Instructions.respond()));
        
        return new BehaviorProtoCell(instructions, metaFrame.variableCount());
    }

    public ArrayCell createArray(int length) {
        return new ArrayCell((new Cell[length]));
    }

    public StringCell createString(String string) {
        return new StringCell(string);
    }
    
    public Instruction[] getInstructions(MetaFrame metaFrame, Cell ast, InstructionEmitter endEmitter) {
        Hashtable<Cell, InstructionMapper.ASTArrayMapper> mappers = new Hashtable<>();
        
        mappers.put(createString("consti"), new InstructionMapper.ASTArrayMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild) {
                if(asExpression) {
                    IntegerCell integer = (IntegerCell) ast.get(1);
                    int value = integer.getIntValue();
                
                    emitters.add(InstructionEmitters.single(Instructions.pushi(value)));
                }
            }
        });
        
        mappers.put(createString("consts"), new InstructionMapper.ASTArrayMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild) {
                if(asExpression) {
                    StringCell string = (StringCell) ast.get(1);
                    String value = string.string;
                
                    emitters.add(InstructionEmitters.single(Instructions.pushs(value)));
                }
            }
        });
        
        mappers.put(createString("addi"), new InstructionMapper.ASTArrayMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild) {
                Cell lhs = (Cell) ast.get(1);
                Cell rhs = (Cell) ast.get(2);
                
                translateChild.accept(lhs);
                translateChild.accept(rhs);
                
                emitters.add(InstructionEmitters.single(Instructions.addi()));
                
                if(!asExpression) {
                    emitters.add(InstructionEmitters.single(Instructions.pop()));
                }
            }
        });
        
        mappers.put(createString("var"), new InstructionMapper.ASTArrayMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild) {
                StringCell name = (StringCell) ast.get(1);
                Cell value = (Cell) ast.get(2);
                
                translateChild.accept(value);
                
                if(asExpression)
                    emitters.add(InstructionEmitters.single(Instructions.dup()));
                
                emitters.add(new InstructionEmitter() {
                    int index;
                    
                    @Override
                    public void prepare(MetaFrame metaFrame) { 
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
        
        mappers.put(createString("get"), new InstructionMapper.ASTArrayMapper() {
            @Override
            public void translate(ArrayCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<Cell> translateChild) {
                StringCell name = (StringCell) ast.get(1);
                
                emitters.add(new InstructionEmitter() {
                    @Override
                    public void prepare(MetaFrame metaFrame) { }

                    @Override
                    public void emit(MetaFrame metaFrame, List<Instruction> instructions) {
                        int index = metaFrame.indexOf(name.string);
                        instructions.add(Instructions.load(index));
                    }
                });
            }
        });
        
        return InstructionMapper.fromAST(metaFrame, ast, mappers, new InstructionMapper.ASTLeafMapper() {
            @Override
            public void translate(Cell ast, List<InstructionEmitter> emitters) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }, endEmitter);
    }
}
