package protomoto;

public class ProtoEvaluator implements Evaluator {
    private ProtoEnvironment protoEnvironment;
    private ProtoFrame currentFrame;
    private boolean run = true;

    public ProtoEvaluator(ProtoEnvironment protoEnvironment) {
        this.protoEnvironment = protoEnvironment;
    }
    
    public void setFrame(ProtoFrame frame) {
        currentFrame = frame;
    }

    public void finish() {
        run = false;
    }

    @Override
    public void proceed() {
        currentFrame.evaluate();
    }

    @Override
    public boolean isFinished() {
        return !run;
    }

    @Override
    public Cell getResponse() {
        return currentFrame.peek();
    }

    public Cell createInteger(int value) {
        return protoEnvironment.createInteger(value);
    }

    public ProtoEnvironment getEnvironment() {
        return protoEnvironment;
    }

    public AbstractProtoCell createArray(int length) {
        return protoEnvironment.createArray(length);
    }
}
