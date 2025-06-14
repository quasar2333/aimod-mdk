package com.yourname.aimod.command;

import com.yourname.aimod.AiMod;
import com.yourname.aimod.ModConfig;
import com.yourname.aimod.ai.ChatStateManager;
import com.yourname.aimod.ai.ModelProfile;
import com.yourname.aimod.network.MessageTogglePrivate;
import com.yourname.aimod.network.PacketHandler;
import com.yourname.aimod.util.ChatUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandAiChat extends CommandBase {
    private final ChatStateManager stateManager = ChatStateManager.INSTANCE;

    @Nonnull
    @Override
    public String getName() {
        return "aichat";
    }

    @Nonnull
    @Override
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/aichat public <ModelName> on\n" +
                "/aichat public off\n" +
                "/aichat private <ModelName> <PlayerSelector> on\n" +
                "/aichat private <PlayerSelector> off\n" +
                "/aichat reload\n" +
                "/aichat list";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // 需要OP权限
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                ModConfig.reloadConfig();
                stateManager.clearAll(); // 重载配置后清除所有状态
                ChatUtil.sendInfo(sender, "Config reloaded and all AI states cleared.");
                break;

            case "list":
                ChatUtil.sendInfo(sender, "Available Models:");
                ModConfig.modelProfiles.values().forEach(p ->
                        ChatUtil.sendInfo(sender, "- " + p.profileName + " (" + (p.isPublic ? "PUBLIC" : "PRIVATE") + ")")
                );
                String activePublic = stateManager.getActivePublicModelName();
                ChatUtil.sendInfo(sender, "Active Public: " + (activePublic == null ? "None" : activePublic));
                break;

            case "public":
                // /aichat public <ModelName> on  OR /aichat public off
                handlePublic(sender, args);
                break;

            case "private":
                // /aichat private <ModelName> <Selector> on OR /aichat private <Selector> off
                handlePrivate(server, sender, args);
                break;

            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private void handlePublic(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 2 && args[1].equalsIgnoreCase("off")) {
            String oldModel = stateManager.getActivePublicModelName();
            stateManager.setPublicModel(null);
            ChatUtil.sendInfo(sender, "Public AI chat disabled." + (oldModel != null ? " Model: " + oldModel : ""));
            return;
        }
        if (args.length == 3 && args[2].equalsIgnoreCase("on")) {
            String modelName = args[1].toLowerCase();
            ModelProfile profile = ModConfig.modelProfiles.get(modelName);
            if(profile == null){
                ChatUtil.sendError(sender, "Model '" + modelName + "' not found.");
                return;
            }
            if(!profile.isPublic){
                ChatUtil.sendError(sender, "Model '" + modelName + "' is not configured as public.");
                return;
            }
            if (stateManager.setPublicModel(modelName)) {
                ChatUtil.sendInfo(sender, "Public AI chat enabled with model: " + TextFormatting.GREEN + modelName);
            } else {
                ChatUtil.sendError(sender, "Failed to enable public AI with model: " + modelName + ". Check if model exists and is public.");
            }
            return;
        }
        throw new WrongUsageException("Usage: /aichat public <ModelName> on | /aichat public off");
    }

    private void handlePrivate(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        List<EntityPlayerMP> targets;
        // Case 1: /aichat private <Selector> off
        if (args.length == 3 && args[2].equalsIgnoreCase("off")) {
            targets = getPlayers(server, sender, args[1]);
            for (EntityPlayerMP target : targets) {
                if(stateManager.isPlayerInPrivateSession(target.getUniqueID())){
                    stateManager.endPrivateSession(target.getUniqueID());
                    PacketHandler.INSTANCE.sendTo(new MessageTogglePrivate(false), target); // 告知客户端关闭屏蔽
                    ChatUtil.sendInfo(target, "Your private AI chat session has been closed by admin.");
                    ChatUtil.sendInfo(sender, "Closed private AI session for " + target.getName());
                } else {
                    ChatUtil.sendInfo(sender,  target.getName() + " was not in a private session.");
                }
            }
            return;
        }
        // Case 2: /aichat private <ModelName> <Selector> on
        if (args.length == 4 && args[3].equalsIgnoreCase("on")) {
            String modelName = args[1].toLowerCase();
            ModelProfile profile = ModConfig.modelProfiles.get(modelName);
            if(profile == null){
                ChatUtil.sendError(sender, "Model '" + modelName + "' not found.");
                return;
            }
            if(profile.isPublic){
                ChatUtil.sendError(sender, "Model '" + modelName + "' is configured as public, cannot be used for private chat.");
                return;
            }

            targets = getPlayers(server, sender, args[2]);
            for (EntityPlayerMP target : targets) {
                if(stateManager.isPlayerInPrivateSession(target.getUniqueID())) {
                    ChatUtil.sendInfo(sender, target.getName() + " is already in a private session. Close it first.");
                    continue;
                }
                if(stateManager.startPrivateSession(target.getUniqueID(), modelName)){
                    PacketHandler.INSTANCE.sendTo(new MessageTogglePrivate(true), target); // 告知客户端开启屏蔽
                    ChatUtil.sendInfo(target, "You entered a private AI chat session with: "+ TextFormatting.GREEN + modelName + TextFormatting.YELLOW + ". Other players' chat is hidden. Your messages are only seen by AI.");
                    ChatUtil.sendInfo(sender, "Started private AI session for " + target.getName() + " with model " + modelName);
                } else {
                    ChatUtil.sendError(sender, "Failed to start private session for " + target.getName() + " with model " + modelName + ". Check logs.");
                }
            }
            return;
        }
        throw new WrongUsageException("Usage: /aichat private <ModelName> <Selector> on | /aichat private <Selector> off");
    }


    @Nonnull
    @Override
    public List<String> getTabCompletions(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "public", "private", "reload", "list");
        }
        String sub = args[0].toLowerCase();
        if(sub.equals("public")) {
            if (args.length == 2) {
                List<String> publicModels = ModConfig.modelProfiles.values().stream()
                        .filter(p -> p.isPublic)
                        .map(p -> p.profileName)
                        .collect(Collectors.toList());
                publicModels.add("off");
                return getListOfStringsMatchingLastWord(args, publicModels);
            }
            if(args.length == 3 && !args[1].equalsIgnoreCase("off")){
                return getListOfStringsMatchingLastWord(args, "on");
            }
        }
        if(sub.equals("private")) {
            if(args.length == 2){
                List<String> privateModels = ModConfig.modelProfiles.values().stream()
                        .filter(p -> !p.isPublic)
                        .map(p -> p.profileName)
                        .collect(Collectors.toList());
                // Also allow player names/selectors for the 'off' command form: /aichat private <selector> off
                List<String> allOptions = new ArrayList<>(privateModels);
                allOptions.addAll(Arrays.asList(server.getOnlinePlayerNames()));
                allOptions.add("@a"); allOptions.add("@p");
                return getListOfStringsMatchingLastWord(args, allOptions);
            }
            if(args.length == 3) {
                // check if arg[1] is a model or a player selector to decide if it's on or off command
                ModelProfile profile = ModConfig.modelProfiles.get(args[1].toLowerCase());
                boolean isModel = ( profile != null && !profile.isPublic);
                if(isModel) {
                    // form: /aichat private <model> <HERE> on
                    List<String> players = new ArrayList<>(Arrays.asList(server.getOnlinePlayerNames()));
                    players.add("@a"); players.add("@p");
                    return getListOfStringsMatchingLastWord(args, players);
                } else {
                    // form: /aichat private <selector> <HERE> off
                    return getListOfStringsMatchingLastWord(args, "off");
                }
            }
            if(args.length == 4){
                ModelProfile profile = ModConfig.modelProfiles.get(args[1].toLowerCase());
                boolean isModel = ( profile != null && !profile.isPublic);
                if(isModel) { // only show "on" if the second arg was a valid private model
                    return getListOfStringsMatchingLastWord(args, "on");
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        // Tell MC that player selectors can be here
        if(args.length > 0 && args[0].equalsIgnoreCase("private")){
            if(index == 1) return true; // for /aichat private <selector> off
            if(index == 2) return true; // for /aichat private <model> <selector> on
        }
        return false;
    }
}
 