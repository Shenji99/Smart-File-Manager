package backend;

import backend.tasks.Callback;

public abstract class TaskRunnable {

    public abstract void run(Callback callback);
}