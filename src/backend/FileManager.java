package backend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
                if(child.getName().contains(text)){
                    foundFiles.add(child);
                }
            }
        }
        return foundFiles;
    }
}
