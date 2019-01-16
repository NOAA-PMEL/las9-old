package pmel.sdig.las

class Dataset {

    String status
    String title
    String hash
    String url
    String type = "dataset"
    Boolean variableChildren
    List variables

    static String INGEST_NOT_STARTED = "Ingest not started"
    static String INGEST_STARTED = "Ingest started"
    static String INGEST_FAILED = "Ingest failed"
    static String INGEST_FINISHED = "Ingest finished"


    // A data set can contain other datasets or variables.
    List datasets

    static hasMany = [datasetProperties: DatasetProperty, variables: Variable, datasets: Dataset]


    static belongsTo = [parent: Dataset]

    static searchable = {
        variables reference: true
        only = ['title', "variables"]
    }

    static mapping = {
        url type: "text"
        title type: "text"
        datasets cascade: 'all-delete-orphan'
    }


    static constraints = {
        parent(nullable: true)
        url(nullable: true)
        title(nullable: true)
        variables(nullable: true)
        datasets(nullable: true)
        datasetProperties(nullable: true)
        status(nullable: true)
        variableChildren (nullable: true)
    }


//    @Override
//    int compareTo(Object o) {
//        if ( o instanceof Dataset ) {
//            Dataset od = (Dataset) o
//            return this.title.compareTo(od.title)
//
//        } else {
//            return 0
//        }
//    }
//
//    @Override
//    String toString() {
//        title
//    }
}
