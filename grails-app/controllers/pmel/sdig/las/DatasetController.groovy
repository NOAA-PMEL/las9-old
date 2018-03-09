package pmel.sdig.las

import pmel.sdig.las.DateTimeService
import pmel.sdig.las.IngestService

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional
import grails.converters.JSON

@Transactional
class DatasetController {

    static scaffold = Dataset
    IngestService ingestService
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
            dataset = Dataset.get(did)
        }
        withFormat {
            html { respond dataset }
            json {
                if ( dataset ) {
                    respond dataset
                }
            }
        }
    }
}
