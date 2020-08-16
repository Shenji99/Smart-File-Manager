package backend;

import backend.data.DataFile;

public interface FileObserver {

    void onFileUpdate(DataFile dataFile);

}
