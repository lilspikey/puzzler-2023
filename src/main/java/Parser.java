import ast.DataType;
import ast.Expression;
import ast.FloatAddition;
import ast.FloatAssignment;
import ast.FloatConstant;
import ast.FloatInput;
import ast.FloatVariable;
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
                case INPUT -> nextInputStatement(label, tokenizer);
            };
        } else if (first.type() == Token.Type.NAME) {
            statement = nextFloatAssignment(label, tokenizer);
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
                case EOL, EOF -> done = true;
                default -> expressions.add(nextExpression(tokenizer));
            }
        }
        return new PrintStatement(label, expressions);
    }

    private Expression nextExpression(Tokenizer tokenizer) throws IOException {
        Expression lhs = nextAtomExpression(tokenizer);
        while (true) {
            Token maybeOp = tokenizer.peek();
            if (maybeOp.type() == Token.Type.SYMBOL && "+".equals(maybeOp.text())) {
                nextExpectedSymbol(tokenizer, "+");
                Expression rhs = nextAtomExpression(tokenizer);
                lhs = new FloatAddition(lhs, rhs);
            } else {
                return lhs;
            }
        }
    }

    private Expression nextAtomExpression(Tokenizer tokenizer) throws IOException {
        Token token = tokenizer.next();
        return switch (token.type()) {
            case STRING -> new StringConstant(token.text());
            case NUMBER -> new FloatConstant(Float.parseFloat(token.text()));
            case NAME -> new FloatVariable(token.text());
            default -> throw new IllegalStateException("Unexpected token: " + tokenizer.peek());
        };
    }

    private GotoStatement nextGotoStatement(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.GOTO);
        Token destinationLabel = nextExpectedNumber(tokenizer);
        return new GotoStatement(label, destinationLabel.text());
    }

    private FloatInput nextInputStatement(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.INPUT);
        Token name = nextExpectedName(tokenizer);
        return new FloatInput(label, name.text());
    }

    private FloatAssignment nextFloatAssignment(String label, Tokenizer tokenizer) throws IOException {
        Token name = nextExpectedName(tokenizer);
        nextExpectedSymbol(tokenizer, "=");
        Expression expression = nextExpression(tokenizer);
        if (expression.getDataType() != DataType.FLOAT) {
            throw new IllegalStateException("Expected float expression, but got: " + expression);
        }
        return new FloatAssignment(label, name.text(), expression);
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
