package pt.ist.fenixframework.test.jta;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import pt.ist.fenixframework.FenixFramework;

@RunWith(JUnit4.class)
public class SuspendResumeTest {

    private TransactionManager transactionManager;

    /*
     * Setups
     */

    @Before
    public void setTxManager() {
	transactionManager = FenixFramework.getTransactionManager();
    }

    @AfterClass
    public static void shutdown() {
	FenixFramework.shutdown();
    }

    @Test
    public void suspendShouldReturnNullWhenNoTransaction() throws SystemException {
	assertThat(transactionManager.suspend(), nullValue());
    }

    @Test
    public void suspendShouldDetatchThreadFromTransaction() throws NotSupportedException, SystemException {
	transactionManager.begin();
	transactionManager.suspend();
	assertThat(transactionManager.getTransaction(), nullValue());
    }

    @Test
    public void suspendShouldReturnTheCurrentTransaction() throws NotSupportedException, SystemException, SecurityException,
	    IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
	transactionManager.begin();
	Transaction tx = transactionManager.getTransaction();
	assertThat(transactionManager.suspend(), is(tx));
	assertThat(tx, notNullValue());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenResumingARunningTransaction() throws NotSupportedException, SystemException,
	    InvalidTransactionException, IllegalStateException {
	transactionManager.begin();
	Transaction tx = transactionManager.suspend();
	assertThat(transactionManager.getTransaction(), nullValue());
	transactionManager.begin();
	transactionManager.resume(tx);
    }

    @Test(expected = InvalidTransactionException.class)
    public void shouldThrowExceptionWhenResumingInvalidTransaction() throws InvalidTransactionException, IllegalStateException,
	    SystemException {
	transactionManager.resume(new DummyTransaction());
    }

}
