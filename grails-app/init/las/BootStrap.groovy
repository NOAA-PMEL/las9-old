package las

import grails.converters.JSON
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
import pmel.sdig.las.TimeAxis
import pmel.sdig.las.Variable

class BootStrap {

    InitializationService initializationService
    IngestService ingestService
    AsyncIngestService asyncIngestService
    AsyncFerretService asyncFerretService

    def init = { servletContext ->

        log.debug("Starting the init bootstrap closure...")

        Ferret ferret = Ferret.first()
        FerretEnvironment ferretEnvironment = FerretEnvironment.first()

        if ( !ferret || !ferretEnvironment ) {
            initializationService.initEnvironment()
        }



        initializationService.createProducts()

        initializationService.createDefaultRegions()

        // TODO this is the default data sets - make sure it's on for distribution
//        initializationService.loadDefaultLasDatasets()
//        asyncFerretService.makeThumbnails()

        // This should be kicked off from an add(catalog_url) in the admin controller when somebody is doing this interactively from admin console,
        // should also launch at addVariablesToAll in the asnyc ingest service after finishing reading the catalog hierarchy. :-)


//
//
        Site site = Site.first();
        if (!site) {
            // Main UAF config...
            site = new Site([title: "LAS for UAF"])

//            def u = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalog.xml"
            def u = "https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/www.ncei.noaa.gov/thredds/marine-ocean/marine-ocean.xml"
            Dataset uaf = ingestService.ingestFromThredds(u, IngestService.getDigest(u),null, false)
            site.addToDatasets(uaf);
//

//            def dsgURL = "https://upwell.pfeg.noaa.gov/erddap/tabledap/"
//            List<AddProperty> addProperties = new ArrayList<>();
//            AddProperty auto = new AddProperty([name: "auto_display", value: "true"])
//            addProperties.add(auto)
//            Dataset dsg = ingestService.ingestAllFromErddap(dsgURL, addProperties)
//            site.addToDatasets(dsg)


/* This installs and organizes the Engineering data ...
            def dsgURL = "https://ferret.pmel.noaa.gov/engineering/erddap"
            List<AddProperty> addProperties = new ArrayList<>();
            AddProperty auto = new AddProperty([name: "auto_display", value: "true"])

            addProperties.add(auto)

            Dataset dsg = ingestService.ingestAllFromErddap(dsgURL, addProperties)

            Dataset dart = dsg.getDatasets().get(0)
            Dataset praw = dsg.getDatasets().get(1)

            Dataset f51 = new Dataset([title: "Dart F51 46451 (Oregon)", hash: IngestService.getDigest("Dart F51 46451 (Oregon)")])
            Dataset f52 = new Dataset([title: "Dart F52 (Oregon)",  hash: IngestService.getDigest("Dart F52 (Oregon)")])
            Dataset f53 = new Dataset([title: "Dart F53 46452 (Oregon)",  hash: IngestService.getDigest("Dart F53 46452 (Oregon)")])
            Dataset FG1 = new Dataset([title: "Dart FG1 (Alaska)",  hash: IngestService.getDigest("Dart FG1 (Alaska)")])
            Dataset N03 = new Dataset([title: "Dart N03 32403 (Chile)",  hash: IngestService.getDigest("Dart N03 32403 (Chile)")])
            Dataset N04 = new Dataset([title: "Dart N04 32404 (Chile)",  hash: IngestService.getDigest("Dart N04 32404 (Chile)")])
            Dataset W20 = new Dataset([title: "Dart W20 34420 (Chile)",  hash: IngestService.getDigest("Dart W20 34420 (Chile)")])

            for (int i = 0; i < dart.getDatasets().size(); i++) {
                Dataset dartI = dart.getDatasets().get(i)
                dart.removeFromDatasets(dartI)
                // Make time ranges display entire range
                for (int j = 0; j < dartI.getVariables().size(); j++) {
                    Variable v = dartI.getVariables().get(j)
                    TimeAxis t = v.getTimeAxis()
                    t.setDisplay_hi("end")
                    t.setDisplay_lo("start")
                }
                if ( dartI.getTitle().contains("F51") ) {
                    f51.addToDatasets(dartI);
                } else if ( dartI.getTitle().contains("F52") ) {
                    f52.addToDatasets(dartI)
                } else if ( dartI.getTitle().contains("F53") ) {
                    f53.addToDatasets(dartI)
                } else if ( dartI.getTitle().contains("FG1") ) {
                    FG1.addToDatasets(dartI)
                } else if ( dartI.getTitle().contains("N03") ) {
                    N03.addToDatasets(dartI)
                } else if ( dartI.getTitle().contains("N04") ) {
                    N04.addToDatasets(dartI)
                } else if ( dartI.getTitle().contains("W20") ) {
                    W20.addToDatasets(dartI)
                }
            }
            site.addToDatasets(f51)
            site.addToDatasets(f52)
            site.addToDatasets(f53)
            site.addToDatasets(FG1)
            site.addToDatasets(N03)
            site.addToDatasets(N04)
            site.addToDatasets(W20)

            Dataset p = praw.getDatasets().get(0)
            for (int j = 0; j < p.getVariables().size(); j++) {
                Variable v = p.getVariables().get(j)
                TimeAxis t = v.getTimeAxis()
                t.setDisplay_hi("end")
                t.setDisplay_lo("start")
            }
            praw.removeFromDatasets(p)
            site.addToDatasets(p)

 */

//            def dsgURL = "https://ferret.pmel.noaa.gov/alamo/erddap/tabledap/"
//            List<AddProperty> addProperties = new ArrayList<>();
//            AddProperty auto = new AddProperty([name: "auto_display", value: "true"])
//            addProperties.add(auto)
//            Dataset dsg = ingestService.ingestAllFromErddap(dsgURL, addProperties)
//            dsg.setTitle("Arctic Heat Open Science Experiment")
//            site.addToDatasets(dsg)

//            def dsgURL1 = "https://upwell.pfeg.noaa.gov/erddap/tabledap/erdCalCOFIeggcnt"
//            List<AddProperty> addProperties1 = new ArrayList<>();
//            AddProperty auto = new AddProperty([name: "auto_display", value: "true"])
//            addProperties1.add(auto)
//            Dataset dsg1 = ingestService.ingestFromErddap_using_json(dsgURL1, addProperties1)
//            dsg1.setTitle("CalCOFI Egg Counts")
//            site.addToDatasets(dsg1)

//            def dsgURL2 = "https://ferret.pmel.noaa.gov/soi/erddap/tabledap/"
//            List<AddProperty> addProperties2 = new ArrayList<>();
//            AddProperty auto2 = new AddProperty([name: "auto_display", value: "true"])
//            addProperties2.add(auto2)
//            Dataset dsg2 = ingestService.ingestAllFromErddap(dsgURL2, addProperties2)
//            dsg2.setTitle("Schmidt Ocean Institute")
//            site.addToDatasets(dsg2)

//            def dsgURL3 = "https://ferret.pmel.noaa.gov/engineering/erddap/tabledap/"
//            List<AddProperty> addProperties3 = new ArrayList<>();
//            AddProperty auto3 = new AddProperty([name: "auto_display", value: "true"])
//            addProperties3.add(auto3)
//            Dataset dsg3 = ingestService.ingestAllFromErddap(dsgURL3, addProperties3)
//            dsg3.setTitle("PMEL Engineering Data")
//            site.addToDatasets(dsg3)

//            log.debug("Ingesting Levitus climatology")
//            def levhash = IngestService.getDigest("levitus_climatology.cdf")
//            Dataset levitusDS = Dataset.findByHash(levhash)
//            if ( !levitusDS ) {
////                levitusDS = ingestService.ingest(levhash, "/home/users/tmap/ferret/linux/fer_dsets/data/levitus_climatology.cdf")
//                levitusDS = ingestService.ingest(levhash, "/usr/local/fer_data/data/levitus_climatology.cdf")
//                levitusDS.setTitle("Levitus Ocean Climatology")
//                levitusDS.setStatus(Dataset.INGEST_FINISHED)
//                levitusDS.save(flush: true)
//            }
//            if ( levitusDS ) {
//                site.addToDatasets(levitusDS)
//            }


//            log.debug("Ingesting Carbon Tracker Data")
//            def levhash = IngestService.getDigest("levitus_climatology.cdf")
//            Dataset levitusDS = Dataset.findByHash(levhash)
//            if ( !levitusDS ) {
////                levitusDS = ingestService.ingest(levhash, "/home/users/tmap/ferret/linux/fer_dsets/data/levitus_climatology.cdf")
//                levitusDS = ingestService.ingest(levhash, "/usr/local/fer_data/data/levitus_climatology.cdf")
//                levitusDS.setTitle("Levitus Ocean Climatology")
//                levitusDS.setStatus(Dataset.INGEST_FINISHED)
//                levitusDS.save(flush: true)
//            }
//            if ( levitusDS ) {
//                site.addToDatasets(levitusDS)
//            }

//            def dsgURL4 = "http://apdrc.soest.hawaii.edu/thredds/catalog/ncml_aggregation/oisss/catalog.xml"
//            Dataset dsg4 = ingestService.ingestFromThredds(dsgURL4, IngestService.getDigest(dsgURL4),null, false)
//            dsg4.setTitle("APDRC Sea Surface Salinity")
//            site.addToDatasets(dsg4);
//            site.save(failOnError: true)

            site.save(failOnError: true)

        }
        asyncIngestService.addVariablesToAll().onComplete {
            ingestService.cleanup()
            asyncFerretService.makeThumbnails();
        }
    }
    def destroy = {
    }
}
