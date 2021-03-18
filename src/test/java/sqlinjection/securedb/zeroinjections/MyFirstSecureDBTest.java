package sqlinjection.securedb.zeroinjections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sqlinjection.secureddb.SecureDbApi;
import sqlinjection.tododb.MyDB;

import java.sql.SQLException;
import java.util.List;

class MyFirstSecureDBTest {

    // the secure DB API should not be vulnerable
    // to the same injections as before
    static SecureDbApi myDB;

    @BeforeAll
    static void setupDB(){
        myDB = new SecureDbApi(new MyDB().create());
    }


    @Test
    void myFirstSQLInjection() throws SQLException {

        String doneStatus = "-1 UNION SELECT name || '~' || password as description from users";

        List<String> descriptions = myDB.getTodosOfStatus(doneStatus);

        Assertions.assertEquals(0, descriptions.size(),
                        "should return 0 records when no injection possible");
    }
}