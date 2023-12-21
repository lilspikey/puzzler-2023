import ast.AndExpression;
import ast.ArrayDim;
import ast.AstVisitor;
import ast.BinaryExpression;
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
import ast.RestoreStatement;
import ast.ReturnStatement;
import ast.StopStatement;
import ast.StringConstant;
import ast.VarName;
import ast.Variable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import runtime.BasRuntime;
import runtime.FunctionDef;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM4;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.D2F;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.F2D;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FALOAD;
import static org.objectweb.asm.Opcodes.FASTORE;
import static org.objectweb.asm.Opcodes.FCMPG;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

public class JavaASM implements AstVisitor {
    private String className;
    private final List<Object> dataConstants = new ArrayList<>();
    private final NavigableMap<Integer, Integer> dataPositions = new TreeMap<>();
    private final AtomicInteger nextLocalVarIndex = new AtomicInteger(1);
    private final Map<String, Integer> localVarIndexes = new HashMap<>();
    private final Map<String, Label> linesToLabels = new HashMap<>();
    private Label endLabel;
    private final Set<Label> targetLabels = new HashSet<>();
    private final List<Label> returnLabels = new ArrayList<>();
    private final Set<VarName> declaredVariables = new HashSet<>();
    private final Map<String, ArrayDim> dimensionedArrays = new HashMap<>();
    private final AtomicInteger nextForNum = new AtomicInteger(1);
    private final Deque<OpenForStatement> openForStatements = new ArrayDeque<>();
    private final List<Consumer<MethodVisitor>> methodCallbacks = new ArrayList<>();
    private final NavigableSet<Line> lines = new TreeSet<>(Comparator.comparing(Line::numericLabel));
    private Line currentLine;
    private MethodVisitor currentMethodVisitor;

