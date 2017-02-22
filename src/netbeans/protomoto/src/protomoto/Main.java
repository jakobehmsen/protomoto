package protomoto;

import java.io.IOException;
import java.util.List;
import org.jparsec.Parser;

public class Main {
    public static void main(String[] args) throws IOException {        
        Environment environment = new Environment();
        
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
    if temp at ordinal 3 is ExceptionFrameTag
        return true;
    else
        return false;
}

Exception.signal: aSignal >> {
    var closestExceptionFrame = thisContext.closestExceptionFrame;
    test if handler (aBlock / ordinal 2/) is appropriate
    if not, find next closest exception frame
    repeat till no exception frame is found and handle exception by sending defaultAction to aSignal
}

ExceptionFrameTag = Any.clone

Block.on: aSignal Do: aBlock >> {
    // Something that indiciates that this is an exception frame
    // aSignal is assigned to ordinal 1
    // aBlock is assigned to ordinal 2
    var magic = ExceptionFrameTag;
    // magic is assigned to ordinal 3
    return [
        self.execute()
    ]
}


var x = ...
{
    // x should be accessible
    ...
} on: SomeSignal Do: {
    // x should be accessible
    // Should be run on
}
        
        */
        
        Cell program = cellParser.parse(
            "(set_slot (get_slot (environment) 'Frame') 'whatever' (behavior () (consts 'Heyyy')))\n" +
            "(send (this_frame) 'whatever')\n"
        );
        
        /*Cell program = cellParser.parse(
            "(set_slot (get_slot (environment) 'Integer') '-' (behavior (other) (subi (self) (get other))))\n" +
            "(set_slot (get_slot (environment) 'Integer') '/' (behavior (other) (divi (self) (get other))))\n" +
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
        
        if(evaluator.getReturnCode() == 0) {
            Cell response = evaluator.getResponse();
            System.out.println(response);
        } else {
            Cell response = evaluator.getResponse();
            System.err.println(response);
        }
    }
}
