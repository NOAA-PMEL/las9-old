package pmel.sdig.las

import grails.transaction.Transactional
import pmel.sdig.las.Argument
import pmel.sdig.las.Dataset
import pmel.sdig.las.Ferret
import pmel.sdig.las.FerretEnvironment
import pmel.sdig.las.MenuItem
import pmel.sdig.las.MenuOption
import pmel.sdig.las.Operation
import pmel.sdig.las.Product
import pmel.sdig.las.Result
import pmel.sdig.las.ResultSet
import pmel.sdig.las.Site
import pmel.sdig.las.type.GeometryType

@Transactional
class InitializationService {

    IngestService ingestService

    /**
     * Initialize the Ferret environment. If the values of the environment are available, then they are set. If not, the user will be led to a page where they can be filled in on startup.
     * @return
     */
    def initEnvironment() {

        log.debug("Setting up Ferret environment from the runtime environment.")
        // Get their values from the system environment
        def env = System.getenv()

        // Turns out the map constructor does not work when one of the keys is "_"
        // This code clean out that bad value

        def cleanenv = [:]

        // grails and gorm HATE upper case variables names. I don't know why.
        env.each() { key, value ->
            if ( !key.startsWith("_") ) {
                cleanenv.putAt(key.toLowerCase(), value)
            }
        }

        // Use the FER_DIR to set the path to the executable

        def fer_dir = env['FER_DIR']

        if ( !fer_dir ) {
            fer_dir = ""
        }

        def fer_go = cleanenv.get("fer_go")

        URL ferret_go_dir = this.class.classLoader.getResource("ferret/scripts")

        fer_go = fer_go + " " + ferret_go_dir.getPath().replace("file:", "")

        cleanenv.put("fer_go", fer_go)

        def ferretEnvironment = new FerretEnvironment(cleanenv)


        def ferret = new Ferret()
        // TODO figure out python path
        ferret.setPath("/usr/bin/python2.7")
        ferret.setTempDir("/tmp/las")
        ferret.setFerretEnvironment(ferretEnvironment)
        if ( !ferret.validate() ) {
            ferret.errors.each {
                log.debug(it)
            }
        }
        ferret.save(failOnError: true)

        ferret.addToArguments(new Argument([value: "-cimport sys; import pyferret; (errval, errmsg) = pyferret.init(sys.argv[1:], True)"]))
        ferret.addToArguments(new Argument([value: "-nodisplay"]))
        ferret.addToArguments(new Argument([value: "-script"]))


    }

