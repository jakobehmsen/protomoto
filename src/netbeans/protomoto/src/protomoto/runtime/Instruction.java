package protomoto.runtime;

public interface Instruction {
    default void emit(Jitter jitter) {
        new String();
    }
    void execute(Frame frame);
}
