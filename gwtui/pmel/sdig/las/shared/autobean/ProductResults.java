package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Created by rhs on 9/22/15.
 */
public class ProductResults {

    ResultSet resultSet;
    MapScale mapScale;
    List<AnnotationGroup> annotationGroups;

    int targetPanel;

    String error;

    public ResultSet getResultSet() {
        return resultSet;
    }

    public void setResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public MapScale getMapScale() {
        return mapScale;
    }

    public void setMapScale(MapScale mapScale) {
        this.mapScale = mapScale;
    }

    public int getTargetPanel() {
        return targetPanel;
    }

    public void setTargetPanel(int targetPanel) {
        this.targetPanel = targetPanel;
    }

    public List<AnnotationGroup> getAnnotationGroups() {
        return annotationGroups;
    }

    public void setAnnotationGroups(List<AnnotationGroup> annotationGroups) {
        this.annotationGroups = annotationGroups;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
