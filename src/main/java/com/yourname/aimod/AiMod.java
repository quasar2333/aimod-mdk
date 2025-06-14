package com.yourname.aimod;

import com.yourname.aimod.ai.ChatStateManager;
import com.yourname.aimod.command.CommandAiChat;
import com.yourname.aimod.proxy.CommonProxy;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION, acceptedMinecraftVersions = Reference.ACCEPTED_VERSIONS, acceptableRemoteVersions = "*")
public class AiMod {

    @Instance
    public static AiMod instance;

    public static Logger logger;

    @SidedProxy(clientSide = Reference.CLIENT_PROXY_CLASS, serverSide = Reference.COMMON_PROXY_CLASS)
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        proxy.preInit(event);
        logger.info("AI Mod Pre-Initialization");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
        logger.info("AI Mod Initialization");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
        logger.info("AI Mod Post-Initialization");
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        // 注册指令
        event.registerServerCommand(new CommandAiChat());
        // 清理内存中的旧状态
        ChatStateManager.INSTANCE.clearAll();

        // 如果配置了持久化，则从文件加载会话
        if (ModConfig.persistPrivateSessions) {
            File saveFile = getSessionSaveFile(event.getServer());
            ChatStateManager.INSTANCE.loadSessions(saveFile);
        }
        logger.info("AI Mod ready on server.");
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        // 如果配置了持久化，则将当前会话保存到文件
        if (ModConfig.persistPrivateSessions) {
            // FIX: 使用 FMLCommonHandler 获取服务器实例
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                File saveFile = getSessionSaveFile(server);
                ChatStateManager.INSTANCE.saveSessions(saveFile);
            }
        }
        logger.info("AI Mod server stopping.");
    }

    /**
     * 获取会话存档文件的标准位置
     * @param server Minecraft 服务器实例
     * @return 指向 aimod_sessions.json 的文件对象
     */
    private File getSessionSaveFile(MinecraftServer server) {
        // 将存档文件放在世界目录的 'data' 文件夹下，这是标准的做法
        // 使用 server.getWorld(0) 获取主世界，从而得到存档处理器
        return new File(server.getWorld(0).getSaveHandler().getWorldDirectory(), "data/aimod_sessions.json");
    }
}