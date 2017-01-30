package protomoto;

public class Evaluator {
    private Environment environment;
    private Frame currentFrame;
    private boolean run = true;

    public Evaluator(Environment environment) {
        this.environment = environment;
    }
    
    public void setFrame(Frame frame) {
        currentFrame = frame;
    }

    public void finish() {
        run = false;
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
}
