package sqlinjection.tododb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sqlinjection.insecuredb.DbApi;


import java.sql.SQLException;
import java.util.List;

class CheckDBSetupTest {

    static DbApi myDB;

    @BeforeAll
    static void setupDB(){
        myDB = new DbApi(new MyDB().create());
    }

    /*
        This is not an SQL Injection this is a simple standard query
        to check that the SQL still matches and the data is roughly as expected
     */
    @Test
    void expectedDoneTodos() throws SQLException {

        List<String> donedescriptions = myDB.getTodosOfStatus("1");

        Assertions.assertEquals(1, donedescriptions.size());
        Assertions.assertEquals("I did that activity", donedescriptions.get(0));

    }

    @Test
    void expectedToDoTodos() throws SQLException {

        List<String> tododescriptions = myDB.getTodosOfStatus("0");

        Assertions.assertEquals(2, tododescriptions.size());
        Assertions.assertEquals("Do this activity", tododescriptions.get(0));

    }

    @Test
    void aUsernameAndPasswordCanLogin() throws SQLException {

        List<String> usernames = myDB.getUserLoggedIn("bob", "dobbs");
        Assertions.assertEquals(1, usernames.size());
        Assertions.assertEquals("bob", usernames.get(0));
    }

    @Test
    void expectedUsersAvailable() throws SQLException {

        List<String> usernames = myDB.getUsernames("%");
        Assertions.assertEquals(2, usernames.size());
        Assertions.assertEquals("admin", usernames.get(0));
        Assertions.assertEquals("bob", usernames.get(1));
    }
}
