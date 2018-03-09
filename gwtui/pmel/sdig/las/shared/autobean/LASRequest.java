package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Created by rhs on 9/19/15.
 */
public class LASRequest {

    long id;

    int targetPanel;

    String xlo;
    String xhi;
    String ylo;
    String yhi;
    String zlo;
    String zhi;
    String tlo;
    String thi;
    String operation;
    List<String> variableHashes;
    List<String> datasetHashes;

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

    public String getXlo() {
        return xlo;
    }

    public void setXlo(String xlo) {
        this.xlo = xlo;
    }

    public String getXhi() {
        return xhi;
    }

    public void setXhi(String xhi) {
        this.xhi = xhi;
    }

    public String getYlo() {
        return ylo;
    }

    public void setYlo(String ylo) {
        this.ylo = ylo;
    }

    public String getYhi() {
        return yhi;
    }

    public void setYhi(String yhi) {
        this.yhi = yhi;
    }

    public String getZlo() {
        return zlo;
    }

    public void setZlo(String zlo) {
        this.zlo = zlo;
    }

    public String getZhi() {
        return zhi;
    }

    public void setZhi(String zhi) {
        this.zhi = zhi;
    }

    public String getTlo() {
        return tlo;
    }

    public void setTlo(String tlo) {
        this.tlo = tlo;
    }

    public String getThi() {
        return thi;
    }

    public void setThi(String thi) {
        this.thi = thi;
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
}

