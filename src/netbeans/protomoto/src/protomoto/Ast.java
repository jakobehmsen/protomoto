package protomoto;

public class Ast {
    private ProtoEnvironment environment;

    public Ast(ProtoEnvironment environment) {
        this.environment = environment;
    }
    
    public ArrayProtoCell addi(AbstractProtoCell lhs, AbstractProtoCell rhs) {
        return new ArrayProtoCell(new AbstractProtoCell[]{
            environment.createString("addi"),
            lhs,
            rhs
        });
    }
    
    public ArrayProtoCell consti(int value) {
        return new ArrayProtoCell(new AbstractProtoCell[]{
            environment.createString("consti"),
            environment.createInteger(value)
        });
    }
    
    public ArrayProtoCell consts(String string) {
        return new ArrayProtoCell(new AbstractProtoCell[]{
            environment.createString("consts"),
            environment.createString(string)
        });
    }
    
    public ArrayProtoCell seq(ArrayProtoCell... sequence) {
        return new ArrayProtoCell(sequence);
    }
    
    public ArrayProtoCell var(String name, ArrayProtoCell initialValue) {
        return new ArrayProtoCell(new AbstractProtoCell[]{
            environment.createString("var"),
            environment.createString(name),
            initialValue
        });
    }
    
    public ArrayProtoCell var(String name) {
        return new ArrayProtoCell(new AbstractProtoCell[]{
            environment.createString("var"),
            environment.createString(name)
        });
    }
    
    public ArrayProtoCell get(String name) {
        return new ArrayProtoCell(new AbstractProtoCell[]{
            environment.createString("get"),
            environment.createString(name)
        });
    }
}
