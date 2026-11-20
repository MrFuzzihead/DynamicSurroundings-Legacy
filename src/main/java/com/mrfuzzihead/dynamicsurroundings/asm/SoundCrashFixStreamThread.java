package com.mrfuzzihead.dynamicsurroundings.asm;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class SoundCrashFixStreamThread extends Transmorgrifier {

    public SoundCrashFixStreamThread() {
        super("paulscode.sound.StreamThread");
    }

    @Override
    public String name() {
        return "removeSource";
    }

    @Override
    public boolean transmorgrify(ClassNode cn) {
        MethodNode m = this.findMethod(cn, "()V", "run");
        if (m != null) {
            for (AbstractInsnNode node = m.instructions.getFirst(); node != null; node = node.getNext()) {
                if (node instanceof MethodInsnNode && ((MethodInsnNode) node).owner.equals("java/util/ListIterator")
                    && ((MethodInsnNode) node).name.equals("next")) {
                    AbstractInsnNode insn = node.getNext()
                        .getNext();
                    LocalVariableNode var = this.findLocalVariable(m, "src");
                    if (var != null) {
                        m.instructions.insert(insn, new VarInsnNode(58, var.index));
                        m.instructions.insert(
                            insn,
                            new MethodInsnNode(
                                184,
                                "com/mrfuzzihead/dynamicsurroundings/client/sound/fix/SoundFixMethods",
                                "removeSource",
                                "(Lpaulscode/sound/Source;)Lpaulscode/sound/Source;",
                                false));
                        m.instructions.insert(insn, new VarInsnNode(25, var.index));
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
