package com.mrfuzzihead.dynamicsurroundings.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.common.config.Property.Type;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.Module;
import com.mrfuzzihead.dynamicsurroundings.data.SoundRegistry;
import com.mrfuzzihead.dynamicsurroundings.mixins.early.accessors.SoundHandlerAccessor;
import com.mrfuzzihead.dynamicsurroundings.util.ConfigProcessor;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries.NumberSliderEntry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class DynamicSurroundingsConfigGui extends GuiConfig {

    private final Configuration config = Module.config();
    private final ConfigElement soundElement;
    private final ConfigCategory soundCategory;
    private final ConfigElement soundVolumeElement;
    private final ConfigCategory soundVolumeCategory;

    public DynamicSurroundingsConfigGui(GuiScreen parentScreen) {
        super(parentScreen, new ArrayList(), "dynamicsurroundings", false, false, "Dynamic Surroundings");
        this.titleLine2 = this.config.getConfigFile()
            .getAbsolutePath();
        this.configElements.add(this.getPropertyConfigElement("aurora", "Enabled", "Aurora Feature"));
        this.configElements.add(this.getPropertyConfigElement("fog", "Desert Fog", "Desert Dust Feature"));
        this.configElements.add(this.getPropertyConfigElement("fog", "Elevation Haze", "Elevation Haze Feature"));
        this.configElements.add(this.getPropertyConfigElement("fog", "Biome Fog", "Biome Fog Feature"));
        this.configElements.add(this.getPropertyConfigElement("sound", "Enable Biome Sounds", "Biome Sound Feature"));
        this.configElements.add(this.getPropertyConfigElement("sound", "Jump Sound", "Player Jump Sound Effect"));
        this.configElements
            .add(this.getPropertyConfigElement("sound", "Swing Sound", "Player Weapon Swing Sound Effect"));
        this.configElements
            .add(this.getPropertyConfigElement("sound", "Crafting Sound", "Player Crafting Sound Effect"));
        this.configElements
            .add(this.getPropertyConfigElement("sound", "Bow Pull Sound", "Player Bow Pull Sound Effect"));
        this.configElements.add(this.getPropertyConfigElement("sound", "Footsteps", "Footstep Sound Effects"));
        this.configElements.add(this.getPropertyConfigElement("player.potion hud", "Enable", "Potion HUD Overlay"));
        this.soundCategory = new ConfigCategory("Blocked Sounds");
        this.soundCategory.setComment("Sounds that will be blocked from playing");
        this.soundElement = new MyConfigElement(this.soundCategory);
        this.generateSoundList(this.soundCategory);
        this.configElements.add(this.soundElement);
        this.soundVolumeCategory = new ConfigCategory("Sound Volumes");
        this.soundVolumeCategory.setComment("Individual sound volume control");
        this.soundVolumeElement = new MyConfigElement(this.soundVolumeCategory);
        this.generateSoundVolumeList(this.soundVolumeCategory);
        this.configElements.add(this.soundVolumeElement);
        this.configElements.add(this.getCategoryConfigElement("general", "General Settings"));
        this.configElements.add(this.getCategoryConfigElement("rain", "Rain Settings"));
        this.configElements.add(this.getCategoryConfigElement("fog", "Fog Settings"));
        this.configElements.add(this.getCategoryConfigElement("aurora", "Aurora Settings"));
        this.configElements.add(this.getCategoryConfigElement("biomes", "Biome Behaviors"));
        this.configElements.add(this.getCategoryConfigElement("dimensions", "Dimension Configuration"));
        this.configElements.add(this.getCategoryConfigElement("sound", "Sound Effects"));
        this.configElements.add(this.getCategoryConfigElement("player", "Player Effects"));
        this.configElements.add(this.getCategoryConfigElement("logging", "Logging Options"));
    }

    private ConfigElement getCategoryConfigElement(String category, String label) {
        ConfigCategory cat = this.config.getCategory(category);
        return new MyConfigElement(cat, label);
    }

    private ConfigElement getPropertyConfigElement(String category, String property, String label) {
        Property prop = this.config.getCategory(category)
            .get(property);
        return new MyConfigElement(prop, label);
    }

    protected void actionPerformed(GuiButton button) {
        if (button.id == 2000) {
            this.saveSoundList();
            this.saveSoundVolumeList();
            this.config.save();
            ConfigProcessor.process(this.config, ModOptions.class);
            SoundRegistry.initialize();
        }

        super.actionPerformed(button);
    }

    protected void saveSoundList() {
        List<String> sounds = new ArrayList<>();

        for (Entry<String, Property> entry : this.soundCategory.entrySet()) {
            if (entry.getValue()
                .getBoolean()) {
                sounds.add(entry.getKey());
            }
        }

        String[] results = sounds.toArray(new String[sounds.size()]);
        this.config.getCategory("sound")
            .get("Blocked Sounds")
            .set(results);
    }

    protected void saveSoundVolumeList() {
        List<String> sounds = new ArrayList<>();

        for (Entry<String, Property> entry : this.soundVolumeCategory.entrySet()) {
            int value = entry.getValue()
                .getInt();
            if (value != 100) {
                sounds.add(entry.getKey() + "=" + value);
            }
        }

        String[] results = sounds.toArray(new String[sounds.size()]);
        this.config.getCategory("sound")
            .get("Sound Volume")
            .set(results);
    }

    protected void generateSoundList(ConfigCategory cat) {
        SoundHandler handler = Minecraft.getMinecraft()
            .getSoundHandler();
        List<String> sounds = new ArrayList<>();

        for (Object resource : ((SoundHandlerAccessor) handler).dynamicsurroundings$getSndRegistry()
            .getKeys()) {
            sounds.add(resource.toString());
        }

        Collections.sort(sounds);

        for (String sound : sounds) {
            Property prop = new Property(sound, "false", Type.BOOLEAN);
            prop.setDefaultValue(false);
            prop.setRequiresMcRestart(false);
            prop.setRequiresWorldRestart(false);
            prop.set(SoundRegistry.isSoundBlocked(sound));
            cat.put(sound, prop);
        }
    }

    protected void generateSoundVolumeList(ConfigCategory cat) {
        SoundHandler handler = Minecraft.getMinecraft()
            .getSoundHandler();
        List<String> sounds = new ArrayList<>();

        for (Object resource : ((SoundHandlerAccessor) handler).dynamicsurroundings$getSndRegistry()
            .getKeys()) {
            sounds.add(resource.toString());
        }

        Collections.sort(sounds);

        for (String sound : sounds) {
            Property prop = new Property(sound, "100", Type.INTEGER);
            prop.setMinValue(0);
            prop.setMaxValue(200);
            prop.setDefaultValue(100);
            prop.setRequiresMcRestart(false);
            prop.setRequiresWorldRestart(false);
            prop.set(MathHelper.floor_float(SoundRegistry.getVolumeScale(sound) * 100.0F));
            prop.setConfigEntryClass(NumberSliderEntry.class);
            cat.put(sound, prop);
        }
    }
}
