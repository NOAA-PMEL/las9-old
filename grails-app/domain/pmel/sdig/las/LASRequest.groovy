package pmel.sdig.las

class LASRequest {

    String xlo
    String xhi
    String ylo
    String yhi
    String zlo
    String zhi
    String tlo
    String thi
    String operation
    int targetPanel;

    static hasMany = [requestProperties: RequestProperty, datasetHashes: String, variableHashes: String]

    List variableHashes
    List datasetHashes
    List requestProperties;

    static constraints = {
        targetPanel(nullable: true)
    }
}
