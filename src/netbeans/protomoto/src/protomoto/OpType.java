package protomoto;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface OpType {
    public void encode(Instruction instruction, int opIndex, DataOutputStream dataOutput);

    public Object decode(DataInputStream dataInput);
}
