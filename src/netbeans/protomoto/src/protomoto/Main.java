package protomoto;

import protomoto.ast.ASTParser;
import protomoto.ast.ASTFactory;
import protomoto.runtime.Evaluator;
import protomoto.cell.Environment;
import protomoto.cell.ArrayCell;
import protomoto.cell.IntegerCell;
import protomoto.cell.StringCell;
import protomoto.cell.Cell;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import org.jparsec.Parser;
import org.jparsec.Scanners;
import protomoto.bootstrap.lang.ReferenceParser;
import protomoto.cell.BehaviorCell;
import protomoto.runtime.EvaluatorInterface;
import protomoto.runtime.Instruction;
import protomoto.runtime.Instructions;

public class Main {    
    public static void main(String[] args) throws IOException {
        Environment environment = new Environment();
        
        ASTFactory<Cell> cellFactory = new ASTFactory<Cell>() {
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
        };
        
        Parser<Cell> cellParser2 = ReferenceParser.create();
        Parser<Cell> cellParser = ASTParser.create(cellFactory);
        
        /*
        
        Primitive
    errorOccurred: error

Could it somehow be possible to create custom frames?

Some for exception handling e.g.?

Block
    evaluationContext
    context

Frame
    context >> self
    becomeExceptionFrame:

Frame // Implicitly return to outer frame?
    nest: outer Temps: temps

(var )

Frame.closestExceptionFrame >> {
    return outer.closestExceptionFrame()
}

Exception.signal: aSignal >> {
    var closestExceptionFrame = thisContext.closestExceptionFrame;
    test if handler (aBlock / ordinal 2/) is appropriate
    if not, find next closest exception frame
    repeat till no exception frame is found and handle exception by sending defaultAction to aSignal
}

ExceptionFrame = Frame.clone()
ExceptionFrame.closestExceptionFrame >> {
    return self;
}
ExceptionFrame.handleSignal: signal >> {
    if(getSignal().compatibleWith(signal))
        getHandler().execute(signal);
    else {
        var outerExceptionFrame = outer.closestExceptionFrame()
        return outerExceptionFrame != nil ? outerExceptionFrame.handleSignal(signal) : signal.defaultAction()
    }
}
ExceptionFrame.getSignal >> {
    return self.get(1)
}
ExceptionFrame.getHandler >> {
    return self.get(2)
}
        
Block.on: aSignal Do: aBlock >> {
    self.execute()
} via: ExceptionFrame


var x = ...
{
    // x should be accessible
    ...
} on: SomeSignal Do: {
    // x should be accessible
    // Should be run on
}
        
        */        
        
        //String src2 = "{x: 7, x: 4}";
        /*String src2 = 
            "var x = 46154\n" +
            "x = 2334\n" +
            "x\n";*/
        /*String src2 = 
            "var x = (x, y, z) -> {7}\n" +
            "x\n";*/
        
        //String src2 = "x.y.z = 8\n";
        /*String src2 = 
            //"var x = 46154\n" +
            "Frame.whatever = 'Heyyy'\n" +
            "Frame.whatever\n";*/
        /*String src2 = 
            "print('Heyyy%i', 6)\n";*/
        /*String src2 = 
            "String.test = () -> {'yay'}\n" +
            "String.weeee = 'as'\n" +
            "'afsfds'.test().test().weeee";*/
        
        //java.nio.file.Files.readAllBytes(null)
        
        //environment.getIntegerProto()
        
        environment.getAnyProto().put(environment.getSymbolCode("test"), new BehaviorCell(environment.getFrameProto(), new Instruction[]{
            Instructions.pushs("Hi there"),
            Instructions.respond()
        }, 0));
        
        String src2;
        
        if(args.length > 0) {
            String path = args[0];
            src2 = new String(java.nio.file.Files.readAllBytes(Paths.get(path)));
        } else {
            src2 = 
                "String.test$1 = (arg) -> {arg}\n" +
                "'afsfds'.test('this&that')";
        }
        
        Cell program2 = cellParser2.parse(src2);
        
        String src = 
            "(send (self) 'test')\n";
        
        /*String src = 
            "(set_slot (self) 'field1' (consti 123))\n" +
            "(set_slot (self) 'field2' (consti 456))\n";*/
        
        /*String src = "(push (clone (environment))\n" +
            "    (set_slot (peek) 'x' (consti 1))\n" +
            "    (set_slot (peek) 'y' (consti 4))\n" +
            ")\n";*/
        
        Cell program = cellParser.parse(src);
        
        /*
        Cell program = cellParser.parse(
            "(set_slot (get_slot (environment) 'Frame') 'whatever' (behavior (get_slot (environment) 'Frame') () (consts 'Heyyy')))\n" +
            "(send (this_frame) 'whatever')\n"
        );
        */
        
        /*Cell program = cellParser.parse(
            "(set_slot (get_slot (environment) 'Integer') '-' (behavior (get_slot (environment) 'Frame') (other) (subi (self) (get other))))\n" +
            "(set_slot (get_slot (environment) 'Integer') '/' (behavior (get_slot (environment) 'Frame') (other) (divi (self) (get other))))\n" +
            "(set_slot (environment) someField (consts 'ABC'))\n" +
            "(var someClone (clone (environment)))\n" +
            "(get_slot (get someClone) someField)\n" +
            "(send (consti 100) '/' (consti 0))\n"
        );*/
        
        /*Cell program = cellParser.parse(
            "(set arr (array_new (consti 3)))\n" +
            "(array_set (get arr) (consti 0) (consti 5))\n" +
            "(array_set (get arr) (consti 1) (consti 7))\n" +
            "(array_set (get arr) (consti 2) (consti 9))\n" +
            "(get arr)"
        );*/
        
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
        
        EvaluatorInterface evaluator = environment.createEvaluator(program);
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
        
        if(evaluator.getReturnCode() == 0) {
            Cell response = evaluator.getResponse();
            System.out.println(response);
        } else {
            Cell response = evaluator.getResponse();
            System.err.println(response);
        }
    }
}
