package pmel.sdig.las

class Site {
    List datasets
    String title
    static hasMany = [datasets: Dataset, siteProperties: SiteProperty]
    static constraints = {
        siteProperties nullable: true
    }
}
