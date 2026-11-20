package com.mrfuzzihead.dynamicsurroundings.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Transformer extends MyTransformer {

    private static final Logger logger = LogManager.getLogger("dynamicsurroundings Transform");

    public static Logger log() {
        return logger;
    }

    public Transformer() {
        super(logger);
    }

    @Override
    protected void initTransmorgrifiers() {
        this.addTransmorgrifier(new PatchEntityRenderer());
        this.addTransmorgrifier(new PatchWorld());
        this.addTransmorgrifier(new PatchWorldServer());
        this.addTransmorgrifier(new PatchSoundManager());
        this.addTransmorgrifier(new PatchSoundManagerClampVolume());
        this.addTransmorgrifier(new PatchSoundManagerClampPitch());
        this.addTransmorgrifier(new PatchSoundManagerPlayTime());
        this.addTransmorgrifier(new SoundPlayFlush());
        this.addTransmorgrifier(new PatchSoundManagerSync());
        this.addTransmorgrifier(new SoundCrashFixSource());
        this.addTransmorgrifier(new SoundCrashFixLibrary());
        this.addTransmorgrifier(new SoundCrashFixStreamThread());
    }
}
