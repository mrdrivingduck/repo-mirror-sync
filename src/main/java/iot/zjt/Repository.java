package iot.zjt;

/**
 * The object to describe an online repository.
 *
 * @author Mr Dk.
 * @version 2020/12/31
 */
public class Repository {
    private String owner;
    private String name;
    private boolean visibilityPrivate;
    private int id;

    public Repository() {
    }

    public Repository(Repository another) {
        this.owner = another.owner;
        this.name = another.name;
        this.visibilityPrivate = another.visibilityPrivate;
        this.id = another.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setVisibilityPrivate(boolean visibilityPrivate) {
        this.visibilityPrivate = visibilityPrivate;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public boolean getVisibilityPrivate() {
        return visibilityPrivate;
    }

    public int getId() {
        return id;
    }
}
