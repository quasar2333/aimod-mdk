package com.yourname.aimod.network;

import com.yourname.aimod.AiMod;
import com.yourname.aimod.proxy.ClientProxy;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

// 服务器发送给客户端，告知其开启/关闭私聊屏蔽模式
public class MessageTogglePrivate implements IMessage {

    private boolean active;

    // 需要一个无参构造函数
    public MessageTogglePrivate() {}

    public MessageTogglePrivate(boolean active) {
        this.active = active;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.active = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.active);
    }

    // 消息处理器，内部静态类
    // 标记只在客户端运行
    public static class Handler implements IMessageHandler<MessageTogglePrivate, IMessage> {
        @Override
        // 这个方法在网络线程运行
        public IMessage onMessage(MessageTogglePrivate message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                // 调度到客户端主线程处理
                Minecraft.getMinecraft().addScheduledTask(() -> handleClient(message));
            }
            return null; // No response packet
        }

        @SideOnly(Side.CLIENT)
        // 这个方法在客户端主线程运行
        private void handleClient(MessageTogglePrivate message){
            // 通过代理设置客户端状态
            // 确保 proxy 是 ClientProxy 实例
            if(AiMod.proxy instanceof ClientProxy) {
                ((ClientProxy)AiMod.proxy).setClientPrivateMode(message.active);
                // AiMod.logger.info("Client received private mode toggle: " + message.active);
            }
        }
    }
}