package backend.data;

import backend.Constants;
import backend.FileManager;
import backend.exceptions.InvalidFileNameException;
import backend.exceptions.InvalidNameException;
import backend.exceptions.UnexpectedErrorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DataFile {

    private DataFile parent;

    private String name;
    private long size;
    private FileTime changeDate;
    private String type;
    private String path;

    private ArrayList<String> tags;
    private ArrayList<String> artists;
    private ArrayList<DataFile> files;

    private boolean tagsLoaded;
    private boolean artistsLoaded;

    //only for image and video
    private int width, height;

    public DataFile() {
        this.files = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.artists = new ArrayList<>();
        this.tagsLoaded = false;
        this.artistsLoaded = false;
    }

    public DataFile(DataFile parent, File file) throws IOException {
        this();
        this.parent = parent;
        Path fp = Paths.get(file.getAbsolutePath());
        BasicFileAttributes attr = Files.readAttributes(fp, BasicFileAttributes.class);

        String extension = "", name = "";
        //if it is a folder it should not have an extension
        if(file.listFiles() == null) {
            int li = file.getName().lastIndexOf('.');
            if (li > 0) {
                extension = file.getName().substring(li+1);
                name = file.getName().substring(0,li);
            }
        }
        this.type = extension;
        this.name = name;
        this.size = file.length();
        this.changeDate = attr.lastModifiedTime();
        this.path = file.getAbsolutePath();

        try {
            loadSubfiles();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void loadSubfiles() throws IOException {
        File[] dir = new File(this.path).listFiles();
        if(dir != null){
            for(File f: dir) {
                if(FileManager.getInstance().findFileByPath(f.getAbsolutePath()) == null){
                    files.add(new DataFile(this, f));
                }
            }
        }
    }

    public DataFile get(String path) {
        if(this.path.equals(path)) {
            return this;
        }
        for(DataFile df: files) {
            DataFile d = df.get(path);
            if(d != null){
                return d;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String s = "DataFile{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", changeDate=" + changeDate +
                ", type='" + type + '\'' +
                ", path='" + path + '\'' +
                '}';
        if(files.size() > 0){
            s+= " - ORDNER";
        }
        return s;
    }

    public String formatDate() {
        return new SimpleDateFormat("dd.MM.yyyy").format(this.changeDate.toMillis());
    }

    public ArrayList<DataFile> getChildren() {
        try {
            return getChildrenRecursive(new ArrayList<>());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private ArrayList<DataFile> getChildrenRecursive(ArrayList<DataFile> dataFiles) {
        dataFiles.add(this);
        for(DataFile df: this.files) {
            df.getChildrenRecursive(dataFiles);
        }
        return dataFiles;
    }

    public void rename(String newName) throws InvalidFileNameException, UnexpectedErrorException {
        if(newName.length() > Constants.MAX_FILENAME_LENGTH){
            throw new InvalidFileNameException("Dateiname ist zu lang");
        }
        for(String s: Constants.INVALID_CHARACTERS) {
            if(newName.contains(s)){
                throw new InvalidFileNameException();
            }
        }

        int lastindexof = this.path.lastIndexOf("\\");
        String pth = this.path.substring(0, lastindexof);
        File oldFile = new File(pth+"\\"+this.name+"."+this.type);
        File newFile = new File(pth+"\\"+newName+"."+this.type);

        if(newFile.exists()) {
            throw new InvalidFileNameException("Name ist bereits vergeben");
        }

        try {
            boolean renameSuccessful = oldFile.renameTo(newFile);
            if(renameSuccessful) {
                this.name = newName;
            }else{
                throw new UnexpectedErrorException("Fehler beim Umbenennen der Datei");
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public String getFormattedSize() {
        return getFormattedSize(this.size);
    }

    public static String getFormattedSize(long size) {
        if(size >= 1000000000){
            float res = size/1000000000f;
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            return df.format(res)+" GB";
        }else if(size >= 1000000) {
            return size/1000000+" MB";
        }else if(size >= 1000) {
            return size/1000+" KB";
        }else {
            return size+" Bytes";
        }
    }


    public void deleteChild(DataFile file) {
        this.files.remove(file);
    }

    public void addTag(String tag) throws InvalidNameException {
        if(!this.tags.contains(tag)){
            if(FileManager.containsSpecialChar(tag)){
                throw new InvalidNameException("Keine Sonderzeichen in Tags!");
            }
            for(int i = 0; i < this.tags.size(); i++) {
                if (tag.toLowerCase().compareTo(this.tags.get(i).toLowerCase()) < 0){
                    tags.add(i, tag);
                    return;
                }
            }
            tags.add(tag);
        }
    }

    public boolean isInTags(String str) {
        for(String tag: this.tags){
            if(tag.toLowerCase().contains(str.toLowerCase())){
                return true;
            }
        }
        return false;
    }

    //GETTER SETTER

    public ArrayList<String> getTags() {
        return this.tags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public FileTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(FileTime changeDate) {
        this.changeDate = changeDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ArrayList<DataFile> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<DataFile> files) {
        this.files = files;
    }

    public DataFile getParent() {
        return parent;
    }

    public void setParent(DataFile parent) {
        this.parent = parent;
    }

    public boolean isTagsLoaded() {
        return tagsLoaded;
    }

    public void setTagsLoaded(boolean tagsLoaded) {
        this.tagsLoaded = tagsLoaded;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void removeAllTags() {
        this.tags.clear();
    }

    public boolean hasTag(String tag) {
        return this.getTags().contains(tag);
    }

    public void removeTags(Set<String> tags) {
        for (String tag : tags) {
            this.tags.remove(tag);
        }
    }

    public void addArtist(String artist) {
        if(!this.artists.contains(artist)){
            this.artists.add(artist);
        }
    }

    public List getArtists() {
        return this.artists;
    }

    public String getArtistsAsString() {
        String str = "";
        for(int i = 0; i < artists.size(); i++){
            str += artists.get(i);
            if(i != artists.size()-1){
                str += ", ";
            }
        }
        return str;
    }

    public boolean isArtistsLoaded() {
        return artistsLoaded;
    }

    public void setArtistsLoaded(boolean b) {
        this.artistsLoaded = b;
    }
}


