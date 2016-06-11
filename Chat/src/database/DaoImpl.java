package database;

import javafx.util.Pair;

import java.util.LinkedList;

public interface DaoImpl {

    boolean authorization(String login, String password);

    boolean register(String login, String password);

    void messageInsert(String login, String message);

    LinkedList<Pair<String, String>> messageChoice();
}
