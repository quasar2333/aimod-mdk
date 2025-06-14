package com.yourname.aimod.event;

import com.yourname.aimod.ModConfig;
import com.yourname.aimod.ai.ChatStateManager;
import com.yourname.aimod.ai.ModelProfile;
import com.yourname.aimod.network.MessageTogglePrivate;
import com.yourname.aimod.network.PacketHandler;
import com.yourname.aimod.util.ChatUtil;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.UUID;

public class ServerChatEventHandler {

    private final ChatStateManager stateManager = ChatStateManager.INSTANCE;

    // onServerChat 方法保持不变
    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        EntityPlayerMP player = event.getPlayer();
        String message = event.getMessage();
        UUID playerUUID = player.getUniqueID();
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        if (message.startsWith("/")) {
            return;
        }

        ModelProfile privateModel = stateManager.getPrivateSessionModel(playerUUID);
        if (privateModel != null) {
            event.setCanceled(true);
            ChatUtil.sendPrivateUserEcho(player, message);

            stateManager.processUserMessageAndTriggerAI(playerUUID.toString(), privateModel, player, message, response -> {
                if (response.isSuccess()) {
                    ChatUtil.sendPrivateAiMessage(player, response.getContent());
                    ChatUtil.checkAndExecuteCommands(privateModel, response.getContent(), player.getName());
                } else {
                    ChatUtil.sendError(player, "Private AI Error: " + response.getError());
                }
            });
            return;
        }

        ModelProfile publicModel = stateManager.getActivePublicModel();
        if (publicModel != null) {
            if (message.trim().isEmpty()) return;

            String publicModelKey = stateManager.getActivePublicModelName();
            stateManager.processUserMessageAndTriggerAI(publicModelKey, publicModel, player, message, response -> {
                if (response.isSuccess()) {
                    ChatUtil.sendPublicMessage(server, publicModel.profileName, response.getContent());
                    ChatUtil.checkAndExecuteCommands(publicModel, response.getContent(), player.getName());
                } else {
                    ChatUtil.sendError(server, "Public AI (" + publicModel.profileName + ") Error: " + response.getError());
                }
            });
        }
    }


    // onPlayerLogin 方法保持不变
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            UUID playerUUID = player.getUniqueID();

            if (stateManager.isPlayerInPrivateSession(playerUUID)) {
                PacketHandler.INSTANCE.sendTo(new MessageTogglePrivate(true), player);
                ModelProfile profile = stateManager.getPrivateSessionModel(playerUUID);
                if (profile != null) {
                    ChatUtil.sendInfo(player, "Your private AI session with " + TextFormatting.GREEN + profile.profileName + TextFormatting.YELLOW + " has been restored.");
                }
            } else {
                PacketHandler.INSTANCE.sendTo(new MessageTogglePrivate(false), player);
            }
        }
    }

    /**
     * 调整：监听玩家退出事件。
     * 现在只在非持久化模式下清除会话。在持久化模式下，会话状态会由 FMLServerStoppingEvent 保存。
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!ModConfig.persistPrivateSessions) {
            if (event.player instanceof EntityPlayerMP) {
                // 在非持久化模式下，玩家登出时立即清除会话
                stateManager.endPrivateSession(event.player.getUniqueID());
            }
        }
    }
}