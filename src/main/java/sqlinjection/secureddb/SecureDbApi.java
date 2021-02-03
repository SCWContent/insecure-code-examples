package sqlinjection.secureddb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SecureDbApi {

    private final Connection dbConnection;
    Logger logger = Logger.getLogger(SecureDbApi.class.getName());

    public SecureDbApi(Connection connection) {
        this.dbConnection = connection;
    }

    public List<String> getTodosOfStatus(String status) {

        List<String> todos = new ArrayList<>();

        PreparedStatement pstm=null;

        try {
            pstm = dbConnection.prepareStatement("SELECT description from todos where status= ?");
            pstm.setString(1, status);
            ResultSet res = pstm.executeQuery();

            while(res.next()){
                todos.add(res.getString("description"));
            }

            pstm.close();

        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage());
        }finally{
            closeStatement(pstm);
        }

        return todos;
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

    public List<String> getUserNotesForName(String name) {

        List<String> notes = new ArrayList<>();

        PreparedStatement pstm=null;

        try {
            String sql = "SELECT notes from users where name= ?";

            pstm = dbConnection.prepareStatement(sql);
            pstm.setString(1, name);
            ResultSet res = pstm.executeQuery();

            while(res.next()){
                notes.add(res.getString("notes"));
            }

            pstm.close();

        }catch(Exception e){
            logger.log(Level.SEVERE, e.getMessage());
        }finally{
            closeStatement(pstm);
        }

        return notes;
    }
}
