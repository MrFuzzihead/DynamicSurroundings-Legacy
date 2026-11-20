package com.mrfuzzihead.dynamicsurroundings.asm;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;

public class PatchWorld extends Transmorgrifier {

    public PatchWorld() {
        super("net.minecraft.world.World");
    }

    @Override
    public String name() {
        return "updateWeatherBody";
    }

    @Override
    public boolean isEnabled() {
        return !ModOptions.disableWeatherEffects;
    }

    @Override
    public boolean transmorgrify(ClassNode cn) {
        MethodNode m = this.findMethod(cn, "()V", "updateWeatherBody");
        if (m != null) {
            InsnList list = new InsnList();
            list.add(new VarInsnNode(25, 0));
            String owner = "com/mrfuzzihead/dynamicsurroundings/server/WorldHandler";
            String targetName = "updateWeatherBody";
            String sig = "(Lnet/minecraft/world/World;)V";
            list.add(
                new MethodInsnNode(
                    184,
                    "com/mrfuzzihead/dynamicsurroundings/server/WorldHandler",
                    "updateWeatherBody",
                    "(Lnet/minecraft/world/World;)V",
                    false));
            list.add(new InsnNode(177));
            m.instructions.insertBefore(m.instructions.getFirst(), list);
            return true;
        } else {
            Transformer.log()
                .info("Unable to patch [net.minecraft.world.World]!");
            return false;
        }
    }
}
