package protomoto;

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
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.send(symbolCode, arity);
            }

            @Override
            public String toString() {
                return "send:" + symbolCode + "," + arity;
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
            public String toString() {
                return "pushi:" + value;
            }
        };
    }
    
    public static Instruction addi() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                IntegerCell[] buffer = new IntegerCell[2];
                
                frame.popInto(0, 2, buffer);
                int lhs = buffer[0].getIntValue();
                int rhs = buffer[1].getIntValue();
                frame.pushi(lhs + rhs);
                frame.incIP();
            }

            @Override
            public String toString() {
                return "addi";
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
                return "newBehavior";
            }
        };
    }
    
    public static Instruction newArray() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                IntegerCell index = (IntegerCell) frame.pop();
                frame.newArray(index.getIntValue());
                frame.incIP();
            }

            @Override
            public String toString() {
                return "newArray";
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
    
    public static Instruction setArray() {
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
                return "setArray";
            }
        };
    }
    
    public static Instruction finish() {
        return new Instruction() {
            @Override
            public void execute(Frame frame) {
                frame.finish();
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
            }

            @Override
            public String toString() {
                return "pop";
            }
        };
    }
}
