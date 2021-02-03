package sqlinjection.tododb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyDB {

    Logger logger = Logger.getLogger(MyDB.class.getName());

    private String url = "jdbc:h2:mem:";

    Connection dbConnection;


    public MyDB(){

        try {
            dbConnection = DriverManager.getConnection(url);

        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage());
        }
    }


    public Connection create() {

        try {

            Statement stm = dbConnection.createStatement();

            // 1 for done, 0 for not done yet
            stm.execute("CREATE TABLE todos(id INT PRIMARY KEY AUTO_INCREMENT, description VARCHAR(255), status INT);");
            stm.execute("INSERT INTO todos(description, status) VALUES('Do this activity', 0);");
            stm.execute("INSERT INTO todos(description, status) VALUES('I did that activity', 1);");
            stm.execute("INSERT INTO todos(description, status) VALUES('My third todo', 0);");
            stm.execute("CREATE TABLE users(id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), password VARCHAR(255), notes VARCHAR(255));");
            stm.execute("INSERT INTO users(name, password, notes) VALUES('admin', 'root', 'the admin');");
            stm.execute("INSERT INTO users(name, password, notes) VALUES('bob', 'dobbs', 'the user');");

            stm.close();

        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage());
        }

        return dbConnection;
    }

}
