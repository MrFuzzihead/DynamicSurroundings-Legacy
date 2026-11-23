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

    static void refreshState(World world, EntityPlayer player) {
        EnvironState.tick(world, player);
    }

    @Override
    public void process(World world, EntityPlayer player) {
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
        private static volatile EnvironmentSnapshot snapshot = EnvironmentSnapshot.EMPTY;
        private static int cachedConditionMask = -1;
        private static String cachedConditionTemperature;
        private static String cachedPlayerConditions;

        public static EnvironmentSnapshot getSnapshot() {
            return snapshot;
        }

        private static boolean isCreative(EntityPlayer player) {
            return player.capabilities.isCreativeMode;
        }

        private static boolean isPlayerHurt(EntityPlayer player) {
            return !isCreative(player) && player.getHealth() <= ModOptions.playerHurtThreshold;
        }

        private static boolean isPlayerHungry(EntityPlayer player) {
            return !isCreative(player) && player.getFoodStats()
                .getFoodLevel() <= ModOptions.playerHungerThreshold;
        }

        private static boolean isPlayerBurning(EntityPlayer player) {
            return player.isBurning();
        }

        private static boolean isPlayerSuffocating(EntityPlayer player) {
            return player.getAir() <= 0;
        }

        private static boolean isPlayerFlying(EntityPlayer player) {
            return player.capabilities.isFlying;
        }

        private static boolean isPlayerSprinting(EntityPlayer player) {
            return player.isSprinting();
        }

        private static boolean isPlayerInLava(EntityPlayer player) {
            return player.worldObj.isMaterialInBB(player.boundingBox.expand(-0.1F, -0.4F, -0.1F), Material.lava);
        }

        private static boolean isPlayerInvisible(EntityPlayer player) {
            return player.isInvisible();
        }

        private static boolean isPlayerBlind(EntityPlayer player) {
            return player.isPotionActive(Potion.blindness);
        }

        private static boolean isPlayerInWater(EntityPlayer player) {
            return player.isInWater();
        }

        private static boolean isPlayerRiding(EntityPlayer player) {
            return player.isRiding();
        }

        private static String getPlayerConditions(EntityPlayer player, String temperature, boolean freezing,
            boolean fog, boolean humid, boolean dry, boolean inside) {
            int mask = 0;
            if (isPlayerHurt(player)) {
                mask |= 1;
            }
            if (isPlayerHungry(player)) {
                mask |= 1 << 1;
            }
            if (isPlayerBurning(player)) {
                mask |= 1 << 2;
            }
            if (isPlayerSuffocating(player)) {
                mask |= 1 << 3;
            }
            if (isPlayerFlying(player)) {
                mask |= 1 << 4;
            }
            if (isPlayerSprinting(player)) {
                mask |= 1 << 5;
            }
            if (isPlayerInLava(player)) {
                mask |= 1 << 6;
            }
            if (isPlayerInvisible(player)) {
                mask |= 1 << 7;
            }
            if (isPlayerBlind(player)) {
                mask |= 1 << 8;
            }
            if (isPlayerInWater(player)) {
                mask |= 1 << 9;
            }
            if (freezing) {
                mask |= 1 << 10;
            }
            if (fog) {
                mask |= 1 << 11;
            }
            if (humid) {
                mask |= 1 << 12;
            }
            if (dry) {
                mask |= 1 << 13;
            }
            if (inside) {
                mask |= 1 << 14;
            }

            int ride = 0;
            if (isPlayerRiding(player)) {
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
            if (mask == cachedConditionMask && temperature.equals(cachedConditionTemperature)) {
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
                .append(temperature);
            builder.append('#');
            cachedConditionMask = mask;
            cachedConditionTemperature = temperature;
            cachedPlayerConditions = builder.toString();
            return cachedPlayerConditions;
        }

        private static void tick(World world, EntityPlayer player) {
            BiomeGenBase playerBiome = PlayerUtils.getPlayerBiome(player, false);
            BiomeGenBase trueBiome = PlayerUtils.getPlayerBiome(player, true);
            int posX = MathHelper.floor_double(player.posX);
            int posY = MathHelper.floor_double(player.posY);
            int posZ = MathHelper.floor_double(player.posZ);
            boolean freezing = trueBiome.getFloatTemperature(posX, posY, posZ) < 0.15F;
            String temperature = "tc" + trueBiome.getTempCategory()
                .name()
                .toLowerCase();
            boolean humid = trueBiome.isHighHumidity();
            boolean dry = trueBiome.getFloatRainfall() == 0.0F;
            boolean inside = PlayerUtils.isReallyInside(player);
            int nextTick = snapshot.getTickCounter();
            if (!Minecraft.getMinecraft()
                .isGamePaused()) {
                nextTick++;
            }

            snapshot = EnvironmentSnapshot.builder()
                .player(player)
                .world(world)
                .playerBiome(playerBiome)
                .biomeName(BiomeRegistry.resolveName(playerBiome))
                .dimensionInfo(DimensionRegistry.getData(player.worldObj))
                .dimensionId(world.provider.dimensionId)
                .dimensionName(world.provider.getDimensionName())
                .conditions(
                    DimensionRegistry.getConditions(world)
                        + getPlayerConditions(player, temperature, freezing, false, humid, dry, inside))
                .freezing(freezing)
                .foggy(false)
                .humid(humid)
                .dry(dry)
                .inside(inside)
                .temperatureCategory(temperature)
                .tickCounter(nextTick)
                .playerOnGround(player.onGround)
                .playerMoving(player.distanceWalkedModified != player.prevDistanceWalkedModified)
                .build();
        }

        public static String getConditions() {
            return snapshot.getConditions();
        }

        public static BiomeGenBase getPlayerBiome() {
            return snapshot.getPlayerBiome();
        }

        public static String getBiomeName() {
            return snapshot.getBiomeName();
        }

        public static DimensionRegistry getDimensionInfo() {
            return snapshot.getDimensionInfo();
        }

        public static int getDimensionId() {
            return snapshot.getDimensionId();
        }

        public static String getDimensionName() {
            return snapshot.getDimensionName();
        }

        public static EntityPlayer getPlayer() {
            EntityPlayer player = snapshot.getPlayer();
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
            return isCreative(getPlayer());
        }

        public static boolean isPlayerHurt() {
            return isPlayerHurt(getPlayer());
        }

        public static boolean isPlayerHungry() {
            return isPlayerHungry(getPlayer());
        }

        public static boolean isPlayerBurning() {
            return isPlayerBurning(getPlayer());
        }

        public static boolean isPlayerSuffocating() {
            return isPlayerSuffocating(getPlayer());
        }

        public static boolean isPlayerFlying() {
            return isPlayerFlying(getPlayer());
        }

        public static boolean isPlayerSprinting() {
            return isPlayerSprinting(getPlayer());
        }

        public static boolean isPlayerInLava() {
            return isPlayerInLava(getPlayer());
        }

        public static boolean isPlayerInvisible() {
            return isPlayerInvisible(getPlayer());
        }

        public static boolean isPlayerBlind() {
            return isPlayerBlind(getPlayer());
        }

        public static boolean isPlayerInWater() {
            return isPlayerInWater(getPlayer());
        }

        public static boolean isPlayerRiding() {
            return isPlayerRiding(getPlayer());
        }

        public static boolean isPlayerOnGround() {
            return snapshot.isPlayerOnGround();
        }

        public static boolean isPlayerMoving() {
            return snapshot.isPlayerMoving();
        }

        public static boolean isPlayerInside() {
            return snapshot.isInside();
        }

        public static boolean isPlayerUnderground() {
            return snapshot.getPlayerBiome() == BiomeRegistry.UNDERGROUND;
        }

        public static boolean isPlayerInSpace() {
            return snapshot.getPlayerBiome() == BiomeRegistry.OUTERSPACE;
        }

        public static boolean isPlayerInClouds() {
            return snapshot.getPlayerBiome() == BiomeRegistry.CLOUDS;
        }

        public static boolean isFreezing() {
            return snapshot.isFreezing();
        }

        public static boolean isFoggy() {
            return snapshot.isFoggy();
        }

        public static boolean isHumid() {
            return snapshot.isHumid();
        }

        public static boolean isDry() {
            return snapshot.isDry();
        }

        public static World getWorld() {
            World world = snapshot.getWorld();
            if (world != null) {
                return world;
            }

            EntityPlayer current = getPlayer();
            return current == null ? null : current.worldObj;
        }

        public static int getTickCounter() {
            return snapshot.getTickCounter();
        }

        public static double distanceToPlayer(double x, double y, double z) {
            EntityPlayer player = getPlayer();
            return player == null ? Double.MAX_VALUE : player.getDistanceSq(x, y, z);
        }
    }

}
