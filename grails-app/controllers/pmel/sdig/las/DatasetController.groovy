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
                dataset.setMessage("Ingest of data from remote server started... This may take a while. This message will update progress.")
                dataset.save(flush: true)

                asyncIngestService.addVariablesAndSaveFromThredds(dataset.getUrl(), dataset.getHash(), null, true)
//              ingestService.addVariablesAndSaveFromThredds(dataset.getUrl(), null, true)

            } else if ( dataset.variableChildren && (dataset.getStatus().equals(Dataset.INGEST_STARTED) ) ) {
                IngestStatus ingestStatus = IngestStatus.findByHash(dataset.getHash())
                String message = "Loading data set."
                if (ingestStatus) {
                    message = ingestStatus.getMessage()
                }
                dataset.setMessage(message)
            }
        }
        withFormat {
            html { respond dataset }
            json {
                if ( dataset && dataset.variableChildren ) {
                    JSON.use("deep") {
                        render dataset as JSON
                    }
                } else if ( dataset ) {
                    respond dataset
                }
            }
        }
    }
}
