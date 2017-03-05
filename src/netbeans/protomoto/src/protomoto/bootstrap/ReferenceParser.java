package protomoto.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import protomoto.ASTFactory;
import protomoto.ArrayCell;
import protomoto.Cell;
import protomoto.IntegerCell;
import protomoto.StringCell;

public class ReferenceParser {
    public static final Terminals TERMS = Terminals
      .operators("{", "}", ":", ",", "=", "->", "(", ")", ".", "=")
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

    public static Parser<Cell> create() {
        Parser<Compiler> INTEGER = Terminals.IntegerLiteral.PARSER.map((java.lang.String str) -> ctx ->
            new ArrayCell(new Cell[] {new StringCell("consti"), new IntegerCell(Integer.parseInt(str))}));
        Parser<Compiler> STRING = Terminals.StringLiteral.PARSER.map((java.lang.String str) -> ctx ->
            new ArrayCell(new Cell[] {new StringCell("consts"), new StringCell(str)}));
        Parser<String> SYMBOL = Terminals.Identifier.PARSER;
        
        Parser<Compiler> atom = INTEGER.or(STRING);
        
        Parser.Reference<Compiler> expressionRef = Parser.newReference();
        
        Parser<Compiler> expression = expressionRef.lazy();
        Parser<Compiler> expressions = expression.many()
            .map((java.util.List<Compiler> x) -> ctx -> new ArrayCell(x.stream().map(y -> y.compile(ctx)).toArray(s -> new Cell[s])));
        
        Parser<Compiler> varDeclareAssign = Parsers.sequence(term("var"), SYMBOL, term("="), expressionRef.lazy(), (kwVar, id, equals, value) -> ctx -> new ArrayCell(new Cell[] {
            new StringCell("var"),
            new StringCell(id),
            value.compile(ctx)
        }));
        
        // Should depend on whether a var has been declared
        // If a var has been declared, then the same
        // If not, then it should be a (set_slot (environment) <id>, <value>)
        // Use Compiler; return Compiler in parser
        Parser<Compiler> varAssign = Parsers.sequence(SYMBOL, term("="), expressionRef.lazy(), (id, equals, value) -> ctx -> new ArrayCell(new Cell[] {
            new StringCell("set"),
            new StringCell(id),
            value.compile(ctx)
        }));
        
        // Should depend on whether a var has been declared
        // If a var has been declared, then the same
        // If not, then it should be a (get_slot (environment) <id>)
        // Use Compiler; return Compiler in parser
        Parser<Compiler> varRead = SYMBOL.map(id -> ctx -> new ArrayCell(new Cell[] {
            new StringCell("get"),
            new StringCell(id)
        }));
        
        Parser<Compiler> objectSlot = Parsers.sequence(SYMBOL, term(":"), expressionRef.lazy(), (slot, colon, value) -> ctx -> new ArrayCell(new Cell[] {
            new StringCell("set_slot"),
            new ArrayCell(new Cell[] {new StringCell("peek")}),
            new StringCell(slot),
            value.compile(ctx)
        }));
        Parser<Compiler> objectLiteral = objectSlot.sepBy(term(",")).map(slots -> (CompileContext ctx) -> {
            ArrayList<Cell> items = new ArrayList<>();
            
            items.add(new StringCell("push"));
            items.add(new ArrayCell(new Cell[] {new StringCell("clone"), new ArrayCell(new Cell[] {new StringCell("environment")})}));
            for(Compiler s: slots) {
                items.add(s.compile(ctx));
            }
            //slots.forEach(s -> items.add(s.compile(ctx)));
            //items.addAll(slots.stream().map(x -> x.compile(ctx)).collect(Collectors.toList()));
            
            return new ArrayCell(items.toArray(new Cell[items.size()]));
        });
        objectLiteral = objectLiteral.between(term("{"), term("}"));
        /*Parser<Compiler> objectLiteral2 = objectSlot.sepBy(term(",")).map(slots -> new Compiler() {
            @Override
            public Cell compile(CompileContext ctx) {
                ArrayList<Cell> items = new ArrayList<>();

                items.add(new StringCell("push"));
                items.add(new ArrayCell(new Cell[] {new StringCell("clone"), new ArrayCell(new Cell[] {new StringCell("environment")})}));
                items.addAll(slots.stream().map(x -> x.compile(ctx)).collect(Collectors.toList()));

                return new ArrayCell(Arrays.asList(items));
            }
        }).between(term("{"), term("}"));*/
        
        Parser<Compiler> behaviorParams = Parsers.sequence(term("("), SYMBOL.sepBy(term(",")), term(")"), (op, params, cp) -> ctx -> 
            new ArrayCell(params.stream().map(p -> new StringCell(p)).toArray(s -> new Cell[s]))
        );
        
        Parser<Compiler> behaviorBody = Parsers.sequence(term("{"), expressions, term("}"), (os, exprs, cs) -> exprs);
        
        // How to support creating behaviors with proto frames
        // (behavior (get_slot (environment) 'Frame') () (consts 'Heyyy'))
        Parser<Compiler> behavior = Parsers.sequence(behaviorParams, term("->"), behaviorBody, (params, arrow, body) -> ctx -> new ArrayCell(new Cell[] {
            new StringCell("behavior"),
            new ArrayCell(new Cell[] {new StringCell("get_slot"), new ArrayCell(new Cell[] {new StringCell("environment")}), new StringCell("Frame")}),
            params.compile(ctx),
            body.compile(ctx)
        }));
        
        Parser<Compiler> target = Parsers.or(varDeclareAssign, varAssign, varRead, objectLiteral, behavior, atom);
        
        Parser.Reference<Function<Cell, Compiler>> slotSetOrSlotGetChainRef = Parser.newReference();
        Parser<Function<Cell, Compiler>> slotSetChain = Parsers.sequence(term("."), SYMBOL, term("="), expressionRef.lazy(), (dot, id, eq, value) -> t -> ctx ->
            new ArrayCell(new Cell[] {new StringCell("set_slot"), t, new StringCell(id), value.compile(ctx)}));
        /*Parser<Function<Cell, Compiler>> slotGetChain = Parsers.sequence(term("."), SYMBOL, slotSetOrSlotGetChainRef.lazy().asOptional(), (dot, id, chainOpt) -> t -> ctx -> {
            Cell t2 = t;
            t2 = new ArrayCell(new Cell[] {new StringCell("get_slot"), t, new StringCell(id)});
            if(chainOpt.isPresent())
                return chainOpt.get().apply(t2);
            return t2;
        });*/
        Parser<Function<Cell, Compiler>> slotGetChain = Parsers.sequence(term("."), SYMBOL, slotSetOrSlotGetChainRef.lazy().asOptional(), (dot, id, chainOpt) -> t -> new Compiler() {
            @Override
            public Cell compile(CompileContext ctx) {
                Cell t2 = t;
                t2 = new ArrayCell(new Cell[] {new StringCell("get_slot"), t, new StringCell(id)});
                if(chainOpt.isPresent())
                    return chainOpt.get().apply(t2).compile(ctx);
                return t2;
            }
        });
        
        slotSetOrSlotGetChainRef.set(Parsers.or(slotSetChain, slotGetChain));
        /*Parser<Compiler> exprChain = Parsers.sequence(target, slotSetOrSlotGetChainRef.lazy().asOptional(), (t, chain) -> ctx -> {
            if(chain.isPresent())
                return chain.get().apply(t.compile(ctx));
            return t.compile(ctx);
        });*/
        Parser<Compiler> exprChain = Parsers.sequence(target, slotSetOrSlotGetChainRef.lazy().asOptional(), (t, chain) -> new Compiler() {
            @Override
            public Cell compile(CompileContext ctx) {
                if(chain.isPresent())
                    return chain.get().apply(t.compile(ctx)).compile(ctx);
                return t.compile(ctx);
            }
        });
        
        // Chain with slotGet* slotSet?
        expressionRef.set(exprChain);
        
        return expressions.from(TOKENIZER, IGNORED).map(compiler -> compiler.compile());
    }
}
