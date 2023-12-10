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
import ast.FunctionCall;
import ast.GotoStatement;
import ast.IfStatement;
import ast.Line;
import ast.NextStatement;
import ast.PrintStatement;
import ast.Program;
import ast.RemarkStatement;
import ast.Statement;
import ast.StringConstant;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
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
        var lines = new ArrayList<Line>();
        Line line;
        while ((line = nextLine(tokenizer)) != null) {
            lines.add(line);
        }
        lines.sort(Comparator.comparing(Line::numericLabel));
        return new Program(lines);
    }

    private Line nextLine(Tokenizer tokenizer) throws IOException {
        if (tokenizer.peek().type() == Token.Type.EOF) {
            return null;
        }
        var label = nextExpectedNumber(tokenizer).text();
        var statements = nextStatements(tokenizer);
        return new Line(label, statements);
    }

    private List<Statement> nextStatements(Tokenizer tokenizer) throws IOException {
        List<Statement> statements = new ArrayList<>();
        while (true) {
            var statement = nextStatement(tokenizer);
            statements.add(statement);
            if (statement instanceof IfStatement) {
                statements.add(parseThenStatement(tokenizer));
            }
            var next = tokenizer.peek();
            if (next.type() == Token.Type.EOL) {
                tokenizer.next();
                break;
            } else if (next.type() == Token.Type.EOF) {
                break;
            } else if (next.type() == Token.Type.SYMBOL) {
                if (":".equals(next.text())) {
                    tokenizer.next();
                    continue;
                }
            }
            throw new IllegalArgumentException("Expected end of line or end of file, got:" + next);
        }
        return statements;
    }

    private Statement nextStatement(Tokenizer tokenizer) throws IOException {
        var first = tokenizer.peek();
        if (first.type() == Token.Type.KEYWORD) {
            return switch (first.asKeyword()) {
                case PRINT -> nextPrintStatement(tokenizer);
                case GO -> nextGotoStatement(tokenizer);
                case IF -> nextIfStatement(tokenizer);
                case INPUT -> nextInputStatement(tokenizer);
                case REM -> nextComment(tokenizer);
                case FOR -> nextForStatement(tokenizer);
                case NEXT -> nextNextStatement(tokenizer);
                default -> throw new IllegalStateException("Unexpected token:" + first);
            };
        } else if (first.type() == Token.Type.NAME) {
            return nextFloatAssignment(tokenizer);
        }
        throw new IllegalStateException("Unexpected token: " + first);
    }

    private Statement parseThenStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.THEN);
        // see if we have implicit GOTO
        if (tokenizer.peek().type() == Token.Type.NUMBER) {
            var destinationLabel = nextExpectedNumber(tokenizer);
            return new GotoStatement(destinationLabel.text());
        } else {
            return nextStatement(tokenizer);
        }
    }

    private ForStatement nextForStatement(Tokenizer tokenizer) throws IOException {
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
        return new ForStatement(varname, start, end, step);
    }

    private NextStatement nextNextStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.NEXT);
        String varname = null;
        if (tokenizer.peek().type() == Token.Type.NAME) {
            varname = tokenizer.next().text();
        }
        return new NextStatement(varname);
    }

    private RemarkStatement nextComment(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.REM);
        return new RemarkStatement(tokenizer.readTillEndOfLine());
    }

    private PrintStatement nextPrintStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.PRINT);
        var expressions = new ArrayList<Expression>();
        var done = false;
        while (!done) {
            var next = tokenizer.peek();
            switch (next.type()) {
                case EOL, EOF -> done = true;
                default -> {
                    if (next.type() == Token.Type.SYMBOL && ":".equals(next.text())) {
                        done = true;
                    } else {
                        expressions.add(nextExpression(tokenizer));
                    }
                }
            }
        }
        return new PrintStatement(expressions);
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
            case FUNCTION -> nextFunctionCall(tokenizer);
            case SYMBOL -> {
                if ("(".equals(token.text())) {
                    yield nextSubExpression(tokenizer);
                }
                throw new IllegalStateException("Unexpected token: " + tokenizer.peek());
            }
            default -> throw new IllegalStateException("Unexpected token: " + tokenizer.peek());
        };
    }

    private FunctionCall nextFunctionCall(Tokenizer tokenizer) throws IOException {
        var fun = nextExpectedFunction(tokenizer);
        nextExpectedSymbol(tokenizer, "(");
        var arg = nextExpression(tokenizer);
        nextExpectedSymbol(tokenizer, ")");
        return new FunctionCall(fun.text(), arg);
    }

    private Expression nextSubExpression(Tokenizer tokenizer) throws IOException {
        nextExpectedSymbol(tokenizer, "(");
        var expression = nextExpression(tokenizer);
        nextExpectedSymbol(tokenizer, ")");
        return expression;
    }

    private GotoStatement nextGotoStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.GO);
        nextExpectedKeyword(tokenizer, Keyword.TO);
        var destinationLabel = nextExpectedNumber(tokenizer);
        return new GotoStatement(destinationLabel.text());
    }

    private IfStatement nextIfStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.IF);
        var predicate = nextExpression(tokenizer);
        peekExpectedKeyword(tokenizer, Keyword.THEN);
        return new IfStatement(predicate);
    }

    private FloatInput nextInputStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.INPUT);
        var name = nextExpectedName(tokenizer);
        return new FloatInput(name.text());
    }

    private FloatAssignment nextFloatAssignment(Tokenizer tokenizer) throws IOException {
        var name = nextExpectedName(tokenizer);
        nextExpectedSymbol(tokenizer, "=");
        Expression expression = nextExpression(tokenizer);
        if (expression.getDataType() != DataType.FLOAT) {
            throw new IllegalStateException("Expected float expression, but got: " + expression);
        }
        return new FloatAssignment(name.text(), expression);
    }

    private void peekExpectedKeyword(Tokenizer tokenizer, Keyword expected) throws IOException {
        var token = tokenizer.peek();
        if (token.type() != Token.Type.KEYWORD || token.asKeyword() != expected) {
            throw new IllegalStateException("Expected " + expected + " got: " + token);
        }
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

    private Token nextExpectedFunction(Tokenizer tokenizer) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.FUNCTION) {
            throw new IllegalStateException("Expected FUNCTION got: " + token);
        }
        return token;
    }

    record OperatorInfo(int precedence, BiFunction<Expression, Expression, BinaryExpression> newOperator) {

    }
}
