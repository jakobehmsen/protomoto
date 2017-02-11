package protomoto.patterns;

import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Tokens;
import org.jparsec.Tokens.Fragment;

public class PatternParser {
    private static final Object ANY_TAG = "ANY";
    
    private static final Parser<Fragment> anyLexer = 
            org.jparsec.pattern.Patterns.isChar('_').toScanner("any").source().map(str -> Tokens.fragment(str, ANY_TAG));
    private static final Parser<Pattern> anyParser = Terminals.fragment(ANY_TAG).map(str -> 
            Patterns.any());
    
    private static final Terminals OPERATORS = Terminals.operators("(", ")", ":", "*");
    private static final Parser<Pattern> INTEGER = Terminals.IntegerLiteral.PARSER.map(str -> Patterns.equalsInt(Integer.parseInt(str)));
    private static final Parser<Pattern> STRING = Terminals.StringLiteral.PARSER.map(str -> 
            Patterns.equalsString(str));
    private static final Parser<Pattern> SYMBOL = Terminals.Identifier.PARSER.map(str -> 
            Patterns.equalsString(str));
    private static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();
    private static final Parser<?> TOKENIZER = Parsers.or(anyLexer, Terminals.IntegerLiteral.TOKENIZER, Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER, Terminals.Identifier.TOKENIZER, OPERATORS.tokenizer());
    
    private static Parser<?> term(String... names) {
        return OPERATORS.token(names);
    }
    
    /*
    return new Function<String, Fragment>() {
      @Override public Fragment apply(String text) {
        return Tokens.fragment(text, tag);
      }
      @Override public String toString() {
        return String.valueOf(tag);
      }
    };
    */
    
    

    private static Parser<Pattern> parser(Parser<Pattern> atom) {
        //Parser<Fragment> anyLexer = org.jparsec.pattern.Patterns.isChar('_').toScanner("any").source().map(str -> Tokens.fragment(str, ANY_TAG));
        //Parser<Pattern> anyParser = Terminals.fragment(ANY_TAG).map(str -> Patterns.any());
        
        Parser.Reference<Pattern> ref = Parser.newReference();
        Parser<Pattern> unit = ref.lazy().between(term("("), term(")")).or(atom);
        
        Parser<ListPattern> listPatternAtom = unit.map(p -> ListPatterns.single(p));
        Parser<String> listPatternCapture = Parsers.sequence(term(":"), Terminals.Identifier.PARSER, (colon, str) -> str);
        
        Parser<ListPattern> listItemPattern = Parsers.sequence(
            Parsers.sequence(listPatternAtom, term("*").asOptional(), (lp, opt) -> opt.isPresent() ? ListPatterns.lazy(lp) : lp),
            listPatternCapture.asOptional(), (lp, opt) -> opt.isPresent() ? ListPatterns.capture(opt.get(), lp) : lp
        );
        Parser<Pattern> patternListPattern = listItemPattern.many().map(lp -> 
            Patterns.subsumesList(ListPatterns.sequence(lp.toArray(new ListPattern[lp.size()]))));
        ref.set(patternListPattern);
        
        return unit;
    }
    
    public static final Parser<Pattern> PARSER = parser(Parsers.or(anyParser, SYMBOL, INTEGER, STRING)).from(TOKENIZER, IGNORED);
}
