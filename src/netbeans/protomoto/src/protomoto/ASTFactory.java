package protomoto;

import java.util.List;

public interface ASTFactory<T> {
    T createList(List<T> items);
    T createString(String str);
    T createInt(int i);
}
