package test.osgi.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.felix.connect.launch.PojoServiceRegistry;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.namepath.NamePathMapper;
import org.apache.jackrabbit.oak.run.osgi.OakOSGiRepositoryFactory;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.security.user.action.AbstractAuthorizableAction;
import org.apache.jackrabbit.oak.spi.security.user.action.AuthorizableAction;
import org.apache.jackrabbit.oak.spi.security.user.action.AuthorizableActionProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import static org.apache.commons.io.FilenameUtils.concat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OakOSGiRepositoryFactoryTest {

    private String repositoryHome;
    private RepositoryFactory repositoryFactory = new CustomOakFactory();
    private Map config = new HashMap();
    private String newPassword;
    private boolean calledOnStart;
    private boolean calledOnStop;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        repositoryHome = tmpFolder.newFolder("target").getAbsolutePath();
        config.put("org.apache.jackrabbit.repository.home", repositoryHome);
        config.put("repository.home", repositoryHome);

        File repoHome = new File(repositoryHome);
        if (repoHome.exists()) {
            FileUtils.cleanDirectory(new File(repositoryHome));
        }
        copyConfig("common");
    }

    @Test(timeout = 60 * 1000)
    public void testRepositoryTar() throws Exception {
        copyConfig("tar");
        config.put(BundleActivator.class.getName(), new TestActivator());
        Repository repository = repositoryFactory.getRepository(config);

        assertTrue("test activator should have been called on startup", calledOnStart);
        //Give time for system to stablize :(
        TimeUnit.SECONDS.sleep(1);

        assertNotNull(repository);
        System.out.println("Repository started ");

        basicCrudTest(repository);

        //For now SecurityConfig is giving some issue
        //so disable that
        testCallback(repository);

        shutdown(repository);
        assertTrue("test activator should have been called on stop", calledOnStop);
    }

    private void testCallback(Repository repository) throws RepositoryException {
        JackrabbitSession session = (JackrabbitSession)
                repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

        String testUserId = "footest";

        User testUser = (User) session.getUserManager().getAuthorizable(testUserId);
        if (testUser == null) {
            testUser = session.getUserManager().createUser(testUserId, "password");
        }

        session.save();

        testUser.changePassword("newPassword");
        session.save();

        assertEquals("newPassword", newPassword);
        session.logout();
    }

    private void basicCrudTest(Repository repository) throws RepositoryException {
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        Node rootNode = session.getRootNode();

        Node child = JcrUtils.getOrAddNode(rootNode, "child", "oak:Unstructured");
        child.setProperty("foo3", "bar3");
        session.logout();

        System.out.println("Basic test passed");
    }

    private void shutdown(Repository repository) {
        if (repository instanceof JackrabbitRepository) {
            ((JackrabbitRepository) repository).shutdown();
        }
    }

    private void copyConfig(String type) throws IOException {
        FileUtils.copyDirectory(new File(concat(getBaseDir(), "src/test/resources/config-" + type)),
                new File(concat(repositoryHome, "config")));
    }

    private static String getBaseDir() {
        return new File(".").getAbsolutePath();
    }

    private class CustomOakFactory extends OakOSGiRepositoryFactory {

        @Override
        protected void postProcessRegistry(PojoServiceRegistry registry) {
            registry.registerService(AuthorizableActionProvider.class.getName(), new AuthorizableActionProvider() {

                @Override
                public List<? extends AuthorizableAction> getAuthorizableActions(SecurityProvider securityProvider) {
                    return Collections.singletonList(new TestAction());
                }
            }, null);
        }
    }

    private class TestActivator implements BundleActivator {

        @Override
        public void start(BundleContext bundleContext) throws Exception {
            calledOnStart = true;
        }

        @Override
        public void stop(BundleContext bundleContext) throws Exception {
            calledOnStop = true;
        }
    }

    private class TestAction extends AbstractAuthorizableAction {

        @Override
        public void onPasswordChange(User user, String newPassword,
                                     Root root, NamePathMapper namePathMapper) throws RepositoryException {
            OakOSGiRepositoryFactoryTest.this.newPassword = newPassword;
        }
    }
}