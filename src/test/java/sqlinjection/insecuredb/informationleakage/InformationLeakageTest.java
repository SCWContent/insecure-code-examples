package sqlinjection.insecuredb.informationleakage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sqlinjection.insecuredb.DbApi;
import sqlinjection.tododb.MyDB;


import java.sql.SQLException;
import java.util.List;

class InformationLeakageTest {

    static DbApi myDB;

    @BeforeAll
    static void setupDB(){
        myDB = new DbApi(new MyDB().create());
    }


    /*
        When the app itself leaks information when an sql injection happens
        then it makes it a lot easier for the attacker to gain information
        about the database.

        e.g. what is the format of the query to then create a valid query that they
        can build on to get information or bypass functionality.

        The exception thrown when a syntax error in the sql statement is found
        reveals the database type and the sql statement
     */

    @Test
    void exceptionExposesInformation() throws SQLException {

        Assertions.assertThrows(RuntimeException.class, ()-> {
            List<String> usernames = myDB.getUsernames("' a --");
        });
    }

}
