package com.mrfuzzihead.dynamicsurroundings.client.footsteps;

import java.util.List;
import java.util.Scanner;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent.Unload;

import com.mrfuzzihead.dynamicsurroundings.ModLog;
import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.IClientEffectHandler;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.ForgeDictionary;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.PFIsolator;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.PFReaderH;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.PFResourcePackDealer;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.PFSolver;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.ResourceLoader;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.game.system.UserConfigSoundPlayerWrapper;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem.AcousticsManager;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem.BasicPrimitiveMap;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.implem.LegacyCapableBlockMap;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IBlockMap;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.mcpackage.interfaces.IPrimitiveMap;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.parsers.AcousticsJsonReader;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.parsers.Register;
import com.mrfuzzihead.dynamicsurroundings.client.footsteps.util.property.simple.ConfigProperty;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.EntityAccessor;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Footsteps implements IResourceManagerReloadListener, IClientEffectHandler {

    public static Footsteps INSTANCE = null;
    private final PFResourcePackDealer dealer = new PFResourcePackDealer();
    private PFIsolator isolator;
    private boolean isFirstTime = true;

    public Footsteps() {
        INSTANCE = this;
        this.isolator = new PFIsolator();
    }

    public void reloadEverything() {
        this.isolator = new PFIsolator();
        List<IResourcePack> repo = this.dealer.findResourcePacks();
        this.reloadBlockMap(repo);
        this.reloadPrimitiveMap(repo);
        this.reloadAcoustics(repo);
        this.isolator.setSolver(new PFSolver(this.isolator));
        this.isolator.setGenerator(new PFReaderH(this.isolator));
    }

    private void reloadBlockMap(List<IResourcePack> repo) {
        IBlockMap blockMap = new LegacyCapableBlockMap();
        ForgeDictionary.initialize(blockMap);

        ResourceLoader.forEachPack(
            repo,
            "block map",
            this.dealer::openBlockMap,
            stream -> Register.setup(ConfigProperty.fromStream(stream), blockMap));

        this.isolator.setBlockMap(blockMap);
    }

    private void reloadPrimitiveMap(List<IResourcePack> repo) {
        IPrimitiveMap primitiveMap = new BasicPrimitiveMap();

        ResourceLoader.forEachPack(
            repo,
            "primitive map",
            this.dealer::openPrimitiveMap,
            stream -> Register.setup(ConfigProperty.fromStream(stream), primitiveMap));

        this.isolator.setPrimitiveMap(primitiveMap);
    }

    private void reloadAcoustics(List<IResourcePack> repo) {
        AcousticsManager acoustics = new AcousticsManager(this.isolator);

        ResourceLoader.forEachPack(repo, "acoustics", this.dealer::openAcoustics, stream -> {
            Scanner scanner = new Scanner(stream);
            try {
                new AcousticsJsonReader("").parseJSON(
                    scanner.useDelimiter("\\Z")
                        .next(),
                    acoustics);
            } finally {
                scanner.close();
            }
        });

        this.isolator.setAcoustics(acoustics);
        this.isolator.setSoundPlayer(new UserConfigSoundPlayerWrapper(acoustics));
        this.isolator.setDefaultStepPlayer(acoustics);
    }

    public void onResourceManagerReload(IResourceManager var1) {
        ModLog.info("Resource Pack reload detected...");
        this.reloadEverything();
    }

    @Override
    public void process(World world, EntityPlayer player) {
        if (this.isFirstTime) {
            this.isFirstTime = false;
            this.reloadEverything();
        }

        this.isolator.onFrame();
        EntityAccessor accessor = (EntityAccessor) player;
        if (ModOptions.footstepsSoundFactor > 0.0F) {
            if (accessor.dynamicsurroundings$getNextStepDistance() != Integer.MAX_VALUE) {
                accessor.dynamicsurroundings$setNextStepDistance(Integer.MAX_VALUE);
            }
        } else if (accessor.dynamicsurroundings$getNextStepDistance() == Integer.MAX_VALUE) {
            accessor.dynamicsurroundings$setNextStepDistance(0);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldUnload(Unload event) {
        if (event.world.isRemote && event.world.provider.dimensionId == 0) {
            this.isFirstTime = true;
        }
    }

    @Override
    public boolean hasEvents() {
        return true;
    }

    public IBlockMap getBlockMap() {
        return this.isolator.getBlockMap();
    }
}
