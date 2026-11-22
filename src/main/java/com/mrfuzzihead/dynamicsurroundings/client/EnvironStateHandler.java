package com.mrfuzzihead.dynamicsurroundings.client;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Text;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent.Start;

import com.mrfuzzihead.dynamicsurroundings.ModOptions;
import com.mrfuzzihead.dynamicsurroundings.client.sound.SoundEffect;
import com.mrfuzzihead.dynamicsurroundings.client.sound.SoundManager;
import com.mrfuzzihead.dynamicsurroundings.client.weather.Weather;
import com.mrfuzzihead.dynamicsurroundings.client.weather.WeatherBackend;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionRegistry;
import com.mrfuzzihead.dynamicsurroundings.event.DiagnosticEvent;
import com.mrfuzzihead.dynamicsurroundings.util.PlayerUtils;
import com.mrfuzzihead.dynamicsurroundings.util.random.XorShiftRandom;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EnvironStateHandler implements IClientEffectHandler {

    private static final SoundEffect JUMP;
    private static final SoundEffect SWORD;
    private static final SoundEffect AXE;
    private static final SoundEffect CRAFTING;
    private static final SoundEffect BOW_PULL;
    private List<String> diagnostics;
    private int craftSoundThrottle = 0;

    @Override
    public void process(World world, EntityPlayer player) {
        EnvironStateHandler.EnvironState.tick(world, player);
        if (Minecraft.getMinecraft().gameSettings.showDebugInfo && ModOptions.enableDebugLogging) {
            DiagnosticEvent.Gather gather = new DiagnosticEvent.Gather(world, player);
            MinecraftForge.EVENT_BUS.post(gather);
            this.diagnostics = gather.output;
        } else {
            this.diagnostics = null;
        }
    }

    @Override
    public boolean hasEvents() {
        return true;
    }

    @SubscribeEvent
    public void onJump(LivingJumpEvent event) {
        if (JUMP != null && event.entity != null && event.entity.worldObj != null) {
            if (event.entity.worldObj.isRemote && EnvironStateHandler.EnvironState.isPlayer(event.entity)) {
                SoundManager.playSoundAtPlayer(JUMP);
            }
        }
    }

    @SubscribeEvent
    public void onItemUse(AttackEntityEvent event) {
        if (SWORD != null && event.entityPlayer != null && event.entityPlayer.worldObj != null) {
            if (event.entityPlayer.worldObj.isRemote && EnvironStateHandler.EnvironState.isPlayer(event.entityPlayer)) {
                ItemStack currentItem = event.entityPlayer.getCurrentEquippedItem();
                if (currentItem != null) {
                    SoundEffect sound = null;
                    Item item = currentItem.getItem();
                    if (item instanceof ItemSword) {
                        sound = SWORD;
                    } else if (item instanceof ItemAxe) {
                        sound = AXE;
                    }

                    if (sound != null) {
                        SoundManager.playSoundAtPlayer(sound);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onCrafting(ItemCraftedEvent event) {
        if (CRAFTING != null && event.player != null && event.player.worldObj != null) {
            if (event.player.worldObj.isRemote && EnvironStateHandler.EnvironState.isPlayer(event.player)
                && this.craftSoundThrottle < EnvironStateHandler.EnvironState.getTickCounter() - 30) {
                this.craftSoundThrottle = EnvironStateHandler.EnvironState.getTickCounter();
                SoundManager.playSoundAtPlayer(CRAFTING);
            }
        }
    }

    @SubscribeEvent
    public void onItemUse(Start event) {
        if (BOW_PULL != null && event.entityPlayer != null
            && event.entityPlayer.worldObj != null
            && event.item != null
            && event.item.getItem() != null) {
            if (event.entityPlayer.worldObj.isRemote && event.item.getItem() instanceof ItemBow) {
                SoundManager.playSoundAtPlayer(BOW_PULL);
            }
        }
    }

    @SubscribeEvent
    public void onGatherText(@Nonnull Text event) {
        if (this.diagnostics != null && !this.diagnostics.isEmpty()) {
            event.left.add("");
            event.left.addAll(this.diagnostics);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void diagnostics(DiagnosticEvent.Gather event) {
        EntityPlayer player = EnvironStateHandler.EnvironState.getPlayer();
        event.output.add(
            "Dim: " + EnvironStateHandler.EnvironState.getDimensionId()
                + "/"
                + EnvironStateHandler.EnvironState.getDimensionName());
        event.output.add(
            "Player: h " + player.getHealth()
                + "/"
                + player.getMaxHealth()
                + "; f "
                + player.getFoodStats()
                    .getFoodLevel()
                + "; s "
                + player.getFoodStats()
                    .getSaturationLevel());
        event.output.add(Weather.diagnostic());
        event.output.add("Weather backend: " + WeatherBackend.owner(Minecraft.getMinecraft().theWorld));
        event.output.add("Biome: " + EnvironStateHandler.EnvironState.getBiomeName());
        event.output.add("Conditions: " + EnvironStateHandler.EnvironState.getConditions());
    }

    static {
        if (ModOptions.enableJumpSound) {
            JUMP = new SoundEffect("dynamicsurroundings:jump", 0.2F, 1.0F, true);
        } else {
            JUMP = null;
        }

        if (ModOptions.enableSwingSound) {
            SWORD = new SoundEffect("dynamicsurroundings:swoosh", 1.0F, 1.0F);
            AXE = new SoundEffect("dynamicsurroundings:swoosh", 1.0F, 0.5F);
        } else {
            SWORD = null;
            AXE = null;
        }

        if (ModOptions.enableCraftingSound) {
            CRAFTING = new SoundEffect("dynamicsurroundings:crafting");
        } else {
            CRAFTING = null;
        }

        if (ModOptions.enableBowPullSound) {
            BOW_PULL = new SoundEffect("dynamicsurroundings:bowpull");
        } else {
            BOW_PULL = null;
        }
    }

    public static class EnvironState {

        public static final Random RANDOM = new XorShiftRandom();
        private static String conditions = "";
        private static String biomeName = "";
        private static BiomeGenBase playerBiome = null;
        private static DimensionRegistry dimensionInfo = null;
        private static int dimensionId;
        private static String dimensionName;
        private static EntityPlayer player;
        private static boolean freezing;
        private static boolean fog;
        private static boolean humid;
        private static boolean dry;
        private static String temperatureCategory = "";
        private static boolean inside;
        private static int tickCounter;
        private static final String CONDITION_TOKEN_HURT = "hurt";
        private static final String CONDITION_TOKEN_HUNGRY = "hungry";
        private static final String CONDITION_TOKEN_BURNING = "burning";
        private static final String CONDITION_TOKEN_NOAIR = "noair";
        private static final String CONDITION_TOKEN_FLYING = "flying";
        private static final String CONDITION_TOKEN_SPRINTING = "sprinting";
        private static final String CONDITION_TOKEN_INLAVA = "inlava";
        private static final String CONDITION_TOKEN_INWATER = "inwater";
        private static final String CONDITION_TOKEN_INVISIBLE = "invisible";
        private static final String CONDITION_TOKEN_BLIND = "blind";
        private static final String CONDITION_TOKEN_MINECART = "ridingminecart";
        private static final String CONDITION_TOKEN_HORSE = "ridinghorse";
        private static final String CONDITION_TOKEN_BOAT = "ridingboat";
        private static final String CONDITION_TOKEN_PIG = "ridingpig";
        private static final String CONDITION_TOKEN_RIDING = "riding";
        private static final String CONDITION_TOKEN_FREEZING = "freezing";
        private static final String CONDITION_TOKEN_FOG = "fog";
        private static final String CONDITION_TOKEN_HUMID = "humid";
        private static final String CONDITION_TOKEN_DRY = "dry";
        private static final String CONDITION_TOKEN_INSIDE = "inside";
        private static final char CONDITION_SEPARATOR = '#';

        private static String getPlayerConditions(EntityPlayer player) {
            StringBuilder builder = new StringBuilder();
            if (isPlayerHurt()) {
                builder.append('#')
                    .append("hurt");
            }

            if (isPlayerHungry()) {
                builder.append('#')
                    .append("hungry");
            }

            if (isPlayerBurning()) {
                builder.append('#')
                    .append("burning");
            }

            if (isPlayerSuffocating()) {
                builder.append('#')
                    .append("noair");
            }

            if (isPlayerFlying()) {
                builder.append('#')
                    .append("flying");
            }

            if (isPlayerSprinting()) {
                builder.append('#')
                    .append("sprinting");
            }

            if (isPlayerInLava()) {
                builder.append('#')
                    .append("inlava");
            }

            if (isPlayerInvisible()) {
                builder.append('#')
                    .append("invisible");
            }

            if (isPlayerBlind()) {
                builder.append('#')
                    .append("blind");
            }

            if (isPlayerInWater()) {
                builder.append('#')
                    .append("inwater");
            }

            if (isFreezing()) {
                builder.append('#')
                    .append("freezing");
            }

            if (isFoggy()) {
                builder.append('#')
                    .append("fog");
            }

            if (isHumid()) {
                builder.append('#')
                    .append("humid");
            }

            if (isDry()) {
                builder.append('#')
                    .append("dry");
            }

            if (isPlayerInside()) {
                builder.append('#')
                    .append("inside");
            }

            if (isPlayerRiding()) {
                builder.append('#');
                if (player.ridingEntity instanceof EntityMinecart) {
                    builder.append("ridingminecart");
                } else if (player.ridingEntity instanceof EntityHorse) {
                    builder.append("ridinghorse");
                } else if (player.ridingEntity instanceof EntityBoat) {
                    builder.append("ridingboat");
                } else if (player.ridingEntity instanceof EntityPig) {
                    builder.append("ridingpig");
                } else {
                    builder.append("riding");
                }
            }

            builder.append('#')
                .append(temperatureCategory);
            builder.append('#');
            return builder.toString();
        }

        private static void tick(World world, EntityPlayer player) {
            EnvironStateHandler.EnvironState.player = player;
            conditions = DimensionRegistry.getConditions(world) + getPlayerConditions(player);
            playerBiome = PlayerUtils.getPlayerBiome(player, false);
            biomeName = BiomeRegistry.resolveName(playerBiome);
            dimensionInfo = DimensionRegistry.getData(player.worldObj);
            dimensionId = world.provider.dimensionId;
            dimensionName = world.provider.getDimensionName();
            inside = PlayerUtils.isReallyInside(EnvironStateHandler.EnvironState.player);
            int posX = MathHelper.floor_double(player.posX);
            int posY = MathHelper.floor_double(player.posY);
            int posZ = MathHelper.floor_double(player.posZ);
            BiomeGenBase trueBiome = PlayerUtils.getPlayerBiome(player, true);
            freezing = trueBiome.getFloatTemperature(posX, posY, posZ) < 0.15F;
            temperatureCategory = "tc" + trueBiome.getTempCategory()
                .name()
                .toLowerCase();
            humid = trueBiome.isHighHumidity();
            dry = trueBiome.getFloatRainfall() == 0.0F;
            if (!Minecraft.getMinecraft()
                .isGamePaused()) {
                tickCounter++;
            }
        }

        public static String getConditions() {
            return conditions;
        }

        public static BiomeGenBase getPlayerBiome() {
            return playerBiome;
        }

        public static String getBiomeName() {
            return biomeName;
        }

        public static DimensionRegistry getDimensionInfo() {
            return dimensionInfo;
        }

        public static int getDimensionId() {
            return dimensionId;
        }

        public static String getDimensionName() {
            return dimensionName;
        }

        public static EntityPlayer getPlayer() {
            if (player == null) {
                player = Minecraft.getMinecraft().thePlayer;
            }

            return player;
        }

        public static boolean isPlayer(Entity entity) {
            if (entity instanceof EntityPlayer) {
                EntityPlayer ep = (EntityPlayer) entity;
                return ep.getUniqueID()
                    .equals(getPlayer().getUniqueID());
            } else {
                return false;
            }
        }

        public static boolean isPlayer(UUID id) {
            return getPlayer().getUniqueID()
                .equals(id);
        }

        public static boolean isCreative() {
            return getPlayer().capabilities.isCreativeMode;
        }

        public static boolean isPlayerHurt() {
            return !isCreative() && getPlayer().getHealth() <= ModOptions.playerHurtThreshold;
        }

        public static boolean isPlayerHungry() {
            return !isCreative() && getPlayer().getFoodStats()
                .getFoodLevel() <= ModOptions.playerHungerThreshold;
        }

        public static boolean isPlayerBurning() {
            return getPlayer().isBurning();
        }

        public static boolean isPlayerSuffocating() {
            return getPlayer().getAir() <= 0;
        }

        public static boolean isPlayerFlying() {
            return getPlayer().capabilities.isFlying;
        }

        public static boolean isPlayerSprinting() {
            return getPlayer().isSprinting();
        }

        public static boolean isPlayerInLava() {
            return getPlayer().worldObj
                .isMaterialInBB(getPlayer().boundingBox.expand(-0.1F, -0.4F, -0.1F), Material.lava);
        }

        public static boolean isPlayerInvisible() {
            return getPlayer().isInvisible();
        }

        public static boolean isPlayerBlind() {
            return getPlayer().isPotionActive(Potion.blindness);
        }

        public static boolean isPlayerInWater() {
            return getPlayer().isInWater();
        }

        public static boolean isPlayerRiding() {
            return getPlayer().isRiding();
        }

        public static boolean isPlayerOnGround() {
            return getPlayer().onGround;
        }

        public static boolean isPlayerMoving() {
            return getPlayer().distanceWalkedModified != player.prevDistanceWalkedModified;
        }

        public static boolean isPlayerInside() {
            return inside;
        }

        public static boolean isPlayerUnderground() {
            return playerBiome == BiomeRegistry.UNDERGROUND;
        }

        public static boolean isPlayerInSpace() {
            return playerBiome == BiomeRegistry.OUTERSPACE;
        }

        public static boolean isPlayerInClouds() {
            return playerBiome == BiomeRegistry.CLOUDS;
        }

        public static boolean isFreezing() {
            return freezing;
        }

        public static boolean isFoggy() {
            return fog;
        }

        public static boolean isHumid() {
            return humid;
        }

        public static boolean isDry() {
            return dry;
        }

        public static World getWorld() {
            return getPlayer().worldObj;
        }

        public static int getTickCounter() {
            return tickCounter;
        }

        public static double distanceToPlayer(double x, double y, double z) {
            return player == null ? Double.MAX_VALUE : player.getDistanceSq(x, y, z);
        }
    }
}
