import org.h2.command.Prepared;
import spark.ModelAndView;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by zach on 10/19/15.
 */
public class People {
    public static final int SHOW_COUNT = 20;

    public static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("DROP TABLE IF EXISTS people");
        stmt.execute("CREATE TABLE IF NOT EXISTS people (id IDENTITY, first_name VARCHAR, last_name VARCHAR, email VARCHAR, country VARCHAR, ip VARCHAR)");
    }

    public static void insertPerson(Connection conn, String firstName, String lastName, String email, String country, String ip) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO people VALUES (NULL, ?, ?, ?, ?, ?)");
        stmt.setString(1, firstName);
        stmt.setString(2, lastName);
        stmt.setString(3, email);
        stmt.setString(4, country);
        stmt.setString(5, ip);
        stmt.execute();
    }

    public static Person selectPerson(Connection conn, int id) throws SQLException {
        Person person = new Person();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM people WHERE id = ?");
        stmt.setInt(1, id);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            person = new Person();
            person.id = results.getInt("id");
            person.firstName = results.getString("first_name");
            person.lastName = results.getString("last_name");
            person.email = results.getString("email");
            person.country = results.getString("country");
            person.ip = results.getString("ip");
        }
        return person;
    }

    public static void populateDatabase(Connection conn) throws SQLException {
        String fileContent = readFile("people.csv");
        String[] lines = fileContent.split("\n");

        for (String line : lines) {
            if (line == lines[0])
                continue;
            String[] columns = line.split(",");
            Person person = new Person(Integer.valueOf(columns[0]), columns[1], columns[2], columns[3], columns[4], columns[5]);
            insertPerson(conn, person.firstName, person.lastName, person.email, person.country, person.ip);
        }
    }

    public static ArrayList<Person> selectPeople(Connection conn, int offset) throws SQLException {
        ArrayList<Person> people = new ArrayList();
        String query = String.format("SELECT * FROM people LIMIT ? OFFSET ?");
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, SHOW_COUNT);
        stmt.setInt(2, offset);
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            Person person = new Person();
            person.id = results.getInt("id");
            person.firstName = results.getString("first_name");
            person.lastName = results.getString("last_name");
            person.email = results.getString("email");
            person.country = results.getString("country");
            person.ip = results.getString("ip");
            people.add(person);
        }
        return people;
    }

    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./people");
        createTables(conn);
        populateDatabase(conn);

        Spark.get(
                "/",
                ((request, response) -> {
                    String offset = request.queryParams("offset");
                    int offsetNum;
                    if(offset == null) {
                        offsetNum = 0;
                    }
                    else {
                        offsetNum = Integer.valueOf(offset);
                    }
                    selectPeople(conn, offsetNum);

                    HashMap m = new HashMap();
                    m.put("people", selectPeople(conn, offsetNum));
                    m.put("previousLink", offsetNum - SHOW_COUNT);
                    m.put("nextLink", offsetNum + SHOW_COUNT);

                    boolean showPrevious = offsetNum > SHOW_COUNT;
                    m.put("showPrevious", showPrevious);

                    boolean showNext = (offsetNum - SHOW_COUNT < selectPeople(conn, offsetNum).size());
                    m.put("showNext", showNext);

                    return new ModelAndView(m, "people.html");

                }),
                new MustacheTemplateEngine()
        );
        Spark.get(
                "/person",
                ((request, response) -> {
                    HashMap m = new HashMap();
                    String id = request.queryParams("id");
                    try {
                        int idNum = Integer.valueOf(id);
                        m.put("person", selectPerson(conn, idNum));
                    } catch (Exception e) {

                    }
                    return new ModelAndView(m, "person.html");
                }),
                new MustacheTemplateEngine()
        );
    }

    static String readFile(String fileName) {
        File f = new File(fileName);
        try {
            FileReader fr = new FileReader(f);
            int fileSize = (int) f.length();
            char[] fileContent = new char[fileSize];
            fr.read(fileContent);
            return new String(fileContent);
        } catch (Exception e) {
            return null;
        }

    }
}
