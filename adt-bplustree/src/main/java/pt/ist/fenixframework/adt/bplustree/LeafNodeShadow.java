package pt.ist.fenixframework.adt.bplustree;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

public class LeafNodeShadow extends LeafNodeShadow_Base {
    
    public LeafNodeShadow() {
	setEntries(new TreeMap<Comparable,Serializable>(BPlusTree.COMPARATOR_SUPPORTING_LAST_KEY));
    }

    private LeafNodeShadow(TreeMap<Comparable,Serializable> entries) {
	setEntries(entries);
    }

    private TreeMap<Comparable,Serializable> duplicateMap() {
        return new TreeMap<Comparable,Serializable>(getEntries());
    }

    public AbstractNodeShadow insert(Comparable key, Serializable value) {
	TreeMap<Comparable,Serializable> localMap = justInsert(key, value);

	if (localMap.size() <= BPlusTree.MAX_NUMBER_OF_ELEMENTS) { // it still fits :-)
	    return getRootShadow();
	} else { // must split this node
	    // find middle position
	    Comparable keyToSplit = findRightMiddlePosition(localMap.keySet());

	    // split node in two
	    LeafNodeShadow leftNode = new LeafNodeShadow(new TreeMap<Comparable,Serializable>(localMap.headMap(keyToSplit)));
	    LeafNodeShadow rightNode = new LeafNodeShadow(new TreeMap<Comparable,Serializable>(localMap.tailMap(keyToSplit)));
	    fixLeafNodeShadowsListAfterSplit(leftNode, rightNode);

	    // propagate split to parent
	    if (getParent() == null) {  // make new root node
		InnerNodeShadow newRoot = new InnerNodeShadow(leftNode, rightNode, keyToSplit);
		return newRoot;
	    } else {
		// leftNode.parent = getParent();
		// rightNode.parent = getParent();
		return getParent().rebase(leftNode, rightNode, keyToSplit);
	    }
	}
    }
    
    private <T extends Comparable> Comparable findRightMiddlePosition(Collection<T> keys) {
	Iterator<T> keysIterator = keys.iterator();

	for (int i = 0; i < BPlusTree.LOWER_BOUND + 1; i++) {
	    keysIterator.next();
	}
	return keysIterator.next();
    }

    private TreeMap<Comparable,Serializable> justInsert(Comparable key, Serializable value) {
	TreeMap<Comparable,Serializable> localEntries = this.getEntries();

	// this test is performed because we need to return a new structure in
	// case an update occurs.  Value types must be immutable.
	Serializable currentValue = localEntries.get(key);
	if (currentValue == value && localEntries.containsKey(key)) {
	    return localEntries;
	} else {
	    TreeMap<Comparable,Serializable> newMap = duplicateMap();
	    newMap.put(key, value);
            setEntries(newMap);
	    return newMap;
	}
    }

    private void fixLeafNodeShadowsListAfterSplit(LeafNodeShadow leftNode, LeafNodeShadow rightNode) {
	leftNode.setPrevious(this.getPrevious());
	rightNode.setNext(this.getNext());
	leftNode.setNext(rightNode);
    }

    public AbstractNodeShadow remove(Comparable key) {
	TreeMap<Comparable,Serializable> localMap = justRemove(key);

	if (getParentShadow() == null) {
	    return this;
	} else {
	    // if the removed key was the first we need to replace it in some parent's index
	    Comparable replacementKey = getReplacementKeyIfNeeded(key);

	    if (localMap.size() < BPlusTree.LOWER_BOUND) {
		return getParent().underflowFromLeaf(key, replacementKey);
	    } else if (replacementKey != null) {
		return getParent().replaceDeletedKey(key, replacementKey);
	    } else {
		return getParentShadow().getRootShadow();  // maybe a tiny faster than just getRoot() ?!
	    }
	}
    }

    private TreeMap<Comparable,Serializable> justRemove(Comparable key) {
	TreeMap<Comparable,Serializable> localEntries = this.getEntries();

	// this test is performed because we need to return a new structure in
	// case an update occurs.  Value types must be immutable.
	if (!localEntries.containsKey(key)) {
	    return localEntries;
	} else {
	    TreeMap<Comparable,Serializable> newMap = duplicateMap();
	    newMap.remove(key);
            setEntries(newMap);
	    return newMap;
	}
    }

    // This method assumes that there is at least one more key (which is
    // always true if this is not the root node)
    private Comparable getReplacementKeyIfNeeded(Comparable deletedKey) {
	Comparable firstKey = this.getEntries().firstKey();
	if (BPlusTree.COMPARATOR_SUPPORTING_LAST_KEY.compare(deletedKey, firstKey) < 0) {
	    return firstKey;
	} else {
	    return null; // null means that key does not need replacement
	}
    }

    Map.Entry<Comparable,Serializable> removeBiggestKeyValue() {
	TreeMap<Comparable,Serializable> newMap = duplicateMap();
	Map.Entry<Comparable,Serializable> lastEntry = newMap.pollLastEntry();
        setEntries(newMap);
	return lastEntry;
    }

    Map.Entry<Comparable,Serializable> removeSmallestKeyValue() {
	TreeMap<Comparable,Serializable> newMap = duplicateMap();
	Map.Entry<Comparable,Serializable> firstEntry = newMap.pollFirstEntry();
        setEntries(newMap);
	return firstEntry;
    }

    Comparable getSmallestKey() {
	return this.getEntries().firstKey();
    }

