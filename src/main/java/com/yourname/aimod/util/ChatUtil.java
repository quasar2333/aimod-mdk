package com.yourname.aimod.util;

import com.yourname.aimod.AiMod;
import com.yourname.aimod.ai.ModelProfile;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.List;
import java.util.Map;

public class ChatUtil {

    // 私聊消息标识符, 用于客户端识别并放行这些消息，同时屏蔽其他消息
    // 使用颜色代码作为一种隐藏标记
    public static final String PRIVATE_MARKER = TextFormatting.DARK_PURPLE.toString() + TextFormatting.OBFUSCATED.toString() + "P" + TextFormatting.RESET.toString();
    public static final String PUBLIC_PREFIX = TextFormatting.AQUA + "[AI] ";
    public static final String PRIVATE_AI_PREFIX = PRIVATE_MARKER + TextFormatting.LIGHT_PURPLE + "[Private AI] ";
    public static final String PRIVATE_USER_PREFIX = PRIVATE_MARKER + TextFormatting.GRAY + "[You] ";
    public static final String ERROR_PREFIX = TextFormatting.RED + "[AI Error] ";
    public static final String INFO_PREFIX = TextFormatting.YELLOW + "[AI Mod] ";

    public static void sendPublicMessage(MinecraftServer server, String modelName, String message) {
        if (server != null) {
            ITextComponent component = new TextComponentString(PUBLIC_PREFIX + TextFormatting.WHITE + "<"+modelName+"> " + message);
            server.getPlayerList().sendMessage(component);
        }
    }

    public static void sendPrivateAiMessage(EntityPlayerMP player, String message) {
        if (player != null) {
            player.sendMessage(new TextComponentString(PRIVATE_AI_PREFIX + TextFormatting.WHITE + message));
        }
    }
    // 私聊时，玩家自己消息的回显
    public static void sendPrivateUserEcho(EntityPlayerMP player, String message) {
        if (player != null) {
            player.sendMessage(new TextComponentString(PRIVATE_USER_PREFIX + TextFormatting.ITALIC + message));
        }
    }

    public static void sendError(ICommandSender sender, String message) {
        if(sender != null){
            sender.sendMessage(new TextComponentString(ERROR_PREFIX + message));
        }
        AiMod.logger.error(message);
    }
    public static void sendInfo(ICommandSender sender, String message) {
        if(sender != null){
            sender.sendMessage(new TextComponentString(INFO_PREFIX + message));
        }
    }

    // 检查关键词并执行指令
    public static void checkAndExecuteCommands(ModelProfile profile, String aiResponseText, String playerName) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if(server == null || profile == null || aiResponseText == null || aiResponseText.isEmpty() || profile.keywordActions.isEmpty()) {
            return;
        }
        String textLower = aiResponseText.toLowerCase();

        for(Map.Entry<String, List<String>> entry : profile.keywordActions.entrySet()) {
            String keyword = entry.getKey().toLowerCase();
            if(textLower.contains(keyword)) {
                AiMod.logger.info("Keyword '"+ keyword +"' detected for model '"+ profile.profileName +"'. Executing commands...");
                List<String> commands = entry.getValue();
                for(String command : commands) {
                    try {
                        // 替换指令中的 @p 为触发AI的玩家名字 (只替换精确的 @p)
                        String commandToExecute = command.replaceAll("\\@p\\b", playerName);
                        AiMod.logger.info("Executing: /" + commandToExecute);
                        // 以服务器控制台身份执行指令
                        server.getCommandManager().executeCommand(server, commandToExecute);
                    } catch (Exception e) {
                        AiMod.logger.error("Failed to execute command: " + command, e);
                    }
                }
            }
        }
    }
}