package protomoto;

import java.util.Hashtable;
import java.util.List;
import java.util.function.Consumer;

public class ProtoEnvironment implements Environment {
    private AbstractProtoCell anyProto;
    private AbstractProtoCell integerProto;
    private AbstractProtoCell behaviorProto;
    private AbstractProtoCell arrayProto;
    private AbstractProtoCell stringProto;
    private Hashtable<String, Integer> stringToSymbolCode = new Hashtable<>();
    
    public ProtoEnvironment() {
        anyProto = new DefaultProtoCell(null);
        integerProto = new DefaultProtoCell(anyProto);
    }

    public AbstractProtoCell getAnyProto() {
        return anyProto;
    }

    public AbstractProtoCell getIntegerProto() {
        return integerProto;
    }

    @Override
    public Evaluator createEvaluator(Cell receiver, Cell ast) {
        ProtoEvaluator evaluator = new ProtoEvaluator(this);
        MetaFrame metaFrame = new MetaFrame();
        Instruction[] instructions = getInstructions(metaFrame, (AbstractProtoCell) ast, InstructionEmitters.single(Instructions.finish()));
        BehaviorProtoCell behavior = new BehaviorProtoCell(instructions, metaFrame.variableCount());
        ProtoFrame frame = behavior.createSendFrame(evaluator, null, anyProto, 0);
        evaluator.setFrame(frame);
        return evaluator;
    }

    @Override
    public AbstractProtoCell createInteger(int value) {
        return new IntegerProtoCell(value);
    }

    public AbstractProtoCell getBehaviorProto() {
        return behaviorProto;
    }

    public int getSymbolCode(String string) {
        return stringToSymbolCode.computeIfAbsent(string, k -> stringToSymbolCode.size());
    }

    public AbstractProtoCell getArrayProto() {
        return arrayProto;
    }

    public AbstractProtoCell getStringProto() {
        return stringProto;
    }
    
    public Instruction[] getInstructions(MetaFrame metaFrame, AbstractProtoCell ast, InstructionEmitter endEmitter) {
        Hashtable<AbstractProtoCell, InstructionMapper.ASTArrayMapper> mappers = new Hashtable<>();
        
        mappers.put(createString("consti"), new InstructionMapper.ASTArrayMapper() {
            @Override
            public void translate(ArrayProtoCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<AbstractProtoCell> translateChild) {
                if(asExpression) {
                    IntegerProtoCell integer = (IntegerProtoCell) ast.get(1);
                    int value = integer.getIntValue();
                
                    emitters.add(InstructionEmitters.single(Instructions.pushi(value)));
                }
            }
        });
        
        mappers.put(createString("consts"), new InstructionMapper.ASTArrayMapper() {
            @Override
            public void translate(ArrayProtoCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<AbstractProtoCell> translateChild) {
                if(asExpression) {
                    StringProtoCell string = (StringProtoCell) ast.get(1);
                    String value = string.string;
                
                    emitters.add(InstructionEmitters.single(Instructions.pushs(value)));
                }
            }
        });
        
        mappers.put(createString("addi"), new InstructionMapper.ASTArrayMapper() {
            @Override
            public void translate(ArrayProtoCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<AbstractProtoCell> translateChild) {
                AbstractProtoCell lhs = (AbstractProtoCell) ast.get(1);
                AbstractProtoCell rhs = (AbstractProtoCell) ast.get(2);
                
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
            public void translate(ArrayProtoCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<AbstractProtoCell> translateChild) {
                StringProtoCell name = (StringProtoCell) ast.get(1);
                AbstractProtoCell value = (AbstractProtoCell) ast.get(2);
                
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
            public void translate(ArrayProtoCell ast, List<InstructionEmitter> emitters, boolean asExpression, Consumer<AbstractProtoCell> translateChild) {
                StringProtoCell name = (StringProtoCell) ast.get(1);
                
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
            public void translate(AbstractProtoCell ast, List<InstructionEmitter> emitters) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }, endEmitter);
    }

    public BehaviorProtoCell createBehavior(AbstractProtoCell ast) {
        MetaFrame metaFrame = new MetaFrame();
        Instruction[] instructions = getInstructions(metaFrame, ast, InstructionEmitters.single(Instructions.respond()));
        
        return new BehaviorProtoCell(instructions, metaFrame.variableCount());
    }

    public AbstractProtoCell createArray(int length) {
        return new ArrayProtoCell((new AbstractProtoCell[length]));
    }

    public AbstractProtoCell createString(String string) {
        return new StringProtoCell(string);
    }
}
