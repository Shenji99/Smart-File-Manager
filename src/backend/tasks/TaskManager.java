package backend.tasks;

import java.util.ArrayList;

public class TaskManager {

    public ArrayList<Task> tasks;
    public ArrayList<TaskObserver> observers;

    public TaskManager() {
        this.tasks = new ArrayList<>();
        this.observers = new ArrayList<>();
    }

    public void addTask(Task t) {
        this.tasks.add(t);
    }

    public void startAllTasks() {
        for(Task t: this.tasks){
            t.run(args -> notifyObservers(t));
        }
    }

    private void notifyObservers(Task t) {
        for(TaskObserver o: observers){
            o.notify(t);
        }
    }

    public void addTaskObserver(TaskObserver observer){
        this.observers.add(observer);
    }

    public void removeTaskObserver(TaskObserver observer){
        this.observers.remove(observer);
    }

    public int getTasksSize() {
        return this.tasks.size();
    }
}
