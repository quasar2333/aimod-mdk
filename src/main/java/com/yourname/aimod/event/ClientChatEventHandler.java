package com.yourname.aimod.event;

import com.yourname.aimod.AiMod;
import com.yourname.aimod.proxy.ClientProxy;
import com.yourname.aimod.util.ChatUtil;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.util.text.TextComponentString;

// 客户端事件处理，只在客户端加载和注册
@SideOnly(Side.CLIENT)
public class ClientChatEventHandler {

    @SubscribeEvent
    public void onClientChatReceived(ClientChatReceivedEvent event) {
        // 检查客户端代理，判断当前玩家是否处于私聊模式
        if (AiMod.proxy.isClientInPrivateMode()) {
            // 获取包含格式代码的消息文本
            String formattedText = event.getMessage().getFormattedText();
            // 如果消息不是以私聊标记开头的，就取消显示
            if (!formattedText.startsWith(ChatUtil.PRIVATE_MARKER)) {
                event.setCanceled(true);
            }
              /*
               // 可选: 如果你想移除标记再显示, 但可能丢失部分格式, 且需要小心处理
              else {
                  try {
                       String cleanText = formattedText.substring(ChatUtil.PRIVATE_MARKER.length());
                        event.setMessage(new TextComponentString(cleanText));
                  } catch (Exception e){
                      // fallback, keep original if error
                  }
              }
              */
        }
    }
}