/**
 * Sample code for the blog posting:
 * 
 * Installing and using Apache Cassandra With Java Part 4 (Thrift Client)
 * http://www.sodeso.nl/?p=251
 * 
 * Please report any discrepancies that you may find,
 * if you have any requests for examples not mentioned here
 * but are within the scope of the blog posting then also
 * please let me know so i can add them..
 */
package nl.sodeso.blog;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ColumnPath;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.Deletion;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.thrift.KeySlice;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * @author Ronald Mathies
 */
public class Authors {

    private static final String KEYSPACE = "Blog";
    private static final String COLUMN_FAMILY = "Authors";

    public static final String ENCODING = "utf-8";
    
    private static TTransport tr = null;

    public static void main(String[] args) throws TException, InvalidRequestException, UnavailableException, UnsupportedEncodingException, NotFoundException, TimedOutException {
        Cassandra.Client client = setupConnection();

        System.out.println("Remove all the authors we might have created before.\n");
        removeAuthor(client, "Eric Long");
        removeAuthor(client, "Ronald Mathies");
        removeAuthor(client, "John Steward");

        System.out.println("Create the authors.\n");
        createAuthor(client, "Eric Long", "eric (at) long.com", "United Kingdom", "01/01/2002");
        createAuthor(client, "Ronald Mathies", "ronald (at) sodeso.nl", "Netherlands, The", "01/01/2010");
        createAuthor(client, "John Steward", "john.steward (at) somedomain.com", "Australia", "01/01/2009");

        System.out.println("Select Eric Long.\n");
        selectSingleAuthorWithAllColumns(client, "Eric Long");
        
        System.out.println("Select Ronald Mathies.\n");
        selectSingleAuthorWithAllColumns(client, "Ronald Mathies");
        
        System.out.println("Select John Steward.\n");
        selectSingleAuthorWithAllColumns(client, "John Steward");

        System.out.println("Select all authors with all columns.\n");
        selectAllAuthorsWithAllColumns(client);
        
        System.out.println("Select all authors with only the email column.\n");
        selectAllAuthorsWithOnlyTheEmailColumn(client);

        System.out.println("Update John Steward.\n");
        updateJohnStewardAuthor(client);

        System.out.println("Select John Steward.\n");
        selectSingleAuthorWithAllColumns(client, "John Steward");

        System.out.println("Remove email address and birthday from John Steward.\n");
        deleteEmailAndBirthdayFromJohnSteward(client);

        System.out.println("Select John Steward.\n");
        selectSingleAuthorWithAllColumns(client, "John Steward");

        closeConnection();
    }

