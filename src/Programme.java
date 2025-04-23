package src;

/**
 * Represents a Programme with details such as code, title, year, school, and term.
 */
public class Programme {
    private String programmeCode; // Code of the programme
    private String programmeTitle; // Title of the programme
    private int programmeYear;    // Year of the programme
    private String programmeSchool; // School offering the programme
    private String programmeTerm; // Term of the programme

    // Empty constructor
    public Programme() {
    }

    // Non-empty constructor
    public Programme(String programmeCode, String programmeTitle, int programmeYear, String programmeSchool, String programmeTerm) {
        this.programmeCode = programmeCode;
        this.programmeTitle = programmeTitle;
        this.programmeYear = programmeYear;
        this.programmeSchool = programmeSchool;
        this.programmeTerm = programmeTerm;
    }

    // Getters and Setters
    public String getProgrammeCode() {
        return programmeCode;
    }

    public void setProgrammeCode(String programmeCode) {
        this.programmeCode = programmeCode;
    }

    public String getProgrammeTitle() {
        return programmeTitle;
    }

    public void setProgrammeTitle(String programmeTitle) {
        this.programmeTitle = programmeTitle;
    }

    public int getProgrammeYear() {
        return programmeYear;
    }

    public void setProgrammeYear(int programmeYear) {
        this.programmeYear = programmeYear;
    }

    public String getProgrammeSchool() {
        return programmeSchool;
    }

    public void setProgrammeSchool(String programmeSchool) {
        this.programmeSchool = programmeSchool;
    }

    public String getProgrammeTerm() {
        return programmeTerm;
    }

    public void setProgrammeTerm(String programmeTerm) {
        this.programmeTerm = programmeTerm;
    }

    // toString method
    @Override
    public String toString() {
        return "Programme{" +
                "programmeCode='" + programmeCode + '\'' +
                ", programmeTitle='" + programmeTitle + '\'' +
                ", programmeYear=" + programmeYear +
                ", programmeSchool='" + programmeSchool + '\'' +
                ", programmeTerm='" + programmeTerm + '\'' +
                '}';
    }
}