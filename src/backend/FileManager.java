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
        for(DataFile file: this.files) {
            System.out.print(file.getPath() + ", ");
        }
        System.out.println();
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

    public void sort(List<DataFile> files) {
        boolean unsorted = true;
        while (unsorted) {
            unsorted = false;
            for (int i = 0; i < files.size() - 1; i++) {
                if (files.get(i).getSize() > files.get(i + 1).getSize()) {
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
            this.files.add(new DataFile(file));
        }
    }
}