    /**
     * Create the options and save them...
     */
    def createOptions() {

        MenuOption palettes = MenuOption.findByName("palettes")

        if ( !palettes ) {

            palettes = new MenuOption(help: "Set the color scale of the plot. Only applies to shaded plots.", name: "palette", title: "Color palettes", defaultValue: "rainbow")

            MenuItem p001 = new MenuItem([value: "rainbow", title: "default rainbow"])
            MenuItem p002 = new MenuItem([value: "rnb2", title: "alternative rainbow"])
            MenuItem p003 = new MenuItem([value: "light_rainbow", title: "pastel rainbow"])
            MenuItem p004 = new MenuItem([value: "rainbow_by_levels", title: "rainbow (by-level)"])
            MenuItem p005 = new MenuItem([value: "ocean_temp", title: "ocean temperature (by-value)"])
            MenuItem p006 = new MenuItem([value: "land_sea_values", title: "topography/bathymetry (by-value)"])
            MenuItem p007 = new MenuItem([value: "etop_values", title: "ocean/terrestrial elevation (by-value)"])
            MenuItem p008 = new MenuItem([value: "light_bottom", title: "light bottom"])
            MenuItem p009 = new MenuItem([value: "light_centered", title: "anomaly"])
            MenuItem p010 = new MenuItem([value: "land_sea", title: "topography/bathymetry"])
            MenuItem p011 = new MenuItem([value: "dark_land_sea", title: "dark topography/bathymetry"])
            MenuItem p012 = new MenuItem([value: "ocean_blue", title: "blue bathymetry"])
            MenuItem p013 = new MenuItem([value: "terrestrial", title: "topography"])
            MenuItem p014 = new MenuItem([value: "dark_terrestrial", title: "dark topography"])

            MenuItem p015 = new MenuItem([value: "bluescale", title: "range of blues"])
            MenuItem p016 = new MenuItem([value: "inverse_bluescale", title: "inverse range of blues"])
            MenuItem p017 = new MenuItem([value: "redscale", title: "range of reds"])
            MenuItem p018 = new MenuItem([value: "inverse_redscale", title: "inverse range of reds"])
            MenuItem p019 = new MenuItem([value: "greenscale", title: "range of greens"])
            MenuItem p020 = new MenuItem([value: "inverse_greenscale", title: "inverse range of greens"])
            MenuItem p021 = new MenuItem([value: "grayscale", title: "range of grays"])
            MenuItem p022 = new MenuItem([value: "inverse_grayscale", title: "inverse range of grays"])

            MenuItem p023 = new MenuItem([value: "low_blue", title: "low_blue"])
            MenuItem p024 = new MenuItem([value: "no_blue", title: "no_blue"])
            MenuItem p025 = new MenuItem([value: "low_green", title: "low_green"])
            MenuItem p026 = new MenuItem([value: "no_green", title: "no_green"])
            MenuItem p027 = new MenuItem([value: "low_red", title: "low_red"])
            MenuItem p028 = new MenuItem([value: "no_red", title: "no_red"])

            MenuItem p029 = new MenuItem([value: "no_blue_centered", title: "no_blue_centered"])
            MenuItem p030 = new MenuItem([value: "no_green_centered", title: "no_green_centered"])
            MenuItem p031 = new MenuItem([value: "no_red_centered", title: "no_red_centered"])
            MenuItem p032 = new MenuItem([value: "white_centered", title: "white_centered"])

            MenuItem p033 = new MenuItem([value: "orange", title: "solid orange"])
            MenuItem p034 = new MenuItem([value: "gray", title: "solid gray"])
            MenuItem p035 = new MenuItem([value: "green", title: "solid green"])
            MenuItem p036 = new MenuItem([value: "red", title: "solid red"])

            MenuItem p037 = new MenuItem([value: "violet", title: "solid violet"])
            MenuItem p038 = new MenuItem([value: "white", title: "solid white"])
            MenuItem p039 = new MenuItem([value: "yellow", title: "solid yellow"])


            palettes.addToMenuItems(p001)
            palettes.addToMenuItems(p002)
            palettes.addToMenuItems(p003)
            palettes.addToMenuItems(p004)
            palettes.addToMenuItems(p005)
            palettes.addToMenuItems(p006)
            palettes.addToMenuItems(p007)
            palettes.addToMenuItems(p008)
            palettes.addToMenuItems(p009)

            palettes.addToMenuItems(p010)
            palettes.addToMenuItems(p011)
            palettes.addToMenuItems(p012)
            palettes.addToMenuItems(p013)
            palettes.addToMenuItems(p014)
            palettes.addToMenuItems(p015)
            palettes.addToMenuItems(p016)
            palettes.addToMenuItems(p017)
            palettes.addToMenuItems(p018)
            palettes.addToMenuItems(p019)

            palettes.addToMenuItems(p020)
            palettes.addToMenuItems(p021)
            palettes.addToMenuItems(p022)
            palettes.addToMenuItems(p023)
            palettes.addToMenuItems(p024)
            palettes.addToMenuItems(p025)
            palettes.addToMenuItems(p026)
            palettes.addToMenuItems(p027)
            palettes.addToMenuItems(p028)
            palettes.addToMenuItems(p029)

            palettes.addToMenuItems(p030)
            palettes.addToMenuItems(p031)
            palettes.addToMenuItems(p032)
            palettes.addToMenuItems(p033)
            palettes.addToMenuItems(p034)
            palettes.addToMenuItems(p035)
            palettes.addToMenuItems(p036)
            palettes.addToMenuItems(p037)
            palettes.addToMenuItems(p038)
            palettes.addToMenuItems(p039)

            palettes.save(failOnError: true)

            YesNoOption interpolate = new YesNoOption([name: "interpolate_data",
                                                       title: "Interpolate Data Normal to the Plot?",
                                                       help: "&lt;p&gt;This interpolation affects the interpretation of coordinates\n" +
                                                               "that lie normal to the current view.\n" +
                                                               "For example, in a lat-long view (a traditional map) the time and\n" +
                                                               "depth axes are normal to the view.  If This interpolation is\n" +
                                                               "on LAS performs an interpolation to the exact specified normal\n" +
                                                               "coordinate(s) --  time and depth for a map view.  If off, LAS\n" +
                                                               "instead uses the data at the nearest grid point.\n" +
                                                               "(To be more precise, it uses the data at the grid point of the\n" +
                                                               "cell that contains the specified coordinate).\n" +
                                                               "&lt;/p&gt;\n" +
                                                               "&lt;p&gt;For example:&lt;/p&gt;\n" +
                                                               "\n" +
                                                               "&lt;p&gt;If the grid underlying the variable has points defined at Z=5\n" +
                                                               "and at Z=15 (with the grid box boundary at Z=10) and data is\n" +
                                                               "requested at Z=12 then with View interpolation set to &#8217;On&#8217; the\n" +
                                                               "data in the X-Y plane will be obtained by calculating the\n" +
                                                               "interpolated value of data at Z=12 between the Z=5 and Z=15 planes.\n" +
                                                               "With View interpolation set to &#8217;Off&#8217;, the data will be obtained\n" +
                                                               "from the data at Z=15.&lt;/p&gt;",
                                                       defaultValue: "no"])

            interpolate.save(failOnError: true)

            TextOption expression = new TextOption([name: "fill_levels",
                                                    title: "Color Fill Levels",
                                                    help: "Set the color levels of the plot. Levels are described using Ferret syntax. The\n" +
                                                            "          number of levels is approximate, and may be changed as the algorithm rounds off the values. Examples:\n" +
                                                            "&lt;li>&lt;b>60V&lt;/b> Draw 60 levels based on the variance of the data with open-ended extrema\n" +
                                                            "&lt;li>&lt;b>30H&lt;/b> Draw 30 levels based on a histogram\n" +
                                                            "&lt;li>&lt;b>25&lt;/b> Draw 25 levels spanning the range of the data\n" +
                                                            "&lt;li>&lt;b>30C&lt;/b> Draw 30 levels centered at 0\n" +
                                                            "&lt;li>&lt;b>(0,100,10)&lt;/b>  Bands of color starting at 0, ending at 100, with an interval of 10\n" +
                                                            "&lt;li>&lt;b>(-inf)(-10,10,0.25)(inf)&lt;/b> Bands of color between -10 and 10 with an additional color at each end of the spectrum representing all values below (-inf) or above (inf)\n" +
                                                            "&lt;li>&lt;b>(-100)(-10,10,0.25)(100)&lt;/b> Bands of color between -10 and 10 with a additional bands for all outlying values up to +/- 100.\n" +
                                                            "&lt;/ul>\n" +
                                                            "Detailed info is available in the Ferret User\\'s Guide., see Levels at\n" +
                                                            "http://ferret.pmel.noaa.gov/Ferret/documentation/users-guide/customizing-plots/CONTOURING#_VPINDEXENTRY_853"])
            expression.save(failOnError: true)
        }

    }

