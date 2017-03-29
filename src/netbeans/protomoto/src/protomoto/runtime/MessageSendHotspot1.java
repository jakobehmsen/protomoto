package protomoto.runtime;

import protomoto.cell.Cell;
import protomoto.cell.Environment;

public interface MessageSendHotspot1 extends MessageSendHotspot {
    @Override
    default int getArity() { return 1; }
    Cell evaluate(Environment environment, Cell self, Cell arg0);
}
