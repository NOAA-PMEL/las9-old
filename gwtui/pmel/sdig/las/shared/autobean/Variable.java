package pmel.sdig.las.shared.autobean;

import java.util.Set;

public class Variable implements Comparable {

	long id;
	String url;
	String name;
	String title;
	String hash;
	String intervals;
	String geometry;
	String units;
	Dataset dataset;
	String type;
	TimeAxis timeAxis;
	GeoAxisX geoAxisX;
	GeoAxisY geoAxisY;
	VerticalAxis verticalAxis;
	Stats stats;
	Set<VariableProperty> variableProperties;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setGeometry (String geometry) { this.geometry = geometry; }

	public String getGeometry () { return this.geometry; }

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public void setDataset(Dataset dataset) {
		this.dataset = dataset;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public TimeAxis getTimeAxis() {
		return timeAxis;
	}

	public void setTimeAxis(TimeAxis timeAxis) {
		this.timeAxis = timeAxis;
	}

	public GeoAxisX getGeoAxisX() {
		return geoAxisX;
	}

	public void setGeoAxisX(GeoAxisX geoAxisX) {
		this.geoAxisX = geoAxisX;
	}

	public GeoAxisY getGeoAxisY() {
		return geoAxisY;
	}

	public void setGeoAxisY(GeoAxisY geoAxisY) {
		this.geoAxisY = geoAxisY;
	}

    public String getIntervals() {
        return intervals;
    }

    public void setIntervals(String intervals) {
        this.intervals = intervals;
    }

    public VerticalAxis getVerticalAxis() {
		return verticalAxis;
	}

	public void setVerticalAxis(VerticalAxis verticalAxis) {
		this.verticalAxis = verticalAxis;
	}

	public Stats getStats() {
		return stats;
	}

	public void setStats(Stats stats) {
		this.stats = stats;
	}

	public Set<VariableProperty> getVariableProperties() {
		return variableProperties;
	}

	public void setVariableProperties(Set<VariableProperty> variableProperties) {
		this.variableProperties = variableProperties;
	}

	public void setUnits(String units) {
		this.units = units;
	}

	public String getUnits() {
		return this.units;
	}

	@Override
	public int compareTo(Object o) {
		if ( o instanceof Variable ) {
			Variable v = (Variable) o;
			return this.getTitle().compareTo(v.getTitle());
		} else {
			return 0;
		}
	}

}
