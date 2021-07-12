package pmel.sdig.las

import grails.async.DelegateAsync
import grails.gorm.transactions.Transactional

@Transactional
class AsyncFerretService {
    @DelegateAsync FerretService ferretService
}
