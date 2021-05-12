package top.porchwood.SimpleLogin;

import com.google.gson.Gson;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import top.porchwood.Config.FileIO;
import top.porchwood.LPath;
import top.porchwood.Logger;
import top.porchwood.SimpleLogin.Config.IConfig;
import top.porchwood.SimpleLogin.Config.MainConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

@OnlyIn(Dist.DEDICATED_SERVER)
@Mod("simple_login")
public class SimpleLogin {
    HashMap<String, UnloginPlayer> un_login_player = new HashMap<>();
    Logger logger = Logger.getLogger();
    public static final String configFoldPath = LPath.join(System.getProperty("user.dir"), "config", "SimpleLogin");
    public static final String accountFoldPath = LPath.join(configFoldPath, "accounts");
    HashMap<String, IConfig> configs = new HashMap<>();
    String prefix;
    private Timer makePlayerLoginTimer;

    public SimpleLogin() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 本插件(?)的主入口点
     *
     * @param event 服务器启动事件
     */
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        logger.info("&6开始载入SimpleLogin");
        logger.info("Path: " + configFoldPath);
        logger.info("OS: " + System.getProperties().getProperty("os.name"));
        logger.info("ModVersion: 1.0.0_beta");
        if (!new File(configFoldPath).exists()) new File(configFoldPath).mkdir();
        if (!new File(accountFoldPath).exists()) new File(accountFoldPath).mkdir();
        loadConfig();
        makePlayerLoginTimer = new Timer();
        makePlayerLoginTimer.schedule(MakePlayerLoginTimer(), 0, 1000);
    }

    private void loadConfig() {
        configs.clear();
        String mainConfigPath = LPath.join(configFoldPath, "config.json");
        configs.put("main", FileIO.CheckObject(FileIO.LoadConfig(MainConfig.class, mainConfigPath), mainConfigPath, new MainConfig()));
        prefix = ((MainConfig) configs.get("main")).prefix;
        logger.info("配置文件载入完成");
    }

    @SubscribeEvent
    public void onServerClose(FMLServerStoppingEvent event) {
        logger.info("正在关闭SimpleLogin");
        for (UnloginPlayer unplayer : un_login_player.values()) {
            logger.info("正在保存玩家" + unplayer.player.getName().getString() + "的数据");
            //unplayer.player.inventory.dropAll();
            unplayer.player.inventory.load(unplayer.inventory);
        }
        makePlayerLoginTimer.cancel();
        un_login_player.clear();
    }

    //@SubscribeEvent
    //public void onClientStarting(FMLClientSetupEvent event) {
    //    logger.info("&6客户端启动");
    //}

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        logger.info("&6玩家" + player.getName().getString() + "登入服务器");
        UnloginPlayer unloginPlayer;
        un_login_player.put(player.getStringUUID(),
                unloginPlayer = new UnloginPlayer(
                        event.getPlayer(),
                        new File(LPath.join(accountFoldPath, player.getStringUUID() + ".json")).exists(),
                        player.position(),
                        new Vector2f(player.getViewXRot(0), player.getViewYRot(0))));
        un_login_player.get(player.getStringUUID()).inventory = player.inventory.save(new ListNBT());
        player.inventory.clearContent();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!un_login_player.containsKey(player.getStringUUID())) {
                    timer.cancel();
                    return;
                }
                player.teleportTo(unloginPlayer.position.x, unloginPlayer.position.y, unloginPlayer.position.z);
                //player.inventory.dropAll();
            }
        }, 0, 50);
    }

    @SubscribeEvent
    public void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerEntity player = event.getPlayer();
        if (un_login_player.containsKey(event.getPlayer().getStringUUID())) {
            player.inventory.load(un_login_player.get(player.getStringUUID()).inventory);
            un_login_player.remove(player.getStringUUID());
        }
    }

    @SubscribeEvent
    public void onPlyerClick(PlayerInteractEvent event) {
        if (un_login_player.containsKey(event.getPlayer().getStringUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerOpenBox(PlayerContainerEvent.Open event) {
        if (un_login_player.containsKey(event.getPlayer().getStringUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (un_login_player.containsKey(event.getPlayer().getStringUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerItemPickup(EntityItemPickupEvent event) {
        if (un_login_player.containsKey(event.getPlayer().getStringUUID())) event.setCanceled(true);
    }

    //@SubscribeEvent
    //public void onPlayerUseBackpack()
    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        if (un_login_player.containsKey(event.getPlayer().getStringUUID())) {
            event.setCanceled(true);
            Str.sendToPlayer(event.getPlayer(), prefix + "&c你必须在登录后才能聊天");
        }
    }

    @SubscribeEvent
    public void onPlayerRunCommand(CommandEvent event) {
        Entity entity = event.getParseResults().getContext().getSource().getEntity();
        if (!(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        String raw_command = event.getParseResults().getReader().getString();
        String[] command = raw_command.substring(1).split(" ");
        if (un_login_player.containsKey(player.getStringUUID())) {
            switch (command[0]) {
                case "login":
                case "l":
                    if (command.length != 2) {
                        Str.sendToPlayer(player, prefix + "&c错误的登陆格式，请输入 &b/login 密码 &c以登陆");
                        break;
                    }
                    playerTryLogin(player, command[1]);
                    break;
                case "register":
                case "reg":
                    if (command.length != 3) {
                        Str.sendToPlayer(player, prefix + "&c错误的注册格式，请输入 &b/reg 密码 密码 &c以注册");
                    } else {
                        if (!command[1].equals(command[2]))
                            Str.sendToPlayer(player, prefix + "&c前后密码不一致，请确认输入了相同的密码");
                        else if (command[1].length() < 4)
                            Str.sendToPlayer(player, prefix + "&c密码过短，至少为4位");
                        else if (!Pattern.matches("^[a-zA-Z0-9.,;:\\-+=_!@#$%^&*()~`|?<>\"]*$", command[1])) {
                            Str.sendToPlayer(player, prefix + "&c密码中含有非法字符，可用字符为:");
                            Str.sendToPlayer(player, prefix + "&b&na-z&r &b&nA-Z&r &b&n0-9 &b.,;:-+=_!@#$%^&*()~`|?<>\"");
                        } else playerRegsiter(player, command[1]);
                    }
                    break;
                default:
                    Str.sendToPlayer(player, prefix + "&c你需要登录后才能使用指令");
                    break;
            }
            event.setCanceled(true);
        } else switch (command[0]) {
            case "cpassword":
            case "changepassword":
                if (command.length != 4)
                    Str.sendToPlayer(player, prefix + "&c错误的命令格式，请输入 &b/cpassword 旧密码 新密码 新密码 &c以修改密码");
                else if (!command[2].equals(command[3]))
                    Str.sendToPlayer(player, prefix + "&c新密码前后不一致，请确认输入了相同的密码");
                else if (command[2].length() < 4)
                    Str.sendToPlayer(player, prefix + "&c密码过短，至少为4位");
                else if (!Pattern.matches("^[a-zA-Z0-9.,;:\\-+=_!@#$%^&*()~`|?<>\"]*$", command[2])) {
                    Str.sendToPlayer(player, prefix + "&c新密码中含有非法字符，可用字符为:");
                    Str.sendToPlayer(player, prefix + "&b&na-z&r &b&nA-Z&r &b&n0-9 &b.,;:-+=_!@#$%^&*()~`|?<>\"");
                } else {
                    playerTryChangePassword(player, command[1], command[2]);
                }
                event.setCanceled(true);
                break;
            case "deleteaccount":
                if (command.length != 2) {
                    Str.sendToPlayer(player, prefix + "&c你确定要删除账户吗？这将&6&l&n不可逆&c！");
                    Str.sendToPlayer(player, prefix + "&f如要确认，请输入 /deleteaccount confirm");
                } else if (command[1].equals("confirm")) {
                    if (new File(LPath.join(accountFoldPath, player.getStringUUID() + ".json")).delete()) {
                        try {
                            player.getServer().getPlayerList().getPlayer(player.getUUID()).connection.disconnect(Str.getClientString("&a成功的删除了账户"));
                        } catch (NullPointerException exception) {
                            logger.error(exception);
                            Str.sendToPlayer(player, prefix + "&c内部错误，未能删除账户，请重试");
                        }
                    } else {
                        Str.sendToPlayer(player, prefix + "&c内部错误，未能删除账户，请重试");
                    }
                } else
                    Str.sendToPlayer(player, prefix + "&c错误的指令， 输入 &b/deleteaccount confirm 删除账户");
                event.setCanceled(true);
                break;
            case "simplelogin":
            case "sl":
                if (command.length == 1) Str.sendToPlayer(player, prefix + "&6输入 &b/sl help &6查看帮助");
                else switch (command[1]) {
                    case "reload":
                        if (Tool.OPS.isOP(player)) {
                            Str.sendToPlayer(player, prefix + "&6正在重载SimpleLogin的配置文件");
                            loadConfig();
                            Str.sendToPlayer(player, prefix + "&a重载成功！");
                        } else Str.sendToPlayer(player, prefix + "&c无权操作，请确认你是管理员？");
                        break;
                    case "help":
                        Tool.sendHelp(player);
                        break;
                    default:
                        Str.sendToPlayer(player, prefix + "&6输入 &b/sl help &6查看帮助");
                        break;
                }
                logger.info("玩家" + player.getName().getString() + "执行指令: " + raw_command);
                event.setCanceled(true);
        }
        //logger.debug(event.getParseResults().getReader().getString());
    }

    private void playerTryChangePassword(PlayerEntity player, String old, String now) {
        try {
            File file = new File(accountFoldPath, player.getStringUUID() + ".json");
            FileReader fr = null;
            fr = new FileReader(file);
            char[] bytes = new char[(int) file.length()];
            fr.read(bytes);
            fr.close();
            String s = new String(bytes);
            KeyValuePair oldFile = new Gson().fromJson(s, KeyValuePair.class);
            if (SHA256Utils.getSHA256(old).equals(oldFile.getValue())) {
                KeyValuePair newFile = new KeyValuePair(player.getName().getString(), SHA256Utils.getSHA256(now));
                String s1 = new Gson().toJson(newFile);
                FileWriter fw = new FileWriter(file);
                fw.write(s1);
                fw.close();
                Str.sendToPlayer(player, prefix + "&6密码修改成功，下次别忘用新密码登录哦～");
            } else {
                Str.sendToPlayer(player, prefix + "&c旧密码错误，请重新输入");
            }
        } catch (IOException e) {
            Str.sendToPlayer(player, prefix + "&c内部错误，请寻找管理员解决");
            logger.error(e);
        }
    }

    private void playerTryLogin(PlayerEntity player, String passward) {
        try {
            File file = new File(accountFoldPath, player.getStringUUID() + ".json");
            if(!file.exists()) {
                Str.sendToPlayer(player, prefix + "&c操作无效");
                return;
            }
            FileReader fr = new FileReader(file);
            char[] bytes = new char[(int) file.length()];
            fr.read(bytes);
            String s = new String(bytes);
            KeyValuePair kv = new Gson().fromJson(s, KeyValuePair.class);
            if (SHA256Utils.getSHA256(passward).equals(kv.getValue())) {
                //player.inventory.dropAll();
                player.inventory.load(un_login_player.get(player.getStringUUID()).inventory);
                un_login_player.remove(player.getStringUUID());
                Str.sendToPlayer(player, prefix + "&6登陆成功，请愉快的游戏吧～");
            } else {
                Str.sendToPlayer(player, prefix + "&c密码错误，请重新输入");
            }
        } catch (IOException e) {
            Str.sendToPlayer(player, prefix + "&c内部错误，请寻找管理员解决");
            logger.error(e);

        }
    }

    private void playerRegsiter(PlayerEntity player, String passward) {
        try {
            File file = new File(LPath.join(accountFoldPath, player.getStringUUID() + ".json"));
            if(file.exists()) {
                Str.sendToPlayer(player, prefix + "&c操作无效");
                return;
            }
            file.createNewFile();
            KeyValuePair kv = new KeyValuePair(player.getName().getString(), SHA256Utils.getSHA256(passward));
            String s = new Gson().toJson(kv);
            FileWriter fw = new FileWriter(file);
            fw.write(s);
            fw.close();
            player.inventory.dropAll();
            player.inventory.load(un_login_player.get(player.getStringUUID()).inventory);
            un_login_player.remove(player.getStringUUID());
            Str.sendToPlayer(player, prefix + "&6注册成功，请愉快的游戏吧～");
        } catch (IOException e) {
            Str.sendToPlayer(player, prefix + "&c内部错误，请寻找管理员解决");
            logger.warn(e);
        }
    }

    private TimerTask MakePlayerLoginTimer() {
        return new TimerTask() {
            @Override
            public void run() {
                int timeout = ((MainConfig) configs.get("main")).playerLoinTimeOut;
                for (String uuid : un_login_player.keySet()) {
                    UnloginPlayer unloginp = un_login_player.get(uuid);
                    unloginp.timeOut++;
                    PlayerEntity player = unloginp.player;
                    ServerPlayerEntity serPlayer = player.getServer() == null ? null : player.getServer().getPlayerList().getPlayer(player.getUUID());
                    if (serPlayer == null) continue;
                    if (unloginp.timeOut > timeout)
                        serPlayer.connection.disconnect(Str.getClientString("&7[&aChobits&6MC&7] &4给你的登陆时间已经过了"));
                    if (unloginp.hasAccount)
                        Str.sendToPlayer(player, prefix + "&6请输入 &b/login 密码 &6以登陆 &r &r &7(" + (timeout - unloginp.timeOut) + "s)");
                    else
                        Str.sendToPlayer(player, prefix + "&6请输入 &b/reg 密码 密码 &6以注册 &r &r &7(" + (timeout - unloginp.timeOut) + "s)");
                }
            }
        };
    }

    static class UnloginPlayer {
        public PlayerEntity player;
        public boolean hasAccount;
        public Vector3d position;
        public Vector2f rotation;
        public int timeOut = 0;
        public ListNBT inventory;

        public UnloginPlayer(PlayerEntity player, boolean hasAccount, Vector3d position, Vector2f view) {
            this.player = player;
            this.hasAccount = hasAccount;
            this.position = new Vector3d(position.x, position.y, position.z);
            this.rotation = view;
        }
    }
}