    void addKeyValue(Map.Entry keyValue) {
	TreeMap<Comparable,Serializable> newMap = duplicateMap();
	newMap.put((Comparable)keyValue.getKey(), (Serializable)keyValue.getValue());
        setEntries(newMap);
    }

    void mergeWithLeftNode(AbstractNodeShadow leftNode, Comparable splitKey) {
	LeafNodeShadow left = (LeafNodeShadow)leftNode; // this node does not know how to merge with another kind
	
	TreeMap<Comparable,Serializable> newMap = duplicateMap();
	newMap.putAll(left.getEntries());
        setEntries(newMap);

	LeafNodeShadow nodeBefore = left.getPrevious();

	this.setPrevious(nodeBefore);
	if (nodeBefore != null) {
	    nodeBefore.setNext(this);
	}

	// no need to update parents, because they are always the same for the two merging leaf nodes
	assert(this.getParent() == leftNode.getParent());
    }

    public Serializable get(Comparable key) {
	return this.getEntries().get(key);
    }

    public Serializable getIndex(int index) {
	if (index < 0) {
	    throw new IndexOutOfBoundsException();
	}

	if (index < shallowSize()) { // the required position is here
    	    Iterator<Serializable> values = this.getEntries().values().iterator();
    	    for (int i = 0; i < index; i++) {
    	    	values.next();
    	    }
	    return values.next();
	} else {
	    LeafNodeShadow next = this.getNext();
	    if (next == null) {
		throw new IndexOutOfBoundsException();
	    }
	    return next.getIndex(index - shallowSize());
	}
    }

    public AbstractNodeShadow removeIndex(int index) {
	if (index < 0) {
	    throw new IndexOutOfBoundsException();
	}

	if (index < shallowSize()) { // the required position is here
	    Iterator<Comparable> keys = this.getEntries().keySet().iterator();
	    for (int i = 0; i < index; i++) {
		keys.next();
	    }
	    return this.remove(keys.next());
	} else {
	    LeafNodeShadow next = this.getNext();
	    if (next == null) {
		throw new IndexOutOfBoundsException();
	    }
	    return next.removeIndex(index - shallowSize());
	}
    }

    public boolean containsKey(Comparable key) {
	return this.getEntries().containsKey(key);
    }

    int shallowSize() {
	return this.getEntries().size();
    }

    public int size() {
	return this.getEntries().size();
    }

    @Override
    Iterator<? extends Comparable> keysIterator() {
        return new LeafNodeShadowKeysIterator(this);
    }

    public Iterator<Serializable> iterator() {
	return new LeafNodeShadowValuesIterator(this);
    }

    protected abstract class GenericLeafNodeShadowIterator<T> implements Iterator<T> {
	private Iterator<T> iterator;
	private LeafNodeShadow current;
	

	GenericLeafNodeShadowIterator(LeafNodeShadow LeafNodeShadow) {
            this.iterator = getInternalIterator(LeafNodeShadow);
	    this.current = LeafNodeShadow;
	}

        protected abstract Iterator<T> getInternalIterator(LeafNodeShadow LeafNodeShadow);

	public boolean hasNext() {
	    if (this.iterator.hasNext()) {
		return true;
	    } else {
		return this.current.getNext() != null;
	    }
	}

        public T next() {
	    if (!this.iterator.hasNext()) {
		LeafNodeShadow nextNode = this.current.getNext();
		if (nextNode != null) {
		    this.current = nextNode;
		    this.iterator = getInternalIterator(this.current);
		} else {
		    throw new NoSuchElementException();
		}
	    }
	    return this.iterator.next();
	}

        public void remove() {
	    throw new UnsupportedOperationException("This implementation does not allow element removal via the iterator");
	}

    }

    private class LeafNodeShadowValuesIterator extends GenericLeafNodeShadowIterator<Serializable> {

	LeafNodeShadowValuesIterator(LeafNodeShadow LeafNodeShadow) {
            super(LeafNodeShadow);
	}

        protected Iterator<Serializable> getInternalIterator(LeafNodeShadow LeafNodeShadow) {
            return LeafNodeShadow.getEntries().values().iterator();
        }

    }

    private class LeafNodeShadowKeysIterator extends GenericLeafNodeShadowIterator<Comparable> {

	LeafNodeShadowKeysIterator(LeafNodeShadow LeafNodeShadow) {
            super(LeafNodeShadow);
	}

        protected Iterator<Comparable> getInternalIterator(LeafNodeShadow LeafNodeShadow) {
            return LeafNodeShadow.getEntries().keySet().iterator();
        }

    }

    public String dump(int level, boolean dumpKeysOnly, boolean dumpNodeIds) {
	StringBuilder str = new StringBuilder();
	str.append(BPlusTree.spaces(level));
	if (dumpNodeIds) {
	    str.append(this.getPrevious() + "<-[" + this + ": ");
	} else {
	    str.append("[: ");
	}

	for (Map.Entry<Comparable, Serializable> entry : this.getEntries().entrySet()) {
	    Comparable key = entry.getKey();
	    Serializable value = entry.getValue();
	    str.append("(" + key);
	    str.append(dumpKeysOnly ? ") " : "," + value + ") ");
	}
	if (dumpNodeIds) {
	    str.append("]->" + this.getNext() + " ^" + getParent() + "\n");
	} else {
	    str.append("]\n");
	}

	return str.toString();
    }

    @Override
    Collection<? extends Comparable> getKeys() {
	return this.getEntries().keySet();
    }

}