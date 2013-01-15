package pt.ist.fenixframework.test.jta;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

public class DummyTransaction implements Transaction {

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
	    IllegalStateException, SystemException {
	throw new UnsupportedOperationException("Dummy transaction!");
    }

    @Override
    public boolean delistResource(XAResource arg0, int arg1) throws IllegalStateException, SystemException {
	throw new UnsupportedOperationException("Dummy transaction!");
    }

    @Override
    public boolean enlistResource(XAResource arg0) throws RollbackException, IllegalStateException, SystemException {
	throw new UnsupportedOperationException("Dummy transaction!");
    }

    @Override
    public int getStatus() throws SystemException {
	throw new UnsupportedOperationException("Dummy transaction!");
    }

    @Override
    public void registerSynchronization(Synchronization arg0) throws RollbackException, IllegalStateException, SystemException {
	throw new UnsupportedOperationException("Dummy transaction!");
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {
	throw new UnsupportedOperationException("Dummy transaction!");
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
	throw new UnsupportedOperationException("Dummy transaction!");
    }

}
