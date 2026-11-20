package com.mrfuzzihead.dynamicsurroundings.asm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;

/**
 * The weather replacement is the mod's oldest coremod patch: it overwrites vanilla's {@code
 * World.updateWeatherBody} so that the per-dimension rain intensity is driven from the server handler. Both the
 * happy path (the call is inserted) and the failure path (vanilla moved the method, so the transformer reports no
 * change instead of corrupting the class) are covered.
 */
public class PatchWorldTest {

    @Test
    public void matcherOnlyAcceptsTheVanillaWorld() {
        PatchWorld patch = new PatchWorld();
        assertTrue(patch.matches("net.minecraft.world.World"));
        assertFalse(patch.matches("net.minecraft.world.WorldServer"));
        assertFalse(patch.matches("net.minecraft.world.WorldClient"));
    }

    @Test
    public void insertsTheHandlerCallAtTheTopOfTheMethod() {
        ClassNode world = classWithUpdateWeatherBody();

        assertTrue(new PatchWorld().transmorgrify(world));

        MethodNode method = world.methods.get(0);
        AbstractInsnNode first = method.instructions.getFirst();
        assertTrue(first instanceof VarInsnNode);
        assertEquals(Opcodes.ALOAD, first.getOpcode());
        assertEquals(0, ((VarInsnNode) first).var);

        AbstractInsnNode second = first.getNext();
        assertTrue(second instanceof MethodInsnNode);
        MethodInsnNode call = (MethodInsnNode) second;
        assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
        assertEquals("com/mrfuzzihead/dynamicsurroundings/server/WorldHandler", call.owner);
        assertEquals("updateWeatherBody", call.name);
        assertEquals("(Lnet/minecraft/world/World;)V", call.desc);

        AbstractInsnNode third = second.getNext();
        assertTrue(third instanceof InsnNode);
        assertEquals(Opcodes.RETURN, third.getOpcode());
    }

    @Test
    public void reportsNoChangeWhenTheMethodIsMissing() {
        ClassNode world = new ClassNode(Opcodes.ASM5);
        assertFalse(new PatchWorld().transmorgrify(world));
    }

    @Test
    public void patchIsDisabledWhenWeatherEffectsAreDisabled() {
        boolean original = ModOptions.disableWeatherEffects;
        try {
            ModOptions.disableWeatherEffects = true;
            assertFalse(new PatchWorld().isEnabled());
            ModOptions.disableWeatherEffects = false;
            assertTrue(new PatchWorld().isEnabled());
        } finally {
            ModOptions.disableWeatherEffects = original;
        }
    }

    private static ClassNode classWithUpdateWeatherBody() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "updateWeatherBody", "()V", null, null);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);
        return node;
    }

    @Test
    public void patchTargetsTheVanillaWorldClass() {
        PatchWorld patch = new PatchWorld();
        assertEquals("net.minecraft.world.World", patch.getClassName());
        assertEquals("updateWeatherBody", patch.name());
    }
}
