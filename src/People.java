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
    private static final int SHOW_COUNT = 20;

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
            person.id = results.getInt("people.id");
            person.firstName = results.getString("people.first_name");
            person.lastName = results.getString("people.last_name");
            person.email = results.getString("people.email");
            person.country = results.getString("people.country");
            person.ip = results.getString("people.ip");
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

    public static ArrayList<Person> selectPeople(Connection conn) throws SQLException {
        ArrayList<Person> people = new ArrayList();
        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery("SELECT * FROM people");
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


        ArrayList<Person> people = new ArrayList();



        Spark.get(
                "/",
                ((request, response) -> {
                    String offset = request.queryParams("offset");
                    int counter;
                    if(offset == null) {
                        counter = 0;
                    }
                    else {
                        counter = Integer.valueOf(offset);
                    }
                    if (counter >= people.size()) {
                        Spark.halt(403);
                    }
                    else {
                        ArrayList<Person> tempList = new ArrayList<Person>(people.subList(
                                Math.max(0, Math.min(people.size(), counter)),
                                Math.max(0, Math.min(people.size(), counter + SHOW_COUNT))
                        ));
                        HashMap m = new HashMap();
                        m.put("people", tempList);
                        m.put("oldOffset", counter - SHOW_COUNT);
                        m.put("pagecounter", counter + SHOW_COUNT);

                        boolean showPrevious = counter > 0;
                        m.put("showPrevious", showPrevious);

                        boolean showNext = counter + SHOW_COUNT < people.size();
                        m.put("showNext", showNext);

                        return new ModelAndView(m, "people.html");
                    }

                    return new ModelAndView(new HashMap<>(), "people.html");
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
                        Person person = people.get(idNum-1);
                        m.put("person", person);
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
