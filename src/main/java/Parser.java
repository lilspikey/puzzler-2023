import ast.BinaryExpression;
import ast.DataType;
import ast.Expression;
import ast.FloatAddition;
import ast.FloatAssignment;
import ast.FloatConstant;
import ast.FloatDivision;
import ast.FloatEquals;
import ast.FloatGreaterThan;
import ast.FloatGreaterThanEquals;
import ast.FloatInput;
import ast.FloatLessThan;
import ast.FloatLessThanEquals;
import ast.FloatMultiplication;
import ast.FloatNotEquals;
import ast.FloatSubtraction;
import ast.FloatVariable;
import ast.ForStatement;
import ast.GotoStatement;
import ast.IfStatement;
import ast.NextStatement;
import ast.PrintStatement;
import ast.Program;
import ast.RemarkStatement;
import ast.Statement;
import ast.StringConstant;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class Parser {
    private final Map<String, OperatorInfo> operators = Map.of(
        "=", new OperatorInfo(1, FloatEquals::new),
        "<>", new OperatorInfo(1, FloatNotEquals::new),
        ">", new OperatorInfo(1, FloatGreaterThan::new),
        ">=", new OperatorInfo(1, FloatGreaterThanEquals::new),
        "<", new OperatorInfo(1, FloatLessThan::new),
        "<=", new OperatorInfo(1, FloatLessThanEquals::new),
        "+", new OperatorInfo(2, FloatAddition::new),
        "-", new OperatorInfo(2, FloatSubtraction::new),
        "*", new OperatorInfo(3, FloatMultiplication::new),
        "/", new OperatorInfo(4, FloatDivision::new)
    );

    public Program parse(Reader source) throws IOException {
        var tokenizer = new Tokenizer(source);
        var statements = new ArrayList<Statement>();
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
        var first = tokenizer.peek();
        Statement statement = null;
        if (first.type() == Token.Type.KEYWORD) {
            statement = switch (first.asKeyword()) {
                case PRINT -> nextPrintStatement(label, tokenizer);
                case GO -> nextGotoStatement(label, tokenizer);
                case IF -> nextIfStatement(label, tokenizer);
                case INPUT -> nextInputStatement(label, tokenizer);
                case REM -> nextComment(label, tokenizer);
                case FOR -> nextForStatement(label, tokenizer);
                case NEXT -> nextNextStatement(label, tokenizer);
                default -> throw new IllegalStateException("Unexpected token:" + first);
            };
        } else if (first.type() == Token.Type.NAME) {
            statement = nextFloatAssignment(label, tokenizer);
        }
        var end = tokenizer.peek();
        if (end.type() != Token.Type.EOL && end.type() != Token.Type.EOF) {
            throw new IllegalArgumentException("Expected end of line or end of file, got:" + end);
        }
        if (end.type() == Token.Type.EOL) {
            tokenizer.next();
        }
        return statement;
    }

    private ForStatement nextForStatement(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.FOR);
        var varname = nextExpectedName(tokenizer).text();
        nextExpectedSymbol(tokenizer, "=");
        var start = nextExpression(tokenizer);
        nextExpectedKeyword(tokenizer, Keyword.TO);
        var end = nextExpression(tokenizer);
        Expression step = null;
        if (tokenizer.peek().type() == Token.Type.KEYWORD && tokenizer.peek().asKeyword() == Keyword.STEP) {
            nextExpectedKeyword(tokenizer, Keyword.STEP);
            step = nextExpression(tokenizer);
        }
        return new ForStatement(label, varname, start, end, step);
    }

    private NextStatement nextNextStatement(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.NEXT);
        String varname = null;
        if (tokenizer.peek().type() == Token.Type.NAME) {
            varname = tokenizer.next().text();
        }
        return new NextStatement(label, varname);
    }

    private RemarkStatement nextComment(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.REM);
        return new RemarkStatement(label, tokenizer.readTillEndOfLine());
    }

    private PrintStatement nextPrintStatement(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.PRINT);
        var expressions = new ArrayList<Expression>();
        var done = false;
        while (!done) {
            switch (tokenizer.peek().type()) {
                case EOL, EOF -> done = true;
                default -> expressions.add(nextExpression(tokenizer));
            }
        }
        return new PrintStatement(label, expressions);
    }

    private Expression nextExpression(Tokenizer tokenizer) throws IOException {
        return nextExpression(tokenizer, 1);
    }

    private Expression nextExpression(Tokenizer tokenizer, int minPrecedence) throws IOException {
        var lhs = nextAtomExpression(tokenizer);
        while (true) {
            var maybeOp = tokenizer.peek();
            if (maybeOp.type() == Token.Type.SYMBOL && operators.containsKey(maybeOp.text())) {
                var operatorInfo = operators.get(maybeOp.text());
                if (operatorInfo.precedence() < minPrecedence) {
                    break;
                }
                nextExpectedSymbol(tokenizer);
                var rhs = nextExpression(tokenizer, operatorInfo.precedence() + 1);
                lhs = operatorInfo.newOperator.apply(lhs, rhs);
            } else {
                break;
            }
        }
        return lhs;
    }

    private Expression nextAtomExpression(Tokenizer tokenizer) throws IOException {
        var token = tokenizer.peek();
        return switch (token.type()) {
            case STRING -> new StringConstant(tokenizer.next().text());
            case NUMBER -> new FloatConstant(Float.parseFloat(tokenizer.next().text()));
            case NAME -> new FloatVariable(tokenizer.next().text());
            case SYMBOL -> {
                if ("(".equals(token.text())) {
                    yield nextSubExpression(tokenizer);
                }
                throw new IllegalStateException("Unexpected token: " + tokenizer.peek());
            }
            default -> throw new IllegalStateException("Unexpected token: " + tokenizer.peek());
        };
    }

    private Expression nextSubExpression(Tokenizer tokenizer) throws IOException {
        nextExpectedSymbol(tokenizer, "(");
        var expression = nextExpression(tokenizer);
        nextExpectedSymbol(tokenizer, ")");
        return expression;
    }

    private GotoStatement nextGotoStatement(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.GO);
        nextExpectedKeyword(tokenizer, Keyword.TO);
        var destinationLabel = nextExpectedNumber(tokenizer);
        return new GotoStatement(label, destinationLabel.text());
    }

    private IfStatement nextIfStatement(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.IF);
        var predicate = nextExpression(tokenizer);
        nextExpectedKeyword(tokenizer, Keyword.THEN);
        var destinationLabel = nextExpectedNumber(tokenizer);
        // TODO support the more complex type of if
        return new IfStatement(label, predicate, List.of(new GotoStatement(null, destinationLabel.text())));
    }

    private FloatInput nextInputStatement(String label, Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.INPUT);
        var name = nextExpectedName(tokenizer);
        return new FloatInput(label, name.text());
    }

    private FloatAssignment nextFloatAssignment(String label, Tokenizer tokenizer) throws IOException {
        var name = nextExpectedName(tokenizer);
        nextExpectedSymbol(tokenizer, "=");
        Expression expression = nextExpression(tokenizer);
        if (expression.getDataType() != DataType.FLOAT) {
            throw new IllegalStateException("Expected float expression, but got: " + expression);
        }
        return new FloatAssignment(label, name.text(), expression);
    }

    private void nextExpectedKeyword(Tokenizer tokenizer, Keyword expected) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.KEYWORD || token.asKeyword() != expected) {
            throw new IllegalStateException("Expected " + expected + " got: " + token);
        }
    }

    private Token nextExpectedName(Tokenizer tokenizer) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.NAME) {
            throw new IllegalStateException("Expected name got: " + token);
        }
        return token;
    }

    private Token nextExpectedNumber(Tokenizer tokenizer) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.NUMBER) {
            throw new IllegalStateException("Expected number got: " + token);
        }
        return token;
    }

    private Token nextExpectedSymbol(Tokenizer tokenizer) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.SYMBOL) {
            throw new IllegalStateException("Expected symbol got: " + token);
        }
        return token;
    }

    private Token nextExpectedSymbol(Tokenizer tokenizer, String expected) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.SYMBOL || !expected.equals(token.text())) {
            throw new IllegalStateException("Expected " + expected +" got: " + token);
        }
        return token;
    }

    record OperatorInfo(int precedence, BiFunction<Expression, Expression, BinaryExpression> newOperator) {

    }
}
