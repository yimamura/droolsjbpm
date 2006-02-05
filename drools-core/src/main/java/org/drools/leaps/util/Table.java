package org.drools.leaps.util;

import java.util.Iterator;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

public class Table {

    private final TreeMap map;

    protected TableRecord headRecord;

    protected TableRecord tailRecord;

    protected final static int EQUAL_OR_LESS = -1;

    protected final static int EQUAL_OR_GREATER = 1;

    private boolean empty = true;

    private int count = 0;

    public Table(Comparator comparator) {
        this.map = new TreeMap(comparator);
    }

    protected void clear() {
        this.headRecord = new TableRecord(null);
        this.empty = true;
        this.count = 0;
        this.map.clear();
    }

    /**
     * add work by adding element to the first position so we can use iteration
     * from position
     */
    public void add(Object object) {
        boolean foundEqualObject = false;
        TableRecord newRecord = new TableRecord(object);
        if (this.empty) {
            this.headRecord = newRecord;
            this.empty = false;
        } else {
            SortedMap bufMap = this.map.headMap(object);
            if (!bufMap.isEmpty()) {
                TableRecord bufRec = (TableRecord) map.get(bufMap
                        .lastKey());
                if (bufRec.right != null) {
                    bufRec.right.left = newRecord;
                }
                newRecord.right = bufRec.right;
                bufRec.right = newRecord;
                newRecord.left = bufRec;

            } else {
                this.headRecord.left = newRecord;
                newRecord.right = this.headRecord;
                this.headRecord = newRecord;
            }
        }
        if (!foundEqualObject) {
            // check if the new record was added at the end of the list
            // and assign new value to the tail record
            if (newRecord.right == null) {
                this.tailRecord = newRecord;
            }
            //
            this.count++;
            //
            this.map.put(object, newRecord);
        }
    }

    /**
     * Removes object from the table
     * 
     * @param object
     *            to remove from the table
     */
    public void remove(Object object) {
        if (!this.empty) {
            // LeapsTableRecord record = this.search(object,
            // LeapsTable.EQUAL_OR_GREATER);
            TableRecord record = (TableRecord) this.map.get(object);

            if (record != null) {
                if (record == this.headRecord) {
                    if (record.right != null) {
                        this.headRecord = record.right;
                        this.headRecord.left = null;
                    } else {
                        // single element in table being removed
                        // table is empty now
                        this.headRecord = new TableRecord(null);
                        this.tailRecord = this.headRecord;
                        this.empty = true;
                    }
                } else if (record == this.tailRecord) {
                    // single element in the table case is being solved above
                    // when
                    // we checked for headRecord match
                    this.tailRecord = record.left;
                    this.tailRecord.right = null;
                } else {
                    // left
                    record.left.right = record.right;
                    record.right.left = record.left;
                }
            }
            this.count--;
            //
            this.map.remove(object);
        }
    }

    public boolean contains(Object object) {
        if (!empty) {
            return this.map.containsKey(object);
            /**
             * LeapsTableRecord rec = this.search(object,
             * LeapsTable.EQUAL_OR_GREATER); if (rec != null) { return
             * this.comparator.compare(rec.object, object) == 0; } else { return
             * false; }
             */
        } else {
            return false;
        }
    }

    public TableIterator iterator() {
        TableIterator ret;
        if (this.empty) {
            ret = new TableIterator();
        } else {
            ret = new TableIterator(this.headRecord, this.headRecord,
                    this.tailRecord);
        }
        return ret;
    }

    /**
     * iterator over "tail" part of the table data.
     * 
     * @param objectAtStart -
     *            upper boundary of the iteration
     * @param objectAtPosition -
     *            starting point of the iteration
     * @return leaps table iterator
     * @throws TableOutOfBoundException
     */
    public TableIterator tailIterator(Object objectAtStart,
            Object objectAtPosition) throws TableOutOfBoundException {
        // validate
        if (this.map.comparator().compare(objectAtStart, objectAtPosition) > 0) {
            throw new TableOutOfBoundException(
                    "object at position is out of upper bound");
        }
        TableIterator iterator = null;
        TableRecord startRecord = null;
        TableRecord currentRecord = null;
        TableRecord lastRecord = this.tailRecord;

        if (!this.empty) { // validate
        // if (!this.map.isEmpty()) { // validate
            if (this.map.comparator().compare(objectAtStart,
                    this.tailRecord.object) <= 0) {
                // let's check if we need iterator over the whole table
                SortedMap bufMap = this.map.tailMap(objectAtStart);
                if (!bufMap.isEmpty()) {
                    startRecord = (TableRecord) bufMap.get(bufMap
                            .firstKey());
                    if (this.map.comparator().compare(objectAtStart,
                            objectAtPosition) == 0) {
                        currentRecord = startRecord;
                    } else {
                        // rewind to position
                        bufMap = bufMap.tailMap(objectAtPosition);

                        if (!bufMap.isEmpty()) {
                            currentRecord = ((TableRecord) bufMap
                                    .get(bufMap.firstKey()));
                        } else {
                            currentRecord = startRecord;
                        }
                    }
                    iterator = new TableIterator(startRecord,
                            currentRecord, lastRecord);
                } else {
                    // empty iterator
                    iterator = new TableIterator();
                }
            } else {
                // empty iterator
                iterator = new TableIterator();
            }
        } else {
            // empty iterator
            iterator = new TableIterator();
        }

        return iterator;
    }

    /**
     * iterator over "head" part of the table data. it does not take
     * "positional" parameter because it's used for scanning shadow tables and
     * this scan never "resumes"
     * 
     * @param objectAtEnd -
     *            lower boundary of the iteration
     * @return leaps table iterator
     * @throws TableOutOfBoundException
     */
    public TableIterator headIterator(Object objectAtEnd)
            throws TableOutOfBoundException {
        TableIterator iterator = null;
        TableRecord startRecord = this.headRecord;
        TableRecord currentRecord = this.headRecord;
        TableRecord lastRecord = null;

        if (!this.empty) { // validate
            if (this.map.comparator().compare(this.headRecord.object,
                    objectAtEnd) <= 0) {
                // let's check if we need iterator over the whole table
                SortedMap bufMap = this.map.headMap(objectAtEnd);
                if (!bufMap.isEmpty()) {
                    lastRecord = (TableRecord) bufMap
                            .get(bufMap.lastKey());
                    // check if the next one is what we need
                    if (lastRecord.right != null
                            && this.map.comparator().compare(
                                    lastRecord.right.object, objectAtEnd) == 0) {
                        lastRecord = lastRecord.right;
                    }
                    iterator = new TableIterator(startRecord,
                            currentRecord, lastRecord);
                } else {
                    // empty iterator
                    iterator = new TableIterator();
                }
            } else {
                // empty iterator
                iterator = new TableIterator();
            }
        } else {
            // empty iterator
            iterator = new TableIterator();
        }

        return iterator;
    }

    /**
     * indicates if table has any elements
     * 
     * @return empty indicator
     */
    public boolean isEmpty() {
        return this.empty;
    }

    public String toString() {
        String ret = "";

        for (Iterator it = this.iterator(); it.hasNext();) {
            ret = ret + it.next() + "\n";
        }
        return ret;
    }

    public int size() {
        return this.count;
    }

    public Object top() {
        return this.headRecord.object;
    }

    public Object bottom() {
        return this.tailRecord.object;
    }
}
