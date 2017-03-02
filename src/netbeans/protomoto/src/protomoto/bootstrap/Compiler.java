package protomoto.bootstrap;

public interface Compiler<T> {
    default T compile() {
        return compile(new CompileContext());
    }
    
    T compile(CompileContext ctx);
}
