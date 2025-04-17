package src;

import java.util.ArrayList;
import java.util.List;

public class Programme {
    private String programmeCode; // Code of the programme
    private String programmeName; // Name of the programme
    private int programmeYear;    // Year of the programme
    private List<Student> students; // List of students in the programme

    // Default constructor
    public Programme() {
        this.students = new ArrayList<>();
    }

    // Getters and setters
    public String getProgrammeCode() {
        return programmeCode;
    }

    public void setProgrammeCode(String programmeCode) {
        this.programmeCode = programmeCode;
    }

    public String getProgrammeName() {
        return programmeName;
    }

    public void setProgrammeName(String programmeName) {
        this.programmeName = programmeName;
    }

    public int getProgrammeYear() {
        return programmeYear;
    }

    public void setProgrammeYear(int programmeYear) {
        this.programmeYear = programmeYear;
    }

    public List<Student> getStudents() {
        return students;
    }

    // Add a student to the programme
    public void addStudent(Student student) {
        this.students.add(student);
    }

    // Override toString method for better representation
    @Override
    public String toString() {
        return "Programme{programmeCode='" + programmeCode + "', programmeName='" + programmeName +
               "', programmeYear=" + programmeYear + ", students=" + students + "}";
    }
}