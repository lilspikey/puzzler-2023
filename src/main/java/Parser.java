import ast.AndExpression;
import ast.ArrayInit;
import ast.DataStatement;
import ast.DataType;
import ast.DimStatement;
import ast.EndStatement;
import ast.Equals;
import ast.Expression;
import ast.FloatAddition;
import ast.FloatConstant;
import ast.FloatDivision;
import ast.FloatMultiplication;
import ast.FloatNegation;
import ast.FloatPower;
import ast.FloatSubtraction;
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
import ast.OnGotoStatement;
import ast.OrExpression;
import ast.PrintSeperator;
import ast.PrintStatement;
import ast.Printable;
import ast.Program;
import ast.ReadStatement;
import ast.RemarkStatement;
import ast.RestoreStatement;
import ast.ReturnStatement;
import ast.Statement;
import ast.StopStatement;
import ast.StringConstant;
import ast.VarName;
import ast.Variable;
import runtime.FunctionDef;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Parser {
    private final Map<String, UnaryOperatorInfo> unaryOperators = Map.of(
        "-", new UnaryOperatorInfo(7, FloatNegation::new),
        "+", new UnaryOperatorInfo(7, Function.identity())
    );
    private final Map<String, BinaryOperatorInfo> binaryOperators = new HashMap<>();
    {
        binaryOperators.put("OR", new BinaryOperatorInfo(1, Associativity.LEFT, OrExpression::new));
        binaryOperators.put("AND", new BinaryOperatorInfo(2, Associativity.LEFT, AndExpression::new));
        binaryOperators.put("=", new BinaryOperatorInfo(3, Associativity.LEFT, Equals::new));
        binaryOperators.put("<>", new BinaryOperatorInfo(3, Associativity.LEFT, NotEquals::new));
        binaryOperators.put(">", new BinaryOperatorInfo(3, Associativity.LEFT, GreaterThan::new));
        binaryOperators.put(">=", new BinaryOperatorInfo(3, Associativity.LEFT, GreaterThanEquals::new));
        binaryOperators.put("<", new BinaryOperatorInfo(3, Associativity.LEFT, LessThan::new));
        binaryOperators.put("<=", new BinaryOperatorInfo(3, Associativity.LEFT, LessThanEquals::new));
        binaryOperators.put("+", new BinaryOperatorInfo(4, Associativity.LEFT, FloatAddition::new));
        binaryOperators.put("-", new BinaryOperatorInfo(4, Associativity.LEFT, FloatSubtraction::new));
        binaryOperators.put("*", new BinaryOperatorInfo(5, Associativity.LEFT, FloatMultiplication::new));
        binaryOperators.put("/", new BinaryOperatorInfo(5, Associativity.LEFT, FloatDivision::new));
        binaryOperators.put("^", new BinaryOperatorInfo(6, Associativity.RIGHT, FloatPower::new));
    }

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
        while (tokenizer.peek().type() == Token.Type.EOL) {
            tokenizer.next();
        }
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
            throw parseError("Expected end of line or end of file, got:" + next);
        }
        return statements;
    }
    
    private boolean isEndOfStatement(Token token) {
        if (token.type() == Token.Type.EOL || token.type() == Token.Type.EOF) {
            return true;
        }
        return token.type() == Token.Type.SYMBOL && ":".equals(token.text());
    }

    private Statement nextStatement(Tokenizer tokenizer) throws IOException {
        var first = tokenizer.peek();
        if (first.type() == Token.Type.KEYWORD) {
            return switch (first.asKeyword()) {
                case PRINT -> nextPrintStatement(tokenizer);
                case GO -> nextGoStatement(tokenizer);
                case ON -> nextOnStatement(tokenizer);
                case RETURN -> newReturnStatement(tokenizer);
                case IF -> nextIfStatement(tokenizer);
                case INPUT -> nextInputStatement(tokenizer);
                case REM -> nextComment(tokenizer);
                case FOR -> nextForStatement(tokenizer);
                case NEXT -> nextNextStatement(tokenizer);
                case DATA -> nextDataStatement(tokenizer);
                case READ -> nextReadStatement(tokenizer);
                case END -> nextEndStatement(tokenizer);
                case STOP -> nextStopStatement(tokenizer);
                case RESTORE -> nextRestoreStatement(tokenizer);
                case LET -> nextLetStatement(tokenizer);
                case DIM -> nextDimStatement(tokenizer);
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
        var varnames = new ArrayList<String>();
        while (!isEndOfStatement(tokenizer.peek())) {
            if (!varnames.isEmpty()) {
                nextExpectedSymbol(tokenizer, ",");
            }
            varnames.add(nextExpectedName(tokenizer).text());
        }
        return new NextStatement(varnames);
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
            if (isEndOfStatement(next)) {
                done = true;
            } else {
                if (isPeekSymbol(tokenizer, ";")) {
                    nextExpectedSymbol(tokenizer, ";");
                    printables.add(PrintSeperator.NONE);
                } else if (isPeekSymbol(tokenizer, ",")) {
                    nextExpectedSymbol(tokenizer, ",");
                    printables.add(PrintSeperator.ZONE);
                } else {
                    if (!printables.isEmpty()) {
                        if (!(printables.get(printables.size() - 1) instanceof PrintSeperator)) {
                            printables.add(PrintSeperator.SPACE);
                        }
                    }
                    var expression = nextExpression(tokenizer);
                    printables.add(expression);
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
                        case SYMBOL -> {
                            if (constantToken.text().equals("-")) {
                                yield -Float.parseFloat(nextExpectedNumber(tokenizer).text());
                            }
                            throw parseError("Unexpected token: " + constantToken);
                        }
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
        var names = nextVarNames(tokenizer);
        return new ReadStatement(names);
    }

    private ArrayList<VarName> nextVarNames(Tokenizer tokenizer) throws IOException {
        var names = new ArrayList<VarName>();
        var done = false;
        var first = true;
        while (!done) {
            var next = tokenizer.peek();
            if (isEndOfStatement(next)) {
                done = true;
            } else {
                if (!first) {
                    nextExpectedSymbol(tokenizer, ",");
                }
                first = false;
                var name = nextVarName(tokenizer);
                names.add(name);
            }
        }
        return names;
    }

    private EndStatement nextEndStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.END);
        return new EndStatement();
    }

    private StopStatement nextStopStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.STOP);
        return new StopStatement();
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
            if (isNextTokenBinaryOperator(tokenizer)) {
                var operatorInfo = binaryOperators.get(tokenizer.peek().text());
                if (operatorInfo.precedence() < minPrecedence) {
                    break;
                }
                tokenizer.next();
                var nextPrecedence = switch (operatorInfo.associativity()) {
                    case LEFT -> operatorInfo.precedence() + 1;
                    case RIGHT -> operatorInfo.precedence();
                };
                var rhs = nextExpression(tokenizer, nextPrecedence);
                lhs = operatorInfo.newOperator.apply(lhs, rhs);
            } else {
                break;
            }
        }
        return lhs;
    }

    private boolean isNextTokenBinaryOperator(Tokenizer tokenizer) throws IOException {
        var maybeOp = tokenizer.peek();
        return (maybeOp.type() == Token.Type.SYMBOL || maybeOp.type() == Token.Type.KEYWORD)
            && binaryOperators.containsKey(maybeOp.text());
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
        var name = nextVarName(tokenizer);
        return new Variable(name);
    }

    private FunctionCall nextFunctionCall(Tokenizer tokenizer) throws IOException {
        var name = nextExpectedFunction(tokenizer).text();
        var functions = FunctionDef.findFunctions(name);
        if (functions.isEmpty()) {
            throw parseError("No function found for: " + name);
        }
        var args = nextFunctionParams(tokenizer);
        var fn = functions.stream()
            .filter(f -> f.argTypes().size() == args.size())
            .findFirst()
            .orElseThrow(() -> parseError(name + " does not take: " + args.size() + " parameters"));
        
        var argTypes = args.stream()
            .map(Expression::getDataType)
            .toList();
        if (!fn.argTypes().equals(argTypes)) {
            throw parseError("Wrong args type for: " + name + " expected: " + fn.argTypes() + ", but got: " + argTypes);
        }
        return new FunctionCall(fn, args);
    }

    private List<Expression> nextFunctionParams(Tokenizer tokenizer) throws IOException {
        nextExpectedSymbol(tokenizer, "(");
        var args = new ArrayList<Expression>();
        while (!isNextTokenClosingBracket(tokenizer)) {
            if (!args.isEmpty()) {
                nextExpectedSymbol(tokenizer, ",");
            }
            var arg = nextExpression(tokenizer);
            args.add(arg);
        }
        nextExpectedSymbol(tokenizer, ")");
        return args;
    }

    private boolean isNextTokenClosingBracket(Tokenizer tokenizer) throws IOException {
        return tokenizer.peek().type() == Token.Type.SYMBOL && ")".equals(tokenizer.peek().text());
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
    
    private OnGotoStatement nextOnStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.ON);
        var expression = nextExpression(tokenizer);
        nextExpectedKeyword(tokenizer, Keyword.GO);
        nextExpectedKeyword(tokenizer, Keyword.TO);
        var destinationLabels = new ArrayList<String>();
        destinationLabels.add(nextExpectedNumber(tokenizer).text());
        while (tokenizer.peek().type() == Token.Type.SYMBOL && tokenizer.peek().text().equals(",")) {
            nextExpectedSymbol(tokenizer, ",");
            destinationLabels.add(nextExpectedNumber(tokenizer).text());
        }
        return new OnGotoStatement(expression, destinationLabels);
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
        String prompt = null;
        if (tokenizer.peek().type() == Token.Type.STRING) {
            prompt = tokenizer.next().text();
            nextExpectedSymbol(tokenizer, ";");
        }
        var names = nextVarNames(tokenizer);
        return new InputStatement(prompt, names);
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

    private DimStatement nextDimStatement(Tokenizer tokenizer) throws IOException {
        nextExpectedKeyword(tokenizer, Keyword.DIM);
        var arrays = new ArrayList<ArrayInit>();
        var done = false;
        while (!done) {
            if (!arrays.isEmpty()) {
                nextExpectedSymbol(tokenizer, ",");
            }
            var name = nextExpectedName(tokenizer).text();
            var args = nextFunctionParams(tokenizer);
            arrays.add(new ArrayInit(name, DataType.fromVarName(name), args));
            if (isEndOfStatement(tokenizer.peek())) {
                done = true;
            }
        }
        return new DimStatement(arrays);
    }

    private VarName nextVarName(Tokenizer tokenizer) throws IOException {
        var name = nextExpectedName(tokenizer).text();
        var dataType = DataType.fromVarName(name);
        var peek = tokenizer.peek();
        if (peek.type() == Token.Type.SYMBOL && peek.text().equals("(")) {
            var indexes = nextFunctionParams(tokenizer);
            // append brackets to name so we don't clash with scalar types when assigning
            // local vars
            return new VarName(name + "()", dataType, indexes);
        }
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
    
    private boolean isPeekSymbol(Tokenizer tokenizer, String symbol) throws IOException {
        var token = tokenizer.peek();
        return token.type() == Token.Type.SYMBOL && symbol.equals(token.text());
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

    enum Associativity {
        LEFT, RIGHT;
    }

    record BinaryOperatorInfo(int precedence, Associativity associativity, BiFunction<Expression, Expression, Expression> newOperator) {

    }

    record UnaryOperatorInfo(int precedence, Function<Expression, Expression> newOperator) {

    }
}
