package pmel.sdig.las.shared.autobean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rhs on 9/19/15.
 */
public class LASRequest {

    long id;

    int targetPanel;

    String operation;
    List<String> variableHashes;
    List<String> datasetHashes;
    List<Analysis> analysis;
    List<AxesSet> axesSets = new ArrayList<>();

    List<Constraint> constraints;

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

    public List<AxesSet> getAxesSets() {
        return axesSets;
    }

    public void setAxesSets(List<AxesSet> axesSets) {
        this.axesSets = axesSets;
    }

    public void addProperty(RequestProperty requestProperty) {
        if ( requestProperties == null ) {
            requestProperties = new ArrayList<>();
        }
        requestProperties.add(requestProperty);
    }
    public List<Constraint> getConstraints() {
        return constraints;
    }
    public void setConstraints(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    public void addConstraint(Constraint constraint) {
        if ( constraints == null ) {
            constraints = new ArrayList<>();
        }
        constraints.add(constraint);
    }
}

