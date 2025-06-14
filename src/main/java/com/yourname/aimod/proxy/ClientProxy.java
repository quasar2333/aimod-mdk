package com.yourname.aimod.proxy;

import com.yourname.aimod.event.ClientChatEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

// 标记只在客户端加载
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    // 标记玩家客户端当前是否处于私聊模式（用于屏蔽他人聊天）
    private boolean clientInPrivateMode = false;

    public static ClientProxy INSTANCE;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        INSTANCE = this; // Set instance for packet handler access
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        // 注册客户端独有的事件监听器
        MinecraftForge.EVENT_BUS.register(new ClientChatEventHandler());
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public boolean isClientInPrivateMode() {
        return clientInPrivateMode;
    }
    @Override
    public void setClientPrivateMode(boolean active){
        this.clientInPrivateMode = active;
        // net.minecraft.client.Minecraft.getMinecraft().player.sendMessage(new net.minecraft.util.text.TextComponentString("Client Private Mode: " + active));
    }
}