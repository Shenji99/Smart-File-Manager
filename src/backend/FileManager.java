package backend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

    public void sort(List<DataFile> files, String fieldName) throws NoSuchFieldException {
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

    //MAYBE DO RECURSIVELY WITH -r
    /*public void setTags() {
        List<DataFile> files = getAllFiles();
        int size = 1;
        List<List<Object>> listArr = getSublists(files, size);

        for(int i = 0; i < listArr.size(); i++) {
            List li = listArr.get(i);
            if(li.size() > 0) {
                Thread t = new Thread(() -> {
                    String cmd = getClass().getResource("/exiftool").getPath() + "/exiftool.exe";
                    cmd = cmd
                        .replace("/", "\\")
                        .substring(1);
                    cmd += " -S -m -q -fast2 -fileName -directory -category ";

                    boolean addedFile = false;
                    LinkedList<StringBuilder > filePathLists = new LinkedList<>();
                    StringBuilder fileListString = new StringBuilder();
                    filePathLists.add(fileListString);

                    for (Object o : li) {
                        DataFile file = (DataFile) o;
                        if(!file.getType().isEmpty()) {
                            if(fileListString.length() > 100000) {
                                fileListString = new StringBuilder();
                                filePathLists.add(fileListString);
                            }
                            fileListString.append("\"").append(file.getPath()).append("\" ");
                            addedFile = true;
                        }
                    }

                    if(addedFile) {
                        for(StringBuilder s: filePathLists) {
                            String runCmd = cmd + s.toString();
                            System.out.println(runCmd);
                            try {
                                Process p = Runtime.getRuntime().exec(runCmd);
                                p.waitFor();
                                String res = new String(p.getInputStream().readAllBytes());
                                System.out.println(new String(p.getErrorStream().readAllBytes()));
                                System.out.println(res);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                t.start();
            }
        }
    }*/

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
