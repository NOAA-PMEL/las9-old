package pmel.sdig.las.shared.autobean;

import java.util.List;
import java.util.Set;

public class Dataset implements Comparable {
	
	long id;
	String title;
	String hash;
	String type;
	List<Variable> variables;
	List <Dataset> datasets;
	Dataset dataset;
    Set<DatasetProperty> datasetProperties;
    String status;

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

	@Override
	public int compareTo(Object o) {
		if ( o instanceof Dataset ) {
			Dataset d = (Dataset) o;
			return this.title.compareTo(d.getTitle());
		} else {
			return 0;
		}
	}
}
