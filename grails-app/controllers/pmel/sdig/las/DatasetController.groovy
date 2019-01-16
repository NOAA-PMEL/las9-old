package pmel.sdig.las

import grails.converters.JSON

class DatasetController {

    static scaffold = Dataset
    IngestService ingestService
    AsyncIngestService asyncIngestService
    //MakeStatsService makeStatsService

    def add() {
        String url = params.url
        if ( url ) {
            def dataset = Dataset.findByUrl(url);
            if ( !dataset ) {
                ingestService.ingest(url)
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

            if ( dataset.variableChildren && (dataset.getStatus().equals(Dataset.INGEST_NOT_STARTED) ||  dataset.getStatus().equals(Dataset.INGEST_FAILED)) ) {
                dataset.setStatus(Dataset.INGEST_STARTED)
                dataset.save(flush: true)

                    asyncIngestService.addVariablesAndSaveFromThredds(dataset.getUrl(), null, true)

            }
        }
        withFormat {
            html { respond dataset }
            json {
                if ( dataset ) {
                    JSON.use("deep") {
                        render dataset as JSON
                    }
                }
            }
        }
    }
}
