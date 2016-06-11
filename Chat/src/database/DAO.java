package database;

import javafx.util.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Locale;

/**
 * This is DAO class.
 */
public class DAO implements DaoImpl{

    private Logger logger = LogManager.getLogger(getClass().getName());
    final private static String driverName = "oracle.jdbc.driver.OracleDriver";
    private String dburl = "jdbc:oracle:thin:@localhost:1521:XE";
    private String user = "SYSTEM";
    private String passwd = "cherryblossom";
    private Connection conn;

    /*
     * This is a method to connect database.
     * @return happened or not to connect to the database.
    */
    private boolean connect() {

        Locale.setDefault(Locale.ENGLISH);
        boolean isConnected = false;
        try {
            Class.forName(driverName);
            conn = DriverManager.getConnection(dburl, user, passwd);
            if(conn != null) {
                isConnected = true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.ERROR, "Error in connect", e);
        }
        return isConnected;
    }

    /*
     * This method for break the connection to the database.
    */
    private void disconnect() {
        try {
            conn.close();
        } catch (SQLException e) {
            logger.log(Level.ERROR, "Error in disconnect", e);
        }
    }

    /*
     * This method for user authentication.
     * @param login user login
     * @param password user password
     * @return happened or not to authorization.
    */
    @Override
    public boolean authorization(String login, String password) {

        boolean bol = false;
        if (connect()) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rset =
                        stmt.executeQuery("select * from Users_Chat where login = '" + login +
                                "' and password = '" + password + "'");
                if (rset.next()) {
                    bol = true;
                }
                stmt.close();
            } catch (SQLException e) {
                logger.log(Level.ERROR, "Error in Statement", e);
            }
        }
        disconnect();
        return bol;
    }

    /*
     * This method for user registration.
     * @param login user login
     * @param password user password
     * @return happened or not to register.
    */
    @Override
    public boolean register(String login, String password) {

        boolean bol = false;
        if (connect()) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rset =
                        stmt.executeQuery("select * from Users_Chat where login = '" + login + "'");
                if (!rset.next()) {
                    bol = true;
                }
                if (bol) {
                    String insertUser = "INSERT INTO Users_Chat" +
                            "(login, password)" +
                            "VALUES (\'" + login + "\', \'" + password + "\')";
                    stmt.executeUpdate(insertUser);
                }
                stmt.close();
            } catch (SQLException e) {
                logger.log(Level.ERROR, "Error in Statement", e);
            }
        }
        disconnect();
        return bol;
    }

    /*
     * This method saves the user message into the database.
     * @param login user login
     * @param message user password
    */
    @Override
    public void messageInsert(String login, String message) {

        if (connect()) {
            try (Statement stmt = conn.createStatement()) {
                Date date = new Date(System.currentTimeMillis());
                SimpleDateFormat format1 = new SimpleDateFormat("HH:mm:ss dd.MM.yy");
                String insertMessage = "INSERT INTO Message_Chat" +
                        "(login, message_text, message_date)" +
                        "VALUES (\'" + login + "\', \'" + message
                        + "\', TO_TIMESTAMP(\'"+format1.format(date)+"\', 'HH24:mi:ss dd.MM.yy'))";
                stmt.executeUpdate(insertMessage);
                stmt.close();
            } catch (SQLException e) {
                logger.log(Level.ERROR, "Error in Statement", e);
            }
        }
        disconnect();
    }

    /*
     * This method gets the 10 most recent messages from the database.
     * @return list of messages
    */
    @Override
    public LinkedList<Pair<String, String>> messageChoice() {

        LinkedList<Pair<String, String>> message = new LinkedList<>();
        if (connect()) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rset =
                        stmt.executeQuery("select login, MESSAGE_TEXT, " +
                                "TO_CHAR(message_date, 'HH24:mi:ss dd.MM.yy') cmt " +
                                "from (select login, message_text, message_date " +
                                "from Message_Chat ORDER BY message_date DESC) ss " +
                                "where rownum <= 10 order by message_date");
                while(rset.next()) {
                    message.add(new Pair<>(rset.getString("login") + "%%'#" +
                            rset.getString("message_text"), rset.getString("cmt")));
                }
                stmt.close();
            } catch (SQLException e) {
                logger.log(Level.ERROR, "Error in Statement", e);
            }
        }
        disconnect();
        return message;
    }

}
