package sqlinjection.insecuredb;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbApi {

    private final Connection dbConnection;
    Logger logger = Logger.getLogger(DbApi.class.getName());

    public DbApi(Connection connection) {
        this.dbConnection = connection;
    }


    /*
        Adding this probably seemed like a good idea to someone
        to cut down on the code in the rest of the app, but really
        it just spread an SQL Injection vulnerability to more places
        in the app.
     */
    public List<Map<String, Object>> executeQuery(String query) {


        List<Map<String, Object>> resultData;

        Statement stm=null;

        try {
            String sqlquery = String.format(query);
            stm = dbConnection.createStatement();

            System.out.println("executing: " + sqlquery);
            ResultSet res = stm.executeQuery(sqlquery);
            resultData = resultSetAsList(res);

        }catch(Exception e){
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage());
            throw new RuntimeException(e);
        }finally{
            closeStatement(stm);
        }

        return resultData;
    }


    /*
        The status field should not be  String, it is an int.
        Converting this to an int would remove the SQL intjection problem in this method
        but if we did not also change the executeQuery to use a PreparedStatement then
        we might leave ourselves vulnerable to another programmer changing this to
        a String in the future and adding the vulnerability back into our code.

        int might seem like an easy fix. But we should really make the full change.
     */
    public List<String> getTodosOfStatus(String status) {

        List<String> todos = new ArrayList<>();

        Statement stm=null;

        try {
            stm = dbConnection.createStatement();
            String sqlquery = "SELECT description from todos where status=" + status;
            System.out.println("executing: " + sqlquery);
            ResultSet res = stm.executeQuery(sqlquery);

            while(res.next()){
                todos.add(res.getString("description"));
            }

        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage());
        }finally {
            closeStatement(stm);
        }

        return todos;
    }

    public List<String> getUserLoggedIn(String name, String password) {

        List<String> users = new ArrayList<>();

        Statement stm=null;

        try {
            stm = dbConnection.createStatement();
            String sqlquery = String.format("SELECT name from users where name='%s' and password='%s'",name, password);
            System.out.println("executing: " + sqlquery);
            ResultSet res = stm.executeQuery(sqlquery);

            while(res.next()){
                users.add(res.getString("name"));
            }

        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage());
        }finally {
            closeStatement(stm);
        }

        // if list is empty then no valid user matches so invalid username password combo
        return users;
    }

    public void closeStatement(Statement stm){
        if(stm!=null) {
            try {
                stm.close();
            }catch(SQLException e){
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
    }

    // this might reveal information to the hacker if exceptions exposed externally
    public List<String> getUsernames(String namePartial) {

        List<String> usernames = new ArrayList<>();

        Statement stm=null;

        try {
            stm = dbConnection.createStatement();
            String sqlquery = String.format("SELECT name from users where name like '%s'", namePartial);
            System.out.println("executing: " + sqlquery);
            ResultSet res = stm.executeQuery(sqlquery);

            while(res.next()){
                usernames.add(res.getString("name"));
            }

        }catch(Exception e){
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage());
            throw new RuntimeException(e);
        }finally{
            closeStatement(stm);
        }

        return usernames;
    }

    public List<Map<String, Object>> getTodoDetails(String todoPartial) {
        List<Map<String, Object>> userdetails = new ArrayList<>();

        Statement stm=null;

        try {
            stm = dbConnection.createStatement();
            String sqlquery = String.format("SELECT * from todos where description like '%s'", todoPartial);
            System.out.println("executing: " + sqlquery);
            ResultSet res = stm.executeQuery(sqlquery);

            userdetails = resultSetAsList(res);

        }catch(Exception e){
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage());
            throw new RuntimeException(e);
        }finally{
            closeStatement(stm);
        }

        return userdetails;
    }

    public List<Map<String, Object>> getUserDetails(String namePartial) {
        List<Map<String, Object>> userdetails = new ArrayList<>();

        Statement stm=null;

        try {
            stm = dbConnection.createStatement();
            String sqlquery = String.format("SELECT * from users where name='%s'", namePartial);
            System.out.println("executing: " + sqlquery);
            ResultSet res = stm.executeQuery(sqlquery);

            userdetails = resultSetAsList(res);

        }catch(Exception e){
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage());
            throw new RuntimeException(e);
        }finally{
            closeStatement(stm);
        }

        return userdetails;
    }

    public List<Map<String, Object>> resultSetAsList(ResultSet res){

        ArrayList retlist = new ArrayList();

        try {
            ResultSetMetaData metadata = res.getMetaData();
            int columns = metadata.getColumnCount();


            while (res != null && res.next()) {

                Map<String, Object> row = new HashMap();

                for (int index = 1; index <= columns; ++index) {
                    row.put(metadata.getColumnName(index),
                            res.getObject(index));
                }

                retlist.add(row);
            }
        }catch(SQLException e){
            logger.log(Level.SEVERE, e.getMessage());
        }

        return retlist;
    }

    public void printMap(List<Map<String, Object>> details) {
        for(Map detail : details){
            System.out.println("---");
            for( Map.Entry<String,Object> d : (Set<Map.Entry<String,Object>>) detail.entrySet()){
                if(d.getValue() instanceof Integer){
                    System.out.println(String.format("%s : %d", d.getKey(), (Integer)d.getValue()));
                }else{
                    System.out.println(String.format("%s : %s", d.getKey(), (String)d.getValue()));
                }
            }
        }
    }
}
