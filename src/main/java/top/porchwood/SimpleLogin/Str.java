package top.porchwood.SimpleLogin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;

public class Str {
    public static TranslationTextComponent getClientString(String message){
        return new TranslationTextComponent(message.replace("\\&","\u007f").replace("&","§").replace("\u007f","&"));
    }
    public static void sendToPlayer(PlayerEntity player, String message){
        sendToPlayer(player, getClientString(message));
    }
    public static void sendToPlayer(PlayerEntity player, TranslationTextComponent message){
        if(player.getServer() == null) return;
        player.sendMessage(message, player.getUUID());
    }
}
