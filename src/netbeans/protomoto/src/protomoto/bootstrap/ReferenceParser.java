package protomoto.bootstrap;

import java.util.ArrayList;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import protomoto.ASTFactory;

public class ReferenceParser {
    private static final Terminals OPERATORS = Terminals.operators("{", "}", ":", ",");
    private static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();
    private static final Parser<?> TOKENIZER = Parsers.or(Terminals.IntegerLiteral.TOKENIZER, Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER, Terminals.Identifier.TOKENIZER, OPERATORS.tokenizer());
    
    private static Parser<?> term(String... names) {
        return OPERATORS.token(names);
    }

    public static <T> Parser<T> create(ASTFactory<T> factory) {
        Parser<T> INTEGER = Terminals.IntegerLiteral.PARSER.map((java.lang.String str) -> 
            factory.createList(factory.createString("consti"), factory.createInt(Integer.parseInt(str))));
        Parser<T> STRING = Terminals.StringLiteral.PARSER.map((java.lang.String str) ->
            factory.createList(factory.createString("consts"), factory.createString(str)));
        Parser<String> SYMBOL = Terminals.Identifier.PARSER;
        
        Parser<T> atom = INTEGER.or(STRING);
        
        Parser.Reference<T> ref = Parser.newReference();
        
        Parser<T> objectSlot = Parsers.sequence(SYMBOL, term(":"), ref.lazy(), (slot, colon, value) -> factory.createList(
            factory.createString("set_slot"),
            factory.createList(factory.createString("peek")),
            factory.createString(slot),
            value
        ));
        Parser<T> objectLiteral = objectSlot.sepBy(term(",")).map(slots -> {
            ArrayList<T> items = new ArrayList<>();
            
            items.add(factory.createString("push"));
            items.add(factory.createList(factory.createString("clone"), factory.createList(factory.createString("environment"))));
            items.addAll(slots);
            
            return factory.createList(items);
        }).between(term("{"), term("}"));
        Parser<T> expression = Parsers.or(objectLiteral, atom);   
        ref.set(expression);
        
        Parser<T> unit = ref.lazy();//.between(term("("), term(")")).or(atom);
        Parser<T> parser = unit.many().map((java.util.List<T> x) -> (T) factory.createList(x));
        //ref.set(parser);
        return parser.from(TOKENIZER, IGNORED);
    }
}
