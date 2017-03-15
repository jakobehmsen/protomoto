package protomoto.runtime;

import java.io.PrintWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

public class SingleClassLoader extends ClassLoader {
    private ClassNode classNode;

    public SingleClassLoader(ClassLoader parent, ClassNode classNode) {
        super(parent);
        this.classNode = classNode;
    }

    public SingleClassLoader(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if(classNode.name.equals(name)) {
            classNode.accept(new TraceClassVisitor(new PrintWriter(System.out)));

            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            classNode.accept(classWriter);

            try {
                org.objectweb.asm.util.CheckClassAdapter.verify(new ClassReader(classWriter.toByteArray()), true, new PrintWriter(System.out));
            } catch(Exception e) {
                /*if(!hasASMMethodNodes)
                    throw e;*/
            }

            byte[] classBytes = classWriter.toByteArray();
            return defineClass(name, classBytes, 0, classBytes.length);
        }

        return getParent().loadClass(name);
    }
}
