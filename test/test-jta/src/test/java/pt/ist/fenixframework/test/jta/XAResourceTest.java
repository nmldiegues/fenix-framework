package pt.ist.fenixframework.test.jta;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import pt.ist.fenixframework.FenixFramework;

@RunWith(JUnit4.class)
public class XAResourceTest {

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
    public void xaResourcesNotImplementedYet() throws SystemException {
	transactionManager.getStatus();
    }
}
