package backend;

import backend.data.DataFile;
import frontend.controllers.FilePropertyController;
import javafx.scene.image.Image;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileManager {

    private static final int THUMBNAIL_THREAD_AMOUNT = 7;
    private static final int TAG_THREAD_AMOUNT = 7;
    private static final int EXIF_MAX_FILES_FOR_CMD = 20;

    private final LinkedList<FileObserver> observers;
    private static FileManager instance;
    private final ArrayList<DataFile> files;

    private boolean loadThumbnails;
    private boolean loadTags;
    private boolean loadResolutions;

    public FileManager() {
        this.files = new ArrayList<>();
        this.observers = new LinkedList<>();
        this.loadThumbnails = true;
        this.loadTags = true;
        this.loadResolutions = true;
    }

    public void notifyObservers(DataFile file) {
        for(FileObserver o: observers){
            o.onFileUpdate(file);
        }
    }

    public static FileManager getInstance() {
        if(instance == null) {
            instance = new FileManager();
        }
        return instance;
    }

    public ArrayList<DataFile> getFiles(String path) {
        try {
            path = path.replace("[", "");
            path = path.replace("]", "");
            for(DataFile file: this.files) {
                DataFile root = file.get(path);
                if(root != null) {
                    return root.getChildren();
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DataFile findFileByPath(String path) {
        for(DataFile file: this.files) {
            DataFile found = file.get(path);
            if(found != null){
                return found;
            }
        }
        return null;
    }

    public void sort(List<DataFile> files, String fieldName) {
        boolean unsorted = true;
        while (unsorted) {
            unsorted = false;
            for (int i = 0; i < files.size() - 1; i++) {
                boolean comparision = false;
                DataFile f1 = files.get(i);
                DataFile f2 = files.get(i+1);
                switch(fieldName){
                    case "size": comparision = f1.getSize() > f2.getSize(); break;
                    case "type": comparision = f1.getType().compareTo(f2.getType()) > 0; break;
                    case "name": comparision = f1.getName().compareTo(f2.getName()) > 0; break;
                    case "changeDate": comparision = f1.getChangeDate().compareTo(f2.getChangeDate()) >0 ; break;
                }
                if (comparision) {
                    DataFile temp = files.get(i);
                    files.set(i, files.get(i + 1));
                    files.set(i + 1, temp);
                    unsorted = true;
                }
            }
        }
    }

    public List<DataFile> getRootFiles() {
        return this.files;
    }

    public List<DataFile> getAllFiles() {
        ArrayList<DataFile> allFiles = new ArrayList<>();
        for(DataFile file: this.files) {
            allFiles.addAll(file.getChildren());
        }
        return allFiles;
    }

    public void addChildren(List<File> files) throws IOException {
        for(File f: files) {
            this.addChild(f);
        }
    }


    public void addChild(File file) throws IOException {
        DataFile foundFile = findFileByPath(file.getAbsolutePath());
        if(foundFile == null){
            this.files.add(new DataFile(null, file));
        }else {
            File[] subFiles = file.listFiles();
            if(subFiles != null){
                for(File f: subFiles) {
                    if(findFileByPath(f.getAbsolutePath()) == null) {
                        this.files.add(new DataFile(null, f));
                    }
                }
            }
        }
    }

    /**
     * this method splits all files into a list of sublists with equally distributed files
     * for example: [1,2,3,4,5,6,7] -> [1,2], [3,4], [5,6], [7]
     * each list gets processed by a separate thread
     * each thread builds the exiftool command. Each command contains "maxFileAmountForCmd" files
     * (to not reach maximum cmd line length)
     * It is important to process as many files at a time as possible to reduce computational resources. (opening exiftool is expensive)
     * The threads then process all the files and extract the tags (Category or Subject) and adds them to the files tag field
     */
    public void setTags() {
        this.loadTags = true;
        List<DataFile> files = getAllFiles();
        List<List<Object>> listArr = getSublists(files, TAG_THREAD_AMOUNT);

        for(int i = 0; i < listArr.size(); i++) {
            List li = listArr.get(i);
            if(li.size() > 0) {
                new Thread(() -> {
                    String cmd = getResourcePath(getClass(), "exiftool", "exiftool.exe");
                    cmd += " -L -S -m -q -fast2 -fileName -directory -category -XMP:Subject ";

                    boolean addedFile = false;
                    LinkedList<String> filePaths = new LinkedList<>();
                    for (Object o : li) {
                        DataFile file = (DataFile) o;
                        if(!file.getType().isEmpty()) {
                            filePaths.add("\""+file.getPath()+"\" ");
                            addedFile = true;
                        }
                    }

                    if(addedFile) {
                        StringBuilder runCmd = new StringBuilder(cmd);
                        for(int j = 0; j < filePaths.size(); j++) {
                            if(!loadTags){
                                break;
                            }
                            runCmd.append(filePaths.get(j));
                            //amount files for each command
                            if ((j != 0 && j % EXIF_MAX_FILES_FOR_CMD == 0) || j == filePaths.size() - 1) {
                                //System.out.println(runCmd);
                                try {
                                    if (!loadTags) {
                                        break;
                                    }
                                    Process p = Runtime.getRuntime().exec(runCmd.toString());
                                    //System.out.println("waiting for ...");
                                    p.waitFor();
                                    String res = new String(p.getInputStream().readAllBytes());
                                    String err = new String(p.getErrorStream().readAllBytes()).trim();
                                    if (!err.isEmpty()) {
                                        System.err.println(err);
                                    }
                                    //System.out.println("loading tags..");
                                    updateFiles(res);
                                    runCmd = new StringBuilder(cmd);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();
            }
        }
    }

    /**
     * gets the response of the exiftool containing the
     * name of the files, directory and the tags (if the file has)
     * this method then processes this string and adds the tags to the files
     *
     * Example string:
     *
     * FileName: file1.mp4
     * Directory: C:/path/to/file1/
     * Category: tag1, tag2, tag3
     * FileName: file2.mp4
     * Directory: C:/path/to/file2/
     * FileName: file3.jpg
     * Directory: C:/path/to/file3/
     * Subject: tag1, tag2
     *
     * @param res the string from exiftool
     */
    public void updateFiles(String res) {
        res = res.trim();
        String[] lines = res.split("\n");

        for(int i = 0; i < lines.length; i+=2) {
            String name = lines[i].split(": ")[1].strip();
            String dir = lines[i+1].split(": ")[1].strip();
            String path = (dir+"/"+name).replace("/", "\\");
            DataFile file = findFileByPath(path);
            //category for videos, subject for images
            if(i+2 <= lines.length-1){
                if(lines[i+2] != null && (lines[i+2].startsWith("Category") || lines[i+2].startsWith("Subject"))) {
                    String[] categories = lines[i+2].split(":")[1].strip().split(", ");
                    if(file != null){
                        file.getTags().clear();
                        for(String tag: categories) {
                            file.addTag(tag);
                        }
                    }
                    i++;
                }
            }
            if(file != null) {
                file.setTagsLoaded(true);
                notifyObservers(file);
            }
        }
    }

    /**
     * distrubutes the items in the given list equally into new sublists
     * the size parameter defines the number of sublists
     * @param list input list containing the elements
     * @param size number of sublists
     * @return new list containing the sublists
     */
    private List<List<Object>> getSublists(List list, int size) {
        LinkedList<List<Object>> all = new LinkedList<List<Object>>();

        for(int i = 0; i < size; i++){
            all.add(new LinkedList<>());
        }
        int n = 0;
        for(int i = 0; i < list.size(); i++) {
            all.get(n).add(list.get(i));
            n++;
            if(n > all.size()-1){
                n = 0;
            }
        }
        return all;
    }

    public void deleteFile(File f){
        if(f != null){
            DataFile file = findFileByPath(f.getAbsolutePath());
            if(file.getParent() == null){
                this.files.remove(file);
            }else {
                file.getParent().deleteChild(file);
            }
        }
    }

    public void deleteFile(String path) {
        deleteFile(new File(path));
    }

    public void deleteAllFiles() {
        this.files.clear();
    }

    public List<DataFile> searchFiles(String text) {
        ArrayList<DataFile> foundFiles = new ArrayList<>();
        for(DataFile file: this.files) {
            for(DataFile child: file.getChildren()){
                if(child.getName().contains(text) || child.getName().toLowerCase().contains(text.toLowerCase())){
                    foundFiles.add(child);
                }
            }
        }
        return foundFiles;
    }

    public static String getResourcePath(Class c, String foldername, String name){
        String path = c.getResource("/"+foldername).getPath() + "/"+name;
        path = path
            .replace("/", "\\")
            .substring(1);
        return path;
    }

    public static String getResourcePath(Class c, String foldername) {
        return getResourcePath(c, foldername, "");
    }

    public void loadThumbnailsInThread(FilePropertyController filePropertyController) {
        this.loadThumbnails = true;
        List<List<Object>> sublists = getSublists(getAllFiles(), THUMBNAIL_THREAD_AMOUNT);
        for(List<Object> list: sublists) {
            new Thread(() -> {
                for(Object o: list) {
                    //System.out.println("loading thumbnail..");
                    if(loadThumbnails){
                        try {
                            String path = filePropertyController.createThumbnailPath((DataFile) o);
                            filePropertyController.createThumbnail((DataFile) o, path);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else {
                        break;
                    }
                }
            }).start();
        }
    }

    public void loadResolutionsInThread() {
        this.loadResolutions = true;
        List<List<Object>> sublists = getSublists(getAllFiles(), THUMBNAIL_THREAD_AMOUNT);
        for(List<Object> list: sublists) {
            new Thread(() -> {
                for(Object o: list) {
                    //System.out.println("loading res..");
                    if(loadResolutions){
                        DataFile df = (DataFile) o;
                        try {
                            String res = getResolution(df);
                            if(!res.isEmpty()){
                                String[] res2 = res.split("x");
                                int width = Integer.parseInt(res2[0].trim());
                                int height = Integer.parseInt(res2[1].trim());
                                df.setWidth(width);
                                df.setHeight(height);
                                notifyObservers(df);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    public static String getResolution(DataFile df) throws IOException, InterruptedException {
        String cmd = "ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=s=x:p=0 ";
        cmd += "\""+df.getPath()+"\"";
        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();
        return new String(p.getInputStream().readAllBytes());
    }

    public void clearThumbnails(String path) {
        File dir = new File(path);
        for(File f: dir.listFiles()){
            f.delete();
        }
    }

    public Image createImageThumbnail(DataFile f, String outpath) throws IOException, InterruptedException {
        String screenshotCmd = "ffmpeg -i \"" + f.getPath() + "\" -vf scale=320:-1 \"" + outpath + "\"";
        Process p2 = Runtime.getRuntime().exec(screenshotCmd);
        p2.waitFor();
        return new Image(new File(outpath).toURI().toString());
    }

    public Image createVideoThumbnail(DataFile f, String outpath) throws InterruptedException, IOException {
        //get duration of video
        String ffprobeCmd = "ffprobe -i \"" + f.getPath() + "\" -show_entries format=duration -v quiet -of csv=\"p=0\"";
        Process p = Runtime.getRuntime().exec(ffprobeCmd);
        p.waitFor();

        //make screenshot and save it in folder
        String s = new String(p.getInputStream().readAllBytes());
        int output = 0;
        try{
            output = Integer.parseInt(s.split("\\.")[0]);
            output = output / 2;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("FEHLER AUFGETRETEN: "+s.split("\\.")[0]);
        }
        String screenshotCmd = "ffmpeg -ss " + output + " -i \"" + f.getPath() + "\" -frames:v 1 -vf scale=320:-1 \"" + outpath+"\"";
        Process p2 = Runtime.getRuntime().exec(screenshotCmd);
        p2.waitFor();
        return new Image(new File(outpath).toURI().toString());
    }

    public Image createImageGifThumbnail(DataFile f) {
        return new Image(new File(f.getPath()).toURI().toString());
    }


    public void showFileInExplorer(String path) {
        try {
            Runtime.getRuntime().exec("explorer.exe /select," + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openFile(String path){
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeObserver(FileObserver fo){
        this.observers.remove(fo);
    }

    public void addObserver(FileObserver fo){
        this.observers.add(fo);
    }

    public void stopAllBackgroundThreads(){
        this.loadTags = false;
        this.loadThumbnails = false;
        this.loadResolutions = false;
    }

    public static String getDataFileMimeType(DataFile df) {
        switch (df.getType()) {
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "ogg":
                return "video/ogg";
            case "wmv":
                return "video/wmv";
            case "avi":
                return "video/avi";
            case "jpg":
                return "image/jpg";
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            case "tiff":
                return "image/tiff";
            case "bmp":
                return "image/bmp";
            case "gif":
                return "image/gif";
            case "mov":
                return "image/mov";
            case "txt":
                return "text/txt";
            default: return null;
        }
    }

}
