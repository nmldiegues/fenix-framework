package pt.ist.fenixframework.test.jta;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.fail;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class JTATest {

    private static final Logger logger = LoggerFactory.getLogger(JTATest.class);

    @Atomic
    @BeforeClass
    public static void createApplication() {
	Application application = new Application();
	FenixFramework.getDomainRoot().setApplication(application);
    }

    @Test
    @Atomic
    public void testApplicationExists() {
	assertThat(FenixFramework.getDomainRoot().getApplication(), notNullValue());
    }

    @Test
    @Atomic
    public void atomicShouldHaveTransationStatusActive() throws SystemException {
	assertThat(FenixFramework.getTransaction().getStatus(), is(Status.STATUS_ACTIVE));
    }

    @Test
    public void shouldHaveActiveStateOnTxManagerBegin() throws NotSupportedException, SystemException {
	FenixFramework.getTransactionManager().begin();

	try {
	    assertThat(FenixFramework.getTransaction().getStatus(), is(Status.STATUS_ACTIVE));
	} finally {
	    FenixFramework.getTransactionManager().rollback();
	}
    }

    @Test
    /* @Atomic not present! */
    public void shouldBeInactiveOrUnknownStatus() throws SystemException {
	int status = FenixFramework.getTransactionManager().getStatus();

	if (status != Status.STATUS_NO_TRANSACTION && status != Status.STATUS_UNKNOWN) {
	    logger.error("Status is {}", status);
	    fail();
	}
    }

    private static class Data {
	public boolean runBefore = false;
	public int status = -1;
    }

    @Test
    public void synchronizationShouldBeCalledBeforeAndAfter() throws NotSupportedException, SystemException,
	    IllegalStateException, RollbackException, SecurityException, HeuristicMixedException, HeuristicRollbackException {

	final Data data = new Data();

	FenixFramework.getTransactionManager().begin();

	FenixFramework.getTransaction().registerSynchronization(new Synchronization() {

	    @Override
	    public void beforeCompletion() {
		logger.trace("Running before completion");
		data.runBefore = true;
	    }

	    @Override
	    public void afterCompletion(int status) {
		logger.trace("Running after completion with status: {}", status);
		data.status = status;
	    }
	});

	FenixFramework.getTransactionManager().commit();

	assertThat(data.runBefore, is(true));
	assertThat(data.status, is(not(-1)));
    }

    @AfterClass
    public static void shutdownFenixFramework() {
	FenixFramework.shutdown();
    }

}
