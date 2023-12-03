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

}