    public byte[] generateClass(String className) throws IOException {
        this.className = className;
        SimpleRemapper remapper = new SimpleRemapper(BasRuntime.class.getName().replace('.', '/'), className);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        ClassVisitor classVisitor = new ClassVisitor(ASM4, classWriter) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, className, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                var methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if ("run".equals(name)) {
                    currentMethodVisitor = methodVisitor;
                    methodVisitor.visitCode();
                    storeDataConstants(methodVisitor);
                    var defaultArrays = checkForDefaultArrays();
                    initLocalVars(methodVisitor);
                    initDefaultArrays(methodVisitor, defaultArrays);
                    for (var callback: methodCallbacks) {
                        callback.accept(methodVisitor);
                    }
                    methodVisitor.visitEnd();
                } else {
                    return new MethodRemapper(methodVisitor, remapper);
                }
                return methodVisitor;
            }
        };

        try (var in = getBasRuntimeClassBytes()) {
            ClassReader reader = new ClassReader(in);
            reader.accept(classVisitor, 0);
            return classWriter.toByteArray();
        }
    }

    private InputStream getBasRuntimeClassBytes() {
        String className = BasRuntime.class.getName();
        String classAsPath = className.replace('.', '/') + ".class";
        return BasRuntime.class.getClassLoader().getResourceAsStream(classAsPath);
    }

    @Override
    public void visit(Program program) {
        lines.clear();
        lines.addAll(program.lines());
        endLabel = new Label();
        for (var line: program.lines()) {
            var label = new Label();
            linesToLabels.put(line.label(), label);
        }
        AstVisitor.super.visit(program);
        addCallback(methodVisitor -> {
            visitLabelIfTargeted(methodVisitor, endLabel);
            methodVisitor.visitInsn(RETURN);
        });
    }

    @Override
    public void visit(Line line) {
        currentLine = line;
        addCallback(methodVisitor -> {
            currentLine = line;
            var label = linesToLabels.get(line.label());
            visitLabelIfTargeted(methodVisitor, label);
        });
        for (var statement: line.statements()) {
            statement.visit(this);
        }
    }

    @Override
    public void visit(PrintStatement statement) {
        addCallback(methodVisitor -> {
            Printable lastPrintable = null;
            for (var printable: statement.printables()) {
                if (printable == PrintSeperator.ZONE) {
                    methodVisitor.visitVarInsn(ALOAD, 0);
                    methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                        className,
                        "nextPrintZone",
                        "()V");
                } else if (printable != PrintSeperator.NONE) {
                    var expression = (Expression) printable;
                    methodVisitor.visitVarInsn(ALOAD, 0);
                    expression.visit(this);
                    var paramDescriptor = toDescriptorString(expression.getDataType());
                    methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                            className,
                            "print",
                            String.format("(%s)V", paramDescriptor));
                }
                lastPrintable = printable;
            }
            if (lastPrintable != PrintSeperator.NONE && lastPrintable != PrintSeperator.ZONE) {
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                        className,
                        "println",
                        "()V");
            }
        });
    }

    @Override
    public void visit(GotoStatement statement) {
        var label = targetLineLabel(statement.destinationLabel());
        addCallback(methodVisitor -> {
            methodVisitor.visitJumpInsn(GOTO, label);
        });
    }

    @Override
    public void visit(GoSubStatement statement) {
        // the JSR and RET instructions don't seem to want
        // to work with newer Java veersions, so we'll fake it
        // by pushing an int on the stack and generate a switch
        // to go back to the correction calling location
        var returnLabel = targetNextLineLabel(currentLine);
        var returnIndex = returnLabels.size();
        returnLabels.add(returnLabel);
        var destinationLabel = targetLineLabel(statement.destinationLabel());
        if (destinationLabel == null) {
            throw new IllegalStateException("Unknown destination label: " + statement);
        }
        addCallback(methodVisitor -> {
            methodVisitor.visitLdcInsn(returnIndex);
            methodVisitor.visitJumpInsn(GOTO, destinationLabel);
        });
    }

    @Override
    public void visit(ReturnStatement statement) {
        addCallback(methodVisitor -> {
            if (returnLabels.isEmpty()) {
                throw new IllegalStateException("No matching GOSUB for RETURN");
            }
            var defaultLabel = newTargettedLabel();
            var keys = IntStream.range(0, returnLabels.size())
                .toArray();
            var labels = returnLabels.toArray(Label[]::new);
            methodVisitor.visitLookupSwitchInsn(defaultLabel, keys, labels);
            methodVisitor.visitLabel(defaultLabel);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitLdcInsn("Calling GOSUB not found");
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                    className,
                    "runtimeError",
                    "(Ljava/lang/String;)V");
        });
    }

    @Override
    public void visit(OnGotoStatement statement) {
        var labels = statement.destinationLabels().stream()
            .map(this::targetLineLabel)
            .toArray(Label[]::new);
        var defaultLabel = newTargettedLabel();
        addCallback(methodVisitor -> {
            statement.expression().visit(this);
            methodVisitor.visitInsn(F2I);
            var keys = IntStream.range(0, labels.length)
                .map(i -> i + 1)
                .toArray();
            methodVisitor.visitLookupSwitchInsn(defaultLabel, keys, labels);
            methodVisitor.visitLabel(defaultLabel);
            methodVisitor.visitInsn(NOP);
        });
    }

    @Override
    public void visit(IfStatement statement) {
        var label = targetNextLineLabel(currentLine);
        addCallback(methodVisitor -> {
            statement.predicate().visit(this);
            methodVisitor.visitInsn(F2I);
            // NB logic is inverted 0 = true and -1 = false
            methodVisitor.visitJumpInsn(IFNE, label);
        });
    }

    @Override
    public void visit(DataStatement statement) {
        dataPositions.put(currentLine.numericLabel(), dataConstants.size());
        dataConstants.addAll(statement.constants());
    }

    @Override
    public void visit(ReadStatement statement) {
        for (var varName: statement.names()) {
            createLocalVarIndex(varName);
            addCallback(methodVisitor -> {
                varStore(methodVisitor, varName, () -> {
                    var dataType = varName.dataType();
                    var returnType = toDescriptorString(dataType);
                    methodVisitor.visitVarInsn(ALOAD, 0);
                    methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                            className,
                            "read" + dataType,
                            "()" + returnType);
                });
            });
        }
    }

    @Override
    public void visit(RestoreStatement statement) {
        addCallback(methodVisitor -> {
            int nextDataPtr = 0;
            if (statement.label() != null) {
                var nextPositions = dataPositions.tailMap(Integer.parseInt(statement.label()));
                if (nextPositions.isEmpty()) {
                    throw new IllegalStateException("Could not find data after: " + statement.label());
                }
                nextDataPtr = nextPositions.get(nextPositions.firstKey());
            }
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitLdcInsn(nextDataPtr);
            methodVisitor.visitFieldInsn(PUTFIELD, className, "nextDataPtr", "I");
        });
    }

    @Override
    public void visit(ForStatement statement) {
        var continueLabel = newTargettedLabel();
        var forNum = nextForNum.getAndIncrement();
        var varIndex = getLocalVarIndex(statement.varname());
        var endIndex = getLocalVarIndex("#FOR#END#" + forNum);
        var incIndex = getLocalVarIndex("#FOR#INC#" + forNum);
        openForStatements.add(new OpenForStatement(continueLabel, statement, varIndex, endIndex, incIndex));
        addCallback(methodVisitor -> {
            statement.start().visit(this);
            methodVisitor.visitVarInsn(FSTORE, varIndex);
            statement.end().visit(this);
            methodVisitor.visitVarInsn(FSTORE, endIndex);
            if (statement.increment() != null) {
                statement.increment().visit(this);
            } else {
                methodVisitor.visitLdcInsn(1.0f);
            }
            methodVisitor.visitVarInsn(FSTORE, incIndex);
            visitLabelIfTargeted(methodVisitor, continueLabel);
            methodVisitor.visitInsn(NOP);
        });
    }

    @Override
    public void visit(NextStatement statement) {
        var openFors = findMatchingForStatements(statement);
        for (var openFor: openFors) {
            addCallback(methodVisitor -> {
                // add increment to loop
                methodVisitor.visitVarInsn(FLOAD, openFor.varIndex());
                methodVisitor.visitVarInsn(FLOAD, openFor.incIndex());
                methodVisitor.visitInsn(FADD);
                methodVisitor.visitVarInsn(FSTORE, openFor.varIndex());
                // see which direction the loop is going
                methodVisitor.visitVarInsn(FLOAD, openFor.incIndex());
                methodVisitor.visitLdcInsn(0.0f);
                methodVisitor.visitInsn(FCMPG);
                // then compare end vs var
                methodVisitor.visitVarInsn(FLOAD, openFor.varIndex());
                methodVisitor.visitVarInsn(FLOAD, openFor.endIndex());
                methodVisitor.visitInsn(FCMPG);
                // then see if the direction of the comparisons are the same or not
                methodVisitor.visitJumpInsn(IF_ICMPNE, openFor.continueLabel());
            });
        }
    }

    @Override
    public void visit(EndStatement statement) {
        addCallback(methodVisitor -> {
            methodVisitor.visitInsn(RETURN);
        });
    }

    @Override
    public void visit(StopStatement statement) {
        addCallback(methodVisitor -> {
            methodVisitor.visitInsn(RETURN);
        });
    }

    private List<OpenForStatement> findMatchingForStatements(NextStatement statement) {
        if (statement.varnames().isEmpty()) {
            return List.of(openForStatements.pop());
        }
        
        return statement.varnames().stream()
            .map(this::findMatchingForStatement)
            .toList();
    }
    
    private OpenForStatement findMatchingForStatement(String varname) {
        var it = openForStatements.iterator();
        while (it.hasNext()) {
            var forStatement = it.next();
            if (forStatement.forStatement.varname().equals(varname)) {
                it.remove();
                return forStatement;
            }
        }
        throw new IllegalStateException("Could not find matching FOR for: " + varname);
    }

    @Override
    public void visit(InputStatement statement) {
        for (var varName: statement.names()) {
            createLocalVarIndex(varName);
            addCallback(methodVisitor -> {
                if (statement.prompt() != null) {
                    methodVisitor.visitVarInsn(ALOAD, 0);
                    methodVisitor.visitLdcInsn(statement.prompt());
                    methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                            className,
                            "print",
                            "(Ljava/lang/String;)V");
                    methodVisitor.visitVarInsn(ALOAD, 0);
                    methodVisitor.visitLdcInsn("? ");
                    methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                            className,
                            "print",
                            "(Ljava/lang/String;)V");
                }
                varStore(methodVisitor, varName, () -> {
                    var dataType = varName.dataType();
                    var returnType = toDescriptorString(dataType);
                    methodVisitor.visitVarInsn(ALOAD, 0);
                    methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                            className,
                            "input" + dataType,
                            "()" + returnType);
                });
            });
        }
    }

    @Override
    public void visit(DimStatement statement) {
        for (var array: statement.arrays()) {
            var name = array.name();
            var dim = array.getArrayDimensions();
            if (dimensionedArrays.put(dim.name(), dim) != null) {
                throw new IllegalStateException("Cannot re-dim array: " + name);
            }
            addCallback(methodVisitor -> {
                visitArrayCreate(methodVisitor, dim, array.sizes());
            });
        }
    }

    @Override
    public void visit(LetStatement statement) {
        var varName = statement.name();
        createLocalVarIndex(varName);
        addCallback(methodVisitor -> {
            varStore(methodVisitor, varName, () -> statement.expression().visit(this));
        });
    }

    private void varStore(MethodVisitor methodVisitor, VarName varName, Runnable value) {
        var varIndex = getLocalVarIndex(varName);
        var dataType = varName.dataType();
        if (varName.isArray()) {
            methodVisitor.visitVarInsn(ALOAD, varIndex);
            visitArrayIndexes(methodVisitor, varName.indexes());
            value.run();
            var store = switch (dataType) {
                case FLOAT -> FASTORE;
                case STRING -> AASTORE;
            };
            methodVisitor.visitInsn(store);
        } else {
            value.run();
            var store = switch (dataType) {
                case FLOAT -> FSTORE;
                case STRING -> ASTORE;
            };
            methodVisitor.visitVarInsn(store, varIndex);
        }
    }

    private void visitArrayIndexes(MethodVisitor methodVisitor, List<Expression> indexes) {
        var innerIndexes = indexes.subList(0, indexes.size() - 1);
        for (var index: innerIndexes) {
            visitArrayIndex(methodVisitor, index);
            methodVisitor.visitInsn(AALOAD);
        }
        var outerIndex = indexes.get(indexes.size() - 1);
        visitArrayIndex(methodVisitor, outerIndex);
    }

    private void visitArrayIndex(MethodVisitor methodVisitor, Expression expression) {
        // need to handle the fact default is float and arrays start from one in Basic
        expression.visit(this);
        methodVisitor.visitInsn(F2I);
        methodVisitor.visitLdcInsn(1);
        methodVisitor.visitInsn(ISUB);
    }

    @Override
    public void visit(StringConstant expression) {
        currentMethodVisitor.visitLdcInsn(expression.constant());
    }

    @Override
    public void visit(FloatConstant expression) {
        currentMethodVisitor.visitLdcInsn(expression.constant());
    }

    @Override
    public void visit(Variable expression) {
        var varName = expression.name();
        var varIndex = getLocalVarIndex(varName.name());
        if (varName.isArray()) {
            currentMethodVisitor.visitVarInsn(ALOAD, varIndex);
            visitArrayIndexes(currentMethodVisitor, varName.indexes());
            switch (varName.dataType()) {
                case FLOAT -> currentMethodVisitor.visitInsn(FALOAD);
                case STRING -> currentMethodVisitor.visitInsn(AALOAD);
            }
        } else {
            switch (varName.dataType()) {
                case FLOAT -> currentMethodVisitor.visitVarInsn(FLOAD, varIndex);
                case STRING -> currentMethodVisitor.visitVarInsn(ALOAD, varIndex);
            }
        }
    }
    @Override
    public void visit(FloatNegation expression) {
        expression.expr().visit(this);
        currentMethodVisitor.visitInsn(FNEG);
    }

    @Override
    public void visit(Equals expression) {
        visitExpressions(expression);
        comparison(expression, IFEQ);
    }

    @Override
    public void visit(NotEquals expression) {
        visitExpressions(expression);
        comparison(expression, IFNE);
    }

    @Override
    public void visit(GreaterThan expression) {
        visitExpressions(expression);
        comparison(expression, IFGT);
    }

    @Override
    public void visit(GreaterThanEquals expression) {
        visitExpressions(expression);
        comparison(expression, IFGE);
    }

    @Override
    public void visit(LessThan expression) {
        visitExpressions(expression);
        comparison(expression, IFLT);
    }

    @Override
    public void visit(LessThanEquals expression) {
        visitExpressions(expression);
        comparison(expression, IFLE);
    }

    private void comparison(Expression expression, int opcode) {
        switch (expression.getDataType()) {
            case FLOAT -> floatComparison(opcode);
            case STRING -> stringComparison(opcode);
        }
    }

    private void floatComparison(int opcode) {
        currentMethodVisitor.visitInsn(FCMPG);
        // spec wants 0 for true and -1 for false
        // using IF* and GOTO like this seems to be
        // pretty much what Java itself uses for boolean expressions
        compareToTruthFloat(opcode);
    }

    private void stringComparison(int opcode) {
        currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL,
            "java/lang/String",
            "compareTo",
            String.format("(%s)I", String.class.descriptorString())
        );
        compareToTruthFloat(opcode);
    }

    private void compareToTruthFloat(int opcode) {
        // spec wants 0 for true and -1 for false
        // using IF* and GOTO like this seems to be
        // pretty much what Java itself uses for boolean expressions
        var trueLabel = newTargettedLabel();
        var falseLabel = newTargettedLabel();
        currentMethodVisitor.visitJumpInsn(opcode, trueLabel);
        currentMethodVisitor.visitLdcInsn(-1.0f);
        currentMethodVisitor.visitJumpInsn(GOTO, falseLabel);
        visitLabelIfTargeted(currentMethodVisitor, trueLabel);
        currentMethodVisitor.visitInsn(FCONST_0);
        visitLabelIfTargeted(currentMethodVisitor, falseLabel);
        currentMethodVisitor.visitInsn(NOP);
    }

    @Override
    public void visit(AndExpression expression) {
        booleanShortCircuit(expression, IFNE);
    }

    @Override
    public void visit(OrExpression expression) {
        booleanShortCircuit(expression, IFEQ);
    }

    private void booleanShortCircuit(BinaryExpression expression, int opcode) {
        expression.lhs().visit(this);
        currentMethodVisitor.visitInsn(DUP);
        currentMethodVisitor.visitInsn(F2I);
        var label = newTargettedLabel();
        // if can jump past the 2nd expression if the 1st expression evaluates the right way
        currentMethodVisitor.visitJumpInsn(opcode, label);
        currentMethodVisitor.visitInsn(POP);
        expression.rhs().visit(this);
        currentMethodVisitor.visitLabel(label);
        currentMethodVisitor.visitInsn(NOP);
    }

    @Override
    public void visit(FloatAddition expression) {
        visitExpressions(expression);
        currentMethodVisitor.visitInsn(FADD);
    }

    @Override
    public void visit(FloatSubtraction expression) {
        visitExpressions(expression);
        currentMethodVisitor.visitInsn(FSUB);
    }

    @Override
    public void visit(FloatMultiplication expression) {
        visitExpressions(expression);
        currentMethodVisitor.visitInsn(FMUL);
    }

    @Override
    public void visit(FloatDivision expression) {
        visitExpressions(expression);
        currentMethodVisitor.visitInsn(FDIV);
    }

    @Override
    public void visit(FloatPower expression) {
        expression.lhs().visit(this);
        currentMethodVisitor.visitInsn(F2D);
        expression.rhs().visit(this);
        currentMethodVisitor.visitInsn(F2D);
        currentMethodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D");
        currentMethodVisitor.visitInsn(D2F);
    }

    @Override
    public void visit(FunctionCall expression) {
        currentMethodVisitor.visitVarInsn(ALOAD, 0);
        for (var arg: expression.args()) {
            arg.visit(this);
        }
        currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL,
            className,
            FunctionDef.toRuntimeFn(expression.fn().name()),
            toDescriptorString(expression.fn())
        );
    }

    private String toDescriptorString(FunctionDef fn) {
        return String.format(
            "(%s)%s",
            fn.argTypes().stream()
                .map(this::toDescriptorString)
                .collect(Collectors.joining()),
            toDescriptorString(fn.returnType())
        );
    }

    private String toDescriptorString(DataType dataType) {
        return switch (dataType) {
            case FLOAT -> Float.TYPE.descriptorString();
            case STRING -> String.class.descriptorString();
        };
    }

    private void visitExpressions(BinaryExpression expression) {
        expression.lhs().visit(this);
        expression.rhs().visit(this);
    }

    private void storeDataConstants(MethodVisitor methodVisitor) {
        if (dataConstants.isEmpty()) {
            return;
        }
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitLdcInsn(dataConstants.size());
        methodVisitor.visitMultiANewArrayInsn("["+Object.class.descriptorString(), 1);
        for (var i = 0; i < dataConstants.size(); i++) {
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitLdcInsn(i);
            var constant = dataConstants.get(i);
            methodVisitor.visitLdcInsn(constant);
            // need to box float as Float object
            if (constant instanceof Float) {
                methodVisitor.visitMethodInsn(INVOKESTATIC,
                    "java/lang/Float",
                    "valueOf",
                    "(F)Ljava/lang/Float;");
            }
            methodVisitor.visitInsn(AASTORE);
        }
        methodVisitor.visitFieldInsn(PUTFIELD, className, "data", "[" + Object.class.descriptorString());
    }

    private List<ArrayDim> checkForDefaultArrays() {
        var arrayDims = declaredVariables.stream()
                .filter(VarName::isArray)
                .map(VarName::getArrayDimensions)
                .collect(Collectors.groupingBy(ArrayDim::name, Collectors.toSet()));
        var defaultArrays = new ArrayList<ArrayDim>();
        for (var entry: arrayDims.entrySet()) {
            var name = entry.getKey();
            var values = entry.getValue();
            if (values.size() > 1) {
                throw new IllegalStateException(name + " uses differing number of array dimensions: " + values);
            }
            var arrayDim = values.iterator().next();
            var dimension = dimensionedArrays.get(name);
            if (dimension != null) {
                if (!arrayDim.equals(dimension)) {
                    throw new IllegalStateException(name + " was dimension as: " + dimension + ", but variable expected: " + arrayDim);
                }
                continue;
            }
            // create array of default size 10 if none has already been DIM'd
            if (arrayDim.dimensions() != 1) {
                throw new IllegalStateException("Can only use 1-dimensional arrays without DIMing first");
            }
            defaultArrays.add(arrayDim);
        }
        return defaultArrays;
    }

    private void initLocalVars(MethodVisitor methodVisitor) {
        // this is done to ensure all variables have the scope of the entire run() method
        // to simplify things (scoping in BASIC is less specific than Java)
        for (var var: declaredVariables) {
            if (var.isArray()) {
                var arrayDim = var.getArrayDimensions();
                var sizes = Collections.nCopies(arrayDim.dimensions(), 0.0f).stream()
                        .map(FloatConstant::new)
                        .toList();
                visitArrayCreate(methodVisitor, arrayDim, sizes);
            } else {
                switch (var.dataType()) {
                    case FLOAT -> {
                        methodVisitor.visitLdcInsn(0.0f);
                        methodVisitor.visitVarInsn(FSTORE, getLocalVarIndex(var));
                    }
                    case STRING -> {
                        methodVisitor.visitLdcInsn("");
                        methodVisitor.visitVarInsn(ASTORE, getLocalVarIndex(var));
                    }
                };
            }
        }
    }

    private void initDefaultArrays(MethodVisitor methodVisitor, List<ArrayDim> defaultArrays) {
        for (var arrayDim: defaultArrays) {
            visitArrayCreate(methodVisitor, arrayDim, List.of(new FloatConstant(10.0f)));
        }
    }

    private void visitArrayCreate(MethodVisitor methodVisitor, ArrayDim arrayDim, List<? extends Expression> sizes) {
        var index = getLocalVarIndex(arrayDim.name());
        for (var size: sizes) {
            size.visit(this);
            methodVisitor.visitInsn(F2I);
        }

        methodVisitor.visitMultiANewArrayInsn(
            "[".repeat(arrayDim.dimensions()) + toDescriptorString(arrayDim.dataType()),
            arrayDim.dimensions()
        );
        methodVisitor.visitVarInsn(ASTORE, index);
    }

    private void addCallback(Consumer<MethodVisitor> callback) {
        methodCallbacks.add(callback);
    }

    private void createLocalVarIndex(VarName varName) {
        getLocalVarIndex(varName);
    }

    private int getLocalVarIndex(VarName varName) {
        declaredVariables.add(varName);
        return getLocalVarIndex(varName.name());
    }

    private int getLocalVarIndex(String name) {
        return localVarIndexes.computeIfAbsent(name, n -> nextLocalVarIndex.getAndIncrement());
    }

    private Label targetLineLabel(String lineLabel) {
        var label = linesToLabels.get(lineLabel);
        if (label == null) {
            throw new IllegalStateException("Unknown destination label: " + lineLabel);
        }
        targetLabels.add(label);
        return label;
    }

    private Label targetNextLineLabel(Line line) {
        var nextLine = lines.higher(line);
        Label label;
        if (nextLine == null) {
            label = endLabel;
        } else {
            label = linesToLabels.get(nextLine.label());
        }
        targetLabels.add(label);
        return label;
    }

    private Label newTargettedLabel() {
        var label = new Label();
        targetLabels.add(label);
        return label;
    }

    private void visitLabelIfTargeted(MethodVisitor methodVisitor, Label label) {
        if (targetLabels.contains(label)) {
            methodVisitor.visitLabel(label);
        }
    }

    record OpenForStatement(Label continueLabel, ForStatement forStatement, int varIndex, int endIndex, int incIndex) {

    }
}
