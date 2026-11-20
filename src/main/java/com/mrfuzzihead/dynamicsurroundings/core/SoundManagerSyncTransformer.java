package com.mrfuzzihead.dynamicsurroundings.core;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The one coremod patch that is not expressible as a mixin: {@code PatchSoundManagerSync} used to set
 * {@code ACC_SYNCHRONIZED} on every public, non-constructor method of {@code net.minecraft.client.audio.SoundManager}
 * so the mod's sound queue can touch the manager from its own worker thread without racing the OpenAL thread.
 *
 * <p>
 * Mixin can inject into, redirect and overwrite members, but it has no facility to change a member's access flags.
 * Reimplementing a dozen vanilla methods with {@code @Overwrite} just to append {@code synchronized} would be far
 * more fragile than this transform, so it is kept as a minimal {@link IClassTransformer}. It only flips a flag, so
 * frames and max stack are untouched; the class is rewritten only when at least one method changed.
 * </p>
 */
public final class SoundManagerSyncTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("dynamicsurroundings Transform");
    private static final String TARGET = "net.minecraft.client.audio.SoundManager";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET.equals(transformedName)) {
            return basicClass;
        }

        ClassNode classNode = new ClassNode(Opcodes.ASM5);
        new ClassReader(basicClass).accept(classNode, 0);

        boolean modified = false;
        for (MethodNode method : classNode.methods) {
            if (method.name.startsWith("<")) {
                continue;
            }
            if ((method.access & Opcodes.ACC_PUBLIC) == 0) {
                continue;
            }
            if ((method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_SYNCHRONIZED)) != 0) {
                continue;
            }
            method.access |= Opcodes.ACC_SYNCHRONIZED;
            modified = true;
        }

        if (!modified) {
            return basicClass;
        }

        LOGGER.info("Synchronized [{}] methods", TARGET);
        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
