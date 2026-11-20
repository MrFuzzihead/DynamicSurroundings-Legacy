package org.blockartistry.mod.DynSurround.asm;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public abstract class MyTransformer implements IClassTransformer {
   protected final Logger logger;
   private final List<Transmorgrifier> morgers = new ArrayList<>();

   public MyTransformer(Logger logger) {
      this.logger = logger;
      this.initTransmorgrifiers();
   }

   protected abstract void initTransmorgrifiers();

   public void addTransmorgrifier(Transmorgrifier t) {
      this.morgers.add(t);
   }

   public byte[] transform(String name, String transformedName, byte[] classBytes) {
      for (Transmorgrifier t : this.morgers) {
         if (t.matches(transformedName) && t.isEnabled()) {
            try {
               ClassReader cr = new ClassReader(classBytes);
               ClassNode cn = new ClassNode(327680);
               cr.accept(cn, 0);
               this.logger.info(String.format("Transmorgrifying [%s]: %s", transformedName, t.name()));
               boolean modified = t.transmorgrify(cn);
               ClassWriter cw = new ClassWriter(t.classWriterFlags());
               cn.accept(cw);
               classBytes = cw.toByteArray();
               if (modified) {
                  this.logger.info(String.format("Transmorgrified [%s]: %s", transformedName, t.name()));
               }
            } catch (Throwable var10) {
               var10.printStackTrace();
               throw var10;
            }
         }
      }

      return classBytes;
   }
}
