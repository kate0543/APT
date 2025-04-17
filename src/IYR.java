package src;
public class IYR {
    private int year;
    private String summary;
    private String status;

    // Getters and setters
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "IYR{year=" + year + ", summary='" + summary + "', status='" + status + "'}";
    }
}