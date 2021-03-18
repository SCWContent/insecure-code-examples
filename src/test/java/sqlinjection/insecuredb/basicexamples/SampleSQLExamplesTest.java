package sqlinjection.insecuredb.basicexamples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sqlinjection.insecuredb.DbApi;
import sqlinjection.tododb.MyDB;

import java.util.List;
import java.util.Map;

class SampleSQLExamplesTest {

    /* basic examples to show principles of SQL */

    static DbApi myDB;

    @BeforeAll
    static void setupDB(){
        myDB = new DbApi(new MyDB().create());
    }

    @Test
    void columnConstants(){

        // SQL can have constants returned to find out number of rows if necessary,
        // but unlikely to be able to 'inject' into the columns declaration
        String query = "SELECT 1, 'string2' FROM users";
        List<Map<String, Object>> res = myDB.executeQuery(query);

        myDB.printMap(res);

        Assertions.assertEquals(2, res.size());
    }

    @Test
    void testingForSQLInjectionWithAnException(){

        String inject = "'";
        // Initial SQL injection is likely to be into a WHERE clause to trigger an exception
        String query = String.format("SELECT * FROM users WHERE name='%s'", inject);

        Assertions.assertThrows(RuntimeException.class, ()->{
            myDB.executeQuery(query);
        });
    }

// h2 does not support sleep
//    @Test
//    void testingForInjectionWithoutException(){
//
//        // we might not see an exception, we might just see a 'difference'
//        // to check if we use it we might 'slow' the query down
//        String inject = "' AND sleep(2) -- ";
//        String query = String.format("SELECT * FROM users WHERE name='%s'", inject);
//
//        Assertions.assertThrows(RuntimeException.class, ()->{
//            myDB.executeQuery(query);
//        });
//    }

    // UNION is often used to identify column count and type
    // when error messages are exposed through information leakage
    // each error message takes us one step towards identifying the column structre of users
    // note: this does not reveal the names of the columns or what they contain,
    //       but the structure is important when creating UNION queries to expose information
    //       from other tables
    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM users UNION SELECT 1 from users", // reveals more than 1 column
            "SELECT * FROM users UNION SELECT 1, 2 from users", // reveals more than 2 columns
            "SELECT * FROM users UNION SELECT 1, 2, 3 from users", // reveals more than 2 columns
            "SELECT * FROM users UNION SELECT 1, 2, 3, 4 from users", // reveals one of the columns is a string
            "SELECT * FROM users UNION SELECT '1', 2, 3, 4 from users", // but not that one
            "SELECT * FROM users UNION SELECT 1, '2', 3, 4 from users", // found one that is, revealed another one
            "SELECT * FROM users UNION SELECT 1, '2', '3', 4 from users", // and revealed the last one is a string
            })
    void columnConstantsInUnionForBlindSQLExploration(String query){

        Assertions.assertThrows(RuntimeException.class, ()-> {
            myDB.executeQuery(query);
        });
    }

    @Test
    void columnConstantsInUnionFound(){

        // Query revealed from blind SQL union injection above
        String query = "SELECT * FROM users UNION SELECT 1, '2', '3', '4' from todos";
        List<Map<String, Object>> res = myDB.executeQuery(query);

        myDB.printMap(res);

        // 1 extra row due to the Union
        Assertions.assertEquals(3, res.size());
    }


}
