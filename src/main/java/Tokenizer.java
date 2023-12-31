import runtime.FunctionDef;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Tokenizer {
    private static final Set<String> SYMBOLS = Set.of(
        "=", "<>", "<", "<=", ">", ">=", "+", "-", "*", "/", "(", ")", ":", ",", ";", "^"
    );
    private static final String SYMBOL_CHARS = SYMBOLS.stream()
            .map(String::chars)
            .flatMap(IntStream::boxed)
            .distinct()
            .map(ch -> String.valueOf((char) (int) ch))
            .collect(Collectors.joining());
    private static final String NAME_SUFFIXES = "$";
    private final Set<String> keywords = Arrays.stream(Keyword.values())
        .map(String::valueOf)
        .collect(Collectors.toSet());
    private final Set<String> functions = FunctionDef.getFunctionDefs().stream()
        .map(FunctionDef::name)
        .collect(Collectors.toSet());
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
                    this::isUppercaseAlphaNumeric,
                    builder -> isKeyword(builder.toString())
                );
                if (isKeyword(text)) {
                    return new Token(text, Token.Type.KEYWORD);
                }
                var suffix = reader.read();
                if (NAME_SUFFIXES.indexOf(suffix) != -1) {
                    text += (char) suffix;
                } else {
                    unreadIfNotEOF(suffix);
                }

                if (isFunction(text)) {
                    return new Token(text, Token.Type.FUNCTION);
                }

                return new Token(text, Token.Type.NAME);
            }
            if (isNumeric(c)) {
                reader.unread(c);
                String text = consumeAllMatching(this::isNumeric);
                return new Token(text, Token.Type.NUMBER);
            }
            if (isSymbol(c)) {
                reader.unread(c);
                String text = consumeSymbol();
                return new Token(text, Token.Type.SYMBOL);
            }
            if (c == '\"') {
                String text = consumeAllMatching(ch -> ch != '\"');
                if (reader.read() != '\"') {
                    throw new TokenizingException("Expected end of string");
                }
                return new Token(text, Token.Type.STRING);
            }
            throw new TokenizingException("Unexpected character: " + (char) c);
        }
    }

    public String readTillEndOfLine() throws IOException {
        return consumeAllMatching(c -> c != '\n');
    }

    private String consumeAllMatching(Predicate<Character> test) throws IOException {
        return consumeAllMatching(test, s -> false);
    }

    private String consumeSymbol() throws IOException {
        StringBuilder builder = new StringBuilder();
        while (true) {
            int c = reader.read();
            if (c != -1 && SYMBOLS.contains(builder.toString() + (char) c)) {
                builder.append((char) c);
            } else {
                unreadIfNotEOF(c);
                break;
            }
        }
        return builder.toString();
    }

    private String consumeAllMatching(Predicate<Character> charTest, Predicate<StringBuilder> tokenTest) throws IOException {
        StringBuilder builder = new StringBuilder();
        while (true) {
            int c = reader.read();
            if (c != -1 && charTest.test((char) c)) {
                builder.append((char) c);
            } else {
                unreadIfNotEOF(c);
                break;
            }
            if (tokenTest.test(builder)) {
                break;
            }
        }
        return builder.toString();
    }

    private void unreadIfNotEOF(int c) throws IOException {
        if (c != -1) {
            reader.unread(c);
        }
    }

    private boolean isKeyword(String text) {
        return keywords.contains(text);
    }

    private boolean isFunction(String text) {
        return functions.contains(text);
    }

    private boolean isUppercaseAlphabetic(int c) {
        return 'A' <= c && c <= 'Z';
    }

    private boolean isUppercaseAlphaNumeric(int c) {
        return 'A' <= c && c <= 'Z' || '0' <= c && c <= '9';
    }

    private boolean isNumeric(int c) {
        return '0' <= c && c <= '9' || c == '.';
    }

    private boolean isSymbol(int c) {
        return SYMBOL_CHARS.indexOf(c) != -1;
    }

}