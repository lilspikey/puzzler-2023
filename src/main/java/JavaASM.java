import ast.AstVisitor;
import ast.GotoStatement;
import ast.PrintStatement;
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
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.ASM4;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class JavaASM implements AstVisitor {
    private final Map<String, Label> linesToLabels = new HashMap<>();
    private final List<Consumer<MethodVisitor>> methodCallbacks = new ArrayList<>();

    public byte[] generateClass(String className) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classVisitor = new ClassVisitor(ASM4, classWriter) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, className, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                var methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if ("main".equals(name)) {
                    methodVisitor.visitCode();
                    for (var callback: methodCallbacks) {
                        callback.accept(methodVisitor);
                    }
                    methodVisitor.visitEnd();
                }
                return methodVisitor;
            }
        };
        try (var in = getTemplateClassBytes()) {
            ClassReader reader = new ClassReader(in);
            reader.accept(classVisitor, 0);
            return classWriter.toByteArray();
        }
    }

    private InputStream getTemplateClassBytes() {
        String className = Template.class.getName();
        String classAsPath = className.replace('.', '/') + ".class";
        return Template.class.getClassLoader().getResourceAsStream(classAsPath);
    }


    @Override
    public void visit(PrintStatement statement) {
        methodCallbacks.add(methodVisitor -> {
            if (statement.lineLabel() != null) {
                var label = new Label();
                linesToLabels.put(statement.lineLabel(), label);
                methodVisitor.visitLabel(label);
            }
            methodVisitor.visitFieldInsn(GETSTATIC,
                    "java/lang/System",
                    "out",
                    "Ljava/io/PrintStream;");
            methodVisitor.visitLdcInsn(statement.strings().get(0));
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
                    "java/io/PrintStream",
                    "println",
                    "(Ljava/lang/String;)V");
        });
    }

    @Override
    public void visit(GotoStatement statement) {
        methodCallbacks.add(methodVisitor -> {
            var label = linesToLabels.get(statement.destinationLabel());
            if (label == null) {
                throw new IllegalStateException("Unknown destination label: " + statement);
            }
            methodVisitor.visitJumpInsn(GOTO, label);
        });
    }
}
