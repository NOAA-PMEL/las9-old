package las

import grails.converters.JSON
import grails.plugins.elasticsearch.ElasticSearchService
import org.grails.core.exceptions.DefaultErrorsPrinter
import org.joda.time.DateTime
import pmel.sdig.las.AddProperty
import pmel.sdig.las.AddRequest
import pmel.sdig.las.AsyncFerretService
import pmel.sdig.las.AsyncIngestService
import pmel.sdig.las.Dataset
import pmel.sdig.las.DatasetProperty
import pmel.sdig.las.Ferret
import pmel.sdig.las.FerretEnvironment
import pmel.sdig.las.IngestService
import pmel.sdig.las.InitializationService
import pmel.sdig.las.Site

class BootStrap {

    InitializationService initializationService
    IngestService ingestService
    AsyncIngestService asyncIngestService
    AsyncFerretService asyncFerretService
    ElasticSearchService elasticSearchService;

    def init = { servletContext ->

        log.debug("Starting the init bootstrap closure...")

        Ferret ferret = Ferret.first()
        FerretEnvironment ferretEnvironment = FerretEnvironment.first()

        if ( !ferret || !ferretEnvironment ) {
            initializationService.initEnvironment()
        }



        initializationService.createProducts()

        initializationService.createDefaultRegions()

        // TODO this is the default data sets - make sure it's on for distriubtion
//        initializationService.loadDefaultLasDatasets()
//        asyncFerretService.makeThumbnails()

        // This should be kicked off from an add(catalog_url) in the admin controller when somebody is doing this interactively from admin console,
        // should also launch at addVariablesToAll in the asnyc ingest service after finishing reading the catalog hierarchy. :-)


//
//
        Site site = Site.first();
        if (!site) {

            site = new Site([title: "LAS for UAF Data"])
//            def u = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalog.xml"
            def u = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/osmc.noaa.gov/thredds/catalog.xml"
            Dataset uaf = ingestService.ingestFromThredds(u, IngestService.getDigest(u),null, false)
            site.addToDatasets(uaf);

            // Main UAF config...
//            def dsgURL = "https://upwell.pfeg.noaa.gov/erddap/tabledap/"
//            List<AddProperty> addProperties = new ArrayList<>();
//            AddProperty auto = new AddProperty([name: "auto_display", value: "true"])
//            addProperties.add(auto)
//            Dataset dsg = ingestService.ingestAllFromErddap(dsgURL, addProperties)
//            site.addToDatasets(dsg)
            // Do 3 times, and rename according to source

            def dsgURL = "https://ferret.pmel.noaa.gov/alamo/erddap/tabledap/"
            List<AddProperty> addProperties = new ArrayList<>();
            AddProperty auto = new AddProperty([name: "auto_display", value: "true"])
            addProperties.add(auto)
            Dataset dsg = ingestService.ingestAllFromErddap(dsgURL, addProperties)
            dsg.setTitle("Arctic Heat Open Science Experiment")
            site.addToDatasets(dsg)

            def dsgURL2 = "https://ferret.pmel.noaa.gov/soi/erddap/tabledap/"
            List<AddProperty> addProperties2 = new ArrayList<>();
            AddProperty auto2 = new AddProperty([name: "auto_display", value: "true"])
            addProperties2.add(auto2)
            Dataset dsg2 = ingestService.ingestAllFromErddap(dsgURL2, addProperties2)
            dsg2.setTitle("Schmidt Ocean Institute")
            site.addToDatasets(dsg2)

            def dsgURL3 = "https://ferret.pmel.noaa.gov/engineering/erddap/tabledap/"
            List<AddProperty> addProperties3 = new ArrayList<>();
            AddProperty auto3 = new AddProperty([name: "auto_display", value: "true"])
            addProperties.add(auto3)
            Dataset dsg3 = ingestService.ingestAllFromErddap(dsgURL3, addProperties3)
            dsg3.setTitle("PMEL Engineering Data")
            site.addToDatasets(dsg3)

            log.debug("Ingesting Levitus climatology")
            def levhash = IngestService.getDigest("levitus_climatology.cdf")
            Dataset levitusDS = Dataset.findByHash(levhash)
            if ( !levitusDS ) {
//                levitusDS = ingestService.ingest(levhash, "/home/users/tmap/ferret/linux/fer_dsets/data/levitus_climatology.cdf")
                levitusDS = ingestService.ingest(levhash, "/usr/local/fer_data/data/levitus_climatology.cdf")
                levitusDS.setTitle("Levitus Ocean Climatology")
                levitusDS.setStatus(Dataset.INGEST_FINISHED)
                levitusDS.save(flush: true)
                elasticSearchService.index(levitusDS);
            }
            if ( levitusDS ) {
                site.addToDatasets(levitusDS)
            }

            def dsgURL4 = "http://apdrc.soest.hawaii.edu/thredds/catalog/ncml_aggregation/oisss/catalog.xml"
            Dataset dsg4 = ingestService.ingestFromThredds(dsgURL4, IngestService.getDigest(dsgURL4),null, false)
            dsg4.setTitle("APDRC Sea Surface Salinity")
            site.addToDatasets(dsg4);

            site.save(failOnError: true)

            ingestService.cleanup()
            site.save(failOnError: true)


        }
        asyncIngestService.addVariablesToAll().onComplete {
            asyncFerretService.makeThumbnails();
        }
    }
    def destroy = {
    }
}
