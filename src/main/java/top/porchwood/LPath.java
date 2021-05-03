package top.porchwood;

public class LPath {
    public static final boolean isWindows = System.getProperties().getProperty("os.name").startsWith("Windows");
    public static final String mark = isWindows ? "\\" : "/";
    public static String join(String... path){
        StringBuilder s = new StringBuilder();
        for(String nextpath : path) s.append(s.toString().endsWith(mark) ? nextpath : (mark + nextpath));
        return s.toString();
    }
}
