package protomoto;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import org.jparsec.Parser;
import protomoto.ast.ASTCell;
import protomoto.ast.ASTInteger;
import protomoto.ast.ASTList;
import protomoto.ast.ASTString;
import protomoto.patterns.ListPatterns;
import protomoto.patterns.Pattern;
import protomoto.patterns.Patterns;

public class Main {
    public static void main(String[] args) throws IOException {
        Pattern p = Patterns.subsumesList(ListPatterns.sequence(
            ListPatterns.capture("b", ListPatterns.lazy(ListPatterns.single(Patterns.any()))),
            ListPatterns.single(Patterns.subsumesList(ListPatterns.sequence(
                ListPatterns.single(Patterns.equalsString("var")), 
                ListPatterns.single(Patterns.capture(Patterns.any(), "n")), 
                ListPatterns.single(Patterns.capture(Patterns.any(), "v"))
            ))),
            ListPatterns.capture("a", ListPatterns.lazy(ListPatterns.single(Patterns.any())))
        ));
        
        Pattern pattern = protomoto.patterns.PatternParser.PARSER.parse("(_*:b (var _:n _:v) _*:a)");
        
        System.out.println("pattern=" + pattern);
        
        p = pattern;
        
        Parser<ASTCell> astCellParser = AstParser.create(new AstFactory<ASTCell>() {
            @Override
            public ASTCell createList(List<ASTCell> items) {
                return new ASTList(items.toArray(new ASTCell[items.size()]));
            }

            @Override
            public ASTCell createString(String str) {
                return new ASTString(str);
            }

            @Override
            public ASTCell createInt(int i) {
                return new ASTInteger(i);
            }
        });
        
        ASTCell c = astCellParser.parse("(Beginning (var x someValue) End)");
        
        Hashtable<String, ASTCell> captures = new Hashtable<>();
        p.matches(c, captures);
        
        Environment environment = new Environment();
        
        environment.getIntegerProto().put(environment.getSymbolCode("+"), new BehaviorCell(new Instruction[]{
            Instructions.load(0),
            Instructions.load(1),
            Instructions.addi(),
            Instructions.respond()
        }, 0));
        
        /*Instruction[] instructions = new Instruction[]{
            Instructions.pushi(5),
            Instructions.pushi(9),
            Instructions.addi(),
            Instructions.finish()
        };*/
        /*Instruction[] instructions = new Instruction[] {
            Instructions.pushi(5),
            Instructions.pushi(9),
            Instructions.send(environment.getSymbolCode("+"), 1),
            Instructions.finish()
        };*/
        /*Instruction[] instructions = new Instruction[] {
            Instructions.pushi(2),
            Instructions.arrayNew(),
            Instructions.dup(),
            Instructions.pushi(0),
            Instructions.pushs("consti"),
            Instructions.arraySet(),
            Instructions.dup(),
            Instructions.pushi(1),
            Instructions.pushi(5),
            Instructions.arraySet(),
            Instructions.newBehavior(),
            Instructions.finish()
        };*/
        
        Parser<Cell> cellParser = AstParser.create(new AstFactory<Cell>() {
            @Override
            public Cell createList(List<Cell> items) {
                return new ArrayCell(items.toArray(new Cell[items.size()]));
            }

            @Override
            public Cell createString(String str) {
                return new StringCell(str);
            }

            @Override
            public Cell createInt(int i) {
                return new IntegerCell(i);
            }
        });
        
        Cell program = cellParser.parse(
            "(var arr (array_new (consti 3)))\n" +
            "(array_set (get arr) (consti 0) (consti 5))\n" +
            "(array_set (get arr) (consti 1) (consti 7))\n" +
            "(array_set (get arr) (consti 2) (consti 9))\n" +
            "(get arr)"
            //"(array_length (get arr))"
        );
        
        /*Cell program = AstParser.PARSER.parse(
            "(set_slot (environment) someValue (consts 'ABC'))\n" +
            "(get_slot (environment) someValue)\n"
        );*/
        
        /*Cell program = AstParser.PARSER.parse(
            "(set_slot (environment) println (behavior (x) (get x)))\n" +
            "(send (environment) println (consts 'Pass it on'))\n"
        );*/
        
        /*Cell program = AstParser.PARSER.parse(
            "(var x (consti 6))\n" +
            "(var y (consti 7))\n" +
            "(var z (consti 8))\n" +
            "(addi (get x) (subi (get y) (get z)))\n"
        );*/
        
        /*Cell program = ast.seq(
            ast.var("x", ast.consti(6)),
            ast.var("y", ast.consti(7)),
            ast.addi(ast.get("x"), ast.get("y"))
        );*/
        
        //Ast ast = new Ast(environment);
        //Cell program = ast.addi(ast.consti(6), ast.consti(7));
        
        Evaluator evaluator = environment.createEvaluator(program);
        int maxProceeds = 1000;
        int proceeds = 0;
        while(!evaluator.isFinished()) {
            evaluator.proceed();
            
            proceeds++;
            
            if(proceeds >= maxProceeds) {
                System.out.println("Proceed some more?");
                System.in.read();
                proceeds = 0;
            }
        }
        Cell response = evaluator.getResponse();
        System.out.println(response);
    }
}
