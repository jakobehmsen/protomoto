package protomoto.runtime;

import org.objectweb.asm.commons.GeneratorAdapter;

public interface CellDescriptor {

    public void emitBoxing(GeneratorAdapter adapter);
    
}
