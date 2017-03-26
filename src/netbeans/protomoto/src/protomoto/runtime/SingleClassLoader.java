package protomoto.runtime;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

public class SingleClassLoader extends ClassLoader {
    private Hashtable<String, ClassNode> classNodeMap = new Hashtable<>();

    public SingleClassLoader(ClassLoader parent, ClassNode classNode) {
        this(parent, Arrays.asList(classNode));
    }

    public SingleClassLoader(ClassLoader parent, List<ClassNode> classNodes) {
        super(parent);
        classNodeMap = new Hashtable<>(classNodes.stream().collect(Collectors.toMap(cn -> cn.name, cn -> cn)));
    }

    public SingleClassLoader(ClassNode classNode) {
        this(Arrays.asList(classNode));
    }

    public SingleClassLoader(List<ClassNode> classNodes) {
        classNodeMap = new Hashtable<>(classNodes.stream().collect(Collectors.toMap(cn -> cn.name, cn -> cn)));
    }
    
    public void addClassNode(ClassNode classNode) {
        classNodeMap.put(classNode.name, classNode);
    }
    
    private Hashtable<String, Class<?>> classCache = new Hashtable<>();

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if(classCache.containsKey(name))
            return classCache.get(name);
        
        if(classNodeMap.containsKey(name)) {
            ClassNode classNode = classNodeMap.get(name);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            classNode.accept(classWriter);
                
            if(false) {
                classNode.accept(new TraceClassVisitor(new PrintWriter(System.out)));
                try {
                    org.objectweb.asm.util.CheckClassAdapter.verify(new ClassReader(classWriter.toByteArray()), true, new PrintWriter(System.out));
                } catch(Exception e) {
                    /*if(!hasASMMethodNodes)
                        throw e;*/
                }
            }

            byte[] classBytes = classWriter.toByteArray();
            Class<?> c = defineClass(name, classBytes, 0, classBytes.length);
            classCache.put(name, c);
            return c;
        }

        return getParent().loadClass(name);
    }
}
