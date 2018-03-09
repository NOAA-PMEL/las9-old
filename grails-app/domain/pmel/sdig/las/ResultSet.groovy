package pmel.sdig.las

class ResultSet {

    String name

    List results

    static hasMany = [results: Result]
    static belongsTo = [operation: Operation]

    static constraints = {
        operation nullable: true
    }
}
