package backend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileManager {

    private static FileManager instance;
    private ArrayList<DataFile> files;

    public FileManager() {
        this.files = new ArrayList<>();
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
     * @param threadAmount the amount of threads to process the files
     * @param maxFileAmountForCmd the amount of files for each process
     */
    public void setTags(int threadAmount, int maxFileAmountForCmd) {
        List<DataFile> files = getAllFiles();
        List<List<Object>> listArr = getSublists(files, threadAmount);

        for(int i = 0; i < listArr.size(); i++) {
            List li = listArr.get(i);
            if(li.size() > 0) {
                Thread t = new Thread(() -> {
                    String cmd = Constants.getResourcePath(getClass(), "exiftool", "exiftool.exe");
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
                        String runCmd = cmd;
                        for(int j = 0; j < filePaths.size(); j++) {
                            runCmd += filePaths.get(j);
                            //amount files for each command
                            if((j != 0  && j % maxFileAmountForCmd == 0) || j == filePaths.size()-1) { //MAYBE EDIT THIS VALUE
                                System.out.println(runCmd);
                                try {
                                    Process p = Runtime.getRuntime().exec(runCmd);
//                                    System.out.println("waiting for ...");
                                    p.waitFor();
                                    String res = new String(p.getInputStream().readAllBytes());
                                    System.err.println(new String(p.getErrorStream().readAllBytes()));
                                    updateFiles(res);
                                    runCmd = cmd;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
                t.start();
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
    private void updateFiles(String res) {
        res = res.trim();
        String[] lines = res.split("\n");
        for(int i = 0; i < lines.length-2; i+=2) {
            String name = lines[i].split(": ")[1].strip();
            String dir = lines[i+1].split(": ")[1].strip();
            //category for videos, subject for images
            if(lines[i+2] != null && (lines[i+2].startsWith("Category") || lines[i+2].startsWith("Subject"))) {
                String[] categories = lines[i+2].split(":")[1].strip().split(", ");
                String path = (dir+"/"+name).replace("/", "\\");
                DataFile file = findFileByPath(path);
                if(file != null){
                    file.getTags().clear();
                    for(String tag: categories) {
                        file.addTag(tag);
                    }
                    file.setTagsLoaded(true);
                }
                i++;
            }
        }
    }

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

        for(List l: all) {
            System.out.print(l.size()+"[");
            for(Object df: l){
                System.out.print(((DataFile)df).getName() +", ");
            }
            System.out.print("]");
            System.out.println();
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
}
