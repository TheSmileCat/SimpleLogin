package top.porchwood.SimpleLogin;

import com.google.gson.Gson;
import net.minecraft.entity.player.PlayerEntity;
import top.porchwood.LPath;
import top.porchwood.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Tool {
    public static class OPS {
        public static Logger logger = Logger.getLogger();
        public String uuid;
        public String name;
        public int level;
        public boolean bypassesPlayerLimit;

        public static OPS[] getOPS() {
            String path = LPath.join(System.getProperty("user.dir"), "ops.json");
            StringBuilder sb = new StringBuilder();
            OPS[] ops = null;
            try {
                BufferedReader sr = new BufferedReader(new FileReader(path));
                String sbf;
                while ((sbf = sr.readLine()) != null) sb.append(sbf);
                ops = new Gson().fromJson(sb.toString(), OPS[].class);
                sr.close();
            } catch (IOException exception) {
                logger.error(exception);
            }
            return ops;
        }

        public static boolean isOP(PlayerEntity player) {
            for (OPS op : getOPS()) {
                if (op.uuid.equals(player.getStringUUID())) return true;
            }
            return false;
        }
    }

    public static void sendHelp(PlayerEntity player) {
        Str.sendToPlayer(player, "&3=============&aSimpleLogin&b帮助&3=============");
        Str.sendToPlayer(player, " &b/login&7, &b/l: &f登录");
        Str.sendToPlayer(player, " &b/register&7, &b/reg: &f注册");
        Str.sendToPlayer(player, " &b/changepassword&7, &b/cpassword: &f修改密码");
        Str.sendToPlayer(player, " &b/deleteaccount&7: &f删除账户");
        Str.sendToPlayer(player, " &b/sl help&7: &f显示帮助页面");
        if (OPS.isOP(player)) Str.sendToPlayer(player, " &b/sl reload&7: &f重载SimpleLogin配置文件");
        Str.sendToPlayer(player, "&3=============&aSimpleLogin&b帮助&3=============");
    }
}
