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
    private int craftSoundThrottle = Integer.MIN_VALUE;

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

        private static int cachedConditionMask = -1;
        private static String cachedConditionTemperature;
        private static String cachedPlayerConditions;

        private static String getPlayerConditions(EntityPlayer player) {
            int mask = 0;
            if (isPlayerHurt()) {
                mask |= 1;
            }
            if (isPlayerHungry()) {
                mask |= 1 << 1;
            }
            if (isPlayerBurning()) {
                mask |= 1 << 2;
            }
            if (isPlayerSuffocating()) {
                mask |= 1 << 3;
            }
            if (isPlayerFlying()) {
                mask |= 1 << 4;
            }
            if (isPlayerSprinting()) {
                mask |= 1 << 5;
            }
            if (isPlayerInLava()) {
                mask |= 1 << 6;
            }
            if (isPlayerInvisible()) {
                mask |= 1 << 7;
            }
            if (isPlayerBlind()) {
                mask |= 1 << 8;
            }
            if (isPlayerInWater()) {
                mask |= 1 << 9;
            }
            if (isFreezing()) {
                mask |= 1 << 10;
            }
            if (isFoggy()) {
                mask |= 1 << 11;
            }
            if (isHumid()) {
                mask |= 1 << 12;
            }
            if (isDry()) {
                mask |= 1 << 13;
            }
            if (isPlayerInside()) {
                mask |= 1 << 14;
            }

            int ride = 0;
            if (isPlayerRiding()) {
                if (player.ridingEntity instanceof EntityMinecart) {
                    ride = 1;
                } else if (player.ridingEntity instanceof EntityHorse) {
                    ride = 2;
                } else if (player.ridingEntity instanceof EntityBoat) {
                    ride = 3;
                } else if (player.ridingEntity instanceof EntityPig) {
                    ride = 4;
                } else {
                    ride = 5;
                }
            }

            mask |= ride << 15;
            if (mask == cachedConditionMask && temperatureCategory.equals(cachedConditionTemperature)) {
                return cachedPlayerConditions;
            }

            StringBuilder builder = new StringBuilder();
            if ((mask & 1) != 0) {
                builder.append("#hurt");
            }
            if ((mask & 1 << 1) != 0) {
                builder.append("#hungry");
            }
            if ((mask & 1 << 2) != 0) {
                builder.append("#burning");
            }
            if ((mask & 1 << 3) != 0) {
                builder.append("#noair");
            }
            if ((mask & 1 << 4) != 0) {
                builder.append("#flying");
            }
            if ((mask & 1 << 5) != 0) {
                builder.append("#sprinting");
            }
            if ((mask & 1 << 6) != 0) {
                builder.append("#inlava");
            }
            if ((mask & 1 << 7) != 0) {
                builder.append("#invisible");
            }
            if ((mask & 1 << 8) != 0) {
                builder.append("#blind");
            }
            if ((mask & 1 << 9) != 0) {
                builder.append("#inwater");
            }
            if ((mask & 1 << 10) != 0) {
                builder.append("#freezing");
            }
            if ((mask & 1 << 11) != 0) {
                builder.append("#fog");
            }
            if ((mask & 1 << 12) != 0) {
                builder.append("#humid");
            }
            if ((mask & 1 << 13) != 0) {
                builder.append("#dry");
            }
            if ((mask & 1 << 14) != 0) {
                builder.append("#inside");
            }
            if (ride != 0) {
                builder.append('#');
                if (ride == 1) {
                    builder.append("ridingminecart");
                } else if (ride == 2) {
                    builder.append("ridinghorse");
                } else if (ride == 3) {
                    builder.append("ridingboat");
                } else if (ride == 4) {
                    builder.append("ridingpig");
                } else {
                    builder.append("riding");
                }
            }

            builder.append('#')
                .append(temperatureCategory);
            builder.append('#');
            cachedConditionMask = mask;
            cachedConditionTemperature = temperatureCategory;
            cachedPlayerConditions = builder.toString();
            return cachedPlayerConditions;
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
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null) {
                    player = mc.thePlayer;
                }
            }

            return player;
        }

        public static boolean isPlayer(Entity entity) {
            EntityPlayer current = getPlayer();
            if (current == null || !(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer ep = (EntityPlayer) entity;
            return ep.getUniqueID()
                .equals(current.getUniqueID());
        }

        public static boolean isPlayer(UUID id) {
            EntityPlayer current = getPlayer();
            return current != null && current.getUniqueID()
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
            EntityPlayer current = getPlayer();
            return current != null && current.distanceWalkedModified != player.prevDistanceWalkedModified;
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
            EntityPlayer current = getPlayer();
            return current == null ? null : current.worldObj;
        }

        public static int getTickCounter() {
            return tickCounter;
        }

        public static double distanceToPlayer(double x, double y, double z) {
            return player == null ? Double.MAX_VALUE : player.getDistanceSq(x, y, z);
        }
    }
}
