package pmel.sdig.las

class LASRequest {

    String operation
    int targetPanel;

    static hasMany = [requestProperties: RequestProperty, datasetHashes: String, variableHashes: String, analysis: Analysis, constraints: Constraint, axesSets: AxesSet]

    List variableHashes
    List datasetHashes
    List requestProperties;
    List analysis;
    List axesSets

    static constraints = {
        targetPanel(nullable: true)
    }
}
