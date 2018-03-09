package pmel.sdig.las

class SiteController {

    static scaffold = Site

    def show() {
        def sid = params.id
        Site site = Site.get(sid)
        if ( site ) {
            withFormat {
                html { respond site }
                json { respond site}
            }
        } else {
            log.error("No site found for this installation.")
        }
    }
}
