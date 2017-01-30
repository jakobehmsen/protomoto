package protomoto;

public class Ast {
    private Environment environment;

    public Ast(Environment environment) {
        this.environment = environment;
    }
    
    public ArrayCell addi(Cell lhs, Cell rhs) {
        return new ArrayCell(new Cell[]{
            environment.createString("addi"),
            lhs,
            rhs
        });
    }
    
    public ArrayCell consti(int value) {
        return new ArrayCell(new Cell[]{
            environment.createString("consti"),
            environment.createInteger(value)
        });
    }
    
    public ArrayCell consts(String string) {
        return new ArrayCell(new Cell[]{
            environment.createString("consts"),
            environment.createString(string)
        });
    }
    
    public ArrayCell seq(ArrayCell... sequence) {
        return new ArrayCell(sequence);
    }
    
    public ArrayCell var(String name, ArrayCell initialValue) {
        return new ArrayCell(new Cell[]{
            environment.createString("var"),
            environment.createString(name),
            initialValue
        });
    }
    
    public ArrayCell var(String name) {
        return new ArrayCell(new Cell[]{
            environment.createString("var"),
            environment.createString(name)
        });
    }
    
    public ArrayCell get(String name) {
        return new ArrayCell(new Cell[]{
            environment.createString("get"),
            environment.createString(name)
        });
    }
}
