package javahibernate;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/* Table:
 * CREATE TABLE Users
  (
    id       NUMBER PRIMARY KEY,
    login    VARCHAR2(30) NOT NULL,
    password VARCHAR2(20) NOT NULL
  );
*/

/**
 * Users - class entity, which will stored in a the database
 */
@Entity
@Table(name="Users")
public class Users implements Serializable {
    
    protected Long id;    
    protected String login;
    protected String password;
    
    public Users(){ login = null; }
    
    @Id
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    @Column(name="id")
    public Long getId() {
        return id;
    }
    
    @Column(name="login")
    public String getLogin() {
        return login;
    }
    
    @Column(name="password")
    public String getPassword() {
        return password;
    }
    
    public void setId(Long i) {
        id = i;     
    }
    
    public void setLogin(String s) {
        login = s;
    }

    public void setPassword(String s) {
        password = s;
    }
}
