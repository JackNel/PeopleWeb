import spark.ModelAndView;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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
        stmt.execute("CREATE TABLE people (id IDENTITY, first_name VARCHAR, last_name VARCHAR, email VARCHAR, country VARCHAR, ip VARCHAR)");
    }

    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./people");
        createTables(conn);


        ArrayList<Person> people = new ArrayList();

        String fileContent = readFile("people.csv");
        String[] lines = fileContent.split("\n");

        for (String line : lines) {
            if (line == lines[0])
                continue;

            String[] columns = line.split(",");
            Person person = new Person(Integer.valueOf(columns[0]), columns[1], columns[2], columns[3], columns[4], columns[5]);
            people.add(person);
        }

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
