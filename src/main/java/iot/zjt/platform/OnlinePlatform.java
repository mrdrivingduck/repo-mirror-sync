package iot.zjt.platform;

import iot.zjt.repo.Repository;

import java.util.List;

public interface OnlinePlatform {
    List<Repository> getRepositories(boolean includePrivate);
    boolean createRepository(Repository repo);
    boolean deleteRepository(Repository repo);
    String getPlatform();
}
