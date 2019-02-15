package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Created by rhs on 5/21/15.
 */
public class Site {

    String title;

    int total;
    int grids;
    int discrete;

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

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getGrids() {
        return grids;
    }

    public void setGrids(int grids) {
        this.grids = grids;
    }

    public int getDiscrete() {
        return discrete;
    }

    public void setDiscrete(int discrete) {
        this.discrete = discrete;
    }
}
