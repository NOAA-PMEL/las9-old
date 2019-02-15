package pmel.sdig.las


class Vector {
    String name
    String type = "vector"
    String title
    String hash
    String geometry;
    Map<String, String> attributes
    Variable u;
    Variable v;
    Variable w;

    static belongsTo = [dataset: Dataset]
    static mapping = {
        sort "title"
        url type: "text"
    }


    static searchable = {

        only = ['title', 'geometry']

    }
    static constraints = {
        w(nullable: true)
        dataset(nullable: true)
    }

    @Override
    def String toString() {
        if ( title ) return title
        return name
    }

}
