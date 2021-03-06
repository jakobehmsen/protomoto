package protomoto.bootstrap.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.Terminals;
import org.jparsec.Token;
import org.jparsec.pattern.Patterns;
import protomoto.ast.ASTFactory;
import protomoto.ast.ASTParser;
import protomoto.cell.ArrayCell;
import protomoto.cell.Cell;
import protomoto.cell.IntegerCell;
import protomoto.cell.StringCell;

public class ReferenceParser {
    private static final Parser<String> IDENTIFIER = Patterns.isChar(Character::isJavaIdentifierStart)
      .next(Patterns.isChar(Character::isJavaIdentifierPart).many())
      .toScanner("word")
      .source();
    
    private static boolean isOperator(char ch) {
        switch(ch) {
            case '+':
            case '-':
            case '*':
            case '/':
                return true;
        }
        
        return false;
    }
    
    public static final Terminals TERMS = Terminals
      .operators("{", "}", ":", ",", "=", "->", "#", "(", ")", ".", "=", "+", "-", "*", "/")
      //.words(Scanners.IDENTIFIER)
      .words(IDENTIFIER)
      .keywords("var")
      .build();
    public static final Parser<Void> IGNORED = Parsers.or(Scanners.JAVA_LINE_COMMENT, Scanners.JAVA_BLOCK_COMMENT, Scanners.WHITESPACES).skipMany();
    public static final Parser<?> TOKENIZER = Parsers.or(
        Terminals.IntegerLiteral.TOKENIZER, 
        Terminals.StringLiteral.SINGLE_QUOTE_TOKENIZER,
        TERMS.tokenizer(),
        Terminals.Identifier.TOKENIZER);
    
    public static Parser<Token> term(String... names) {
        return TERMS.token(names);
    }

