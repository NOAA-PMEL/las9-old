package pmel.sdig.las

import grails.async.DelegateAsync
import grails.gorm.transactions.Transactional


/**
 * Created by rhs on 1/18/17.
 */
@Transactional
class AsyncIngestService {
    @DelegateAsync IngestService ingestService
}
