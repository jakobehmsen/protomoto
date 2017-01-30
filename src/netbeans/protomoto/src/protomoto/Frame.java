package protomoto;

public interface Frame {
    void push(Cell cell);
    void pushi(int i);
    void pushFrom(int offset, int count, Cell[] buffer);
    Cell pop();
    void pop(int count);
    int popInto(int offset, int count, Cell[] buffer);
    Cell peek();
    int peekInto(int offset, int count, Cell[] buffer);
    void send(int symbolCode, int arity);
    void jump(int index);
    void load(int index);
    void store(int index);
    void respond();
    void finish();
    void incIP();
    void newBehavior();
    void newArray(int length);
    void dup();
    void pushs(String string);
}
