package backend.tasks;

public interface TaskObserver {

    /**
     * when a task is finished this method gets called
     */
    public void notify(Task task);

}
