package backend;

import java.util.Set;

public interface TagObserver {

    public void notify(Set<String> tags);
}
