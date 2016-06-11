package chat;

import database.DAO;

import javafx.util.Pair;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class of server, which is a chat.
 */
@ServerEndpoint("/chatroomServerEndpoint")
public class ChatroomServerEndpoint{

    static Set<Session> chatroomUsers = Collections.synchronizedSet(new HashSet<Session>());
    static List<String> onlineUsers = Collections.synchronizedList(new ArrayList<>());
    private DAO db = new DAO();

    /**
     * This method saves each websocket user session.
     * @param userSession websocket session
     */
    @OnOpen
    public void handleOpen(Session userSession) {

        chatroomUsers.add(userSession);
    }

    /**
     * This is a method that receives a message from one user and then sends it to all online users.
     * @param message user message
     * @param userSession websocket session
     * @throws IOException linked to input-output information from the websocket sessions
     */
    @OnMessage
    public void handleMessage(String message, Session userSession) throws IOException {

        String username = (String) userSession.getUserProperties().get("username");
        if (username == null) {
            if (!onlineUsers.contains(message)) {
                onlineUsers.add(message);
            }
            userSession.getUserProperties().put("username", message);
            userSession.getBasicRemote().sendText(getMessageFromDatabase());
            Iterator<Session> iterator = chatroomUsers.iterator();
            while (iterator.hasNext()) {
                iterator.next().getBasicRemote().sendText(getListOnlineUsers());
            }
        } else {
            if (message != null && !message.equals("") && !message.equals("log out")) {
                db.messageInsert(userSession.getUserProperties().get("username").toString(), message);
            }
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat format1 = new SimpleDateFormat("HH:mm:ss dd.MM.yy");
            if (message != null && message.equals("log out")) {
                onlineUsers.remove(username);
                Iterator<Session> iterator2 = chatroomUsers.iterator();
                while (iterator2.hasNext()) {
                    iterator2.next().getBasicRemote().sendText(getListOnlineUsers());
                }
            }
            Iterator<Session> iterator = chatroomUsers.iterator();
            while (iterator.hasNext()) {
                iterator.next().getBasicRemote().sendText(getMessageJson(username + "%%'#" +
                        message, format1.format(date)));
            }
        }
    }

    /**
     * This is a method that receives a message from the database and makes out them in a json string.
     * @return json string
     */
    private String getMessageFromDatabase() {

        LinkedList<Pair<String, String>> mes = new LinkedList<>();
        mes.addAll(db.messageChoice());
        StringBuilder jsonString = new StringBuilder();
        jsonString.append("{\"messageFromBD\":[");
        for (Pair<String, String> o: mes) {
            jsonString.append("{\"" + o.getKey() + "\":\"" + o.getValue() + "\"},");
        }
        jsonString.deleteCharAt(jsonString.length() - 1);
        jsonString.append("]}");
        return jsonString.toString();
    }

    /**
     * This is a method that receives the list of users online and makes out them in a json string.
     * @return json string
     */
    private String getListOnlineUsers() {

        StringBuilder jsonStringUsers = new StringBuilder();
        jsonStringUsers.append("{\"userOnline\":[");
        for(int i = 0; i < onlineUsers.size(); i++) {
            jsonStringUsers.append("{\"userOnline_%@\":\"" + onlineUsers.get(i) + "\"},");
        }
        jsonStringUsers.deleteCharAt(jsonStringUsers.length() - 1);
        jsonStringUsers.append("]}");
        return jsonStringUsers.toString();
    }

    /**
     * This is a method that receives a user message and makes out it to a json string.
     * @param username user login
     * @param message user message
     * @return json string
     */
    private String getMessageJson(String username, String message) {

        StringBuilder jsonStringMessage = new StringBuilder();
        jsonStringMessage.append("{\"messageUser\":[");
        jsonStringMessage.append("{\"" + username + "\":\"" + message + "\"}");
        jsonStringMessage.append("]}");
        return jsonStringMessage.toString();
    }

    /**
     * This method to close the connection with the user (called when the user log out the chat).
     * @param userSession websocket session
     */
    @OnClose
    public void handleClose(Session userSession) {

        chatroomUsers.remove(userSession);
    }

}
