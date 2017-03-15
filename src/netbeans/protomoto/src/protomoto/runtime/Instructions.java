package protomoto.runtime;

import java.util.Hashtable;
import protomoto.cell.ArrayCell;
import protomoto.cell.BehaviorDescriptor;
import protomoto.cell.BehaviorCell;
import protomoto.cell.IntegerCell;
import protomoto.cell.Cell;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.objectweb.asm.commons.GeneratorAdapter;

public class Instructions {
    public static Instruction respond() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.respond();
            }

            @Override
            public String toString() {
                return "respond";
            }
        };
    }

    public static Instruction send(int symbolCode, int arity) {
        return new CallSiteInstruction() {
            @Override
            public void execute(Frame frame) {
                Cell receiver = frame.peek(arity);
                
                BehaviorCell behavior = receiver.resolveBehavior(frame.getEnvironment(), symbolCode);
                
                frame.replaceInstruction(send(symbolCode, arity, receiver.getTag(), behavior));
            }

            @Override
            public String toString() {
                return "send:" + symbolCode + "," + arity;
            }

            @Override
            public Instruction uncache(int tag) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    public static Instruction send(int symbolCode, int arity, int tag0, BehaviorCell behavior0) {
        return new CallSiteInstruction() {
            @Override
            public void execute(Frame frame) {
                Cell receiver = frame.peek(arity);
                
                BehaviorCell behavior;
                
                if(receiver.getTag() == tag0) {
                    behavior = behavior0;
                } else {
                    behavior = receiver.resolveBehavior(frame.getEnvironment(), symbolCode);
                }

                Frame sendFrame = behavior.createSendFrame(frame.getEvalutator(), frame, arity);
                frame.getEvalutator().setFrame(sendFrame);
            }

            @Override
            public String toString() {
                return "send:" + symbolCode + "," + arity;
            }

            @Override
            public Instruction uncache(int tag) {
                return send(symbolCode, arity);
            }
        };
    }

    public static Instruction load(int index) {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.load(index);
                frame.incIP();
            }

            @Override
            public String toString() {
                return "load:" + index;
            }
        };
    }
    
    public static Instruction pushi(int value) {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.pushi(value);
                frame.incIP();
            }

            @Override
            public void emit(Jitter jitter) {
                jitter.pushi(value);
            }

            @Override
            public String toString() {
                return "pushi:" + value;
            }
        };
    }
    
    public static <T extends Cell> Instruction unaryExpr(Function<T, T> exprFunction) {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                T c = (T) frame.pop();
                T result = exprFunction.apply(c);
                frame.push(result);
                frame.incIP();
            }
        };
    }
    
    public static Instruction binaryMod(BiConsumer<Cell, Cell> mod, String description) {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                Cell[] buffer = new Cell[2];

                frame.popInto(0, 2, buffer);
                mod.accept(buffer[0], buffer[1]);
                frame.incIP();
            }

            @Override
            public String toString() {
                return description;
            }
        };
    }
    
    private interface BinaryInstruction<T> {
        void execute(Frame frame, T lhs, T rhs);
    }
    
    public static Instruction binaryIntExpr(BinaryInstruction<Integer> intFunction, String description) {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                IntegerCell[] buffer = new IntegerCell[2];

                frame.popInto(0, 2, buffer);
                int lhs = buffer[0].getIntValue();
                int rhs = buffer[1].getIntValue();
                intFunction.execute(frame, lhs, rhs);
            }

            @Override
            public String toString() {
                return description;
            }
        };
    }
    
    public static Instruction addi() {
        return binaryIntExpr((frame, lhs, rhs) -> {
            frame.pushi(lhs + rhs);
            frame.incIP();
        }, "addi");
    }
    
    public static Instruction subi() {
        return binaryIntExpr((frame, lhs, rhs) -> {
            frame.pushi(lhs - rhs);
            frame.incIP();
        }, "subi");
    }
    
    public static Instruction muli() {
        return binaryIntExpr((frame, lhs, rhs) -> {
            frame.pushi(lhs * rhs);
            frame.incIP();
        }, "muli");
    }
    
    public static Instruction divi() {
        return binaryIntExpr((frame, lhs, rhs) -> {
            if(rhs > 0) {
                frame.pushi(lhs / rhs);
                frame.incIP();
            } else {
                frame.primitiveErrorOccurred("Division by zero.");
            }
        }, "divi");
    }
    
    public static Instruction setSlot(int symbolCode) {
        return binaryMod((self, value) -> self.put(symbolCode, value), "set_slot:" + symbolCode);
    }
    
    public static Instruction getSlot(int symbolCode) {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                Cell self = frame.pop();
                frame.push(self.get(frame.getEnvironment(), symbolCode));
                frame.incIP();
            }

            @Override
            public String toString() {
                return "get-slot:" + symbolCode;
            }
        };
    }
    
    public static Instruction newBehavior() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.newBehavior();
                frame.incIP();
            }

            @Override
            public String toString() {
                return "new-behavior";
            }
        };
    }
    
    public static Instruction arrayNew() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                IntegerCell index = (IntegerCell) frame.pop();
                frame.newArray(index.getIntValue());
                frame.incIP();
            }

            @Override
            public String toString() {
                return "array-new";
            }
        };
    }
    
    public static Instruction arrayLength() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                ArrayCell array = (ArrayCell) frame.pop();
                frame.pushi(array.length());
                frame.incIP();
            }

            @Override
            public String toString() {
                return "array-length";
            }
        };
    }
    
    public static Instruction dup() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.dup();
                frame.incIP();
            }

            @Override
            public String toString() {
                return "dup";
            }
        };
    }
    
    public static Instruction dup2() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.dup2();
                frame.incIP();
            }

            @Override
            public String toString() {
                return "dup2";
            }
        };
    }
    
    public static Instruction dup3() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.dup3();
                frame.incIP();
            }

            @Override
            public String toString() {
                return "dup3";
            }
        };
    }
    
    public static Instruction arraySet() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                Cell[] buffer = new Cell[3];
                
                frame.popInto(0, 3, buffer);
                
                ArrayCell array = (ArrayCell) buffer[0];
                IntegerCell index = (IntegerCell) buffer[1];
                Cell cell = buffer[2];
                array.set(index.getIntValue(), cell);
                
                frame.incIP();
            }

            @Override
            public String toString() {
                return "array-set";
            }
        };
    }
    
    public static Instruction arrayGet() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                Cell[] buffer = new Cell[2];
                
                frame.popInto(0, 2, buffer);
                
                ArrayCell array = (ArrayCell) buffer[0];
                IntegerCell index = (IntegerCell) buffer[1];
                Cell value = array.get(index.getIntValue());
                
                frame.push(value);
                
                frame.incIP();
            }

            @Override
            public String toString() {
                return "array-get";
            }
        };
    }
    
    public static Instruction finish(int returnCode) {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.finish(returnCode);
            }

            @Override
            public void emit(Jitter jitter) {
                jitter.finish(returnCode);
            }

            @Override
            public String toString() {
                return "finish";
            }
        };
    }

    public static Instruction pushs(String string) {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.pushs(string);
                frame.incIP();
            }

            @Override
            public String toString() {
                return "pushs:" + string;
            }
        };
    }

    public static Instruction store(int index) {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.store(index);
                frame.incIP();
            }

            @Override
            public String toString() {
                return "store:" + index;
            }
        };
    }

    public static Instruction pop() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.pop();
                frame.incIP();
            }

            @Override
            public String toString() {
                return "pop";
            }
        };
    }

    public static Instruction peek() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.push(frame.peek());
                frame.incIP();
            }

            @Override
            public String toString() {
                return "peek";
            }
        };
    }

    public static Instruction environment() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.push(frame.getEnvironment().getAnyProto());
                frame.incIP();
            }

            @Override
            public String toString() {
                return "environment";
            }
        };
    }

    public static Instruction pushb(BehaviorDescriptor behaviorDescriptor) {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                Cell frameProto = frame.pop();
                BehaviorCell behavior = behaviorDescriptor.createBehavior(frameProto);
                frame.push(behavior);
                frame.incIP();
            }

            @Override
            public String toString() {
                return "pushb";
            }
        };
    }

    public static Instruction cloneCell() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                Cell proto = frame.pop();
                Cell clone = proto.cloneCell();
                frame.push(clone);
                frame.incIP();
            }

            @Override
            public String toString() {
                return "clone_cell";
            }
        };
    }

    public static Instruction thisFrame() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.push(frame);
                frame.incIP();
            }

            @Override
            public String toString() {
                return "this_frame";
            }
        };
    }
}
