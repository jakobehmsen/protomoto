package protomoto.runtime;

public interface Instruction {
    default void emit(Jitter jitter) {
        
    }
    void execute(Frame frame);
}
