package com.mrfuzzihead.dynamicsurroundings.mixins;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.renderer.EntityRenderer;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Checks the bytecode our injectors point at, not just the method names. The annotation processor verifies that a
 * member exists, but not that the instruction inside it is still there; a Forge or library update could rename a call
 * or drop a constant and the mixin would silently stop applying in production. Each check below mirrors one
 * {@code @At} target.
 */
public class MixinInjectionPointTest {

    @Test
    public void playSoundStillContainsThePlayTimeConstant() throws Exception {
        MethodNode playSound = findMethod(
            read(SoundManager.class),
            "playSound",
            "(Lnet/minecraft/client/audio/ISound;)V");
        boolean found = false;
        for (AbstractInsnNode insn : instructions(playSound)) {
            if (insn instanceof IntInsnNode && ((IntInsnNode) insn).operand == 20) {
                found = true;
                break;
            }
        }
        assertTrue("@ModifyConstant(intValue = 20) no longer matches playSound", found);
    }

    @Test
    public void playSoundStillCallsTheSoundSystem() throws Exception {
        MethodNode playSound = findMethod(
            read(SoundManager.class),
            "playSound",
            "(Lnet/minecraft/client/audio/ISound;)V");
        MethodInsnNode call = findCall(
            playSound,
            "net/minecraft/client/audio/SoundManager$SoundSystemStarterThread",
            "play",
            "(Ljava/lang/String;)V");
        assertNotNull(call);
    }

    @Test
    public void addRainParticlesIsStillAPrivateMethod() throws Exception {
        ClassNode entityRenderer = read(EntityRenderer.class);
        MethodNode method = findMethod(entityRenderer, "addRainParticles", "()V");
        assertTrue((method.access & Opcodes.ACC_PRIVATE) != 0);
    }

    @Test
    public void paulscodeRemoveSourceStillCallsCleanup() throws Exception {
        MethodNode removeSource = findMethod(read("paulscode.sound.Library"), "removeSource", "(Ljava/lang/String;)V");
        assertNotNull(findCall(removeSource, "paulscode/sound/Source", "cleanup", "()V"));
    }

    @Test
    public void streamThreadStillIteratesWithListIterator() throws Exception {
        MethodNode run = findMethod(read("paulscode.sound.StreamThread"), "run", "()V");
        assertNotNull(findCall(run, "java/util/ListIterator", "next", "()Ljava/lang/Object;"));
    }

    private static ClassNode read(Class<?> type) throws Exception {
        return read(type.getName());
    }

    private static ClassNode read(String name) throws Exception {
        String resource = "/" + name.replace('.', '/') + ".class";
        try (InputStream stream = MixinInjectionPointTest.class.getResourceAsStream(resource)) {
            assertTrue("missing class resource " + resource, stream != null);
            ClassNode node = new ClassNode(Opcodes.ASM5);
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    private static MethodNode findMethod(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return method;
            }
        }
        throw new AssertionError("missing method " + node.name + "." + name + desc);
    }

    private static MethodInsnNode findCall(MethodNode method, String owner, String name, String desc) {
        for (AbstractInsnNode insn : instructions(method)) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)) {
                    return call;
                }
            }
        }
        return null;
    }

    private static List<AbstractInsnNode> instructions(MethodNode method) {
        List<AbstractInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            result.add(insn);
        }
        return result;
    }
}
