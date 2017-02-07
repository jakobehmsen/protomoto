package protomoto.patterns;

import protomoto.ast.ASTCell;

public interface ListStream {
    ASTCell peek();
    void consume();
    int remaining();
    ListStreamPosition position();
    ListStream sublist();
}
