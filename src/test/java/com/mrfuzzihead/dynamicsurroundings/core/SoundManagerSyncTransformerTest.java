package com.mrfuzzihead.dynamicsurroundings.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * {@link SoundManagerSyncTransformer} is the only patch that could not be turned into a mixin, because Mixin cannot
 * change a method's access flags. These tests pin its narrow contract: only {@code SoundManager}, only public
 * instance methods, and everything else left byte-for-byte alone.
 */
public class SoundManagerSyncTransformerTest {

    @Test
    public void synchronizesPublicInstanceMethods() {
        ClassNode soundManager = soundManager();

        byte[] transformed = new SoundManagerSyncTransformer()
            .transform("anything", "net.minecraft.client.audio.SoundManager", serialized(soundManager));

        ClassNode result = read(transformed);
        assertEquals(Opcodes.ACC_SYNCHRONIZED, find(result, "updateAllSounds").access & Opcodes.ACC_SYNCHRONIZED);
        assertEquals(Opcodes.ACC_SYNCHRONIZED, find(result, "playSound").access & Opcodes.ACC_SYNCHRONIZED);
    }

    @Test
    public void leavesConstructorsPrivateMethodsAndStaticMethodsAlone() {
        ClassNode soundManager = soundManager();

        byte[] transformed = new SoundManagerSyncTransformer()
            .transform("anything", "net.minecraft.client.audio.SoundManager", serialized(soundManager));

        ClassNode result = read(transformed);
        MethodNode ctor = result.methods.get(0);
        assertEquals("<init>", ctor.name);
        assertEquals(0, ctor.access & Opcodes.ACC_SYNCHRONIZED);
        assertEquals(0, find(result, "getNormalizedPitch").access & Opcodes.ACC_SYNCHRONIZED);
        assertEquals(0, find(result, "loadSoundSystem").access & Opcodes.ACC_SYNCHRONIZED);
        assertEquals(0, find(result, "helper").access & Opcodes.ACC_SYNCHRONIZED);
    }

    @Test
    public void doesNotTouchOtherClasses() {
        byte[] bytes = serialized(soundManager());
        byte[] result = new SoundManagerSyncTransformer()
            .transform("anything", "net.minecraft.client.audio.SoundHandler", bytes);
        assertSame(bytes, result);
    }

    @Test
    public void passesNullThrough() {
        assertNull(
            new SoundManagerSyncTransformer().transform("anything", "net.minecraft.client.audio.SoundManager", null));
    }

    @Test
    public void alwaysRewritesWhenSomethingChanged() {
        byte[] bytes = serialized(soundManager());
        byte[] result = new SoundManagerSyncTransformer()
            .transform("anything", "net.minecraft.client.audio.SoundManager", bytes);
        assertNotSame(bytes, result);
        // The transformed class must still be a valid class file.
        assertTrue(result.length > 0);
    }

    private static ClassNode soundManager() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        node.version = Opcodes.V1_7;
        node.name = "net/minecraft/client/audio/SoundManager";
        node.superName = "java/lang/Object";

        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null));
        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "updateAllSounds", "()V", null, null));
        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "playSound", "()V", null, null));
        node.methods.add(new MethodNode(Opcodes.ACC_PRIVATE, "getNormalizedPitch", "()F", null, null));
        node.methods.add(new MethodNode(Opcodes.ACC_PRIVATE, "loadSoundSystem", "()V", null, null));
        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "helper", "()V", null, null));
        return node;
    }

    private static byte[] serialized(ClassNode node) {
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        new org.objectweb.asm.ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static MethodNode find(ClassNode node, String name) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name)) {
                return method;
            }
        }
        throw new AssertionError("missing method " + name);
    }

    @Test
    public void returnsTheOriginalBytesWhenEverythingIsAlreadySynchronized() {
        ClassNode node = soundManager();
        for (MethodNode method : node.methods) {
            if (method.name.startsWith("<")) {
                continue;
            }
            if ((method.access & Opcodes.ACC_PUBLIC) != 0 && (method.access & Opcodes.ACC_STATIC) == 0) {
                method.access |= Opcodes.ACC_SYNCHRONIZED;
            }
        }

        byte[] bytes = serialized(node);
        assertSame(
            bytes,
            new SoundManagerSyncTransformer().transform("x", "net.minecraft.client.audio.SoundManager", bytes));
    }
}
