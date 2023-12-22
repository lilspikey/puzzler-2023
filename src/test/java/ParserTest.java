import ast.DataType;
import ast.Addition;
import ast.FloatConstant;
import ast.FloatMultiplication;
import ast.FloatNegation;
import ast.GotoStatement;
import ast.IfStatement;
import ast.LetStatement;
import ast.Line;
import ast.PrintSeperator;
import ast.PrintStatement;
import ast.Program;
import ast.StringConstant;
import ast.VarName;
import ast.Variable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParserTest {

    @Test
    public void givenBasicLoop_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 PRINT \"HELLO WORLD\"\n" +
            "200 GOTO 100"
        ));
        assertEquals(
            new Program(
                List.of(
                    new Line("100", List.of(new PrintStatement(List.of(new StringConstant("HELLO WORLD"))))),
                    new Line("200", List.of(new GotoStatement("100")))
                )
            ),
            program
        );
    }

    @Test
    public void givenAssignment_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 A = 1\n" +
            "200 PRINT \"A = \" ; A\n" +
            "300 B = A\n" +
            "400 PRINT \"B = \" ; B"
        ));
        assertEquals(
            new Program(
                List.of(
                    new Line("100", List.of(new LetStatement(new VarName("A", DataType.FLOAT), new FloatConstant(1.0f)))),
                    new Line("200", List.of(new PrintStatement(List.of(new StringConstant("A = "), PrintSeperator.NONE, new Variable(new VarName("A", DataType.FLOAT)))))),
                    new Line("300", List.of(new LetStatement(new VarName("B", DataType.FLOAT), new Variable(new VarName("A", DataType.FLOAT))))),
                    new Line("400", List.of(new PrintStatement(List.of(new StringConstant("B = "), PrintSeperator.NONE, new Variable(new VarName("B", DataType.FLOAT))))))
                )
            ),
            program
        );
    }

    @Test
    public void givenAddition_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 A = 2\n" +
            "200 A = A + 1 + 2"
        ));
        // make sure addition is left associative
        assertEquals(
            new Program(
                List.of(
                    new Line("100", List.of(new LetStatement(new VarName("A", DataType.FLOAT), new FloatConstant(2.0f)))),
                    new Line("200", List.of(
                        new LetStatement(new VarName("A", DataType.FLOAT),
                            new Addition(
                                new Addition(
                                    new Variable(new VarName("A", DataType.FLOAT)), new FloatConstant(1.0f)
                                ),
                                new FloatConstant(2.0f)
                            )
                        )
                    ))
                )
            ),
            program
        );
    }

    @Test
    public void givenMultiplication_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 A = 2\n" +
            "200 A = A * 1 * 2"
        ));
        // make sure multiplication is left associative
        assertEquals(
            new Program(
                List.of(
                    new Line("100", List.of(new LetStatement(new VarName("A", DataType.FLOAT), new FloatConstant(2.0f)))),
                    new Line("200", List.of(
                        new LetStatement(new VarName("A", DataType.FLOAT),
                            new FloatMultiplication(
                                new FloatMultiplication(
                                    new Variable(new VarName("A", DataType.FLOAT)), new FloatConstant(1.0f)
                                ),
                                new FloatConstant(2.0f)
                            )
                        )
                    ))
                )
            ),
            program
        );
    }

    @Test
    public void givenMultiplicationAndAddition_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 A = 2\n" +
            "200 A = A + 3 * 2"
        ));
        // make sure multiplication has higher precedence than addition
        assertEquals(
            new Program(
                List.of(
                    new Line("100", List.of(new LetStatement(new VarName("A", DataType.FLOAT), new FloatConstant(2.0f)))),
                    new Line("200", List.of(
                        new LetStatement(new VarName("A", DataType.FLOAT),
                            new Addition(
                                new Variable(new VarName("A", DataType.FLOAT)),
                                new FloatMultiplication(
                                    new FloatConstant(3.0f), new FloatConstant(2.0f)
                                )
                            )
                        )
                    ))
                )
            ),
            program
        );
    }

    @Test
    public void givenIfWithLabel_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 A = 2\n" +
            "200 IF A THEN 100"
        ));
        assertEquals(
            new Program(
                List.of(
                    new Line("100", List.of(new LetStatement(new VarName("A", DataType.FLOAT), new FloatConstant(2.0f)))),
                    new Line("200", List.of(
                        new IfStatement(new Variable(new VarName("A", DataType.FLOAT))),
                        new GotoStatement("100")
                    ))
                )
            ),
            program
        );
    }

    @Test
    public void givenComplexExpression_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 A = (1 + 2) * 4 + 1 + 2"
        ));
        assertEquals(
            new Program(
                List.of(
                    new Line("100", List.of(
                        new LetStatement(new VarName("A", DataType.FLOAT), new Addition(
                            new Addition(
                                new FloatMultiplication(
                                    new Addition(new FloatConstant(1.0f), new FloatConstant(2.0f)),
                                    new FloatConstant(4.0f)
                                ),
                                new FloatConstant(1.0f)
                            ),
                            new FloatConstant(2.0f)
                        ))
                    ))
                )
            ),
            program
        );
    }

    @Test
    public void givenMultiStatementLine_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 PRINT : PRINT \"hi\""
        ));
        assertEquals(
            new Program(List.of(
                new Line("100",
                    List.of(
                        new PrintStatement(List.of()),
                        new PrintStatement(List.of(new StringConstant("hi")))
                    )
                )
            )),
            program
        );
    }

    @Test
    public void givenPrint_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 PRINT \"hi\";"
        ));
        assertEquals(
            new Program(List.of(
                new Line("100",
                    List.of(
                        new PrintStatement(List.of(new StringConstant("hi"), PrintSeperator.NONE))
                    )
                )
            )),
            program
        );
    }

    @Test
    public void givenUnaryExpressions_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 A = -2 + -3 * +7"
        ));
        assertEquals(
            new Program(List.of(
                new Line("100",
                    List.of(
                        new LetStatement(new VarName("A", DataType.FLOAT),
                            new Addition(
                                new FloatNegation(new FloatConstant(2.0f)),
                                new FloatMultiplication(new FloatNegation(new FloatConstant(3.0f)), new FloatConstant(7.0f))
                            )
                        )
                    )
                )
            )),
            program
        );
    }

    @Test
    public void givenArrayVariables_whenParsing_thenProgramReturned() throws IOException {
        Parser parser = new Parser();
        Program program = parser.parse(new StringReader(
            "100 LET A(1) = 0\n"
            + "200 PRINT A(1)"
        ));
        assertEquals(
            new Program(List.of(
                new Line("100",
                    List.of(
                        new LetStatement(new VarName("A()", DataType.FLOAT, List.of(new FloatConstant(1.0f))), new FloatConstant(0.0f))
                    )
                ),
                new Line("200",
                    List.of(
                        new PrintStatement(List.of(new Variable(new VarName("A()", DataType.FLOAT, List.of(new FloatConstant(1.0f))))))
                    )
                )
            )),
            program
        );
    }

}