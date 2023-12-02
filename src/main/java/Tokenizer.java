import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Tokenizer {
    private final Map<String, Keyword> keywordMapping = Arrays.stream(Keyword.values())
        .collect(Collectors.toMap(String::valueOf, Function.identity()));
    private final PushbackReader reader;

    public Tokenizer(Reader reader) {
        this.reader = new PushbackReader(reader);
    }

    public Token next() throws IOException {
        while (true) {
            int c = reader.read();
            if (c == -1) {
                return new Token(null, Token.Type.EOF);
            }
            if (c == '\n') {
                return new Token("\n", Token.Type.EOL);
            }
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (isUppercaseAlphabetic(c)) {
                reader.unread(c);
                String text = consumeAllMatching(this::isUppercaseAlphabetic);
                if (keywordMapping.containsKey(text)) {
                    return new Token(text, Token.Type.KEYWORD);
                }
                return new Token(text, Token.Type.VAR);
            }
            if (isDigit(c)) {
                reader.unread(c);
                String text = consumeAllMatching(this::isDigit);
                return new Token(text, Token.Type.NUMBER);
            }
            if (isSymbol(c)) {
                reader.unread(c);
                String text = consumeAllMatching(this::isSymbol);
                return new Token(text, Token.Type.SYMBOL);
            }
            if (c == '\"') {
                String text = consumeAllMatching(ch -> ch != '\"');
                if (reader.read() != '\"') {
                    throw new IllegalStateException("Expected end of string");
                }
                return new Token(text, Token.Type.STRING);
            }
        }
    }

    private String consumeAllMatching(Predicate<Character> test) throws IOException {
        StringBuilder builder = new StringBuilder();
        while (true) {
            int c = reader.read();
            if (c != -1 && test.test((char) c)) {
                builder.append((char) c);
            } else {
                reader.unread(c);
                break;
            }
        }
        return builder.toString();
    }

    private boolean isUppercaseAlphabetic(int c) {
        return 'A' <= c && c <= 'Z';
    }

    private boolean isDigit(int c) {
        return '0' <= c && c <= '9';
    }

    private boolean isSymbol(int c) {
        return c == '=';
    }

}