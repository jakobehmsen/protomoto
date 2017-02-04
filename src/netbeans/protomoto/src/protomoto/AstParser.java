package protomoto;

import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;

public class AstParser {
    private static final Parser<Cell> INTEGER = Terminals.IntegerLiteral.PARSER.map((java.lang.String str) -> new IntegerCell(Integer.parseInt(str)));
    private static final Parser<Cell> STRING = Terminals.StringLiteral.PARSER.map((java.lang.String str) -> new StringCell(str));
    private static final Parser<Cell> SYMBOL = Terminals.Identifier.PARSER.map((java.lang.String str) -> new StringCell(str));
    private static final Terminals OPERATORS = Terminals.operators("(", ")");
    private static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();
    private static final Parser<?> TOKENIZER = Parsers.or(Terminals.IntegerLiteral.TOKENIZER, Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER, Terminals.Identifier.TOKENIZER, OPERATORS.tokenizer());
    
    private static Parser<?> term(String... names) {
        return OPERATORS.token(names);
    }

    private static Parser<Cell> calculator(Parser<Cell> atom) {
        Parser.Reference<Cell> ref = Parser.newReference();
        Parser<Cell> unit = ref.lazy().between(term("("), term(")")).or(atom);
        Parser<Cell> parser = unit.many().map((java.util.List<protomoto.Cell> x) -> 
            (Cell) new ArrayCell(x.toArray(new Cell[x.size()])));
        ref.set(parser);
        return parser;
    }
    
    public static final Parser<Cell> PARSER = calculator(INTEGER.or(STRING).or(SYMBOL)).from(TOKENIZER, IGNORED);
}
