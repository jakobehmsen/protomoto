package protomoto;

import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;

public class AstParser<T> {
    private static final Terminals OPERATORS = Terminals.operators("(", ")");
    private static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();
    private static final Parser<?> TOKENIZER = Parsers.or(Terminals.IntegerLiteral.TOKENIZER, Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER, Terminals.Identifier.TOKENIZER, OPERATORS.tokenizer());
    
    private static Parser<?> term(String... names) {
        return OPERATORS.token(names);
    }

    public static <T> Parser<T> create(AstFactory<T> factory) {
        Parser<T> INTEGER = Terminals.IntegerLiteral.PARSER.map((java.lang.String str) -> factory.createInt(Integer.parseInt(str)));
        Parser<T> STRING = Terminals.StringLiteral.PARSER.map((java.lang.String str) -> factory.createString(str));
        Parser<T> SYMBOL = Terminals.Identifier.PARSER.map((java.lang.String str) -> factory.createString(str));
        
        Parser<T> atom = INTEGER.or(STRING).or(SYMBOL);
        
        Parser.Reference<T> ref = Parser.newReference();
        Parser<T> unit = ref.lazy().between(term("("), term(")")).or(atom);
        Parser<T> parser = unit.many().map((java.util.List<T> x) -> (T) factory.createList(x));
        ref.set(parser);
        return parser.from(TOKENIZER, IGNORED);
    }
}
