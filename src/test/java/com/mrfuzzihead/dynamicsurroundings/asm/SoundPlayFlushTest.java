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

/**
 * Vanilla's {@code SoundManager.playSound} queues a sound on the OpenAL worker thread, so the mod has to flush its
 * own buffered "spot" sounds right before the queue is touched. The transformer looks for SoundSystem's
 * {@code play} call and appends a static flush hook after it.
 */
public class SoundPlayFlushTest {

    private static final String SOUND_MANAGER = "net.minecraft.client.audio.SoundManager";
    private static final String PLAY_SIGNATURE = "(Lnet/minecraft/client/audio/ISound;)V";

    @Test
    public void matcherOnlyAcceptsTheClientSoundManager() {
        SoundPlayFlush patch = new SoundPlayFlush();
        assertTrue(patch.matches(SOUND_MANAGER));
        assertFalse(patch.matches("net.minecraft.world.World"));
    }

    @Test
    public void flushHookIsInsertedAfterTheSoundSystemPlayCall() {
        ClassNode soundManager = soundManagerWithPlayCall();

        assertTrue(new SoundPlayFlush().transmorgrify(soundManager));

        MethodNode playSound = soundManager.methods.get(0);
        AbstractInsnNode play = findPlayCall(playSound);
        AbstractInsnNode afterPlay = play.getNext();
        assertTrue(afterPlay instanceof MethodInsnNode);
        MethodInsnNode flush = (MethodInsnNode) afterPlay;
        assertEquals(Opcodes.INVOKESTATIC, flush.getOpcode());
        assertEquals("com/mrfuzzihead/dynamicsurroundings/client/sound/SoundManager", flush.owner);
        assertEquals("flushSound", flush.name);
        assertEquals("()V", flush.desc);
    }

    @Test
    public void reportsNoChangeWhenPlaySoundIsMissing() {
        ClassNode soundManager = new ClassNode(Opcodes.ASM5);
        assertFalse(new SoundPlayFlush().transmorgrify(soundManager));
    }

    @Test
    public void reportsNoChangeWhenTheSoundSystemCallIsMissing() {
        ClassNode soundManager = new ClassNode(Opcodes.ASM5);
        MethodNode playSound = new MethodNode(Opcodes.ACC_PUBLIC, "playSound", PLAY_SIGNATURE, null, null);
        playSound.instructions.add(new InsnNode(Opcodes.RETURN));
        soundManager.methods.add(playSound);

        assertFalse(new SoundPlayFlush().transmorgrify(soundManager));
    }

    private static ClassNode soundManagerWithPlayCall() {
        ClassNode node = new ClassNode(Opcodes.ASM5);
        MethodNode playSound = new MethodNode(Opcodes.ACC_PUBLIC, "playSound", PLAY_SIGNATURE, null, null);
        playSound.instructions.add(
            new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/client/audio/SoundManager$SoundSystemStarterThread",
                "play",
                "(Ljava/lang/String;)V",
                false));
        playSound.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(playSound);
        return node;
    }

    private static AbstractInsnNode findPlayCall(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode && "play".equals(((MethodInsnNode) insn).name)) {
                return insn;
            }
        }
        throw new AssertionError("the play call disappeared");
    }
}
