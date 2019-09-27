package pmel.sdig.las

/**
 *
 */
class Site {
    List datasets
    String title
    long total
    long grids
    long discrete

    boolean toast // show the toast message (mostly for debugging)
    boolean dashboard // limit the map interaction to region and point selection and show "unrolled" graphs of selected points

    String infoUrl;

    String base // This is the base URL from the point of view of the client.
    static hasMany = [datasets: Dataset, siteProperties: SiteProperty]
    static constraints = {
        siteProperties nullable: true
        base nullable: true
        infoUrl nullable: true
    }
}
