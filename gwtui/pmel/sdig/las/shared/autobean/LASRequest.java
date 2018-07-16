package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Created by rhs on 9/19/15.
 */
public class LASRequest {

    long id;

    int targetPanel;

    AxesSet axesSet1 = new AxesSet();
    AxesSet axesSet2 = new AxesSet();
    // Will there ever be more than 4 unique axes sets?

    String operation;
    List<String> variableHashes;
    List<String> datasetHashes;
    List<Analysis> analysis;

    public List<Analysis> getAnalysis() {
        return analysis;
    }

    public void setAnalysis(List<Analysis> analysis) {
        this.analysis = analysis;
    }

    List<RequestProperty> requestProperties;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getTargetPanel() {
        return targetPanel;
    }

    public void setTargetPanel(int targetPanel) {
        this.targetPanel = targetPanel;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public List<String> getVariableHashes() {
        return variableHashes;
    }

    public void setVariableHashes(List<String> variableHashes) {
        this.variableHashes = variableHashes;
    }

    public List<String> getDatasetHashes() {
        return datasetHashes;
    }

    public void setDatasetHashes(List<String> datasetHashes) {
        this.datasetHashes = datasetHashes;
    }

    public List<RequestProperty> getRequestProperties() {
        return requestProperties;
    }

    public void setRequestProperties(List<RequestProperty> requestProperties) {
        this.requestProperties = requestProperties;
    }

    public AxesSet getAxesSet1() {
        return axesSet1;
    }

    public void setAxesSet1(AxesSet axesSet1) {
        this.axesSet1 = axesSet1;
    }

    public AxesSet getAxesSet2() {
        return axesSet2;
    }

    public void setAxesSet2(AxesSet axesSet2) {
        this.axesSet2 = axesSet2;
    }
}

