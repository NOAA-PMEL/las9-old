package pmel.sdig.las

import grails.converters.JSON

class AdminController {
    IngestService ingestService
    def index() {
        respond "You logged in."
    }
    def addDataset() {


        def requestJSON = request.JSON

        AddRequest addReq = new AddRequest(requestJSON)

        def url = addReq.url

        if ( addReq.url && addReq.type ) {

            Dataset dataset = ingestService.processRequset(addReq);

            dataset.setStatus(Dataset.INGEST_FINISHED)
            if ( !dataset.validate() ) {
                dataset.errors.each{
                    log.debug(it.toString())
                }
            }
            dataset.save(flush: true)

            // TODO for now add to first site
            Site site = Site.first()
            site.addToDatasets(dataset)

            site.save(flush: true)

            JSON.use("deep") {
                render dataset as JSON
            }

        }

    }
}
