package protomoto.runtime;

import protomoto.runtime.Evaluator;
import protomoto.runtime.Instruction;
import protomoto.cell.Environment;
import protomoto.cell.BehaviorDescriptor;
import protomoto.cell.BehaviorCell;
import protomoto.cell.AbstractCell;
import protomoto.cell.Cell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class Frame extends AbstractCell {
    private Cell proto;
    private Evaluator evaluator;
    private Frame sender;
    private Instruction[] instructions;
    private int ip;
    private Stack<Cell> stack = new Stack<>();
    
    public Frame(Cell proto, Evaluator evaluator, Frame sender, Instruction[] instructions) {
        this.proto = proto;
        this.evaluator = evaluator;
        this.sender = sender;
        this.instructions = instructions;
    }

    public Environment getEnvironment() {
        return evaluator.getEnvironment();
    }

    public void push(Cell cell) {
        stack.push(cell);
    }

    public void pushFrom(int offset, int count, Cell[] buffer) {
        int end = offset + count;
        for(int i = offset; i < end; i++)
            stack.push(buffer[i]);
    }

    public Cell pop() {
        return stack.pop();
    }

    public void pop(int count) {
        for(int i = 0; i < count; i++)
            stack.pop();
    }

    public int popInto(int offset, int count, Cell[] buffer) {
        if(stack.size() < count)
            return 0;
        
        for(int i = 0; i < count; i++) {
            buffer[offset + count - i - 1] = stack.pop();
        }
        
        return count;
    }

    public Cell peek() {
        return stack.peek();
    }

    public void send(int symbolCode, int arity) {
        /*Cell[] cells = new Cell[1 + arity];
        popInto(0, 1 + arity, cells);
        Cell receiver = cells[0];
        BehaviorCell behavior = receiver.resolveBehavior(evaluator.getEnvironment(), symbolCode);
        
        Frame sendFrame = behavior.createSendFrame(evaluator, this, arity, cells);
        
        evaluator.setFrame(sendFrame);*/
    }

    public void respond() {
        sender.stack.push(this.stack.pop());
        evaluator.setFrame(sender);
        sender.incIP();
    }

    public void jump(int index) {
        this.ip = index;
    }
    
    public void evaluate() {
        instructions[ip].execute(this);
    }

    public void finish(int returnCode) {
        evaluator.finish(returnCode);
    }

    public int peekInto(int offset, int count, Cell[] buffer) {
        if(stack.size() < count)
            return 0;
        
        for(int i = 0; i < count; i++) {
            buffer[offset + count - i] = stack.get(stack.size() - i - 1);
        }
        
        return count;
    }

    public void load(int index) {
        Cell cell = stack.get(index);
        stack.push(cell);
    }

    public void store(int index) {
        Cell cell = stack.pop();
        stack.set(index, cell);
    }

    public void incIP() {
        ip++;
    }

    public void pushi(int i) {
        push(evaluator.getEnvironment().createInteger(i));
    }

    public void newBehavior() {
        Cell ast = stack.pop();
        Cell frameProto = stack.pop();
        ArrayList<String> errors = new ArrayList<>();
        BehaviorDescriptor behaviorDescriptor = evaluator.getEnvironment().createBehavior(null, ast, errors);
        if(errors.size() > 0) {
            String errorString = "Compile error:\n" + errors.stream().collect(Collectors.joining("\n"));
            primitiveErrorOccurred(errorString);
        } else {
            BehaviorCell behavior = behaviorDescriptor.createBehavior(frameProto);
            stack.push(behavior);
        }
    }
    
    public void primitiveErrorOccurred(String error) {
        int primitiveErrorOccurredSymbolCode = getEnvironment().getSymbolCode("primitiveErrorOccurred");
        send(getEnvironment().getFrameProto(), primitiveErrorOccurredSymbolCode, new Cell[]{getEnvironment().createString(error)});
    }
    
    public void send(Cell receiver, int symbolCode, Cell[] args) {
        BehaviorCell behavior = receiver.resolveBehavior(getEnvironment(), symbolCode);
        Cell[] selfAndArgs = new Cell[1 + args.length];
        selfAndArgs[0] = receiver;
        System.arraycopy(args, 0, selfAndArgs, 1, args.length);
        Frame signalFrame = behavior.createSendFrame(evaluator, this, 1, selfAndArgs);
        evaluator.setFrame(signalFrame);
    }

    public void newArray(int length) {
        stack.push(evaluator.getEnvironment().createArray(length));
    }

    public void dup() {
        stack.push(stack.peek());
    }

    public void dup2() {
        stack.add(stack.size() - 2, stack.peek());
    }

    public void dup3() {
        stack.add(stack.size() - 3, stack.peek());
    }

    public void pushs(String string) {
        stack.push(evaluator.getEnvironment().createString(string));
    }

    public void allocate(int variableCount) {
        for(int i = 0; i < variableCount; i++) {
            stack.push(null);
        }
    }

    public void popInto(int arity, Frame target) {
        int start = this.stack.size() - arity;
        int end = this.stack.size();
        
        List<Cell> items = this.stack.subList(start, end);
        target.stack.addAll(items);
        this.stack.subList(start, end).clear();
    }

    @Override
    public Cell resolveProto(Environment environment) {
        return proto;
    }

    @Override
    public BehaviorCell resolveEvaluateBehavior() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Cell cloneCell() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String toString() {
        return Arrays.toString(instructions);// + "/" + stack.toString();
    }

    public Evaluator getEvalutator() {
        return evaluator;
    }

    public void replaceInstruction(Instruction instruction) {
        instructions[ip] = instruction;
    }

    public Cell peek(int delta) {
        return stack.get(stack.size() - 1 - delta);
    }
}
