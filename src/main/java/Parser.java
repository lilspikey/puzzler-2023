import ast.Expression;
import ast.FloatAssignment;
import ast.GotoStatement;
import ast.PrintStatement;
import ast.Program;
import ast.Statement;
import ast.StringConstant;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    public Program parse(Reader source) throws IOException {
        Tokenizer tokenizer = new Tokenizer(source);
        List<Statement> statements = new ArrayList<>();
        Statement statement;
        while ((statement = nextStatement(tokenizer)) != null) {
            statements.add(statement);
        }
        return new Program(statements);
    }

    private Statement nextStatement(Tokenizer tokenizer) throws IOException {
        if (tokenizer.peek().type() == Token.Type.EOF) {
            return null;
        }
        String label = null;
        if (tokenizer.peek().type() == Token.Type.NUMBER) {
            label = tokenizer.next().text();
        }
        Token first = tokenizer.peek();
        Statement statement = null;
        if (first.type() == Token.Type.KEYWORD) {
            Keyword keyword = first.asKeyword();
            statement = switch (keyword) {
                case PRINT -> nextPrintStatement(label, tokenizer);
                case GOTO -> nextGotoStatement(label, tokenizer);
            };
        } else if (first.type() == Token.Type.NAME) {
            return nextFloatAssignment(label, tokenizer);
        }
        Token end = tokenizer.peek();
        if (end.type() != Token.Type.EOL && end.type() != Token.Type.EOF) {
            throw new IllegalArgumentException("Expected end of line or end of file, got:" + end);
        }
        if (end.type() == Token.Type.EOL) {
            tokenizer.next();
        }
        return statement;
    }

    private PrintStatement nextPrintStatement(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.PRINT);
        List<Expression> expressions = new ArrayList<>();
        boolean done = false;
        while (!done) {
            switch (tokenizer.peek().type()) {
                case STRING -> {
                    Token token = tokenizer.next();
                    expressions.add(new StringConstant(token.text()));
                }
                case EOL, EOF -> done = true;
                default -> throw new IllegalStateException("Unexpected token: " + tokenizer.peek());
            }
        }
        return new PrintStatement(label, expressions);
    }

    private GotoStatement nextGotoStatement(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.GOTO);
        Token destinationLabel = nextExpectedNumber(tokenizer);
        return new GotoStatement(label, destinationLabel.text());
    }

    private FloatAssignment nextFloatAssignment(String label, Tokenizer tokenizer) throws IOException {
        Token name = nextExpectedName(tokenizer);
        nextExpectedSymbol(tokenizer, "=");
        Token value = nextExpectedNumber(tokenizer);
        return new FloatAssignment(label, name.text(), Float.parseFloat(value.text()));
    }

    private void nextExpectedKeyword(Tokenizer tokenizer, Keyword expected) throws IOException {
        Token token = tokenizer.next();
        if (token.type() != Token.Type.KEYWORD || token.asKeyword() != expected) {
            throw new IllegalStateException("Expected " + expected + " got: " + token);
        }
    }

    private Token nextExpectedName(Tokenizer tokenizer) throws IOException {
        Token token = tokenizer.next();
        if (token.type() != Token.Type.NAME) {
            throw new IllegalStateException("Expected name got: " + token);
        }
        return token;
    }

    private Token nextExpectedNumber(Tokenizer tokenizer) throws IOException {
        Token token = tokenizer.next();
        if (token.type() != Token.Type.NUMBER) {
            throw new IllegalStateException("Expected number got: " + token);
        }
        return token;
    }

    private Token nextExpectedSymbol(Tokenizer tokenizer, String expected) throws IOException {
        Token token = tokenizer.next();
        if (token.type() != Token.Type.SYMBOL || !expected.equals(token.text())) {
            throw new IllegalStateException("Expected " + expected +" got: " + token);
        }
        return token;
    }
}
