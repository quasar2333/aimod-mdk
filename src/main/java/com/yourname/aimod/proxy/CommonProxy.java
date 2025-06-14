package com.yourname.aimod.proxy;

import com.yourname.aimod.ModConfig;
import com.yourname.aimod.event.ServerChatEventHandler;
import com.yourname.aimod.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.loadConfig(event.getSuggestedConfigurationFile());
        PacketHandler.init();
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new ServerChatEventHandler());
    }

    public void postInit(FMLPostInitializationEvent event) {

    }

    // 客户端用来检查自己是否在私聊模式，服务端调用返回false
    public boolean isClientInPrivateMode() {
        return false;
    }
    // 客户端用来设置自己是否在私聊模式，服务端调用为空
    public void setClientPrivateMode(boolean active){
        // Only on client
    }
}