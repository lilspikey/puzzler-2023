import ast.AstVisitor;
import ast.BinaryExpression;
import ast.DataStatement;
import ast.DataType;
import ast.EndStatement;
import ast.Equals;
import ast.Expression;
import ast.FloatAddition;
import ast.FloatAssignment;
import ast.FloatConstant;
import ast.FloatDivision;
import ast.FloatMultiplication;
import ast.FloatNegation;
import ast.FloatSubtraction;
import ast.FloatVariable;
import ast.ForStatement;
import ast.FunctionCall;
import ast.GotoStatement;
import ast.GreaterThan;
import ast.GreaterThanEquals;
import ast.IfStatement;
import ast.InputStatement;
import ast.LessThan;
import ast.LessThanEquals;
import ast.Line;
import ast.NextStatement;
import ast.NotEquals;
import ast.PrintSeperator;
import ast.PrintStatement;
import ast.Printable;
import ast.Program;
import ast.ReadStatement;
import ast.StringAssignment;
import ast.StringConstant;
import ast.StringVariable;
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
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM4;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.FADD;
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
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

public class JavaASM implements AstVisitor {
    private String className;
    private final List<Object> dataConstants = new ArrayList<>();
    private final AtomicInteger nextLocalVarIndex = new AtomicInteger(1);
    private final Map<String, Integer> localVarIndexes = new HashMap<>();
    private final Map<String, Label> linesToLabels = new HashMap<>();
    private Label endLabel;
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
        AstVisitor.super.visit(program);
        addCallback(methodVisitor -> {
            methodVisitor.visitLabel(endLabel);
            methodVisitor.visitInsn(RETURN);
        });
    }

    @Override
    public void visit(Line line) {
        var label = new Label();
        linesToLabels.put(line.label(), label);
        currentLine = line;
        addCallback(methodVisitor -> {
            currentLine = line;
            methodVisitor.visitLabel(label);
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
        addCallback(methodVisitor -> {
            var label = linesToLabels.get(statement.destinationLabel());
            if (label == null) {
                throw new IllegalStateException("Unknown destination label: " + statement);
            }
            methodVisitor.visitJumpInsn(GOTO, label);
        });
    }

    @Override
    public void visit(IfStatement statement) {
        addCallback(methodVisitor -> {
            statement.predicate().visit(this);
            methodVisitor.visitInsn(F2I);
            // NB logic is inverted 0 = true and -1 = false
            methodVisitor.visitJumpInsn(IFNE, nextLineLabel(currentLine));
        });
    }

    @Override
    public void visit(DataStatement statement) {
        dataConstants.addAll(statement.constants());
    }

    @Override
    public void visit(ReadStatement statement) {
        for (var name: statement.names()) {
            var index = getLocalVarIndex(name);
            addCallback(methodVisitor -> {
                var dataType = DataType.fromVarName(name);
                var returnType = toDescriptorString(dataType);
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                        className,
                        "read" + dataType,
                        "()" + returnType);
                var store = switch (dataType) {
                    case FLOAT -> FSTORE;
                    case STRING -> ASTORE;
                };
                methodVisitor.visitVarInsn(store, index);
            });
        }
    }

    @Override
    public void visit(ForStatement statement) {
        openForStatements.add(new OpenForStatement(currentLine, statement));
        var index = getLocalVarIndex(statement.varname());
        addCallback(methodVisitor -> {
            statement.start().visit(this);
            methodVisitor.visitVarInsn(FSTORE, index);
            // end + increment go on the stack, to be used by NEXT statement
            statement.end().visit(this);
            if (statement.increment() != null) {
                statement.increment().visit(this);
            } else {
                methodVisitor.visitLdcInsn(1.0f);
            }
        });
    }

    @Override
    public void visit(NextStatement statement) {
        var openFor = findMatchingForStatement(statement);
        addCallback(methodVisitor -> {
            var forStatement = openFor.forStatement();
            // copy end + increment from stack
            methodVisitor.visitInsn(DUP2);
            // add increment
            var varIndex = getLocalVarIndex(forStatement.varname());
            methodVisitor.visitVarInsn(FLOAD, varIndex);
            methodVisitor.visitInsn(FADD);
            methodVisitor.visitVarInsn(FSTORE, varIndex);
            // then compare end with the loop variable
            methodVisitor.visitVarInsn(FLOAD, varIndex);
            methodVisitor.visitInsn(FCMPG);
            methodVisitor.visitJumpInsn(IFGE, nextLineLabel(openFor.line()));
            // loop finished, so remove end + increment from the stack
            methodVisitor.visitInsn(POP2);
        });
    }

    @Override
    public void visit(EndStatement statement) {
        addCallback(methodVisitor -> {
            methodVisitor.visitInsn(RETURN);
        });
    }

    private OpenForStatement findMatchingForStatement(NextStatement statement) {
        if (statement.varname() == null) {
            return openForStatements.pop();
        }
        var it = openForStatements.iterator();
        while (it.hasNext()) {
            var forStatement = it.next();
            if (forStatement.forStatement.varname().equals(statement.varname())) {
                it.remove();
                return forStatement;
            }
        }
        throw new IllegalStateException("Could not find matching FOR for: " + statement);
    }

    @Override
    public void visit(InputStatement statement) {
        var index = getLocalVarIndex(statement.name());
        addCallback(methodVisitor -> {
            var dataType = DataType.fromVarName(statement.name());
            var returnType = toDescriptorString(dataType);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                    className,
                    "input" + dataType,
                    "()" + returnType);
            var store = switch (dataType) {
                case FLOAT -> FSTORE;
                case STRING -> ASTORE;
            };
            methodVisitor.visitVarInsn(store, index);
        });
    }

    @Override
    public void visit(FloatAssignment statement) {
        var index = getLocalVarIndex(statement.name());
        addCallback(methodVisitor -> {
            statement.expression().visit(this);
            methodVisitor.visitVarInsn(FSTORE, index);
        });
    }

    @Override
    public void visit(StringAssignment statement) {
        var index = getLocalVarIndex(statement.name());
        addCallback(methodVisitor -> {
            statement.expression().visit(this);
            methodVisitor.visitVarInsn(ASTORE, index);
        });
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
    public void visit(FloatVariable expression) {
        var index = getLocalVarIndex(expression.name());
        currentMethodVisitor.visitVarInsn(FLOAD, index);
    }

    @Override
    public void visit(StringVariable expression) {
        var index = getLocalVarIndex(expression.name());
        currentMethodVisitor.visitVarInsn(ALOAD, index);
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
        var trueLabel = new Label();
        var falseLabel = new Label();
        currentMethodVisitor.visitJumpInsn(opcode, trueLabel);
        currentMethodVisitor.visitLdcInsn(-1.0f);
        currentMethodVisitor.visitJumpInsn(GOTO, falseLabel);
        currentMethodVisitor.visitLabel(trueLabel);
        currentMethodVisitor.visitInsn(FCONST_0);
        currentMethodVisitor.visitLabel(falseLabel);
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
    public void visit(FunctionCall expression) {
        currentMethodVisitor.visitVarInsn(ALOAD, 0);
        for (var arg: expression.args()) {
            arg.visit(this);
        }
        currentMethodVisitor.visitMethodInsn(INVOKEVIRTUAL,
            className,
            "fn" + expression.fn().name(),
            toDescriptorString(expression.fn())
        );
    }

    private String toDescriptorString(FunctionDef fn) {
        return String.format(
            "(%s)%s",
            fn.argTypes().stream()
                .map(this::toDescriptorString)
                .collect(Collectors.joining(",")),
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

    private void addCallback(Consumer<MethodVisitor> callback) {
        methodCallbacks.add(callback);
    }

    private int getLocalVarIndex(String name) {
        return localVarIndexes.computeIfAbsent(name, n -> nextLocalVarIndex.getAndIncrement());
    }

    private Label nextLineLabel(Line line) {
        var nextLine = lines.higher(line);
        if (nextLine == null) {
            return endLabel;
        }
        return linesToLabels.get(nextLine.label());
    }

    record OpenForStatement(Line line, ForStatement forStatement) {

    }
}
