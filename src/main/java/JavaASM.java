import ast.AstVisitor;
import ast.BinaryExpression;
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
import ast.Statement;
import ast.StringConstant;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM4;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FCMPG;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NOP;
import static org.objectweb.asm.Opcodes.POP2;

public class JavaASM implements AstVisitor {
    private String className;
    private final AtomicInteger nextLocalVarIndex = new AtomicInteger(1);
    private final Map<String, Integer> localFloatVarIndexes = new HashMap<>();
    private final Map<String, Label> linesToLabels = new HashMap<>();
    private final Deque<OpenForStatement> openForStatements = new ArrayDeque<>();
    private final List<Consumer<MethodVisitor>> methodCallbacks = new ArrayList<>();
    private MethodVisitor currentMethodVisitor;

    public byte[] generateClass(String className) throws IOException {
        this.className = className;
        SimpleRemapper remapper = new SimpleRemapper(BasRuntime.class.getName(), className);
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
                    for (var callback: methodCallbacks) {
                        callback.accept(methodVisitor);
                    }
                    methodVisitor.visitEnd();
                } else if ("main".equals(name)) {
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
    public void visit(PrintStatement statement) {
        addCallback(statement, methodVisitor -> {
            for (var expression: statement.expressions()) {
                methodVisitor.visitVarInsn(ALOAD, 0);
                expression.visit(this);
                var paramDescriptor = switch (expression.getDataType()) {
                    case FLOAT -> "(F)V";
                    case STRING -> "(Ljava/lang/String;)V";
                };
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                        className,
                        "print",
                        paramDescriptor);
            }
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                    className,
                    "println",
                    "()V");
        });
    }

    @Override
    public void visit(GotoStatement statement) {
        addCallback(statement, methodVisitor -> {
            var label = linesToLabels.get(statement.destinationLabel());
            if (label == null) {
                throw new IllegalStateException("Unknown destination label: " + statement);
            }
            methodVisitor.visitJumpInsn(GOTO, label);
        });
    }

    @Override
    public void visit(IfStatement statement) {
        addCallback(statement, methodVisitor -> {
            var falseLabel = new Label();
            statement.predicate().visit(this);
            methodVisitor.visitInsn(F2I);
            // NB logic is inverted 0 = true and -1 = false
            methodVisitor.visitJumpInsn(IFNE, falseLabel);
            for (var s: statement.trueStatements()) {
                s.visit(this);
            }
            methodVisitor.visitLabel(falseLabel);
            methodVisitor.visitInsn(NOP);
        });
    }

    @Override
    public void visit(ForStatement statement) {
        var continueLabel = new Label();
        openForStatements.add(new OpenForStatement(statement, continueLabel));
        var index = getLocalFloatVarIndex(statement.varname());
        addCallback(statement, methodVisitor -> {
            statement.start().visit(this);
            methodVisitor.visitVarInsn(FSTORE, index);
            // end + increment go on the stack, to be used by NEXT statement
            statement.end().visit(this);
            if (statement.increment() != null) {
                statement.increment().visit(this);
            } else {
                methodVisitor.visitLdcInsn(1.0f);
            }
            methodVisitor.visitLabel(continueLabel);
            methodVisitor.visitInsn(NOP);
        });
    }

    @Override
    public void visit(NextStatement statement) {
        var openFor = findMatchingForStatement(statement);
        addCallback(statement, methodVisitor -> {
            var forStatement = openFor.forStatement();
            // copy end + increment from stack
            methodVisitor.visitInsn(DUP2);
            // add increment
            var varIndex = getLocalFloatVarIndex(forStatement.varname());
            methodVisitor.visitVarInsn(FLOAD, varIndex);
            methodVisitor.visitInsn(FADD);
            methodVisitor.visitVarInsn(FSTORE, varIndex);
            // then compare end with the loop variable
            methodVisitor.visitVarInsn(FLOAD, varIndex);
            methodVisitor.visitInsn(FCMPG);
            methodVisitor.visitJumpInsn(IFGE, openFor.continueLabel());
            // loop finished, so remove end + increment from the stack
            methodVisitor.visitInsn(POP2);
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
    public void visit(FloatInput statement) {
        var index = getLocalFloatVarIndex(statement.name());
        addCallback(statement, methodVisitor -> {
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                    className,
                    "inputFloat",
                    "()F");
            methodVisitor.visitVarInsn(FSTORE, index);
        });
    }

    @Override
    public void visit(FloatAssignment statement) {
        var index = getLocalFloatVarIndex(statement.name());
        addCallback(statement, methodVisitor -> {
            statement.expression().visit(this);
            methodVisitor.visitVarInsn(FSTORE, index);
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
        var index = getLocalFloatVarIndex(expression.name());
        currentMethodVisitor.visitVarInsn(FLOAD, index);
    }

    @Override
    public void visit(FloatEquals expression) {
        visitExpressions(expression);
        floatComparison(IFEQ);
    }

    @Override
    public void visit(FloatNotEquals expression) {
        visitExpressions(expression);
        floatComparison(IFNE);
    }

    @Override
    public void visit(FloatGreaterThan expression) {
        visitExpressions(expression);
        floatComparison(IFGT);
    }

    @Override
    public void visit(FloatGreaterThanEquals expression) {
        visitExpressions(expression);
        floatComparison(IFGE);
    }

    @Override
    public void visit(FloatLessThan expression) {
        visitExpressions(expression);
        floatComparison(IFLT);
    }

    @Override
    public void visit(FloatLessThanEquals expression) {
        visitExpressions(expression);
        floatComparison(IFLE);
    }

    private void floatComparison(int opcode) {
        currentMethodVisitor.visitInsn(FCMPG);
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

    private void visitExpressions(BinaryExpression expression) {
        expression.lhs().visit(this);
        expression.rhs().visit(this);
    }

    private void addCallback(Statement statement, Consumer<MethodVisitor> callback) {
        var label = createLineLabel(statement);
        Consumer<MethodVisitor> methodCallback = methodVisitor -> {
            if (label != null) {
                methodVisitor.visitLabel(label);
            }
            callback.accept(methodVisitor);
        };
        if (currentMethodVisitor == null) {
            methodCallbacks.add(methodCallback);
        } else {
            methodCallback.accept(currentMethodVisitor);
        }
    }

    private Label createLineLabel(Statement statement) {
        if (statement.lineLabel() != null) {
            var label = new Label();
            linesToLabels.put(statement.lineLabel(), label);
            return label;
        }
        return null;
    }

    private int getLocalFloatVarIndex(String name) {
        return localFloatVarIndexes.computeIfAbsent(name, n -> nextLocalVarIndex.getAndIncrement());
    }

    record OpenForStatement(ForStatement forStatement, Label continueLabel) {
    }
}
