package protomoto.runtime;

import protomoto.cell.Cell;
import protomoto.cell.Environment;

public interface Hotspot1 extends Hotspot {
    @Override
    default int getArity() { return 1; }
    Cell evaluate(Environment environment, Cell self, Cell arg0);
}
