package backend.tasks;

public class Task {

    private TaskRunnable runnable;

    public Task(TaskRunnable runnable){
        this.runnable = runnable;
    }

    public void run(Callback finishedCallback) {
        runnable.run(finishedCallback);
    }

}
