package backend.observers;

import backend.data.DataFile;

public interface FileObserver {

    public void notify(DataFile dataFile);

}
