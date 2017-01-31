package protomoto;

public class Main {
    public static void main(String[] args) {
        Environment environment = new Environment();
        
        environment.getIntegerProto().put(environment.getSymbolCode("+"), new BehaviorProtoCell(new Instruction[]{
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
            Instructions.newArray(),
            Instructions.dup(),
            Instructions.pushi(0),
            Instructions.pushs("consti"),
            Instructions.setArray(),
            Instructions.dup(),
            Instructions.pushi(1),
            Instructions.pushi(5),
            Instructions.setArray(),
            Instructions.newBehavior(),
            Instructions.finish()
        };
        
        Ast ast = new Ast(environment);
        
        Cell program = AstParser.PARSER.parse(
            "(var x (consti 6))" +
            "(var y (consti 7))" +
            "(addi (get x) (get y))"
        );
        
        /*Cell program = ast.seq(
            ast.var("x", ast.consti(6)),
            ast.var("y", ast.consti(7)),
            ast.addi(ast.get("x"), ast.get("y"))
        );*/
        
        //Cell program = ast.addi(ast.consti(6), ast.consti(7));
        
        Cell cell = environment.getAnyProto();
        Evaluator evaluator = environment.createEvaluator(cell, program);
        while(!evaluator.isFinished()) {
            evaluator.proceed();
        }
        Cell response = evaluator.getResponse();
        System.out.println(response);
    }
}
