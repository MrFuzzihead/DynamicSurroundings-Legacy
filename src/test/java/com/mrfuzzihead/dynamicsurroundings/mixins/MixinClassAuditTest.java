package com.mrfuzzihead.dynamicsurroundings.mixins;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

public class MixinClassAuditTest {

    private static final String MIXIN_DESCRIPTOR = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String UNIQUE_DESCRIPTOR = "Lorg/spongepowered/asm/mixin/Unique;";
    private static final String SHADOW_DESCRIPTOR = "Lorg/spongepowered/asm/mixin/Shadow;";

    @Test
    public void everyClassInTheMixinPackageIsAMixinOrExcluded() throws Exception {
        URL location = EarlyMixinsLoader.class.getProtectionDomain()
            .getCodeSource()
            .getLocation();
        File root = new File(location.toURI());
        File mixinRoot = new File(root, "com/mrfuzzihead/dynamicsurroundings/mixins");
        assertTrue("compiled mixin package is missing", mixinRoot.isDirectory());

        Set<String> excluded = new HashSet<>();
        TransformerExclusions annotation = EarlyMixinsLoader.class.getAnnotation(TransformerExclusions.class);
        assertNotNull("EarlyMixinsLoader must keep @TransformerExclusions", annotation);
        excluded.addAll(Arrays.asList(annotation.value()));

        int checked = 0;
        for (File file : mixinRoot.listFiles()) {
            checked += check(file, mixinRoot, excluded);
        }
        assertTrue("no mixin classes were found", checked > 0);
    }

    private static int check(File file, File root, Set<String> excluded) throws Exception {
        if (file.isDirectory()) {
            int checked = 0;
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    checked += check(child, root, excluded);
                }
            }
            return checked;
        }
        if (!file.getName()
            .endsWith(".class")) {
            return 0;
        }

        String relative = root.toURI()
            .relativize(file.toURI())
            .getPath()
            .replace('/', '.')
            .replace(".class", "");
        String className = "com.mrfuzzihead.dynamicsurroundings.mixins." + relative;
        if (declaresMixin(file)) {
            assertFieldsAreOwnedOrShadowed(file, className);
            return 1;
        }

        assertTrue(
            className + " is in the mixin package but is not @Mixin and not in @TransformerExclusions",
            excluded.contains(className));
        return 1;
    }

    private static void assertFieldsAreOwnedOrShadowed(File file, String className) throws Exception {
        new ClassReader(Files.readAllBytes(file.toPath())).accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                AtomicBoolean annotated = new AtomicBoolean();
                return new FieldVisitor(Opcodes.ASM5) {

                    @Override
                    public org.objectweb.asm.AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        if (UNIQUE_DESCRIPTOR.equals(annotation) || SHADOW_DESCRIPTOR.equals(annotation)) {
                            annotated.set(true);
                        }
                        return super.visitAnnotation(annotation, visible);
                    }

                    @Override
                    public void visitEnd() {
                        assertTrue(className + " field " + name + " must be @Unique or @Shadow", annotated.get());
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static boolean declaresMixin(File file) throws Exception {
        AtomicBoolean found = new AtomicBoolean();
        new ClassReader(Files.readAllBytes(file.toPath())).accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (MIXIN_DESCRIPTOR.equals(descriptor)) {
                    found.set(true);
                }
                return super.visitAnnotation(descriptor, visible);
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return found.get();
    }
}
