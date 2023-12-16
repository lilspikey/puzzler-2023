import ast.DataStatement;
import ast.DataType;
import ast.EndStatement;
import ast.Equals;
import ast.Expression;
import ast.FloatAddition;
import ast.FloatConstant;
import ast.FloatDivision;
import ast.FloatMultiplication;
import ast.FloatNegation;
import ast.FloatSubtraction;
import ast.FloatVariable;
import ast.ForStatement;
import ast.FunctionCall;
import ast.GoSubStatement;
import ast.GotoStatement;
import ast.GreaterThan;
import ast.GreaterThanEquals;
import ast.IfStatement;
import ast.InputStatement;
import ast.LessThan;
import ast.LessThanEquals;
import ast.LetStatement;
import ast.Line;
import ast.NextStatement;
import ast.NotEquals;
import ast.PrintSeperator;
import ast.PrintStatement;
import ast.Printable;
import ast.Program;
import ast.ReadStatement;
import ast.RemarkStatement;
import ast.RestoreStatement;
import ast.ReturnStatement;
import ast.Statement;
import ast.StringConstant;
import ast.StringVariable;
import ast.VarName;
import runtime.FunctionDef;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Parser {
    private final Map<String, UnaryOperatorInfo> unaryOperators = Map.of(
        "-", new UnaryOperatorInfo(5, FloatNegation::new),
        "+", new UnaryOperatorInfo(5, Function.identity())
    );
    private final Map<String, BinaryOperatorInfo> binaryOperators = Map.of(
        "=", new BinaryOperatorInfo(1, Equals::new),
        "<>", new BinaryOperatorInfo(1, NotEquals::new),
        ">", new BinaryOperatorInfo(1, GreaterThan::new),
        ">=", new BinaryOperatorInfo(1, GreaterThanEquals::new),
        "<", new BinaryOperatorInfo(1, LessThan::new),
        "<=", new BinaryOperatorInfo(1, LessThanEquals::new),
        "+", new BinaryOperatorInfo(2, FloatAddition::new),
        "-", new BinaryOperatorInfo(2, FloatSubtraction::new),
        "*", new BinaryOperatorInfo(3, FloatMultiplication::new),
        "/", new BinaryOperatorInfo(4, FloatDivision::new)
    );

    private final Map<String, FunctionDef> functions = FunctionDef.getFunctionDefs().stream()
        .collect(Collectors.toMap(FunctionDef::name, Function.identity()));

    private String currentLineNumber;

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
        currentLineNumber = label;
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
                case GO -> nextGoStatement(tokenizer);
                case RETURN -> newReturnStatement(tokenizer);
                case IF -> nextIfStatement(tokenizer);
                case INPUT -> nextInputStatement(tokenizer);
                case REM -> nextComment(tokenizer);
                case FOR -> nextForStatement(tokenizer);
                case NEXT -> nextNextStatement(tokenizer);
                case DATA -> nextDataStatement(tokenizer);
                case READ -> nextReadStatement(tokenizer);
                case END -> nextEndStatement(tokenizer);
                case RESTORE -> nextRestoreStatement(tokenizer);
                case LET -> nextLetStatement(tokenizer);
                default -> throw parseError("Unexpected token:" + first);
            };
        } else if (first.type() == Token.Type.NAME) {
            return nextLetStatement(tokenizer);
        }
        throw parseError("Unexpected token: " + first);
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
        var printables = new ArrayList<Printable>();
        var done = false;
        while (!done) {
            var next = tokenizer.peek();
            switch (next.type()) {
                case EOL, EOF -> done = true;
                default -> {
                    if (next.type() == Token.Type.SYMBOL && ":".equals(next.text())) {
                        done = true;
                    } else {
                        var expression = nextExpression(tokenizer);
                        printables.add(expression);
                        var peek = tokenizer.peek();
                        if (peek.type() == Token.Type.SYMBOL) {
                            switch (peek.text()) {
                                case ";" -> {
                                    nextExpectedSymbol(tokenizer, ";");
                                    printables.add(PrintSeperator.NONE);
                                }
                                case "," -> {
                                    nextExpectedSymbol(tokenizer, ",");
                                    printables.add(PrintSeperator.ZONE);
                                }
                                case ":" -> {
                                    done = true;
                                }
                                default -> {
                                    throw parseError("Unexpected symbol: " + peek);
                                }
                            }
                        } else if (peek.type() != Token.Type.EOL && peek.type() != Token.Type.EOF){
                            throw parseError("Unexpected symbol: " + peek);
                        }
                    }
                }
            }
        }
        return new PrintStatement(printables);
    }

    private DataStatement nextDataStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.DATA);
        var constants = new ArrayList<>();
        var done = false;
        var first = true;
        while (!done) {
            var next = tokenizer.peek();
            switch (next.type()) {
                case EOL, EOF -> done = true;
                default -> {
                    if (!first) {
                        nextExpectedSymbol(tokenizer, ",");
                    }
                    first = false;
                    var constantToken = tokenizer.next();
                    var constant = switch (constantToken.type()) {
                        case NUMBER -> Float.parseFloat(constantToken.text());
                        case STRING -> constantToken.text();
                        default -> {
                            throw parseError("Unexpected token: " + constantToken);
                        }
                    };
                    constants.add(constant);
                }
            }
        }
        return new DataStatement(constants);
    }

    private ReadStatement nextReadStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.READ);
        var names = new ArrayList<String>();
        var done = false;
        var first = true;
        while (!done) {
            var next = tokenizer.peek();
            switch (next.type()) {
                case EOL, EOF -> done = true;
                default -> {
                    if (!first) {
                        nextExpectedSymbol(tokenizer, ",");
                    }
                    first = false;
                    var name = nextExpectedName(tokenizer).text();
                    names.add(name);
                }
            }
        }
        return new ReadStatement(names);
    }

    private EndStatement nextEndStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.END);
        return new EndStatement();
    }

    private RestoreStatement nextRestoreStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.RESTORE);
        String label = null;
        if (tokenizer.peek().type() == Token.Type.NUMBER) {
            label = nextExpectedNumber(tokenizer).text();
        }
        return new RestoreStatement(label);
    }

    private Expression nextExpression(Tokenizer tokenizer) throws IOException {
        return nextExpression(tokenizer, 1);
    }

    private Expression nextExpression(Tokenizer tokenizer, int minPrecedence) throws IOException {
        var lhs = nextAtomExpression(tokenizer);
        while (true) {
            var maybeOp = tokenizer.peek();
            if (maybeOp.type() == Token.Type.SYMBOL && binaryOperators.containsKey(maybeOp.text())) {
                var operatorInfo = binaryOperators.get(maybeOp.text());
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
            case NAME -> nextVariable(tokenizer);
            case FUNCTION -> nextFunctionCall(tokenizer);
            case SYMBOL -> {
                if (unaryOperators.containsKey(token.text())) {
                    var unaryToken = nextExpectedSymbol(tokenizer);
                    var unaryOperator = unaryOperators.get(unaryToken.text());
                    var expression = nextExpression(tokenizer, unaryOperator.precedence());
                    yield unaryOperator.newOperator().apply(expression);
                }
                if ("(".equals(token.text())) {
                    yield nextSubExpression(tokenizer);
                }
                throw parseError("Unexpected token: " + tokenizer.peek());
            }
            default -> throw parseError("Unexpected token: " + tokenizer.peek());
        };
    }

    private Expression nextVariable(Tokenizer tokenizer) throws IOException {
        var name = tokenizer.next().text();
        return switch (DataType.fromVarName(name)) {
            case FLOAT -> new FloatVariable(name);
            case STRING -> new StringVariable(name);
        };
    }

    private FunctionCall nextFunctionCall(Tokenizer tokenizer) throws IOException {
        var name = nextExpectedFunction(tokenizer).text();
        var fn = Objects.requireNonNull(functions.get(name), "Not function found for: " + name);
        nextExpectedSymbol(tokenizer, "(");
        var args = new ArrayList<Expression>();
        for (var argType: fn.argTypes()) {
            if (!args.isEmpty()) {
                nextExpectedSymbol(tokenizer, ",");
            }
            var arg = nextExpression(tokenizer);
            if (arg.getDataType() != argType) {
                throw parseError("Expected: " + argType);
            }
            args.add(arg);
        }
        nextExpectedSymbol(tokenizer, ")");
        return new FunctionCall(fn, args);
    }

    private Expression nextSubExpression(Tokenizer tokenizer) throws IOException {
        nextExpectedSymbol(tokenizer, "(");
        var expression = nextExpression(tokenizer);
        nextExpectedSymbol(tokenizer, ")");
        return expression;
    }

    private Statement nextGoStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.GO);
        var token = nextExpectedKeyword(tokenizer, Keyword.TO, Keyword.SUB);
        var destinationLabel = nextExpectedNumber(tokenizer);
        return switch (token.asKeyword()) {
            case TO -> new GotoStatement(destinationLabel.text());
            case SUB -> new GoSubStatement(destinationLabel.text());
            default -> throw parseError("Unexpected token: " + token);
        };
    }

    private ReturnStatement newReturnStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.RETURN);
        return new ReturnStatement();
    }

    private IfStatement nextIfStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.IF);
        var predicate = nextExpression(tokenizer);
        peekExpectedKeyword(tokenizer, Keyword.THEN);
        return new IfStatement(predicate);
    }

    private InputStatement nextInputStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.INPUT);
        var name = nextExpectedName(tokenizer);
        return new InputStatement(name.text());
    }

    private LetStatement nextLetStatement(Tokenizer tokenizer) throws IOException {
        if (tokenizer.peek().type() == Token.Type.KEYWORD) {
            nextExpectedKeyword(tokenizer, Keyword.LET);
        }
        var name = nextVarName(tokenizer);
        nextExpectedSymbol(tokenizer, "=");
        Expression expression = nextExpression(tokenizer);
        if (name.dataType() != expression.getDataType()) {
            throw parseError("Expected " + name.dataType() + " expression, but got: " + expression);
        }
        return new LetStatement(name, expression);
    }

    private VarName nextVarName(Tokenizer tokenizer) throws IOException {
        var name = nextExpectedName(tokenizer).text();
        var dataType = DataType.fromVarName(name);
        return new VarName(name, dataType);
    }

    private void peekExpectedKeyword(Tokenizer tokenizer, Keyword... expected) throws IOException {
        var token = tokenizer.peek();
        if (token.type() != Token.Type.KEYWORD || !Arrays.asList(expected).contains(token.asKeyword())) {
            throw parseError("Expected " + Arrays.asList(expected) + " got: " + token);
        }
    }

    private Token nextExpectedKeyword(Tokenizer tokenizer, Keyword... expected) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.KEYWORD || !Arrays.asList(expected).contains(token.asKeyword())) {
            throw parseError("Expected " + Arrays.asList(expected) + " got: " + token);
        }
        return token;
    }

    private Token nextExpectedName(Tokenizer tokenizer) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.NAME) {
            throw parseError("Expected name got: " + token);
        }
        return token;
    }

    private Token nextExpectedNumber(Tokenizer tokenizer) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.NUMBER) {
            throw parseError("Expected number got: " + token);
        }
        return token;
    }

    private Token nextExpectedSymbol(Tokenizer tokenizer) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.SYMBOL) {
            throw parseError("Expected symbol got: " + token);
        }
        return token;
    }

    private Token nextExpectedSymbol(Tokenizer tokenizer, String expected) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.SYMBOL || !expected.equals(token.text())) {
            throw parseError("Expected " + expected +" got: " + token);
        }
        return token;
    }

    private Token nextExpectedFunction(Tokenizer tokenizer) throws IOException {
        var token = tokenizer.next();
        if (token.type() != Token.Type.FUNCTION) {
            throw parseError("Expected FUNCTION got: " + token);
        }
        return token;
    }

    private RuntimeException parseError(String message) {
        return new IllegalStateException("Line " + currentLineNumber + ": " + message);
    }

    record BinaryOperatorInfo(int precedence, BiFunction<Expression, Expression, Expression> newOperator) {

    }

    record UnaryOperatorInfo(int precedence, Function<Expression, Expression> newOperator) {

    }
}
