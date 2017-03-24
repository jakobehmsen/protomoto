package protomoto.emit;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.stream.IntStream;

public class MetaFrame {
    private int offset = 1; // Implicit room for self
    private Hashtable<String, Integer> nameToIndexMap = new Hashtable<>();
    private String[] parameters;

    public MetaFrame(String[] parameters) {
        this.parameters = parameters;
    }

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

    public boolean isArgument(String name) {
        return Arrays.asList(parameters).stream().anyMatch(p -> p.equals(name));
    }

    public int getArgumentIndex(String name) {
        return IntStream.range(0, parameters.length)
            .filter(i -> parameters[i].equals(name))
            .findFirst()
            .orElse(-1);
    }
}
