package src;
public class Qlikview {
    private String dashboardName;
    private String dataSource;
    private String lastUpdated;

    // Getters and setters
    public String getDashboardName() { return dashboardName; }
    public void setDashboardName(String dashboardName) { this.dashboardName = dashboardName; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    @Override
    public String toString() {
        return "Qlikview{dashboardName='" + dashboardName + "', dataSource='" + dataSource +
               "', lastUpdated='" + lastUpdated + "'}";
    }
}