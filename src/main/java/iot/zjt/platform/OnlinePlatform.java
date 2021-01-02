package iot.zjt.platform;

import io.vertx.core.Future;
import iot.zjt.Repository;

import java.util.List;

/**
 * Interface for online version control system to implement.
 *
 * @author Mr Dk.
 * @since 2021/01/01
 */
public interface OnlinePlatform {
    Future<List<Repository>> getRepositories(boolean includePrivate);
    Future<Void> createRepository(Repository repo);
    Future<Void> deleteRepository(Repository repo);
    Future<Void> updateRepository(Repository repo);
    String getPlatform();
    String getRepositoryHttpsUrl(Repository repo);
}
