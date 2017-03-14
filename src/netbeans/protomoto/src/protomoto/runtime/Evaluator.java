package protomoto.runtime;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import protomoto.cell.Environment;
import protomoto.cell.Cell;

public class Evaluator {
    private Environment environment;
    private Frame currentFrame;
    private boolean run = true;
    private int returnCode;

    public Evaluator(Environment environment) {
        this.environment = environment;
    }
    
    public void setFrame(Frame frame) {
        currentFrame = frame;
    }

    public void finish(int returnCode) {
        run = false;
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }
    
    public void proceed() {
        currentFrame.evaluate();
    }

    public boolean isFinished() {
        return !run;
    }

    public Cell getResponse() {
        return currentFrame.peek();
    }

    public Environment getEnvironment() {
        return environment;
    }
    
    private Hashtable<Integer, List<CallSiteBinding>> tagToCallSiteBindings = new Hashtable<>();
    
    public void cache(int tag, Instruction[] instructions, int index) {
        List<CallSiteBinding> callSiteBindings = tagToCallSiteBindings.computeIfAbsent(tag, t -> new ArrayList<>());
        callSiteBindings.add(new CallSiteBinding(index, instructions));
    }
    
    public void uncache(int tag) {
        List<CallSiteBinding> callSiteBindings = tagToCallSiteBindings.computeIfAbsent(tag, t -> new ArrayList<>());
        callSiteBindings.forEach(x -> x.uncache(tag));
        tagToCallSiteBindings.remove(tag);
    }
}
