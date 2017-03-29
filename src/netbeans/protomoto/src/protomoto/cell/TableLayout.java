package protomoto.cell;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class TableLayout {
    private Map<Integer, Integer> symbolCodeToIndex = new Hashtable<>();
    
    public TableLayout(Map<Integer, Integer> symbolCodeToIndex) {
        this.symbolCodeToIndex = symbolCodeToIndex;
    }
    
    public int[] getSymbolCodes() {
        return symbolCodeToIndex.keySet().stream().mapToInt(k -> k).toArray();
    }
    
    public int getIndex(int symbolCode) {
        return symbolCodeToIndex.get(symbolCode);
    }
    
    private Map<Integer, TableLayout> transitions = new Hashtable<>();
    
    public TableLayout modify(int symbolCode) {
        TableLayout tableLayout = transitions.get(symbolCode);
        if(tableLayout == null) {
            HashSet<Integer> allSymbolCodes = new HashSet<>();
            allSymbolCodes.addAll(symbolCodeToIndex.keySet());
            allSymbolCodes.add(symbolCode);
        
            Hashtable<Integer, Integer> symbolCodeToIndex = new Hashtable<>();
            int index = 0;
            for(Integer sc: allSymbolCodes) {
                symbolCodeToIndex.put(sc, index);
                index++;
            }
            tableLayout = new TableLayout(symbolCodeToIndex);
            transitions.put(symbolCode, tableLayout);
        }
        return tableLayout;
    }
    
    public TableLayout modify(Set<Integer> symbolCodes) {
        HashSet<Integer> allSymbolCodes = new HashSet<>();
        allSymbolCodes.addAll(symbolCodeToIndex.keySet());
        allSymbolCodes.addAll(symbolCodes);
        
        Hashtable<Integer, Integer> symbolCodeToIndex = new Hashtable<>();
        int index = 0;
        for(Integer symbolCode: allSymbolCodes) {
            symbolCodeToIndex.put(symbolCode, index);
            index++;
        }
        
        return new TableLayout(symbolCodeToIndex);
    }
}
