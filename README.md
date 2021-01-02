# repo-mirror-sync

🎞️ Synchronize repositories between hosting platforms as mirrors.

Created by : Mr Dk.

2020 / 12 / 28 21:07

Nanjing, Jiangsu, China

---

## About

*Repository Mirror Synchronizer* help you backup repositories from one hosting platform to another as a mirror, keeping your commit records, branches, tags **all the same**. Currently most of the hosting platforms has offered such function **with charge**. This program tries to to the same thing with the help of **APIs provided by the platforms** and the mirroring ability provided by **Git**.

To manipulate your repositories through APIs, you should get the **personal access tokens** from the corresponding platform. Specifically, the token should at least have the permission of **read / write repositories**.

The program use these library as main dependencies:

* [*Eclipse Vert.x*](https://vertx.io/): Reactive applications on the JVM
* [*Eclipse JGit*](https://www.eclipse.org/jgit/)

## Contribute

More hosting platforms to be support:

| Support Status |                        Platform Logo                         |                           Tokens                            |
| :------------: | :----------------------------------------------------------: | :---------------------------------------------------------: |
|       ✅        |   ![github](docs/github.png) [GitHub](https://github.com/)   |         [link](https://github.com/settings/tokens)          |
|       ✅        |   ![gitlab](docs/gitlab.png) [GitLab](https://gitlab.com/)   | [link](https://gitlab.com/-/profile/personal_access_tokens) |
|       ❌        | ![bitbucket](docs/bitbucket.png) [Bitbucket](https://bitbucket.org/) |                              🚧                              |
|       ❌        |     ![gitee](docs/gitee.png) [Gitee](https://gitee.com/)     |                              🚧                              |
|       ❌        |   ![coding](docs/coding.png) [Coding](https://coding.net/)   |                              🚧                              |
|      ...       |                             ...                              |                             ...                             |

To support a platform, methods defined in [`OnlinePlatform`](src/main/java/iot/zjt/platform/OnlinePlatform.java) interface should be implemented: 

```java
public interface OnlinePlatform {
    Future<List<Repository>> getRepositories(boolean includePrivate);
    Future<Void> createRepository(Repository repo);
    Future<Void> deleteRepository(Repository repo);
    Future<Void> updateRepository(Repository repo);
    String getPlatform();
    String getRepositoryHttpsUrl(Repository repo);
}
```

## Usage

For example, if you want to mirror your repositories from GitHub to GitLab, then you should first initialize two `PlatformUser` objects with your platform **username** and **token**:

```java
PlatformUser githubUser = new PlatformUser("github.username", "github.token");
PlatformUser gitlabUser = new PlatformUser("gitlab.username", "gitlab.token");
```

Then, use the `PlatformUser` objects to initialize corresponding sub-class objects which extends from `AbstractOnlinePlatform`.

```java
Vertx vertx = Vertx.vertx();
GitHubPlatform github = new GitHubPlatform(vertx, githubUser);
GitLabPlatform gitlab = new GitLabPlatform(vertx, gitlabUser);
```

Finally, use `AbstractOnlinePlatform.mirrorAllRepoTo()` to start mirroring:

```java
github.mirrorAllRepoTo(gitlab, true /* whether to mirror private repos */)
    .onComplete(ar -> {
        if (ar.succeeded()) {
            System.out.println("Mirroring success");
        } else {
            System.err.println("Mirroring failed");
        }
    });
```

## License

Copyright © 2020-2021, Jingtang Zhang. ([MIT License](LICENSE))

---

