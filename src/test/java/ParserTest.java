import ast.FloatAddition;
import ast.FloatAssignment;
import ast.FloatConstant;
import ast.FloatMultiplication;
import ast.FloatNegation;
import ast.FloatVariable;
import ast.GotoStatement;
import ast.IfStatement;
import ast.Line;
import ast.PrintSeperator;
import ast.PrintStatement;
import ast.Program;
import ast.StringConstant;
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
            "200 PRINT \"A = \" A\n" +
            "300 B = A\n" +
            "400 PRINT \"B = \" B"
        ));
        assertEquals(
            new Program(
                List.of(
                    new Line("100", List.of(new FloatAssignment("A", new FloatConstant(1.0f)))),
                    new Line("200", List.of(new PrintStatement(List.of(new StringConstant("A = "), new FloatVariable("A"))))),
                    new Line("300", List.of(new FloatAssignment("B", new FloatVariable("A")))),
                    new Line("400", List.of(new PrintStatement(List.of(new StringConstant("B = "), new FloatVariable("B")))))
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
                    new Line("100", List.of(new FloatAssignment("A", new FloatConstant(2.0f)))),
                    new Line("200", List.of(
                        new FloatAssignment("A",
                            new FloatAddition(
                                new FloatAddition(
                                    new FloatVariable("A"), new FloatConstant(1.0f)
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
                    new Line("100", List.of(new FloatAssignment("A", new FloatConstant(2.0f)))),
                    new Line("200", List.of(
                        new FloatAssignment("A",
                            new FloatMultiplication(
                                new FloatMultiplication(
                                    new FloatVariable("A"), new FloatConstant(1.0f)
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
                    new Line("100", List.of(new FloatAssignment("A", new FloatConstant(2.0f)))),
                    new Line("200", List.of(
                        new FloatAssignment("A",
                            new FloatAddition(
                                new FloatVariable("A"),
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
                    new Line("100", List.of(new FloatAssignment("A", new FloatConstant(2.0f)))),
                    new Line("200", List.of(
                        new IfStatement(new FloatVariable("A")),
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
                        new FloatAssignment("A", new FloatAddition(
                            new FloatAddition(
                                new FloatMultiplication(
                                    new FloatAddition(new FloatConstant(1.0f), new FloatConstant(2.0f)),
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
                        new FloatAssignment("A",
                            new FloatAddition(
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

}