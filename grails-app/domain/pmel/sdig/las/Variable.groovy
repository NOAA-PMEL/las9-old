package pmel.sdig.las


class Variable {
    String name
    String url
    // This could be a java BreadcrumbType on the rhs, but in the end it's a string
    String type = "variable"
    String title
    String hash
    String intervals
    String geometry;
    String units;
    Map<String, String> attributes

    static belongsTo = [dataset: Dataset]
    static hasMany = [variableProperties: VariableProperty, variableAttributes: VariableAttribute]
    static hasOne = [stats: Stats, timeAxis: TimeAxis, geoAxisX: GeoAxisX, geoAxisY: GeoAxisY, verticalAxis: VerticalAxis]
    static mapping = {
        sort "title"
        url type: "text"
        title type: "text"
    }


    static searchable = {

        only = ['title', 'name', 'geometry']

    }
    static constraints = {
        verticalAxis(nullable: true)
        timeAxis(nullable: true)
        stats(nullable: true)
        variableProperties(nullable: true)
        units(nullable:true)
        dataset (nullable: true)
    }

    @Override
    def String toString() {
        if ( title ) return title
        return name
    }

}
