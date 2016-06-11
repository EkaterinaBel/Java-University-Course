package chat;

import database.DAO;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/chat")
public class AuthorizationAndRegister {

    public static class JsonData {
        public String login;
        public String password;
        public String message;
    }

    /**
     * This method for user authentication.
     * @param input jsonObject
     * @return jsonObject
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("auth")
    public JsonData authorize(JsonData input){

        DAO db = new DAO();
        if (db.authorization(input.login, input.password)) {
            input.message = "authOK";
        } else {
            input.message = "Неверный логин или пароль";
        }
        return input;
    }

    /**
     * This method for user registration.
     * @param input jsonObject
     * @return jsonObject
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("reg")
    public JsonData register(JsonData input){

        DAO db = new DAO();
        if (db.register(input.login, input.password)) {
            input.message = "regOK";
        } else {
            input.message = "Логин занят";
        }
        return input;
    }

}
