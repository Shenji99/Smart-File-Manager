package backend;

import backend.data.DataFile;
import backend.exceptions.InvalidFileNameException;
import backend.exceptions.InvalidNameException;
import backend.exceptions.UnexpectedErrorException;
import backend.tasks.Callback;
import javafx.scene.image.Image;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileManager {

    private static final int THUMBNAIL_THREAD_AMOUNT = 10;
    private static final int TAG_THREAD_AMOUNT = 10;
    private static final int EXIF_MAX_FILES_FOR_CMD = 20;

    private static FileManager instance;
    private final ArrayList<DataFile> files;

    private boolean loadThumbnails;
    private boolean loadTags;
    private boolean loadResolutions;
    private SearchOption searchOption;

    private SortedSet<String> presetTags;
    private ArrayList<TagObserver> tagObservers;

    private ThreadPoolExecutor executor;

    public FileManager() {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        this.files = new ArrayList<>();
        this.tagObservers = new ArrayList<>();
        this.presetTags = new TreeSet<>(Comparator.comparing(String::toLowerCase));
        this.loadThumbnails = true;
        this.loadTags = true;
        this.loadResolutions = true;
        searchOption = SearchOption.Name; //default
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

    public void sort(List<DataFile> files, SearchOption searchOption) {
        boolean unsorted = true;
        while (unsorted) {
            unsorted = false;
            for (int i = 0; i < files.size() - 1; i++) {
                boolean comparision = false;
                DataFile f1 = files.get(i);
                DataFile f2 = files.get(i+1);
                switch(searchOption){
                    case Size: comparision = f1.getSize() > f2.getSize(); break;
                    case Type: comparision = f1.getType().compareTo(f2.getType()) > 0; break;
                    case Name: comparision = f1.getName().compareTo(f2.getName()) > 0; break;
                    case Tags: comparision = f1.getTags().size() < f2.getTags().size(); break;
                    case ChangeDate: comparision = f1.getChangeDate().compareTo(f2.getChangeDate()) >0 ; break;
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

    public void loadThumbnailsInThread(List files, Callback callback) {
        this.loadThumbnails = true;
        List dataFiles = getDataFiles(files);
        List<List<Object>> sublists = getSublists(dataFiles, THUMBNAIL_THREAD_AMOUNT);
        AtomicInteger finishedThreads = new AtomicInteger(0);

        for(List<Object> list: sublists) {
            if(!loadThumbnails){
                break;
            }
            new Thread(() -> {
                for(Object o: list){
                    if(loadThumbnails){
                        try {
                            String path = createThumbnailPath((DataFile) o);
                            createThumbnail((DataFile) o, path);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else {
                        break;
                    }
                }
                //if all threads are finished run the callback method
                int val = finishedThreads.incrementAndGet();
                if(val == sublists.size()){
                    //System.out.println("finished loading thumbnails");
                    callback.run();
                }
            }).start();
        }
    }

    public void loadResolutionsInThread(List files, Callback callback) {
        this.loadResolutions = true;
        List dataFiles = getDataFiles(files);
        List<List<Object>> sublists = getSublists(dataFiles, THUMBNAIL_THREAD_AMOUNT);
        AtomicInteger finishedThreads = new AtomicInteger(0);
        for(List<Object> list: sublists) {
            if(!loadResolutions){
                break;
            }
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
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else {
                        break;
                    }
                }
                //if all threads are finished run the callback method
                int val = finishedThreads.incrementAndGet();
                if(val == sublists.size()){
                    //System.out.println("finished loading resolutions");
                    callback.run();
                }
            }).start();
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
     * After all Threads are finished the callback method gets run
     */
    public void loadTagsInThread(List files, Callback callback) {
        this.loadTags = true;
        List dataFiles = getDataFiles(files);
        List<List<Object>> listArr = getSublists(dataFiles, TAG_THREAD_AMOUNT);
        AtomicInteger finishedThreads = new AtomicInteger(0);

        for(int i = 0; i < listArr.size(); i++) {
            if(!loadTags){
                break;
            }
            List li = listArr.get(i);
            if(li.size() > 0) {
                new Thread(() -> {
                    String cmd = getResourcePath(getClass(), "exiftool", "exiftool.exe");
                    cmd += " -L -S -m -q -fast2 -fileName -directory -category ";

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
                    //if all threads are finished run the callback method
                    int val = finishedThreads.incrementAndGet();
                    if(val == listArr.size()){
                        //System.out.println("finished loading tags");
                        callback.run();
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
    public void updateFiles(String res) throws UnexpectedErrorException, InvalidNameException {
        res = res.trim();
        if(res.isEmpty()){
            throw new UnexpectedErrorException("Tags konnten nicht geladen werden\n(Vielleicht ungültiger Dateiname?)");
        }
        String[] lines = res.split("\n");
        for(int i = 0; i < lines.length; i+=2) {
            String name = lines[i].split(": ")[1].strip();
            String dir = lines[i+1].split(": ")[1].strip();
            String path = (dir+"/"+name).replace("/", "\\");
            DataFile file = findFileByPath(path);
            //category for videos, subject for images
            if(i+2 <= lines.length-1){
                if(lines[i+2] != null && (lines[i+2].startsWith("Category") || lines[i+2].startsWith("Subject"))) {
                    String[] categories = lines[i+2].split(":")[1].strip().split(",");
                    if(file != null){
                        file.getTags().clear();
                        for(String tag: categories) {
                            file.addTag(tag.trim());
                        }
                    }
                    i++;
                }
            }
            if(file != null) {
                file.setTagsLoaded(true);
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
        all.removeIf(e -> e.isEmpty());
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

    public List<DataFile> searchFiles(List<DataFile> files, String text) {

        text = text.trim();
        if(searchOption == null){
            return null;
        }

        Set<String> words = new HashSet<>();
        for(String s: text.replace(",", " ").split(" ")) {
            s = s.trim();
            if(!s.isEmpty()){
                words.add(s);
            }
        }

        switch (searchOption) {
            case Name: return searchByName(files, words);
            case Path: return searchByPath(files, words);
            case Tags: return searchByTags(files, words);
            default: return null;
        }
    }

    private List<DataFile> searchByTags(List<DataFile> files, Set<String> words) {
        HashSet<DataFile> foundFiles = new HashSet<>();
        for (DataFile file : files) {
            childLoop:
            for (DataFile child : file.getChildren()) {
                for (String word : words) {
                    if (!child.isInTags(word)) {
                        continue childLoop;
                    }
                }
                foundFiles.add(child);
            }
        }
        return new LinkedList<>(foundFiles);
    }

    private List<DataFile> searchByPath(List<DataFile> files, Set<String> words) {
        HashSet<DataFile> foundFiles = new HashSet<>();
        for (DataFile file : files) {
            childLoop:
            for (DataFile child : file.getChildren()) {
                for (String word : words) {
                    if (!(child.getPath().contains(word) || child.getPath().toLowerCase().contains(word.toLowerCase()))) {
                        continue childLoop;
                    }
                }
                foundFiles.add(child);
            }
        }
        return new LinkedList<>(foundFiles);
    }

    private List<DataFile> searchByName(List<DataFile> files, Set<String> words){
        HashSet<DataFile> foundFiles = new HashSet<>();
        for(DataFile file: files){
            childLoop:
            for(DataFile child: file.getChildren()){
                for(String word: words) {
                    if(!(child.getName().contains(word) || child.getName().toLowerCase().contains(word.toLowerCase()))){
                        continue childLoop;
                    }
                }
                foundFiles.add(child);
            }
        }
        return new LinkedList<>(foundFiles);
    }

    private List getDataFiles(List<File> files) {
        ArrayList<DataFile> allFiles = new ArrayList();
        for(File f: files) {
            DataFile df = findFileByPath(f.getAbsolutePath());
            if(df != null) {
                allFiles.addAll(df.getChildren());
            }
        }
        return allFiles;
    }

    public void clearThumbnails(String path) {
        File dir = new File(path);
        for(File f: dir.listFiles()){
            f.delete();
        }
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

    public void stopAllBackgroundThreads(){
        this.loadTags = false;
        this.loadThumbnails = false;
        this.loadResolutions = false;
    }


    public SearchOption getSearchOption() {
        return searchOption;
    }

    public void setSearchOption(SearchOption searchOption) {
        this.searchOption = searchOption;
    }



    //utility methods
    public static Image createImageThumbnail(DataFile f, String outpath) throws IOException, InterruptedException {
        if(!new File(outpath).exists()) {
            String screenshotCmd = "ffmpeg -i \"" + f.getPath() + "\" -n -vf scale=320:-1 \"" + outpath + "\"";
            Process p2 = Runtime.getRuntime().exec(screenshotCmd);
            p2.waitFor();
        }
        return new Image(new File(outpath).toURI().toString());
    }

    public static Image createVideoThumbnail(DataFile f, String outpath) throws InterruptedException, IOException, UnexpectedErrorException {
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
            throw new UnexpectedErrorException("Fehler aufgetreten beim erstellen des Thumbnails");
        }
        if(!new File(outpath).exists()){
            String screenshotCmd = "ffmpeg -ss " + output + " -i \"" + f.getPath() + "\" -n -frames:v 1 -vf scale=320:-1 \"" + outpath+"\"";
            Process p2 = Runtime.getRuntime().exec(screenshotCmd);
            p2.waitFor();
        }
        return new Image(new File(outpath).toURI().toString());
    }

    public static Image createImageGifThumbnail(DataFile f) {
        return new Image(new File(f.getPath()).toURI().toString());
    }

    public static String createThumbnailPath(DataFile f){
        String newName = f.getPath().replace("\\", "+");
        newName = newName.replace("/", "+");
        newName = newName.replace(":", "+");

        return FileManager.getResourcePath(FileManager.class, "thumbnails", newName+".jpg");
    }

    public static Image createThumbnail(DataFile f, String outpath) throws IOException, InterruptedException, UnexpectedErrorException {
        String mimetype = FileManager.getDataFileMimeType(f);
        if(mimetype != null) {
            String[] mimetypeSplit = mimetype.split("/");
            String type = mimetypeSplit[0];
            String ending = mimetypeSplit[1];
            if(ending.equals("gif")){
                return createImageGifThumbnail(f);
            }else {
                switch (type) {
                    case "video": return createVideoThumbnail(f, outpath);
                    case "image": return createImageThumbnail(f, outpath);
                }
            }
        }
        return null;
    }

    public static String getDataFileMimeType(DataFile df) {
        switch (df.getType()) {
            //video
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
            case "mov":
                return "video/mov";
            //image
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
            case "txt":
                return "text/txt";
            default: return null;
        }
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

    public static String getResolution(DataFile df) throws IOException, InterruptedException {
        String cmd = "ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=s=x:p=0 ";
        cmd += "\""+df.getPath()+"\"";
        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();
        return new String(p.getInputStream().readAllBytes());
}

    public void loadPresetTags(File file) throws FileNotFoundException {
        Scanner reader = new Scanner(file);
        String data = "";
        while (reader.hasNextLine()) {
            data += reader.nextLine();
        }

        if(!data.isEmpty()){
            String[] tags = data.split(",");
            this.presetTags.addAll(Arrays.asList(tags));
            notifyTagObservers();
        }
    }

    private void notifyTagObservers() {
        for(TagObserver tagObserver: this.tagObservers){
            tagObserver.notify(this.presetTags);
        }
    }

    public void addTagObserver(TagObserver o){
        this.tagObservers.add(o);
    }

    public void removeTagObserver(TagObserver o){
        this.tagObservers.remove(o);
    }

    public boolean addTagToPreset(String tagText) {
        if(!presetTags.contains(tagText)){
            presetTags.add(tagText);
            notifyTagObservers();
            return true;
        }
        return false;
    }

    public void addTagToFile(DataFile df, String tag, Callback callback) throws InvalidNameException {
        if(tag != null && df != null){
            tag = tag.trim();
            if(!tag.isEmpty()){
                df.addTag(tag);
                FileManager.getInstance().saveFileTags(df, callback);
            }
        }
    }

    private void saveFileTags(DataFile df, Callback callback) {
        executor.submit(() -> {
            try {
                if (df.getTags().size() > 0) {

                    String tags = "";
                    for (int i = 0; i < df.getTags().size(); i++) {
                        String tag = df.getTags().get(i);
                        if ((!tag.contains(" ") || containsSpecialChar(tag))) {
                            tags += tag + ",";
                        }
                    }

                    String cmd = getResourcePath(getClass(), "exiftool", "exiftool.exe");
                    cmd += " -overwrite_original -category=";

                    if(!tags.isEmpty()){
                        String firstCmd = cmd + tags.substring(0, tags.length() - 1) + " \"" + df.getPath()+"\"";
                        Process p1 = Runtime.getRuntime().exec(firstCmd);
                        p1.waitFor();
                        callback.run();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static boolean containsSpecialChar(String s) {
        return Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE).matcher(s).find();
    }

    public void deleteAllTags(DataFile df, Callback callback) {
        executor.submit(() -> {
            df.removeAllTags();
            String cmd = getResourcePath(getClass(), "exiftool", "exiftool.exe");
                cmd += " -overwrite_original -category= \""+df.getPath()+"\"";
            try {
                Process p = Runtime.getRuntime().exec(cmd);
                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
            callback.run();
        });
    }
}
