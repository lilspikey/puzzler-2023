public record Token(String text, Type type) {
    public Keyword asKeyword() {
        if (type != Type.KEYWORD) {
            throw new IllegalStateException(text + " is not a keyword");
        }
        return Keyword.valueOf(text);
    }

    public enum Type {
        KEYWORD,
        FUNCTION,
        NUMBER,
        NAME,
        STRING,
        SYMBOL,
        EOL,
        EOF
    }
}
