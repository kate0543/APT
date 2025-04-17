package src;
public class Report {
    private String reportName;
    private String reportType;
    private String generatedDate;
    private String content;

    // Getters and setters
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(String generatedDate) { this.generatedDate = generatedDate; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Override
    public String toString() {
        return "Report{reportName='" + reportName + "', reportType='" + reportType +
               "', generatedDate='" + generatedDate + "', content='" + content + "'}";
    }
}