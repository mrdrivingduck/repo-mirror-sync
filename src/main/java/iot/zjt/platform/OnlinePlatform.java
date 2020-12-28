package iot.zjt.platform;

import io.vertx.core.Future;
import iot.zjt.repo.Repository;

import java.util.List;

/**
 * Interface for online SCM system to implement.
 *
 * @author Mr Dk.
 * @since 2020/12/28
 */
public interface OnlinePlatform {
    Future<List<Repository>> getRepositories(boolean includePrivate);
    Future<Void> createRepository(Repository repo);
    Future<Void> deleteRepository(Repository repo);
    String getPlatform();
}
