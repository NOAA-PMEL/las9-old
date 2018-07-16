package pmel.sdig.las.shared.autobean;

import java.util.List;

public class Analysis {

    String transformation;
    String axes;
    List<AnalysisAxis> analysisAxes;

    public String getAxes() {
        return axes;
    }

    public void setAxes(String axes) {
        this.axes = axes;
    }

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    public List<AnalysisAxis> getAnalysisAxes() {
        return analysisAxes;
    }

    public void setAnalysisAxes(List<AnalysisAxis> analysisAxes) {
        this.analysisAxes = analysisAxes;
    }

}
