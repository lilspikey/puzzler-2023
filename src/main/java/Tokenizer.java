import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Tokenizer {
    private static final String SYMBOL_CHARS = "=>+-*/";
    private final Map<String, Keyword> keywordMapping = Arrays.stream(Keyword.values())
        .collect(Collectors.toMap(String::valueOf, Function.identity()));
    private final PushbackReader reader;
    private Token peeked;

    public Tokenizer(Reader reader) {
        this.reader = new PushbackReader(reader);
    }

    public Token peek() throws IOException {
        if (peeked == null) {
            peeked = readNext();
        }
        return peeked;
    }

    public Token next() throws IOException {
        if (peeked != null) {
            Token next = peeked;
            peeked = null;
            return next;
        }
        return readNext();
    }

    private Token readNext() throws IOException {
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
                String text = consumeAllMatching(
                    this::isUppercaseAlphabetic,
                    builder -> isKeyword(builder.toString())
                );
                if (keywordMapping.containsKey(text)) {
                    return new Token(text, Token.Type.KEYWORD);
                }
                return new Token(text, Token.Type.NAME);
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
        return consumeAllMatching(test, s -> false);
    }

    private String consumeAllMatching(Predicate<Character> charTest, Predicate<StringBuilder> tokenTest) throws IOException {
        StringBuilder builder = new StringBuilder();
        while (true) {
            int c = reader.read();
            if (c != -1 && charTest.test((char) c)) {
                builder.append((char) c);
            } else {
                reader.unread(c);
                break;
            }
            if (tokenTest.test(builder)) {
                break;
            }
        }
        return builder.toString();
    }

    private boolean isKeyword(String text) {
        return keywordMapping.containsKey(text);
    }

    private boolean isUppercaseAlphabetic(int c) {
        return 'A' <= c && c <= 'Z';
    }

    private boolean isDigit(int c) {
        return '0' <= c && c <= '9';
    }

    private boolean isSymbol(int c) {
        return SYMBOL_CHARS.indexOf(c) != -1;
    }

}