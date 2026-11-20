package com.mrfuzzihead.dynamicsurroundings.asm;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;

public class PatchWorldServer extends Transmorgrifier {

    public PatchWorldServer() {
        super("net.minecraft.world.WorldServer");
    }

    @Override
    public String name() {
        return "resetRainAndThunder";
    }

    @Override
    public boolean isEnabled() {
        return !ModOptions.disableWeatherEffects;
    }

    @Override
    public boolean transmorgrify(ClassNode cn) {
        String[] names = new String[] { "resetRainAndThunder", "resetRainAndThunder" };
        String sigs = "()V";
        MethodNode m = this.findMethod(cn, "()V", names);
        if (m != null) {
            this.logMethod(Transformer.log(), m, "Found!");
            m.localVariables = null;
            InsnList list = new InsnList();
            list.add(new VarInsnNode(25, 0));
            String owner = "com/mrfuzzihead/dynamicsurroundings/server/PlayerSleepHandler";
            String targetName = "resetRainAndThunder";
            String sig = "(Lnet/minecraft/world/WorldServer;)V";
            list.add(
                new MethodInsnNode(
                    184,
                    "com/mrfuzzihead/dynamicsurroundings/server/PlayerSleepHandler",
                    "resetRainAndThunder",
                    "(Lnet/minecraft/world/WorldServer;)V",
                    false));
            list.add(new InsnNode(177));
            m.instructions = list;
            return true;
        } else {
            Transformer.log()
                .info("Unable to patch [net.minecraft.world.WorldServer]!");
            return false;
        }
    }
}
