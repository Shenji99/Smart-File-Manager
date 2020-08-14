package backend;

public class Constants {

    public static final String[] INVALID_CHARACTERS = {"\\", "/", ":", "*", "?", "\"", "<", ">", "|"};

    public static final int MAX_FILENAME_LENGTH = 255;

    public static String getResourcePath(Class c, String foldername, String name){
        String path = c.getResource("/"+foldername).getPath() + "/"+name;
        path = path
            .replace("/", "\\")
            .substring(1);
        return path;
    }

}
