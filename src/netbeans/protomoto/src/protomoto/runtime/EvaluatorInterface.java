package protomoto.runtime;

import protomoto.cell.Cell;

public interface EvaluatorInterface {

    public boolean isFinished();

    public void proceed();

    public int getReturnCode();

    public Cell getResponse();
    
}