    /**
     * Open up a new connection to the Cassandra Database.
     * 
     * @return the Cassandra Client
     */
    private static Cassandra.Client setupConnection() throws TTransportException {
        try {
            tr = new TSocket("localhost", 9160);
            TProtocol proto = new TBinaryProtocol(tr);
            Cassandra.Client client = new Cassandra.Client(proto);
            tr.open();

            return client;
        } catch (TTransportException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    /**
     * Close the connection to the Cassandra Database.
     */
    private static void closeConnection() {
        try {
            tr.flush();
            tr.close();
        } catch (TTransportException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Removes an Author from the Authors ColumnFamily.
     * 
     * @param client the Cassandra Client
     * @param authorKey The key of the Author
     */
    private static void removeAuthor(Cassandra.Client client, String authorKey) {
        try {
            ColumnPath columnPath = new ColumnPath(COLUMN_FAMILY);
            client.remove(KEYSPACE, authorKey, columnPath, System.currentTimeMillis(), ConsistencyLevel.ALL);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Creates and stores an Author in the Cassandra Database.
     * 
     * @param client the Cassandra Client
     * @param authorKey The key of the Author
     * @param email the email address
     * @param country the country
     * @param registeredSince the registration date
     */
    private static void createAuthor(Cassandra.Client client, String authorKey, String email, String country, String registeredSince) {
        try {
            long timestamp = System.currentTimeMillis();
            Map<String, List<ColumnOrSuperColumn>> job = new HashMap<String, List<ColumnOrSuperColumn>>();

            List<ColumnOrSuperColumn> columns = new ArrayList<ColumnOrSuperColumn>();
            Column column = new Column("email".getBytes(ENCODING), email.getBytes(ENCODING), timestamp);
            ColumnOrSuperColumn columnOrSuperColumn = new ColumnOrSuperColumn();
            columnOrSuperColumn.setColumn(column);
            columns.add(columnOrSuperColumn);

            column = new Column("country".getBytes(ENCODING), country.getBytes(ENCODING), timestamp);
            columnOrSuperColumn = new ColumnOrSuperColumn();
            columnOrSuperColumn.setColumn(column);
            columns.add(columnOrSuperColumn);

            column = new Column("country".getBytes(ENCODING), country.getBytes(ENCODING), timestamp);
            columnOrSuperColumn = new ColumnOrSuperColumn();
            columnOrSuperColumn.setColumn(column);
            columns.add(columnOrSuperColumn);

            column = new Column("registeredSince".getBytes(ENCODING), registeredSince.getBytes(ENCODING), timestamp);
            columnOrSuperColumn = new ColumnOrSuperColumn();
            columnOrSuperColumn.setColumn(column);
            columns.add(columnOrSuperColumn);

            job.put(COLUMN_FAMILY, columns);

            client.batch_insert(KEYSPACE, authorKey, job, ConsistencyLevel.ALL);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Selects a single author with all the columns from the Cassandra database
     * and display it in the console.
     * 
     * @param client the Cassandra client
     * @param authorKey The key of the Author
     */
    private static void selectSingleAuthorWithAllColumns(Cassandra.Client client, String authorKey) {
        try {
            SlicePredicate slicePredicate = new SlicePredicate();
            SliceRange sliceRange = new SliceRange();
            sliceRange.setStart(new byte[] {});
            sliceRange.setFinish(new byte[] {});
            slicePredicate.setSlice_range(sliceRange);

            ColumnParent columnParent = new ColumnParent(COLUMN_FAMILY);
            List<ColumnOrSuperColumn> result = client.get_slice(KEYSPACE, authorKey, columnParent, slicePredicate, ConsistencyLevel.ONE);

            printToConsole(authorKey, result);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Selects all the authors with all the columns from the Cassandra database.
     * 
     * @param client the Cassandra client
     */
    private static void selectAllAuthorsWithAllColumns(Cassandra.Client client) {
        try {
            KeyRange keyRange = new KeyRange(3);
            keyRange.setStart_key("");
            keyRange.setEnd_key("");

            SliceRange sliceRange = new SliceRange();
            sliceRange.setStart(new byte[] {});
            sliceRange.setFinish(new byte[] {});

            SlicePredicate slicePredicate = new SlicePredicate();
            slicePredicate.setSlice_range(sliceRange);

            ColumnParent columnParent = new ColumnParent(COLUMN_FAMILY);
            List<KeySlice> keySlices = client.get_range_slices(KEYSPACE, columnParent, slicePredicate, keyRange, ConsistencyLevel.ONE);

            for (KeySlice keySlice : keySlices) {
                printToConsole(keySlice.getKey(), keySlice.getColumns());
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Selects all the authors with only the email column from the Cassandra
     * database.
     * 
     * @param client the Cassandra client
     */
    private static void selectAllAuthorsWithOnlyTheEmailColumn(Cassandra.Client client) {
        try {
            KeyRange keyRange = new KeyRange(3);
            keyRange.setStart_key("");
            keyRange.setEnd_key("");

            List<byte[]> columns = new ArrayList<byte[]>();
            columns.add("email".getBytes(ENCODING));

            SlicePredicate slicePredicate = new SlicePredicate();
            slicePredicate.setColumn_names(columns);

            ColumnParent columnParent = new ColumnParent(COLUMN_FAMILY);
            List<KeySlice> keySlices = client.get_range_slices(KEYSPACE, columnParent, slicePredicate, keyRange, ConsistencyLevel.ONE);

            for (KeySlice keySlice : keySlices) {
                printToConsole(keySlice.getKey(), keySlice.getColumns());
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Update the John Steward author with a new email address and a new field, the birthday.
     * 
     * @param client the Cassandra client
     */
    private static void updateJohnStewardAuthor(Cassandra.Client client) {
        try {
            long timestamp = System.currentTimeMillis();

            Map<String, Map<String, List<Mutation>>> job = new HashMap<String, Map<String, List<Mutation>>>();
            List<Mutation> mutations = new ArrayList<Mutation>();
            
            // Change the email address
            Column column = new Column("email".getBytes(ENCODING), "john@steward.nl".getBytes(ENCODING), timestamp);
            ColumnOrSuperColumn columnOrSuperColumn = new ColumnOrSuperColumn();
            columnOrSuperColumn.setColumn(column);

            Mutation mutation = new Mutation();
            mutation.setColumn_or_supercolumn(columnOrSuperColumn);
            mutations.add(mutation);
            
            // Add a new column
            column = new Column("birthday".getBytes(ENCODING), "05-04-1978".getBytes(ENCODING), timestamp);
            columnOrSuperColumn = new ColumnOrSuperColumn();
            columnOrSuperColumn.setColumn(column);

            mutation = new Mutation();
            mutation.setColumn_or_supercolumn(columnOrSuperColumn);
            mutations.add(mutation);

            Map<String, List<Mutation>> mutationsForColumnFamily = new HashMap<String, List<Mutation>>();
            mutationsForColumnFamily.put(COLUMN_FAMILY, mutations);

            job.put("John Steward", mutationsForColumnFamily);

            client.batch_mutate(KEYSPACE, job, ConsistencyLevel.ALL);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Delete the email address and birthday from John Steward.
     * 
     * @param client the Cassandra client
     */
    private static void deleteEmailAndBirthdayFromJohnSteward(Cassandra.Client client) {
        try {
            long timestamp = System.currentTimeMillis();

            // The columns we want to remove
            List<byte[]> columns = new ArrayList<byte[]>();
            columns.add("email".getBytes(ENCODING));
            columns.add("birthday".getBytes(ENCODING));
    
            // Add the columns to a SlicePredicate
            SlicePredicate slicePredicate = new SlicePredicate();
            slicePredicate.setColumn_names(columns);
    
            Deletion deletion = new Deletion(timestamp);
            deletion.setPredicate(slicePredicate);
    
            Mutation mutation = new Mutation();
            mutation.setDeletion(deletion);
    
            List<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(mutation);
    
            Map<String, List<Mutation>> mutationsForColumnFamily = new HashMap<String, List<Mutation>>();
            mutationsForColumnFamily.put(COLUMN_FAMILY, mutations);
    
            Map<String, Map<String, List<Mutation>>> batch = new HashMap<String, Map<String, List<Mutation>>>();
            batch.put("John Steward", mutationsForColumnFamily);
    
            client.batch_mutate(KEYSPACE, batch, ConsistencyLevel.ALL);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Prints out the information to the console.
     * 
     * @param key the key of the Author
     * @param result the result to print out
     */
    private static void printToConsole(String key, List<ColumnOrSuperColumn> result) {
        try {
            System.out.println("Key: '" + key + "'");
            for (ColumnOrSuperColumn c : result) {
                if (c.getColumn() != null) {
                    String name = new String(c.getColumn().getName(), ENCODING);
                    String value = new String(c.getColumn().getValue(), ENCODING);
                    long timestamp = c.getColumn().getTimestamp();
                    System.out.println("  name: '" + name + "', value: '" + value + "', timestamp: " + timestamp);
                } else {
    
                }
            }
        } catch (UnsupportedEncodingException exception) {
            exception.printStackTrace();
        }
    }

}
