package src;
import java.util.ArrayList;
import java.util.List;

public class PriorityGroup {
    private String groupName;
    private List<Student> students; // List of students in the priority group

    public PriorityGroup() {
        this.students = new ArrayList<>();
    }

    // Getters and setters
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public List<Student> getStudents() { return students; }
    public void addStudent(Student student) { this.students.add(student); }

    @Override
    public String toString() {
        return "PriorityGroup{groupName='" + groupName + "', students=" + students + "}";
    }
}