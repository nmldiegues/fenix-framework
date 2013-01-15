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
import javax.transaction.TransactionManager;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@RunWith(JUnit4.class)
public class JTATest {

    private static final Logger logger = LoggerFactory.getLogger(JTATest.class);

    private TransactionManager transactionManager;

    /*
     * Setups
     */

    @Before
    public void setTxManager() {
	transactionManager = FenixFramework.getTransactionManager();
    }

    @Atomic
    @BeforeClass
    public static void createApplication() {
	Application application = new Application();
	FenixFramework.getDomainRoot().setApplication(application);
    }

    @AfterClass
    public static void shutdownFenixFramework() {
	FenixFramework.shutdown();
    }

    /*
     * Status checking tests
     */

    @Test
    @Atomic
    public void atomicShouldHaveTransationStatusActive() throws SystemException {
	assertThat(transactionManager.getTransaction(), notNullValue());
	assertThat(transactionManager.getStatus(), is(Status.STATUS_ACTIVE));
    }

    @Test
    public void shouldHaveActiveStateOnTxManagerBegin() throws NotSupportedException, SystemException {
	transactionManager.begin();

	try {
	    assertThat(transactionManager.getTransaction(), notNullValue());
	    assertThat(transactionManager.getStatus(), is(Status.STATUS_ACTIVE));
	} finally {
	    transactionManager.rollback();
	}
    }

    @Test
    /* @Atomic not present! */
    public void shouldBeInactiveOrUnknownStatus() throws SystemException {
	int status = transactionManager.getStatus();

	if (status != Status.STATUS_NO_TRANSACTION && status != Status.STATUS_UNKNOWN) {
	    logger.error("Status is {}", status);
	    fail();
	}
    }

    @Test(expected = RollbackException.class)
    public void statusShouldBeMarkedRollBack() throws NotSupportedException, SystemException, SecurityException,
	    IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
	transactionManager.begin();

	transactionManager.getTransaction().setRollbackOnly();

	assertThat(transactionManager.getTransaction().getStatus(), is(Status.STATUS_MARKED_ROLLBACK));

	transactionManager.commit();
    }

    /*
     * SynchronizationTests
     */

    private static class Data {
	public boolean runBefore = false;
	public int status = -1;
    }

    @Test
    public void synchronizationShouldBeCalledBeforeAndAfter() throws NotSupportedException, SystemException,
	    IllegalStateException, RollbackException, SecurityException, HeuristicMixedException, HeuristicRollbackException {

	final Data data = new Data();

	transactionManager.begin();

	transactionManager.getTransaction().registerSynchronization(new Synchronization() {

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

	transactionManager.commit();

	assertThat(data.runBefore, is(true));
	assertThat(data.status, is(not(-1)));
    }

    /*
     * Exceptions
     */

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnCommittingAnEmptyTransaction() throws SecurityException, IllegalStateException,
	    RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException {
	transactionManager.commit();
    }

}
