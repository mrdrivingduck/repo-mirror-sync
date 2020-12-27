package iot.zjt.repo;

public class Repository {
    private String owner;
    private String name;
    private boolean visibilityPrivate;

    public Repository() {}

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setVisibilityPrivate(boolean visibilityPrivate) {
        this.visibilityPrivate = visibilityPrivate;
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
}
