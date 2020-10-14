package pmel.sdig.las.shared.autobean;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Dataset implements Comparable {

    long id;
    String title;
    String hash;
    String type;
    List<Variable> variables;
    List<Vector> vectors;
    List <Dataset> datasets;
    Dataset dataset;
    Set<DatasetProperty> datasetProperties;
    String status;
    boolean variableChildren;
    Dataset parent;

    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static String INGEST_NOT_STARTED = "Ingest not started";
    public static String INGEST_STARTED = "Ingest started";
    public static String INGEST_FAILED = "Ingest failed";
    public static String INGEST_FINISHED = "Ingest finished";

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Set<DatasetProperty> getDatasetProperties() {
        return datasetProperties;
    }

    public void setDatasetProperties(Set<DatasetProperty> datasetProperties) {
        this.datasetProperties = datasetProperties;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public List<Vector> getVectors() {
        return vectors;
    }

    public void setVectors(List<Vector> vectors) {
        this.vectors = vectors;
    }

    public Dataset getParent() {
        return parent;
    }

    public void setParent(Dataset parent) {
        this.parent = parent;
    }

    public String getProperty(String type, String name) {
        if ( datasetProperties != null ) {
            for(Iterator vit = datasetProperties.iterator(); vit.hasNext(); ) {
                DatasetProperty dp = (DatasetProperty) vit.next();
                if ( dp.getType().equals(type) && dp.getName().equals(name) ) {
                    return dp.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public int compareTo(Object o) {
        if ( o instanceof Dataset ) {
            Dataset d = (Dataset) o;
            return this.title.compareTo(d.getTitle());
        } else {
            return 0;
        }
    }

    public Variable findVariableByNameAndTitle(String name, String title) {
        for (int i = 0; i < variables.size(); i++) {
            Variable v = variables.get(i);
            if ( v.getName().equals(name) && v.getTitle().equals(title) ) {
                return v;
            }
        }
        return null;
    }
    // Really only save for the standard ERDDAP names of time, latitude, longitude and whatever z is
    public Variable findVariableByName(String name) {
        for (int i = 0; i < variables.size(); i++) {
            Variable v = variables.get(i);
            if ( v.getName().equals(name) ) {
                return v;
            }
        }
        return null;
    }
    public Variable findVariableByHash(String hash) {
        for (int i = 0; i < variables.size(); i++) {
            Variable v = variables.get(i);
            if ( v.getHash().equals(hash) ) {
                return v;
            }
        }
        return null;
    }
    public Variable findVariableById(long id) {
        for (int i = 0; i < variables.size(); i++) {
            Variable v = variables.get(i);
            if ( v.getId() == id ) {
                return v;
            }
        }
        return null;
    }
    public int findVariableIndexByHash(String hash) {
        for (int i = 0; i < variables.size(); i++) {
            Variable v = variables.get(i);
            if ( v.getHash().equals(hash) ) {
                return i;
            }
        }
        return -1;
    }
    public boolean hasVariableChildren() {
        return variableChildren;
    }
    public boolean isVariableChildren () {
        return variableChildren;
    }
    public void setVariableChildren(boolean variableChildren) {
        this.variableChildren = variableChildren;
    }

    public Variable getIdVariable() {
        for (int i = 0; i < variables.size(); i++) {
            Variable v = variables.get(i);
            if ( v.isDsgId() ) {
                return v;
            }
        }
        return null;
    }
}
