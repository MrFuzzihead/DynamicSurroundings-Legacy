package com.mrfuzzihead.dynamicsurroundings.commands;

import java.text.DecimalFormat;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.google.common.collect.ImmutableList;
import com.mrfuzzihead.dynamicsurroundings.data.BiomeRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.BlockRegistry;
import com.mrfuzzihead.dynamicsurroundings.data.DimensionEffectData;

public final class CommandRain extends CommandBase {

    private static final List<String> ALIAS = ImmutableList.<String>builder()
        .add("r", "br")
        .build();
    private static final DecimalFormat FORMATTER = new DecimalFormat("0.0");

    public static String statusOutput(World world, DimensionEffectData data) {
        StringBuilder builder = new StringBuilder();
        float minutes = world.getWorldInfo()
            .getRainTime() / 20.0F
            / 60.0F;
        builder.append(data.toString());
        builder.append("; isRaining: ")
            .append(Boolean.toString(world.isRaining()));
        builder.append("; isSurface: ")
            .append(Boolean.toString(world.provider.isSurfaceWorld()));
        builder.append("; strength: ")
            .append(FORMATTER.format(world.getRainStrength(1.0F) * 100.0F));
        builder.append("; timer: ")
            .append(FORMATTER.format(minutes))
            .append(" minutes");
        return builder.toString();
    }

    public int getRequiredPermissionLevel() {
        return 2;
    }

    public String getCommandName() {
        return "rain";
    }

    public List<String> getCommandAliases() {
        return ALIAS;
    }

    public String getCommandUsage(ICommandSender p_71518_1_) {
        return "/rain <status | reset | reload | 1-100 | <<setmax|setmin> 0-100>";
    }

    public void processCommand(ICommandSender sender, String[] parms) {
        World world = sender.getEntityWorld();
        if (world == null) {
            throw new CommandException("No world available for /rain", new Object[0]);
        }
        DimensionEffectData data = DimensionEffectData.get(world);
        if (parms.length == 1) {
            if ("status".compareToIgnoreCase(parms[0]) == 0) {
                sender.addChatMessage(new ChatComponentText(statusOutput(world, data)));
            } else if ("reset".compareToIgnoreCase(parms[0]) == 0) {
                world.provider.resetRainAndThunder();
                sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("msg.RainReset")));
            } else if ("reload".compareToIgnoreCase(parms[0]) == 0) {
                BiomeRegistry.initialize();
                BlockRegistry.initialize();
                sender.addChatMessage(new ChatComponentText(StatCollector.translateToLocal("msg.BiomeReload")));
            } else {
                double d = parseDoubleBounded(sender, parms[0], 0.0, 100.0) / 100.0;
                data.setRainIntensity((float) d);
                sender.addChatMessage(
                    new ChatComponentText(
                        StatCollector.translateToLocalFormatted(
                            "msg.RainIntensitySet",
                            new Object[] { FORMATTER.format(data.getRainIntensity() * 100.0F) })));
            }
        } else if (parms.length == 2) {
            if ("setmin".compareToIgnoreCase(parms[0]) == 0) {
                double d = parseDoubleBounded(sender, parms[1], 0.0, 100.0) / 100.0;
                data.setMinRainIntensity((float) d);
                sender.addChatMessage(
                    new ChatComponentText(
                        StatCollector.translateToLocalFormatted(
                            "msg.MinRainIntensitySet",
                            new Object[] { FORMATTER.format(data.getMinRainIntensity() * 100.0F) })));
            } else {
                if ("setmax".compareToIgnoreCase(parms[0]) != 0) {
                    throw new CommandException(this.getCommandUsage(sender), new Object[0]);
                }

                double d = parseDoubleBounded(sender, parms[1], 0.0, 100.0) / 100.0;
                data.setMaxRainIntensity((float) d);
                sender.addChatMessage(
                    new ChatComponentText(
                        StatCollector.translateToLocalFormatted(
                            "msg.MaxRainIntensitySet",
                            new Object[] { FORMATTER.format(data.getMaxRainIntensity() * 100.0F) })));
            }
        } else {
            sender.addChatMessage(new ChatComponentText(this.getCommandUsage(sender)));
        }
    }
}
