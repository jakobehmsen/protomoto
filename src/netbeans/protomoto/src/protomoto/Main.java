package protomoto;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
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
        Instruction[] instructions = new Instruction[] {
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
        };
        
        Cell program = AstParser.PARSER.parse(
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
        
        Cell cell = environment.getAnyProto();
        Evaluator evaluator = environment.createEvaluator(cell, program);
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
