package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Created by rhs on 5/21/15.
 */
public class Site {

    String title;
    List<Dataset> datasets;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }
}
