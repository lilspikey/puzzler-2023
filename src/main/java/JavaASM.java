import ast.AstVisitor;
import ast.FloatAddition;
import ast.FloatAssignment;
import ast.FloatConstant;
import ast.FloatInput;
import ast.FloatMultiplication;
import ast.FloatSubtraction;
import ast.FloatVariable;
import ast.GotoStatement;
import ast.IfStatement;
import ast.PrintStatement;
import ast.Statement;
import ast.StringConstant;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM4;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.NOP;

public class JavaASM implements AstVisitor {
    private String className;
    private final AtomicInteger nextLocalVarIndex = new AtomicInteger(1);
    private final Map<String, Integer> localFloatVarIndexes = new HashMap<>();
    private final Map<String, Label> linesToLabels = new HashMap<>();
    private final List<Consumer<MethodVisitor>> methodCallbacks = new ArrayList<>();
    private MethodVisitor currentMethodVisitor;

    public byte[] generateClass(String className) throws IOException {
        this.className = className;
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
                    methodVisitor.visitCode();
                    // simple main method that basically does
                    // new <className>().run();
                    methodVisitor.visitTypeInsn(NEW, className);
                    methodVisitor.visitInsn(DUP);
                    methodVisitor.visitMethodInsn(INVOKESPECIAL,
                            className,
                            "<init>",
                            "()V");
                    methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                            className,
                            "run",
                            "()V");
                    methodVisitor.visitEnd();
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
            methodVisitor.visitJumpInsn(IFEQ, falseLabel);
            for (var s: statement.trueStatements()) {
                s.visit(this);
            }
            methodVisitor.visitLabel(falseLabel);
            methodVisitor.visitInsn(NOP);
        });
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
    public void visit(FloatAddition expression) {
        expression.lhs().visit(this);
        expression.rhs().visit(this);
        currentMethodVisitor.visitInsn(FADD);
    }

    @Override
    public void visit(FloatSubtraction expression) {
        expression.lhs().visit(this);
        expression.rhs().visit(this);
        currentMethodVisitor.visitInsn(FSUB);
    }

    @Override
    public void visit(FloatMultiplication expression) {
        expression.lhs().visit(this);
        expression.rhs().visit(this);
        currentMethodVisitor.visitInsn(FMUL);
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
}
