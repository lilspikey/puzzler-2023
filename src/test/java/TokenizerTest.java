import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenizerTest {

    @Test
    void givenHelloWorld_whenTokenizing_thenTokensReturned() throws IOException {
        assertEquals(
            List.of(
                new Token("100", Token.Type.NUMBER),
                new Token("PRINT", Token.Type.KEYWORD),
                new Token("HELLO WORLD", Token.Type.STRING),
                new Token(null, Token.Type.EOF)
            ),
            tokenize(
                "100 PRINT \"HELLO WORLD\""
            )
        );
    }

    @Test
    void givenBasicLoop_whenTokenizing_thenTokensReturned() throws IOException {
        assertEquals(
            List.of(
                new Token("100", Token.Type.NUMBER),
                new Token("PRINT", Token.Type.KEYWORD),
                new Token("HELLO WORLD", Token.Type.STRING),
                new Token("\n", Token.Type.EOL),
                new Token("200", Token.Type.NUMBER),
                new Token("GOTO", Token.Type.KEYWORD),
                new Token("100", Token.Type.NUMBER),
                new Token(null, Token.Type.EOF)
            ),
            tokenize(
                "100 PRINT \"HELLO WORLD\"\n" +
                "200 GOTO 100"
            )
        );
    }

    @Test
    void givenSimpleAssignment_whenTokenizing_thenTokensReturned() throws IOException {
        assertEquals(
            List.of(
                new Token("100", Token.Type.NUMBER),
                new Token("R", Token.Type.NAME),
                new Token("=", Token.Type.SYMBOL),
                new Token("0", Token.Type.NUMBER),
                new Token(null, Token.Type.EOF)
            ),
            tokenize(
                "100 R=0"
            )
        );
    }

    private List<Token> tokenize(String code) throws IOException {
        List<Token> tokens = new ArrayList<>();
        Tokenizer tokenizer = new Tokenizer(new StringReader(code));
        while (true) {
            Token token = tokenizer.next();
            tokens.add(token);
            if (token.type() == Token.Type.EOF) {
                break;
            }
        }
        return tokens;
    }
}