package las

import grails.converters.JSON
import grails.plugins.elasticsearch.ElasticSearchService
import org.grails.core.exceptions.DefaultErrorsPrinter
import org.joda.time.DateTime
import pmel.sdig.las.AddProperty
import pmel.sdig.las.AddRequest
import pmel.sdig.las.AsyncIngestService
import pmel.sdig.las.Dataset
import pmel.sdig.las.Ferret
import pmel.sdig.las.FerretEnvironment
import pmel.sdig.las.IngestService
import pmel.sdig.las.InitializationService
import pmel.sdig.las.Site

class BootStrap {

    InitializationService initializationService
    IngestService ingestService
    AsyncIngestService asyncIngestService

    def init = { servletContext ->

        log.debug("Starting the init bootstrap closure...")

        Ferret ferret = Ferret.first()
        FerretEnvironment ferretEnvironment = FerretEnvironment.first()

        if ( !ferret || !ferretEnvironment ) {
            initializationService.initEnvironment()
        }



        initializationService.createProducts()

        // TODO this is the default data sets - make sure it's on for distriubtion
//        initializationService.loadDefaultLasDatasets()

        // This should be kicked off from an add(catalog_url) in the admin controller when somebody is doing this interactively from admin console,
        // should also launch at addVariablesToAll in the asnyc ingest service after finishing reading the catalog hierarchy. :-)

//        def u = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/catalog.xml"
//        def u = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/woa_13/catalog.xml";
//        def u = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/ferret.pmel.noaa.gov/pmel/thredds/uaf.xml"
//        def u = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalog.xml"


        Site site = Site.first();
        if (!site) {
            site = new Site([title: "LAS for Unified Access Framework with a really long title the will most certainly wrap or cause some other misfortune."])
        }
//        def u = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/pathfinderAgg/catalog.xml"
//        Dataset uaf = ingestService.ingestFromThredds(u, IngestService.getDigest(u),null, false)
//        site.addToDatasets(uaf);

        def v = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/www.esrl.noaa.gov/psd/thredds/catalog/Datasets/ncep.marine/catalog.xml"
        Dataset vaf = ingestService.ingestFromThredds(v, IngestService.getDigest(v),null, false)
        site.addToDatasets(vaf)

        def w = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/www.esrl.noaa.gov/psd/thredds/catalog/Datasets/noaa.ersst.v5/catalog.xml"
        Dataset waf = ingestService.ingestFromThredds(w, IngestService.getDigest(w),null, false)
        site.addToDatasets(waf)

        def x = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/www.esrl.noaa.gov/psd/thredds/catalog/Datasets/ncep.reanalysis.derived/sigma/catalog.xml"
        Dataset xaf = ingestService.ingestFromThredds(x, IngestService.getDigest(x),null, false)
        site.addToDatasets(xaf)

        site.save(failOnError: true)
        ingestService.cleanup()
        site.save(failOnError: true)
        asyncIngestService.addVariablesToAll()


//
//


    }
    def destroy = {
    }
}