    def createResults() {

        ResultSet results_plot_2d = new ResultSet([name: "plot_2D_results"])

        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "text", suffix: ".txt"])
        Result plot_image = new Result([name: "plot_image", mime_type: "image/png", type: "image", suffix: ".png"])
        Result map_scale = new Result([name: "map_scale", mime_type: "text/xml", type: "xml", suffix: ".xml"])
        Result annotations = new Result([name: "annotations", mime_type: "text/xml", type: "xml", suffix: ".xml"])

        results_plot_2d.addToResults(debug)
        results_plot_2d.addToResults(plot_image)
        results_plot_2d.addToResults(map_scale)
        results_plot_2d.addToResults(annotations)


        results_plot_2d.save(failOnError: true)

    }

    def createProducts() {

        createResults()

        Product plot_2d_xy = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "xy", "xy")

        if (!plot_2d_xy) {

            plot_2d_xy = new Product([name: "Plot_2D_XY", title: "Latitude-Longitude", view: "xy", data_view: "xy", ui_group: "Maps", geometry: GeometryType.GRID])

            Operation operation_plot_2d_xy = new Operation([output_template: "xy_zoom", service_action: "Plot_2D_XY"])

            ResultSet results_plot_2d = ResultSet.findByName("plot_2D_results")
            if (results_plot_2d) {
                ResultSet twod = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_xy.setResultSet(twod)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }
            MenuOption palettes = MenuOption.findByName("palette")
            if (palettes) {
//                MenuOption mo = new MenuOption(palettes.properties)
//                mo.save(failOnError: true)
                operation_plot_2d_xy.addToMenuOptions(palettes)
            } else {
                log.error("Palette options not available. Did you use service menthod createOptions before calling createOperations?")
            }

            YesNoOption interpolate = YesNoOption.findByName("interpolate_data")
            if (interpolate) {
                operation_plot_2d_xy.addToYesNoOptions(interpolate)
            } else {
                log.error("Interpolate option not available. Did you use service menthod createOptions before calling createOperations?")
            }

            TextOption fill_levels = TextOption.findByName("fill_levels")
            if ( fill_levels ) {
                operation_plot_2d_xy.addToTextOptions(fill_levels)
            } else {
                log.error("Fill levels option not available. Did you use service menthod createOptions before calling createOperations?")
            }


            plot_2d_xy.addToOperations(operation_plot_2d_xy)
            plot_2d_xy.save(failOnError: true)



        }

        Product plot_2d_xz = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "xz", "xz")

        if ( !plot_2d_xz) {
            plot_2d_xz = new Product([name: "Plot_2D", title: "Lognitude-z", ui_group: "Vertical cross sections", view: "xz", data_view: "xz", geometry: GeometryType.GRID])
            Operation operation_plot_2d_xz = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
            ResultSet results_plot_2d = ResultSet.findByName("plot_2D_results")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_xz.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }
            MenuOption palettes = MenuOption.findByName("palette")
            if (palettes) {
//                MenuOption mo = new MenuOption(palettes.properties)
                operation_plot_2d_xz.addToMenuOptions(palettes)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createOptions before calling createOperations?")
            }
            plot_2d_xz.addToOperations(operation_plot_2d_xz)
            plot_2d_xz.save(failOnError: true)

        }

        // See if the product already exists by name and title.  This implies name and title combinations should be unique.
