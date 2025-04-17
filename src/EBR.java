package src;
public class EBR {
    private String reportName;
    private String reportType;
    private String generatedDate;

    // Getters and setters
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(String generatedDate) { this.generatedDate = generatedDate; }

    @Override
    public String toString() {
        return "EBR{reportName='" + reportName + "', reportType='" + reportType +
               "', generatedDate='" + generatedDate + "'}";
    }
}