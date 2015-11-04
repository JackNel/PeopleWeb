import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by Jack on 11/4/15.
 */
public class PeopleTest {
    public Connection startConnection() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./test");
        People.createTables(conn);
        return conn;
    }

    public void endConnection(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS people");
        conn.close();
    }

    @Test
    public void testPerson() throws SQLException {
        Connection conn = startConnection();
        People.insertPerson(conn, "Jack", "Neligan", "Jack@Jack.com", "USA", "12345");
        Person person = People.selectPerson(conn, 1);
        endConnection(conn);

        assertTrue(person != null);
    }

    @Test
    public void testPeople() throws SQLException {
        Connection conn = startConnection();
        People.insertPerson(conn, "Jack", "Neligan", "Jack@Jack.com", "USA", "12345");
        People.insertPerson(conn, "Katie", "Neligan", "Katie@Katie.com", "USA", "67890");
        ArrayList<Person> people = People.selectPeople(conn);
        endConnection(conn);

        assertTrue(people.size() == 2);
    }
}