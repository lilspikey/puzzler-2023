import ast.FloatAssignment;
import ast.FloatConstant;
import ast.FloatVariable;
import ast.GotoStatement;
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
                    new PrintStatement("100", List.of(new StringConstant("HELLO WORLD"))),
                    new GotoStatement("200", "100")
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
                    new FloatAssignment("100", "A", new FloatConstant(1.0f)),
                    new PrintStatement("200", List.of(new StringConstant("A = "), new FloatVariable("A"))),
                    new FloatAssignment("300", "B", new FloatVariable("A")),
                    new PrintStatement("400", List.of(new StringConstant("B = "), new FloatVariable("B")))
                )
            ),
            program
        );
    }

}