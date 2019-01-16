package las

import grails.converters.JSON
import org.grails.core.exceptions.DefaultErrorsPrinter
import org.joda.time.DateTime
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



        initializationService.createProducts()

        initializationService.loadDefaultLasDatasets()

        // Dataset uaf = ingestService.ingestFromThredds("https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/catalog.xml", null, false)
        // Dataset uaf = ingestService.ingestFromThredds("https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/woa_13/catalog.xml", null, false)
        // Dataset uaf = ingestService.ingestFromThredds("https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/ferret.pmel.noaa.gov/pmel/thredds/uaf.xml", null, false)
//         Dataset uaf = ingestService.ingestFromThredds("https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalog.xml", null, false)



//        Site site = Site.first();
//        if (!site) {
//            site = new Site([title: "Default LAS Site"])
//        }
//        site.addToDatasets(uaf);
//        site.save(failOnError: true)
//        ingestService.cleanup()
//        site.save(failOnError: true)




    }
    def destroy = {
    }
}
