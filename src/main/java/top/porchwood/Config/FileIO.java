package top.porchwood.Config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import top.porchwood.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Date;

public class FileIO {
    public static Logger logger = Logger.getLogger();

    /**
     * 保存配置文件
     *
     * @param path   配置文件的路径地址
     * @param config 配置文件对象
     */
    public static void SaveConfig(String path, Object config) {
        try {
            FileWriter fw = new FileWriter(path);
            fw.write(new GsonBuilder().setPrettyPrinting().create().toJson(config));
            fw.flush();
            fw.close();
            logger.info("&2Succeed to create config");
        } catch (IOException e) {
            logger.error(e);
            logger.error("Created config throwing an error. Failed to creating the config file.");
        }
    }

    /**
     * 读取配置文件
     *
     * @param type 配置文件的对象类型，为&lt;T>
     * @param path 配置文件的路径，可用Config.getFilePath()获取
     * @param <T>  配置文件的对象类型
     * @return 配置文件
     */
    public static <T> T LoadConfig(Type type, String path) {
        try {
            logger.info("Loading config file at \"&n&1" + path + "&r\"");
            StringBuilder s = new StringBuilder();
            FileReader fr = new FileReader(path);
            char[] cs = null;
            do {
                if (cs != null) s.append(cs);
                cs = new char[1];
            }
            while (fr.read(cs) != -1);
            fr.close();
            //logger.info(s.toString());
            T obj = new Gson().fromJson(s.toString(), type);
            logger.info("Succeed to load config file");
            return obj;
        } catch (JsonSyntaxException e) {
            logger.error(e);
            logger.error("Final Error: Server config has a json error.");
            return null;
        } catch (FileNotFoundException e) {
            logger.error(e);
            logger.error("Error: Server config not found");
            return null;
        } catch (IOException e) {
            logger.error(e);
            logger.error("Final Error: Server config has a I/O error.");
            return null;
        }
    }

    /**
     * 检查一个配置文件对象是否存在，如不存在将返回新的，并且复写掉原有配置文件
     *
     * @param object    需要被检查的对象
     * @param path      配置文件的地址
     * @param newObject 你需要创建一个新的这个对象并且输入这里
     * @param <T>       被检查对象的类型
     * @return 刚刚创建的新对象或输入的旧对象
     */
    public static <T> T CheckObject(T object, String path, T newObject) {
        if (object == null) {
            logger.info("Config object not found, is it has an error? Will using the new config obj and save it");
            File f = new File(path);
            File parentFold = new File(f.getParent());
            if (!parentFold.isDirectory()) {
                parentFold.mkdir();
            }
            if (f.isFile()) {
                String p = f.getAbsolutePath() + "." + new Date().toString().replace(":", "-") + ".bak";
                if (f.renameTo(new File(p))) {
                    logger.info("&2Succeed to backup the old config file at \"&n&1" + f + "\"&r&2");
                } else {
                    logger.warn("Failed to backup the old config file at \"&n&1" + f + "\"&r, the old file will losing");
                }
            }
            SaveConfig(path, newObject);
            return newObject;
        } else {
            logger.info("&2Testing passed the config file at \"&n&1" + path + "&r&2\"");
            return object;
        }
    }

    //public static void CheckDataFold() {
    //    File f = null;
    //    try {
    //        if (!new File(ServerConfig.getWorkPath()).exists()) {
    //            String filename = new Date().toString().replace(":", "-").replace(" ","_");
    //            InputStream is = FileIO.class.getResourceAsStream("/resources.zip");// 流式读取jar包内文件，使用classpath
    //            f = File.createTempFile(filename, ".zip");// 指定输出文件
    //            //File jar = new File(getProgramJarFilePath());
    //            //ZipUtil.unpackEntry(jar, "resources.zip", f);
    //            f.createNewFile();
    //            OutputStream os = new FileOutputStream(f);// 创建输出流
    //            int index = 0;// 当前读取的位数
    //            byte[] bytes = new byte[1024];// 指定每次读取的位数，这里以1024为例
    //            // 开始读取文件，一遍读取一边输出文件。每次读取1024的长度并储存于bytes这个数组中，然后写入至目标文件
    //            while ((index = is.read(bytes)) != -1) {
    //                os.write(bytes, 0, index);// 输出文件，write()方法参数分别为：写入的数据、开始写入的位置（0为从开头写入），写入长度
    //            }
    //            // 关闭流并保存文件
    //            os.flush();
    //            os.close();
    //            is.close();
    //            File forder = new File(ServerConfig.getWorkPath());
    //            forder.mkdir();
    //            ZipUtil.unpack(f, forder);
    //        }
    //
    //    } catch (IOException e) {
    //        logger.error(e);
    //    } finally {
    //        if (f != null && f.exists()) {
    //            try {
    //                f.delete();
    //            } catch (Exception ignored) {
    //            }
    //        }
    //    }
    //}

    public static String getProgramJarFilePath() {
        String jarWholePath = FileIO.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        try {
            jarWholePath = java.net.URLDecoder.decode(jarWholePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println(e.toString());
        }
        return new File(jarWholePath).getParentFile().getAbsolutePath();
    }
}