//
//        Product timeseries_plot = Product.findByNameAndTitle("Timeseries Plot", "Timeseries")
//        if ( !timeseries_plot ) {
//            timeseries_plot = new Product([name: "Timeseries Plot", title: "Timeseries", ui_group: "Line Plots", view: "t", data_view: "xyt", geometry: GeometryType.TIMESERIES])
//            Operation operation_timeseries_plot = new Operation([service_action: "client_plot"])
//            timeseries_plot.addToOperations(operation_timeseries_plot)
//            timeseries_plot.save(failOnError: true)
//        }

        Product charts_timeseries_plot = Product.findByNameAndTitle("Charts Timeseries Plot", "Timeseries Plot");
        if ( !charts_timeseries_plot ) {
            charts_timeseries_plot = new Product([name: "Timeseries Plot", title: "Timeseries Plot", ui_group: "Line Plots", view: "t", data_view: "xyt", geometry: GeometryType.TIMESERIES])
            Operation operation_timeseries_plot = new Operation([service_action: "client_plot"])
            charts_timeseries_plot.addToOperations(operation_timeseries_plot)
            charts_timeseries_plot.save(failOnError: true)
        }
    }
    def loadDefaultLasDatasets() {

        // Only load default datasets if none exist...
        def count = Dataset.count()

        log.debug("Data set count = "+count);


        if ( count  == 0 ) {

            log.debug("No data sets found. Setting up default data sets.  Entered method...");

            // Set up the default LAS
            // We're going to use fer_data to find them, so Ferret must be configured first.
            Site site = Site.first();
            if (!site) {

                site = new Site([title: "Default LAS Site"])
                // No site configured, so build the default site.

                FerretEnvironment ferretEnvironment = FerretEnvironment.first()
                if (!ferretEnvironment) {
                    return
                }

                def dsets = ferretEnvironment.fer_dsets

                def coads
                def ocean_atlas
                def leetmaaSurface
                def leetmaaDepth


                if (dsets) {
                    if (dsets.contains(" ")) {
                        def lookin = dsets.split("\\s")
                        lookin.each { datadir ->
                            if (new File("$datadir" + File.separator + "data" + File.separator + "coads_climatology.cdf").exists()) {
                                coads = "$datadir" + File.separator + "data" + File.separator + "coads_climatology.cdf"
                                ocean_atlas = "$datadir" + File.separator + "data" + File.separator + "ocean_atlas_subset.nc"
                            }
                        }
                    } else {
                        coads = "$dsets" + File.separator + "data" + File.separator + "coads_climatology.cdf"
                        ocean_atlas = "$dsets" + File.separator + "data" + File.separator + "ocean_atlas_subset.nc"

                    }
                } else {

                }
                if (coads) {
                    log.debug("Ingesting COADS")
                    def coadshash = IngestService.getDigest(coads)
                    Dataset coadsDS = Dataset.findByHash(coadshash)
                    if (!coadsDS) {
                        coadsDS = ingestService.ingest(coads)
                        coadsDS.setTitle("COADS")
                        coadsDS.setStatus(Dataset.INGEST_FINISHED)
                        coadsDS.save(flush: true)
                    }
                    if (coadsDS) {
                        site.addToDatasets(coadsDS)
                    }
                }
                if (ocean_atlas) {
                    log.debug("Ingesting the ocean atlas subset.")
                    def oahash = IngestService.getDigest(ocean_atlas)
                    Dataset ocean_atlasDS = Dataset.findByHash(oahash)
                    if (!ocean_atlasDS) {
                        ocean_atlasDS = ingestService.ingest(ocean_atlas)
                        ocean_atlasDS.setTitle("Ocean Atlas Subset")
                        ocean_atlasDS.setStatus(Dataset.INGEST_FINISHED)
                        ocean_atlasDS.save(flush: true)
                    }
                    if (ocean_atlasDS) {
                        site.addToDatasets(ocean_atlasDS)
                    }
                }

//                log.debug("Ingesting carbon tracker THREDDS catalog.")
//                def carbonThredds = "http://ferret.pmel.noaa.gov/pmel/thredds/carbontracker.xml"
//                def carbon = Dataset.findByHash(IngestService.getDigest(carbonThredds))
//                if ( !carbon ) {
//                    carbon = ingestService.ingestFromThredds(carbonThredds, null)
//                    carbon.setStatus(Dataset.INGEST_FINISHED)
//                    carbon.save(flush: true)
//                    site.addToDatasets(carbon)
//                }

                log.debug("Ingesting example Timeseries DSG from ERDDAP")
                def ts = "http://ferret.pmel.noaa.gov/engineering/erddap/tabledap/15min_w20_fdd7_a060"
                List<AddProperty> properties = new ArrayList<>()
                AddProperty hours = new AddProperty([name: "hours", value: ".25"])
                properties.add(hours)
                AddProperty display_hi = new AddProperty([name: "display_hi", value: "2018-02-20T00:00:00.000Z"])
                properties.add(display_hi)
                AddProperty display_lo = new AddProperty(([name: "display_lo", value: "2018-02-05T00:00:00.000Z"]))
                properties.add(display_lo)
                def dsgDataset = Dataset.findByHash(IngestService.getDigest(ts))
                if ( !dsgDataset ) {
                    dsgDataset = ingestService.ingestFromErddap(ts, properties)
                    dsgDataset.setStatus(Dataset.INGEST_FINISHED)
                    dsgDataset.save(flush: true)
                    site.addToDatasets(dsgDataset)
                }

                log.debug("Ingesting UAF THREDDS server")
                //def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalog.xml"
                def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/ecowatch.ncddc.noaa.gov/thredds/oceanNomads/aggs/catalog_g_ncom_aggs.xml"
                // def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/woa13/catalog.xml"
                def erddap = "http://upwell.pfeg.noaa.gov/erddap/"
                def uafDataset = Dataset.findByHash(IngestService.getDigest(uaf))
                if ( ! uafDataset ) {
                    uafDataset = ingestService.ingestFromThredds(uaf, erddap)
                    uafDataset.setStatus(Dataset.INGEST_FINISHED)
                    uafDataset.save(flush: true)
                    site.addToDatasets(uafDataset)
                }

                ingestService.cleanup(site)
                site.save(failOnError: true)
            }
        }
    }
}
