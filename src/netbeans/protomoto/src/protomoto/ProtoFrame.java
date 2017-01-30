package protomoto;

import java.util.List;
import java.util.Stack;

public class ProtoFrame implements Frame {
    private ProtoEvaluator evaluator;
    private ProtoFrame sender;
    private Instruction[] instructions;
    private int ip;
    private Stack<Cell> stack = new Stack<>();
    
    public ProtoFrame(ProtoEvaluator evaluator, ProtoFrame sender, Instruction[] instructions) {
        this.evaluator = evaluator;
        this.sender = sender;
        this.instructions = instructions;
    }

    @Override
    public void push(Cell cell) {
        stack.push(cell);
    }

    @Override
    public void pushFrom(int offset, int count, Cell[] buffer) {
        for(int i = 0; i < count; i++)
            stack.push(buffer[i]);
    }

    @Override
    public Cell pop() {
        return stack.pop();
    }

    @Override
    public void pop(int count) {
        for(int i = 0; i < count; i++)
            stack.pop();
    }

    @Override
    public int popInto(int offset, int count, Cell[] buffer) {
        if(stack.size() < count)
            return 0;
        
        for(int i = 0; i < count; i++) {
            buffer[offset + count - i - 1] = stack.pop();
        }
        
        return count;
    }

    @Override
    public Cell peek() {
        return stack.peek();
    }

    @Override
    public void send(int symbolCode, int arity) {
        // Make new GIT repo for protomoto
        // Cells pushed from the outside isn't necesary AbstractProtoCells
        AbstractProtoCell receiver = (AbstractProtoCell) stack.pop();
        BehaviorProtoCell behavior = receiver.resolveBehavior(evaluator.getEnvironment(), symbolCode);
        
        ProtoFrame sendFrame = behavior.createSendFrame(evaluator, this, receiver, arity);
        
        evaluator.setFrame(sendFrame);
    }

    @Override
    public void respond() {
        sender.stack.push(this.stack.pop());
        evaluator.setFrame(sender);
        sender.incIP();
    }

    @Override
    public void jump(int index) {
        this.ip = index;
    }
    
    public void evaluate() {
        instructions[ip].execute(this);
    }

    @Override
    public void finish() {
        evaluator.finish();
    }

    @Override
    public int peekInto(int offset, int count, Cell[] buffer) {
        if(stack.size() < count)
            return 0;
        
        for(int i = 0; i < count; i++) {
            buffer[offset + count - i] = stack.get(stack.size() - i - 1);
        }
        
        return count;
    }

    @Override
    public void load(int index) {
        Cell cell = stack.get(index);
        stack.push(cell);
    }

    @Override
    public void store(int index) {
        Cell cell = stack.pop();
        stack.set(index, cell);
    }

    @Override
    public void incIP() {
        ip++;
    }

    @Override
    public void pushi(int i) {
        push(evaluator.createInteger(i));
    }

    @Override
    public void newBehavior() {
        AbstractProtoCell ast = (AbstractProtoCell) stack.pop();
        BehaviorProtoCell behavior = evaluator.getEnvironment().createBehavior(ast);
        stack.push(behavior);
    }

    @Override
    public void newArray(int length) {
        stack.push(evaluator.createArray(length));
    }

    @Override
    public void dup() {
        stack.push(stack.peek());
    }

    @Override
    public void pushs(String string) {
        stack.push(evaluator.getEnvironment().createString(string));
    }

    public void allocate(int variableCount) {
        for(int i = 0; i < variableCount; i++) {
            stack.push(null);
        }
    }

    public void popInto(int arity, ProtoFrame target) {
        int start = this.stack.size() - arity;
        int end = this.stack.size();
        
        List<Cell> items = this.stack.subList(start, end);
        target.stack.addAll(items);
        this.stack.subList(start, end).clear();
    }
}
