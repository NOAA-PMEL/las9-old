package pmel.sdig.las

class Site {
    List datasets
    String title
    String base // This is the base URL from the point of view of the client.
    static hasMany = [datasets: Dataset, siteProperties: SiteProperty]
    static constraints = {
        siteProperties nullable: true
        base nullable: true
    }
}
