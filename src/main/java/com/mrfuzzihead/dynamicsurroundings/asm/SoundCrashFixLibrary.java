package com.mrfuzzihead.dynamicsurroundings.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class SoundCrashFixLibrary extends Transmorgrifier {

    public SoundCrashFixLibrary() {
        super("paulscode.sound.Library");
    }

    @Override
    public String name() {
        return "removeSource";
    }

    @Override
    public boolean transmorgrify(ClassNode cn) {
        MethodNode m = this.findMethod(cn, "(Ljava/lang/String;)V", "removeSource");
        if (m != null) {
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).owner.equals("paulscode/sound/Source")
                    && ((MethodInsnNode) insn).name.equals("cleanup")) {
                    m.instructions.insertBefore(
                        insn,
                        new MethodInsnNode(
                            184,
                            "com/mrfuzzihead/dynamicsurroundings/client/sound/fix/SoundFixMethods",
                            "cleanupSource",
                            "(Lpaulscode/sound/Source;)V",
                            false));
                    m.instructions.remove(insn);
                    return true;
                }
            }
        }

        return false;
    }
}
