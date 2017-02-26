package protomoto;

import java.util.Arrays;
import java.util.List;

public interface ASTFactory<T> {
    T createList(List<T> items);
    default T createList(T... items) {
        return createList(Arrays.asList(items));
    }
    T createString(String str);
    T createInt(int i);
}