    public static Parser<Cell> create() {
        Parser<Compiler> INTEGER = Terminals.IntegerLiteral.PARSER.map((java.lang.String str) -> ctx ->
            new ArrayCell(new Cell[] {new StringCell("consti"), new IntegerCell(Integer.parseInt(str))}));
        Parser<Compiler> STRING = Terminals.StringLiteral.PARSER.map((java.lang.String str) -> ctx ->
            new ArrayCell(new Cell[] {new StringCell("consts"), new StringCell(str)}));
        Parser<String> SYMBOL = Terminals.Identifier.PARSER;
        Parser<String> SYMBOL_OP = Parsers.or(
            Terminals.Identifier.PARSER,
            term("+", "-", "*", "/").map(t -> t.value().toString()));
        
        Parser<Compiler> atom = INTEGER.or(STRING);
        
        Parser.Reference<Compiler> expressionRef = Parser.newReference();
        
        Parser<Compiler> expression = expressionRef.lazy();
        Parser<Compiler> expressions = expression.many()
            .map((java.util.List<Compiler> x) -> ctx -> new ArrayCell(x.stream().map(y -> y.compile(ctx)).toArray(s -> new Cell[s])));
        
        Parser<Compiler> varDeclareAssign = Parsers.sequence(term("var"), SYMBOL, term("="), expressionRef.lazy(), (kwVar, id, equals, value) -> ctx -> {
            ctx.declare(id);
            return new ArrayCell(new Cell[] {
                new StringCell("var"),
                new StringCell(id),
                value.compile(ctx)
            });
        });
        
        // Should depend on whether a var has been declared
        // If a var has been declared, then the same
        // If not, then it should be a (set_slot (environment) <id>, <value>)
        // Use Compiler; return Compiler in parser
        Parser<Compiler> varAssign = Parsers.sequence(SYMBOL, term("="), expressionRef.lazy(), (id, equals, value) -> ctx -> {
            if(ctx.isDeclared(id)) {
                return new ArrayCell(new Cell[] {
                    new StringCell("set"),
                    new StringCell(id),
                    value.compile(ctx)
                });
            } else {
                return new ArrayCell(new Cell[] {
                    new StringCell("set_slot"), 
                    new ArrayCell(new Cell[] {new StringCell("self")}), 
                    new StringCell(id),
                    value.compile(ctx)
                });
            }
        });
        
        Parser<List<Compiler>> messageArgs = expressionRef.lazy().sepBy(term(","));
        
        Parser<Compiler> thisMessageSend = Parsers.sequence(SYMBOL, term("("), messageArgs, term(")"), (symbol, op, args, cp) -> ctx -> {
            return createMessageSend(ctx, symbol, args, new ArrayCell(new Cell[] {new StringCell("self")}));
        });
        
        // Should depend on whether a var has been declared
        // If a var has been declared, then the same
        // If not, then it should be a (get_slot (environment) <id>)
        // Use Compiler; return Compiler in parser
        Parser<Compiler> varRead = SYMBOL.map(id -> ctx -> {
            if(ctx.isDeclared(id)) {
                return new ArrayCell(new Cell[] {
                    new StringCell("get"),
                    new StringCell(id)
                });
            } else {
                return new ArrayCell(new Cell[] {
                    new StringCell("get_slot"), 
                    new ArrayCell(new Cell[] {new StringCell("self")}), 
                    new StringCell(id)
                });
            }
        });
        
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
            
            return new ArrayCell(items.toArray(new Cell[items.size()]));
        });
        objectLiteral = objectLiteral.between(term("{"), term("}"));
        
        Parser<List<String>> behaviorParams = Parsers.sequence(term("("), SYMBOL.sepBy(term(",")), term(")"), (op, params, cp) -> params);
        
        Parser<Compiler> behaviorBody = Parsers.sequence(term("{"), expressions, term("}"), (os, exprs, cs) -> ctx -> 
            // Compile in new compile context
            exprs.compile(ctx)
        );
        
        // How to support creating behaviors with proto frames
        // (behavior (get_slot (environment) 'Frame') () (consts 'Heyyy'))
        /*Parser<Compiler> behavior = Parsers.sequence(behaviorParams, term("->"), behaviorBody, (params, arrow, body) -> ctx -> {
            CompileContext bodyCtx = new CompileContext();
            // new ArrayCell(params.stream().map(p -> new StringCell(p)).toArray(s -> new Cell[s]))
            params.forEach(p -> bodyCtx.declare(p));
            Cell paramsCell = new ArrayCell(params.stream().map(p -> new StringCell(p)).toArray(s -> new Cell[s]));
            return new ArrayCell(new Cell[] {
                new StringCell("behavior"),
                new ArrayCell(new Cell[] {new StringCell("get_slot"), new ArrayCell(new Cell[] {new StringCell("environment")}), new StringCell("Frame")}),
                paramsCell,
                body.compile(bodyCtx)
            });
        });*/
        
        Parser<Compiler> behavior = Parsers.sequence(behaviorParams, term("->"), behaviorBody, (params, arrow, body) -> new Compiler() {
            @Override
            public String modifyId(String id) {
                return id + "$" + params.size();
            }
            
            @Override
            public Cell compile(CompileContext ctx) {
                CompileContext bodyCtx = new CompileContext();
                // new ArrayCell(params.stream().map(p -> new StringCell(p)).toArray(s -> new Cell[s]))
                params.forEach(p -> bodyCtx.declare(p));
                Cell paramsCell = new ArrayCell(params.stream().map(p -> new StringCell(p)).toArray(s -> new Cell[s]));
                return new ArrayCell(new Cell[] {
                    new StringCell("behavior"),
                    new ArrayCell(new Cell[] {new StringCell("get_slot"), new ArrayCell(new Cell[] {new StringCell("environment")}), new StringCell("Frame")}),
                    paramsCell,
                    body.compile(bodyCtx)
                });
            }
        });
        
        Parser<Cell> astParser = ASTParser.create1(new ASTFactory<Cell>() {
            @Override
            public Cell createList(List<Cell> items) {
                return new ArrayCell(items.stream().toArray(s -> new Cell[s]));
            }

            @Override
            public Cell createString(String str) {
                return new StringCell(str);
            }

            @Override
            public Cell createInt(int i) {
                return new IntegerCell(i);
            }
        }, TERMS);
        Parser<Compiler> primitive = Parsers.sequence(term("#"), astParser, (os, ast) -> ctx -> ast);
        
        Parser<Compiler> target = Parsers.or(varDeclareAssign, varAssign, thisMessageSend, varRead, objectLiteral, behavior, primitive, atom);
        
        Parser.Reference<Function<Cell, Compiler>> slotSetOrSlotGetChainRef = Parser.newReference();
        Parser<Function<Cell, Compiler>> slotSetChain = Parsers.sequence(term("."), SYMBOL_OP, term("="), expressionRef.lazy(), (dot, id, eq, value) -> t -> ctx -> {
            String idWithArity = value.modifyId(id);
            return new ArrayCell(new Cell[] {new StringCell("set_slot"), t, new StringCell(idWithArity), value.compile(ctx)});
        });
        Parser<Function<Cell, Compiler>> slotGetChain = Parsers.sequence(term("."), SYMBOL, slotSetOrSlotGetChainRef.lazy().asOptional(), (dot, id, chainOpt) -> t -> new Compiler() {
            @Override
            public Cell compile(CompileContext ctx) {
                Cell t2 = t;
                t2 = new ArrayCell(new Cell[] {new StringCell("get_slot"), t2, new StringCell(id)});
                if(chainOpt.isPresent())
                    return chainOpt.get().apply(t2).compile(ctx);
                return t2;
            }
        });
        Parser<Function<Cell, Compiler>> messageSendChain = Parsers.sequence(
                term("."), SYMBOL_OP, term("("), messageArgs, term(")"), slotSetOrSlotGetChainRef.lazy().asOptional(), 
                (dot, id, op, args, cp, chainOpt) -> t -> new Compiler() {
            @Override
            public Cell compile(CompileContext ctx) {
                Cell t2 = t;
                t2 = createMessageSend(ctx, id, args, t2);
                if(chainOpt.isPresent())
                    return chainOpt.get().apply(t2).compile(ctx);
                return t2;
            }
        });
        
        slotSetOrSlotGetChainRef.set(Parsers.or(messageSendChain, slotSetChain, slotGetChain));
        Parser<Compiler> exprChain = Parsers.sequence(target, slotSetOrSlotGetChainRef.lazy().asOptional(), (t, chain) -> new Compiler() {
            @Override
            public String modifyId(String id) {
                return t.modifyId(id);
            }
            
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
    
    private static Cell createMessageSend(CompileContext ctx, String symbol, List<Compiler> args, Cell receiver) {
        ArrayList<Cell> messageSendBuilder = new ArrayList<>();
            
        messageSendBuilder.add(new StringCell("send"));
        messageSendBuilder.add(receiver);
        messageSendBuilder.add(new StringCell(symbol + "$" + args.size()));

        messageSendBuilder.addAll(args.stream().map(c -> c.compile(ctx)).collect(Collectors.toList()));

        return new ArrayCell(messageSendBuilder.stream().toArray(s -> new Cell[s]));
    }
}
