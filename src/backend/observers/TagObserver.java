package backend.observers;

import java.util.Set;

//ONLY FOR PRESET TAGS
public interface TagObserver {

    public void notify(Set<String> tags);
}
