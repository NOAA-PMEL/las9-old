package pmel.sdig.las

class LASRequest {

    AxesSet axesSet1
    AxesSet axesSet2

    String operation
    int targetPanel;

    static hasMany = [requestProperties: RequestProperty, datasetHashes: String, variableHashes: String, analysis: Analysis, constraints: Constraint]

    List variableHashes
    List datasetHashes
    List requestProperties;
    List analysis;

    static constraints = {
        targetPanel(nullable: true)
    }
}
