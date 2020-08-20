package backend.tasks;

public interface TaskObserver {

    /**
     * when a task is finished this method gets called
     */
    void notify(Task task);

}
