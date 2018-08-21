package las

import grails.converters.JSON
import org.grails.core.exceptions.DefaultErrorsPrinter
import pmel.sdig.las.AddProperty
import pmel.sdig.las.AddRequest
import pmel.sdig.las.Dataset
import pmel.sdig.las.Ferret
import pmel.sdig.las.FerretEnvironment
import pmel.sdig.las.IngestService
import pmel.sdig.las.InitializationService
import pmel.sdig.las.Site

class BootStrap {

    InitializationService initializationService
    IngestService ingestService

    def init = { servletContext ->

        log.debug("Starting the init bootstrap closure...")

        Ferret ferret = Ferret.first()
        FerretEnvironment ferretEnvironment = FerretEnvironment.first()

        if ( !ferret || !ferretEnvironment ) {
            initializationService.initEnvironment()
        }

//        JSON.use("deep") {
//            def ferretJ = Ferret.first() as JSON
//            new File("ferret.json").write(ferretJ.toString(true))
//        }


        initializationService.createProducts()

        initializationService.loadDefaultLasDatasets()

        // TODO remove for production. :-)

//        AddProperty a = new AddProperty()
//        a.setName("mapandplot")
//        a.setValue("plotonly")
//        AddProperty h = new AddProperty()
//        h.setName("hours")
//        h.setValue(".25")
//        AddRequest request = new AddRequest()
//        request.setUrl("http://ferret.pmel.noaa.gov/engineering/erddap/tabledap/15min_f51_fdd7_a060")
//        request.setType("dsg")
//        request.addToAddProperties(a)
//        request.addToAddProperties(h)
//
//        Dataset d = ingestService.processRequset(request)
//        d.save()
//        Site s = Site.first()
//        s.addToDatasets(d)
//        s.save(flush: true)



    }
    def destroy = {
    }
}
