package src;

import java.util.List;
import java.util.ArrayList;

public class Student {
    // Fields
    private int id; // Student ID
    private String name; // Student name
    private int programmeYear; // Year of the programme the student is in
    private String programme; // Programme name
    private String registrationStatus; // Registration status of the student
    private String newOrContinue; // Indicates if the student is new or continuing
    private boolean isTrailing; // Indicates if the student is trailing
    private List<Module> modules; // List of modules the student is registered for

    // No-argument constructor
    public Student() {
        this.modules = new ArrayList<>();
    }

    // Parameterized constructor
    public Student(String id, String name, String programme, String registrationStatus) {
        this.id = Integer.parseInt(id); // Assuming id is converted to int
        this.name = name;
        this.programme = programme;
        this.registrationStatus = registrationStatus;
        this.modules = new ArrayList<>();
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(String id) {
        this.id = Integer.parseInt(id); // Overloaded setter for String input
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getProgrammeYear() {
        return programmeYear;
    }

    public void setProgrammeYear(int programmeYear) {
        this.programmeYear = programmeYear;
    }

    public String getProgramme() {
        return programme;
    }

    public void setProgramme(String programme) {
        this.programme = programme;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getNewOrContinue() {
        return newOrContinue;
    }

    public void setNewOrContinue(String newOrContinue) {
        this.newOrContinue = newOrContinue;
    }

    public boolean isTrailing() {
        return isTrailing;
    }

    public void setTrailing(boolean isTrailing) {
        this.isTrailing = isTrailing;
    }

    public List<Module> getModules() {
        return modules;
    }
    public void setModules(List<Module> modules) {
        this.modules = modules; // Setter for modules list
     }
    public void addModule(Module module) {
        this.modules.add(module);
    }

    // Overriding toString method for better object representation
    @Override
    public String toString() {
        return "Student{id=" + id + ", name='" + name + "', programmeYear=" + programmeYear +
               ", programme='" + programme + "', registrationStatus='" + registrationStatus +
               "', newOrContinue='" + newOrContinue + "', modules=" + modules + "}";
    }
}