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

    boolean toast;
    boolean dashboard;

    String infoUrl;

    List<Dataset> datasets;

    public boolean isDashboard() {
        return dashboard;
    }

    public void setDashboard(boolean dashboard) {
        this.dashboard = dashboard;
    }

    public boolean isToast() {
        return toast;
    }

    public void setToast(boolean toast) {
        this.toast = toast;
    }

    public String getInfoUrl() {
        return infoUrl;
    }

    public void setInfoUrl(String infoUrl) {
        this.infoUrl = infoUrl;
    }

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
