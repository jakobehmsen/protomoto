package protomoto.runtime;

import protomoto.cell.Cell;
import protomoto.cell.Environment;

public interface MessageSendHotspot0 extends MessageSendHotspot {
    @Override
    default int getArity() { return 0; }
    Cell evaluate(Environment environment, Cell self);
}
