package pmel.sdig.las

import grails.converters.JSON

class DatasetController {

//    static scaffold = Dataset
    IngestService ingestService
    AsyncIngestService asyncIngestService
    AsyncFerretService asyncFerretService
    //MakeStatsService makeStatsService
    def add() {
        String url = params.url
        if ( url ) {
            def dataset = Dataset.findByUrl(url);
            if ( !dataset ) {
                ingestService.ingest(null, url)
                dataset = Dataset.findByUrl(url)
                if ( dataset ) {
                    // request.message and reqeust.info appear to be a reserved key... using my own key...
                    request.data_ingest_info = "Dataset ingested."

                } else {
                    request.data_ingest_info = "Unable to ingest dataset."
                }
            } else {
                request.data_ingest_info = url+"has already been ingested into this server."
            }
            [datasetInstance:dataset]
        }
    }
    def show() {
        def did = params.id
        def dataset
        if ( did ) {
            try {
                // If it passes valueOf, use it as a GORM ID
                long id = Long.valueOf(did)
                dataset = Dataset.get(did)
            } catch (Exception e ) {
                // If not, it's a hash
                dataset = Dataset.findByHash(did)
            }

            if ( dataset.variableChildren && (dataset.getStatus().equals(Dataset.INGEST_NOT_STARTED) ) ) {
                dataset.setStatus(Dataset.INGEST_STARTED)
                dataset.setMessage("This data set has not been ingested by LAS. That process has been started. This may take a while, but you can check back anytime to see if it's finished.")
                dataset.save(flush: true)

                asyncIngestService.addVariablesAndSaveFromThredds(dataset.getUrl(), dataset.getHash(), null, true)
//              ingestService.addVariablesAndSaveFromThredds(dataset.getUrl(), null, true)

            } else if ( dataset.variableChildren && (dataset.getStatus().equals(Dataset.INGEST_STARTED) ) ) {
                IngestStatus ingestStatus = IngestStatus.findByHash(dataset.getHash())
                String message = "This the process of ingesting this data set has been started. This may take a while, but you can check back anytime to see if it's finished."
                if (ingestStatus) {
                    message = ingestStatus.getMessage()
                }
                dataset.setMessage(message)
            }
        }
        withFormat {
            html { respond dataset }
            json {
                log.debug("Starting response for dataset " + dataset.id)
                respond dataset // uses the custom templates in views/dataset
            }
        }
    }
    def browse() {
        def browseDatasets = []

        def offset = params.offset
        // If no offset is specified send only the first
        if ( !offset ) {
            offset = 0
            def dlist = Dataset.findAllByVariableChildren(true, [offset: offset, max: 1])
            browseDatasets.add(dlist.get(0))


        } else {
            // Send back the next 10
            browseDatasets = Dataset.findAllByVariableChildren(true, [offset: offset, max: 10])
        }
        log.debug("Starting response for dataset list with " + browseDatasets.size() + " members.")
        render(template: "browse",  model: [datasetList: browseDatasets])
    }
}
