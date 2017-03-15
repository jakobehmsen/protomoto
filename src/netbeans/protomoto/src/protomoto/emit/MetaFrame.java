package protomoto.emit;

import java.util.Hashtable;

public class MetaFrame {
    private int offset = 1; // Implicit room for self
    private Hashtable<String, Integer> nameToIndexMap = new Hashtable<>();

    public void declareVar(String name) {
        int index = offset + nameToIndexMap.size();
        nameToIndexMap.put(name, index);
    }

    public int indexOf(String name) {
        return nameToIndexMap.getOrDefault(name, -1);
    }
    
    public int variableCount() {
        return nameToIndexMap.size();
    }
}