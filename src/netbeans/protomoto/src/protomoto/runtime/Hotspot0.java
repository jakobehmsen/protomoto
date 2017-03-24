package protomoto.runtime;

import protomoto.cell.Cell;
import protomoto.cell.Environment;

public interface Hotspot0 extends Hotspot {
    @Override
    default int getArity() { return 0; }
    Cell evaluate(Environment environment, Cell self);
}
