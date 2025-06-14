package com.yourname.aimod.network;

import com.yourname.aimod.Reference;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.CHANNEL_NAME);
    private static int packetId = 0;

    public static void init() {
        // 注册数据包，服务器发往客户端 (Side.CLIENT)
        INSTANCE.registerMessage(MessageTogglePrivate.Handler.class, MessageTogglePrivate.class, packetId++, Side.CLIENT);
        // 如果有客户端发往服务器的包，使用 Side.SERVER
    }
}