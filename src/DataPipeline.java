package src;
public class DataPipeline {
    private String pipelineName;
    private String source;
    private String destination;
    private String status;

    // Getters and setters
    public String getPipelineName() { return pipelineName; }
    public void setPipelineName(String pipelineName) { this.pipelineName = pipelineName; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "DataPipeline{pipelineName='" + pipelineName + "', source='" + source +
               "', destination='" + destination + "', status='" + status + "'}";
    }
}