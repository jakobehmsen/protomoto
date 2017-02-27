package protomoto.bootstrap;

import java.util.ArrayList;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import protomoto.ASTFactory;

public class ReferenceParser {
    public static final Terminals TERMS = Terminals
      .operators("{", "}", ":", ",", "=")
      .words(Scanners.IDENTIFIER)
      .keywords("var")
      .build();
    public static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();
    public static final Parser<?> TOKENIZER = Parsers.or(
        Terminals.IntegerLiteral.TOKENIZER, 
        Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
        TERMS.tokenizer(),
        Terminals.Identifier.TOKENIZER);
    
    public static Parser<?> term(String... names) {
        return TERMS.token(names);
    }

    public static <T> Parser<T> create(ASTFactory<T> factory) {
        Parser<T> INTEGER = Terminals.IntegerLiteral.PARSER.map((java.lang.String str) -> 
            factory.createList(factory.createString("consti"), factory.createInt(Integer.parseInt(str))));
        Parser<T> STRING = Terminals.StringLiteral.PARSER.map((java.lang.String str) ->
            factory.createList(factory.createString("consts"), factory.createString(str)));
        Parser<String> SYMBOL = Terminals.Identifier.PARSER;
        
        Parser<T> atom = INTEGER.or(STRING);
        
        Parser.Reference<T> expressionRef = Parser.newReference();
        
        Parser<T> varDeclareAssign = Parsers.sequence(term("var"), SYMBOL, term("="), expressionRef.lazy(), (kwVar, id, equals, value) -> factory.createList(
            factory.createString("var"),
            factory.createString(id),
            value
        ));
        
        Parser<T> varAssign = Parsers.sequence(SYMBOL, term("="), expressionRef.lazy(), (id, equals, value) -> factory.createList(
            factory.createString("set"),
            factory.createString(id),
            value
        ));
        
        Parser<T> varRead = SYMBOL.map(id -> factory.createList(
            factory.createString("get"),
            factory.createString(id)
        ));
        
        Parser<T> objectSlot = Parsers.sequence(SYMBOL, term(":"), expressionRef.lazy(), (slot, colon, value) -> factory.createList(
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
        
        expressionRef.set(Parsers.or(varDeclareAssign, varAssign, varRead, objectLiteral, atom));
        
        Parser<T> expression = expressionRef.lazy();
        Parser<T> parser = expression.many().map((java.util.List<T> x) -> (T) factory.createList(x));
        
        return parser.from(TOKENIZER, IGNORED);
    }
}
