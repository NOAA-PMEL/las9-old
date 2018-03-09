package pmel.sdig.las

import grails.converters.JSON

class VariableController {
    static scaffold = Variable
    def json() {
        def vid = params.id
        if ( vid ) {

            def var = Variable.get(vid)
            JSON.use("deep") {
                render var as JSON
            }

        }
    }

}
