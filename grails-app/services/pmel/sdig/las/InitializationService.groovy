package pmel.sdig.las

import grails.gorm.transactions.Transactional
import grails.plugins.elasticsearch.ElasticSearchService
import grails.util.Environment
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import pmel.sdig.las.type.GeometryType

import javax.servlet.ServletContext

@Transactional
class InitializationService {

    IngestService ingestService
    OptionsService optionsService
    ResultsService resultsService
    ElasticSearchService elasticSearchService
    ServletContext servletContext

    /**
     * Initialize the Ferret environment. If the values of the environment are available, then they are set. If not, the user will be led to a page where they can be filled in on startup.
     * @return
     */
    @Transactional
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


        // This is an attempt to automate the choice of the full path to the python executable by taking it from the pyferret script
        def ferret = new Ferret()
        File pyferret = new File(ferretEnvironment.getFer_dir()+File.separator+"bin"+File.separator+"pyferret")
        def python = "/usr/bin/python"
        pyferret.eachLine { line ->
            def matcher = line =~ /python_exe\s*=\s*(.*)/
            while (matcher.find()) {
                python = matcher.group(1)
                python = python.replace("\"","")
            }
        }

        ferret.setPath(python)
        ferret.setTempDir("/tmp/las")
        ferret.setFerretEnvironment(ferretEnvironment)
        ferret.addToArguments(new Argument([value: "-cimport sys; import pyferret; (errval, errmsg) = pyferret.init(sys.argv[1:], True)"]))
        ferret.addToArguments(new Argument([value: "-nodisplay"]))
        ferret.addToArguments(new Argument([value: "-script"]))
        if ( !ferret.validate() ) {
            ferret.errors.each {
                log.debug(it)
            }
        }

        def tempFile = new File(ferret.tempDir);
        if ( !tempFile.exists() ) {
            tempFile.mkdirs()
        }

        ferret.save(failOnError: true)

        // This is an attempt to automate the configuration for Ferret as used by F-TDS by writing the config based on the environment/
        // TODO if changes to the Ferret or FerretEnvironment happen in the admin UI, rewrite this file
        writeFerretXml(ferret, ferretEnvironment)

        // Write the F-TDS base catalog
        writeFTDSCatalog(ferret)
        /*

 I could write this with JDOM, but since I have the text here already...

 <catalog name="F-TDS for LAS"
         xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0
         http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.6.xsd">

  <service name="dap" base="" serviceType="compound">
    <service name="odap" serviceType="OpenDAP" base="/las/thredds/dodsC/" />
    <service name="dap4" serviceType="DAP4" base="/las/thredds/dap4/" />
  </service>

  <datasetScan name="Data From LAS" path="las" location="/tmp/las/dynamic" serviceName="dap">
    <filter>
      <include wildcard="*.nc"/>
      <include wildcard="*.fds"/>
      <include wildcard="*.jnl"/>
    </filter>
  </datasetScan>

</catalog>

         */

    }
    def writeFTDSCatalog(Ferret ferret) {

        def dynamicTempDir = ferret.getTempDir()+File.separator+"dynamic"
        def catalog = """
 <catalog name="F-TDS for LAS"
         xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0
         http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.6.xsd">

 <!-- 
      This catalog is automatically written by the LAS start up process.
      Changes made here will be lost.
      This TDS is reserved for LAS functions. You can set up your own
      for whatever purpose you need on the same tomcat using the content/thredds directory.
 -->

  <service name="dap" base="" serviceType="compound">
    <service name="odap" serviceType="OpenDAP" base="/las/thredds/dodsC/" />
    <service name="dap4" serviceType="DAP4" base="/las/thredds/dap4/" />
  </service>

  <datasetScan name="Data From LAS" path="las" location="$dynamicTempDir" serviceName="dap">
    <filter>
      <include wildcard="*.nc"/>
      <include wildcard="*.fds"/>
      <include wildcard="*.jnl"/>
    </filter>
  </datasetScan>

</catalog>
"""

        FileWriter configFileWriter
        if (Environment.current == Environment.DEVELOPMENT) {
            configFileWriter = new FileWriter(new File("/home/rhs/tomcat/content/ftds/catalog.xml"))
        } else {
            configFileWriter = new FileWriter(new File("../content/ftds/catalog.xml"))
        }
        catalog.writeTo(configFileWriter).close()
    }
    def writeFerretXml(Ferret ferret, FerretEnvironment ferretEnvironment) {
        Document doc = new Document()
        Element root = new Element("application")
        doc.setRootElement(root)
        Element invoker = new Element("invoker")
        invoker.setAttribute("executable", ferret.getPath())
        invoker.setAttribute("temp_dir", ferret.getTempDir()+File.separator+"scripts"+File.separator+"temp")
        for (int i = 0; i < ferret.getArguments().size(); i++) {
            Argument a = ferret.getArguments().get(i)
            Element arg = new Element("arg")
            arg.setText(a.getValue())
            invoker.addContent(arg)
        }
        root.addContent(invoker)
        Element environment = new Element("environment")
        root.addContent(environment)
        if ( ferretEnvironment.getFer_data())
            environment.addContent(makeEnvVariable("FER_DATA", ferretEnvironment.getFer_data()))
        if ( ferretEnvironment.getFer_descr() )
            environment.addContent(makeEnvVariable("FER_DESCRS", ferretEnvironment.getFer_descr()))
        if ( ferretEnvironment.getFer_dir() )
            environment.addContent(makeEnvVariable("FER_DIR", ferretEnvironment.getFer_dir()))
        if ( ferretEnvironment.getFer_dsets() )
            environment.addContent(makeEnvVariable("FER_DSETS", ferretEnvironment.getFer_dsets()))
        if ( ferretEnvironment.getFer_external_functions() )
            environment.addContent(makeEnvVariable("FER_EXTERNAL_FUNCTIONS", ferretEnvironment.getFer_external_functions()))
        if ( ferretEnvironment.getFer_fonts() )
            environment.addContent(makeEnvVariable("FER_FONTS", ferretEnvironment.getFer_fonts()))
        if ( ferretEnvironment.getFer_go() )
            environment.addContent(makeEnvVariable("FER_GO", ferretEnvironment.getFer_go()))
        if ( ferretEnvironment.getFer_grids() )
            environment.addContent(makeEnvVariable("FER_GRIDS", ferretEnvironment.getFer_grids()))
        if ( ferretEnvironment.getFer_libs() )
            environment.addContent(makeEnvVariable("FER_LIBS", ferretEnvironment.getFer_libs()))
        if ( ferretEnvironment.getFer_palette() )
            environment.addContent(makeEnvVariable("FER_PALETTE", ferretEnvironment.getFer_palette()))
        if ( ferretEnvironment.getLd_library_path() )
            environment.addContent(makeEnvVariable("LD_LIBRARY_PATH", ferretEnvironment.getLd_library_path()))
        if ( ferretEnvironment.getPythonpath() )
            environment.addContent(makeEnvVariable("PYTHONPATH", ferretEnvironment.getPythonpath()))
        if ( ferretEnvironment.getPlotfonts() )
            environment.addContent(makeEnvVariable("PLOTFONTS", ferretEnvironment.getPlotfonts()))
        // Write
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat())
        FileWriter configFileWriter
        if (Environment.current == Environment.DEVELOPMENT) {
            configFileWriter = new FileWriter(new File("/home/rhs/tomcat/webapps/las#thredds/WEB-INF/classes/FerretConfig.xml"))
        } else {
            configFileWriter = new FileWriter(new File("../webapps/las#thredds/WEB-INF/classes/resources/iosp/FerretConfig.xml"))
        }
        out.output(doc, configFileWriter)
    }
    def Element makeEnvVariable(String name, String value) {
        Element fer_variable = new Element("variable")
        Element fer_name = new Element("name")
        fer_name.setText(name)
        fer_variable.addContent(fer_name)
        String[] v = value.split(" ")
        for (int i = 0; i < v.length; i++) {
            Element fer_value = new Element("value")
            fer_value.setText(v[i])
            fer_variable.addContent(fer_value)
        }
        fer_variable
    }



    /**
     * Create the options and save them...
     */

    def createProducts() {
/*
    <response ID="PlotResp" type="HTML" index="1">
      <result type="map_scale" ID="map_scale" file_suffix=".xml"/>
      <result type="image" ID="plot_image" streamable="true" mime_type="image/png" file_suffix=".png"/>
      <result type="debug" ID="debug" file_suffix=".txt"/>
      <result type="xml" ID="webrowset" file_suffix=".xml"/>
      <result type="cancel" ID="cancel"/>
    </response>

 */

        Product prop_prop = Product.findByName("prop_prop_plot")
        if (!prop_prop) {
            prop_prop = new Product([name: "prop_prop_plot", title: "Property-Property Plot", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.GRID, product_order: "99999", hidden: "true"])
            Operation prop_prop_operation = new Operation([output_template: "zoom", service_action: "prop_prop_plot", type: "ferret"])
            prop_prop_operation.setResultSet(resultsService.getPlotResults())
            prop_prop.addToOperations(prop_prop_operation)
            prop_prop.save(failOnError: true)
        }


        /* data for display as a block in a window (not for download) but I think we only need save as and not show values

    <operation ID="Data_Extract" default="true" name="Table of values (text)" output_template="table" service_action="Data_Extract" order="0300" category="table" minvars="1" maxvars="9999">
    <service>ferret</service>
    <response ID="Data_Extract_Response">
      <result type="text" ID="ferret_listing" streamable="true" mime_type="text/plain"/>
      <result type="debug" ID="debug" file_suffix=".txt"/>
    </response>
         */
        // Hidden for now.  All the button products need a ui_group
        Product data_extract = Product.findByName("Data_Extract")
        if (!data_extract) {
            data_extract = new Product([name: "Data_Extract", title: "Show Values", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.GRID, product_order: "99999", hidden: "true"])
            Operation data_extract_operation = new Operation([output_template: "table", service_action: "Data_Extract", type: "ferret"])
            data_extract_operation.setResultSet(resultsService.getDataExtractResults())
            data_extract.addToOperations(data_extract_operation)
            data_extract.save(failOnError: true)
        }

        // Hidden for now.  All the button products need a ui_group
        Product data_extract_netcdf = Product.findByName("Data_Extract_netCDF")
        if (!data_extract_netcdf) {
            data_extract_netcdf = new Product([name: "Data_Extract_netCDF", title: "Save as...", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.GRID, product_order: "99999", hidden: "true"])
            Operation data_extract_netcdf_operation = new Operation([output_template: "table", service_action: "Data_Extract_netCDF", type: "ferret"])
            data_extract_netcdf_operation.setResultSet(resultsService.getDataExtractResultsCDF())
            data_extract_netcdf.addToOperations(data_extract_netcdf_operation)
            data_extract_netcdf.save(failOnError: true)
        }

        Product data_extract_file = Product.findByName("Data_Extract_File")
        if (!data_extract_file) {
            data_extract_file = new Product([name: "Data_Extract_File", title: "Save as...", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.GRID, product_order: "99999", hidden: "true"])
            Operation data_extract_file_operation = new Operation([output_template: "table", service_action: "Data_Extract_File", type: "ferret"])
            data_extract_file_operation.setResultSet(resultsService.getDataExtractResultsFile())
            data_extract_file.addToOperations(data_extract_file_operation)
            data_extract_file.save(failOnError: true)
        }

        Product data_extract_csv = Product.findByName("Data_Extract_CSV")
        if (!data_extract_csv) {
            data_extract_csv = new Product([name: "Data_Extract_CSV", title: "Save as...", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.GRID, product_order: "99999", hidden: "true"])
            Operation data_extract_csv_operation = new Operation([output_template: "table", service_action: "Data_Extract_File", type: "ferret"])
            data_extract_csv_operation.setResultSet(resultsService.getDataExtractResultsCSV())
            data_extract_csv.addToOperations(data_extract_csv_operation)
            data_extract_csv.save(failOnError: true)
        }
        /*

        This are all the line plots, xyzt

         */

        // Sort order set by the product_order string. I use first three digits to sort the groups, and the next three digits for the operation within the group.
        Product t_line_plot = Product.findByName("Time")
        if (!t_line_plot) {

            t_line_plot = new Product([name: "Time", title: "Time", view: "t", data_view: "t", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200001", minArgs: 1, maxArgs: 10])
            Operation operation_t_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D", type: "ferret"])

            // inherit="#expression,#interpolate_data,#image_format,#size,#use_graticules,#margins,#deg_min_sec"/
            // inherit="#Options_Default_7,#line_or_sym,#trend_line,#line_color,#line_thickness,#dep_axis_scale"

            operation_t_line_plot.setResultSet(resultsService.getPlotResults())

            operation_t_line_plot.addToTextOptions(optionsService.getExpression())
            operation_t_line_plot.addToTextOptions(optionsService.getDep_axis_scale())
            operation_t_line_plot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_t_line_plot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_t_line_plot.addToMenuOptions(optionsService.getUse_graticules())
            operation_t_line_plot.addToMenuOptions(optionsService.getLine_or_sym())
            operation_t_line_plot.addToMenuOptions(optionsService.getLine_color())
            operation_t_line_plot.addToMenuOptions(optionsService.getLine_thickness())

            t_line_plot.addToOperations(operation_t_line_plot)
            t_line_plot.save(failOnError: true)
        }


        Product z_line_plot = Product.findByName("Longitude")
        if (!z_line_plot) {

            z_line_plot = new Product([name: "Z", title: "Z", view: "z", data_view: "z", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200004"])
            Operation operation_z_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D", type: "ferret"])

            operation_z_line_plot.setResultSet(resultsService.getPlotResults())

            operation_z_line_plot.addToTextOptions(optionsService.getExpression())
            operation_z_line_plot.addToTextOptions(optionsService.getDep_axis_scale())
            operation_z_line_plot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_z_line_plot.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_z_line_plot.addToMenuOptions(optionsService.getUse_graticules())
            operation_z_line_plot.addToMenuOptions(optionsService.getLine_or_sym())
            operation_z_line_plot.addToMenuOptions(optionsService.getLine_color())
            operation_z_line_plot.addToMenuOptions(optionsService.getLine_thickness())


            z_line_plot.addToOperations(operation_z_line_plot)
            z_line_plot.save(failOnError: true)
        }


        Product y_line_plot = Product.findByName("Longitude")
        if (!y_line_plot) {

            y_line_plot = new Product([name: "Latitude", title: "Latitude", view: "y", data_view: "y", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200003", minArgs: 1, maxArgs: 10])
            Operation operation_y_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D", type: "ferret"])

            operation_y_line_plot.setResultSet(resultsService.getPlotResults())

            operation_y_line_plot.addToTextOptions(optionsService.getExpression())
            operation_y_line_plot.addToTextOptions(optionsService.getDep_axis_scale())
            operation_y_line_plot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_y_line_plot.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_y_line_plot.addToMenuOptions(optionsService.getUse_graticules())
            operation_y_line_plot.addToMenuOptions(optionsService.getLine_or_sym())
            operation_y_line_plot.addToMenuOptions(optionsService.getLine_color())
            operation_y_line_plot.addToMenuOptions(optionsService.getLine_thickness())

            y_line_plot.addToOperations(operation_y_line_plot)
            y_line_plot.save(failOnError: true)
        }

        Product x_line_plot = Product.findByName("Longitude")
        if (!x_line_plot) {

            x_line_plot = new Product([name: "Longitude", title: "Longitude", view: "x", data_view: "x", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200002", minArgs: 1, maxArgs: 10])
            Operation operation_x_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D", type: "ferret"])

            operation_x_line_plot.setResultSet(resultsService.getPlotResults())

            operation_x_line_plot.addToTextOptions(optionsService.getExpression())
            operation_x_line_plot.addToTextOptions(optionsService.getDep_axis_scale())
            operation_x_line_plot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_x_line_plot.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_x_line_plot.addToMenuOptions(optionsService.getUse_graticules())
            operation_x_line_plot.addToMenuOptions(optionsService.getLine_or_sym())
            operation_x_line_plot.addToMenuOptions(optionsService.getLine_color())
            operation_x_line_plot.addToMenuOptions(optionsService.getLine_thickness())

            x_line_plot.addToOperations(operation_x_line_plot)
            x_line_plot.save(failOnError: true)
        }

        /*

        Difference plot, this is "hidden" from the UI and called when an XY grid is to be differenced.
        All the decorations about the title and ui_group are superfluous

         */
        Product compare_plot = Product.findByName("Compare_Plot")
        if (!compare_plot) {

            // #expression,#interpolate_data,#image_format,#size,#use_ref_map,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            compare_plot = new Product([name: "Compare_Plot", title: "Latitude-Longitude", view: "xy", data_view: "xy", ui_group: "Maps", geometry: GeometryType.GRID, hidden: true, product_order: "999999"]) // Not in ui, order unnecessary
            Operation operation_comparePlot = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot", type: "ferret"])

            operation_comparePlot.setResultSet(resultsService.getPlotResults())

            operation_comparePlot.addToMenuOptions(optionsService.getPalettes())
            operation_comparePlot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_comparePlot.addToTextOptions(optionsService.getFill_levels())

            compare_plot.addToOperations(operation_comparePlot)
            compare_plot.save(failOnError: true)
        }

        /*
        Difference plot, this is "hidden" from the UI and called when an XY grid is to be differenced.
                All the decorations about the title and ui_group are superfluous
                This is going to be for 1D in T

        */
        Product compare_plot_t = Product.findByName("Compare_Plot_T")
        if (!compare_plot_t) {

            // #expression,#interpolate_data,#image_format,#size,#use_ref_map,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            compare_plot_t = new Product([name: "Compare_Plot_T", title: "Time", view: "t", data_view: "t", ui_group: "Line Plots", geometry: GeometryType.GRID, hidden: true, product_order: "999999"]) // Not in ui, order unnecessary
            Operation operation_comparePlot_t = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot", type: "ferret"])

            operation_comparePlot_t.setResultSet(resultsService.getPlotResults())

            operation_comparePlot_t.addToMenuOptions(optionsService.getPalettes())
            operation_comparePlot_t.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_comparePlot_t.addToTextOptions(optionsService.getFill_levels())

            compare_plot_t.addToOperations(operation_comparePlot_t)
            compare_plot_t.save(failOnError: true)
        }
        /*
        Difference plot, this is "hidden" from the UI and called when an XY grid is to be differenced.
                All the decorations about the title and ui_group are superfluous
                This is going to be for 1D in X

        */
        Product compare_plot_x = Product.findByName("Compare_Plot_X")
        if (!compare_plot_x) {

            // #expression,#interpolate_data,#image_format,#size,#use_ref_map,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            compare_plot_x = new Product([name: "Compare_Plot_X", title: "Time", view: "t", data_view: "t", ui_group: "Line Plots", geometry: GeometryType.GRID, hidden: true, product_order: "999999"]) // Not in ui, order unnecessary
            Operation operation_comparePlot_x = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot", type: "ferret"])

            operation_comparePlot_x.setResultSet(resultsService.getPlotResults())

            operation_comparePlot_x.addToMenuOptions(optionsService.getPalettes())
            operation_comparePlot_x.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_comparePlot_x.addToTextOptions(optionsService.getFill_levels())

            compare_plot_x.addToOperations(operation_comparePlot_x)
            compare_plot_x.save(failOnError: true)
        }

        /*
        Difference plot, this is "hidden" from the UI and called when an XY grid is to be differenced.
                All the decorations about the title and ui_group are superfluous
                This is going to be for 1D in Y

        */
        Product compare_plot_y = Product.findByName("Compare_Plot_Y")
        if (!compare_plot_y) {

            // #expression,#interpolate_data,#image_format,#size,#use_ref_map,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            compare_plot_y = new Product([name: "Compare_Plot_Y", title: "Time", view: "t", data_view: "t", ui_group: "Line Plots", geometry: GeometryType.GRID, hidden: true, product_order: "999999"]) // Not in ui, order unnecessary
            Operation operation_comparePlot_y = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot", type: "ferret"])

            operation_comparePlot_y.setResultSet(resultsService.getPlotResults())

            operation_comparePlot_y.addToMenuOptions(optionsService.getPalettes())
            operation_comparePlot_y.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_comparePlot_y.addToTextOptions(optionsService.getFill_levels())

            compare_plot_y.addToOperations(operation_comparePlot_y)
            compare_plot_y.save(failOnError: true)
        }

        /*

        Regular old, lat/lon map xy

        */
        Product plot_2d_xy = Product.findByGeometryAndViewAndData_viewAndHidden(GeometryType.GRID, "xy", "xy", false)

        if (!plot_2d_xy) {

            // #expression,#interpolate_data,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            // Later:
            // #image_format,#size,#use_ref_map,

            plot_2d_xy = new Product([name: "Plot_2D_XY", title: "Latitude-Longitude", view: "xy", data_view: "xy", ui_group: "Maps", geometry: GeometryType.GRID, product_order: "100001"])

            Operation operation_plot_2d_xy = new Operation([output_template: "xy_zoom", service_action: "Plot_2D_XY", type: "ferret"])

            operation_plot_2d_xy.setResultSet(resultsService.getPlotResults())

            operation_plot_2d_xy.addToTextOptions(optionsService.getExpression())
            operation_plot_2d_xy.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_plot_2d_xy.addToMenuOptions(optionsService.getUse_graticules())
            operation_plot_2d_xy.addToYesNoOptions(optionsService.getMargins())
            operation_plot_2d_xy.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_plot_2d_xy.addToMenuOptions(optionsService.getPalettes())
            operation_plot_2d_xy.addToMenuOptions(optionsService.getContour_style())
            operation_plot_2d_xy.addToTextOptions(optionsService.getFill_levels())
            operation_plot_2d_xy.addToTextOptions(optionsService.getContour_levels())
            operation_plot_2d_xy.addToMenuOptions(optionsService.getMark_grid())
            operation_plot_2d_xy.addToYesNoOptions(optionsService.getSet_aspect())
            operation_plot_2d_xy.addToMenuOptions(optionsService.getLand_type())

            plot_2d_xy.addToOperations(operation_plot_2d_xy)
            plot_2d_xy.save(failOnError: true)

        }

        /*
        
        Vertical cross sections xz, yz
        
         */
        Product plot_2d_xz = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "xz", "xz")

        if ( !plot_2d_xz) {
            plot_2d_xz = new Product([name: "Plot_2D_xz", title: "Longitude-z", ui_group: "Vertical Cross Sections", view: "xz", data_view: "xz", geometry: GeometryType.GRID, product_order: "300001"])
            Operation operation_plot_2d_xz = new Operation([output_template:"plot_zoom", service_action: "Plot_2D", type: "ferret"])

            operation_plot_2d_xz.setResultSet(resultsService.getPlotResults())

            operation_plot_2d_xz.addToMenuOptions(optionsService.getPalettes())

            plot_2d_xz.addToOperations(operation_plot_2d_xz)
            plot_2d_xz.save(failOnError: true)

        }

        Product plot_2d_yz = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "yz", "yz")

        if ( !plot_2d_yz) {
            plot_2d_yz = new Product([name: "Plot_2D_yz", title: "Latitude-z", ui_group: "Vertical Cross Sections", view: "yz", data_view: "yz", geometry: GeometryType.GRID, product_order: "300002"])
            Operation operation_plot_2d_yz = new Operation([output_template:"plot_zoom", service_action: "Plot_2D", type: "ferret"])

            operation_plot_2d_yz.setResultSet(resultsService.getPlotResults())


            operation_plot_2d_yz.addToMenuOptions(optionsService.getPalettes())

            plot_2d_yz.addToOperations(operation_plot_2d_yz)
            plot_2d_yz.save(failOnError: true)

        }

        /*
        
        Hovmöller Diagrams, xt, yt and zt
        
         */
        Product plot_2d_xt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "xt", "xt")

        if ( !plot_2d_xt) {
            plot_2d_xt = new Product([name: "Plot_2D_xt", title: "Longitude-time", ui_group: "Hovmöller Diagram", view: "xt", data_view: "xt", geometry: GeometryType.GRID, product_order: "400001"])
            Operation operation_plot_2d_xt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D", type: "ferret"])

            operation_plot_2d_xt.setResultSet(resultsService.getPlotResults())

            operation_plot_2d_xt.addToMenuOptions(optionsService.getPalettes())

            plot_2d_xt.addToOperations(operation_plot_2d_xt)
            plot_2d_xt.save(failOnError: true)

        }

        Product plot_2d_yt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "yt", "yt")

        if ( !plot_2d_yt) {
            plot_2d_yt = new Product([name: "Plot_2D_yt", title: "Latitude-time", ui_group: "Hovmöller Diagram", view: "yt", data_view: "yt", geometry: GeometryType.GRID, product_order: "400002"])
            Operation operation_plot_2d_yt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D", type: "ferret"])

            operation_plot_2d_yt.setResultSet(resultsService.getPlotResults())

            operation_plot_2d_yt.addToMenuOptions(optionsService.getPalettes())

            plot_2d_yt.addToOperations(operation_plot_2d_yt)
            plot_2d_yt.save(failOnError: true)

        }

        Product plot_2d_zt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "zt", "zt")

        if ( !plot_2d_zt) {
            plot_2d_zt = new Product([name: "Plot_2D_zt", title: "Z-time", ui_group: "Hovmöller Diagram", view: "zt", data_view: "zt", geometry: GeometryType.GRID, product_order: "400003"])
            Operation operation_plot_2d_zt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D", type: "ferret"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_zt.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service method createReults before calling createOperations?")
            }
            MenuOption palettes = MenuOption.findByName("palette")
            if (palettes) {
                operation_plot_2d_zt.addToMenuOptions(palettes.properties)
            } else {
                log.error("Results sets not available. Did you use the results service method createOptions before calling createOperations?")
            }
            plot_2d_zt.addToOperations(operation_plot_2d_zt)
            plot_2d_zt.save(failOnError: true)

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
            charts_timeseries_plot = new Product([name: "Timeseries Plot", title: "Timeseries Plot", ui_group: "Line Plots", view: "t", data_view: "xyt", geometry: GeometryType.TIMESERIES, product_order: "dontknowyet"])
            Operation operation_timeseries_plot = new Operation([service_action: "client_plot", type: "client"])
            charts_timeseries_plot.addToOperations(operation_timeseries_plot)
            charts_timeseries_plot.save(failOnError: true)
        }
/*
<!-- animate XY plots -->
<operation ID="Animation_2D_XY" default="true" name="Animation" output_template="output_animation" service_action="Data_Extract_Frames" order="9999" category="animation">
  <service>ferret</service>
  <response ID="Data_Extract_Frames_Response">
    <result type="xml" ID="ferret_listing" streamable="true" mime_type="text/xml" file_suffix=".xml"/>
    <result type="debug" ID="debug" file_suffix=".txt"/>
  </response>
  <region>
    <intervals name="xy"/>
  </region>
  <grid_types>
    <grid_type name="regular"/>
  </grid_types>
  <optiondef IDREF="Options_2D_image_contour_animation_xy"/>
</operation>
*/
        Product animateSetup = Product.findByName("Animation_2D_XY")
        if ( !animateSetup ) {
            animateSetup = new Product([name: "Animation_2D_XY", title: "Setup Animate", ui_group: "NONE", view: "xyt", data_view: "xyt", geometry: GeometryType.GRID, hidden: true, product_order: "999999"])
            Operation animateSetup_op = new Operation([output_template:"none", service_action: "Data_Extract_Frames", type: "ferret"])

            animateSetup_op.setResultSet(resultsService.getAnimateSetupResults())

            animateSetup_op.addToMenuOptions(optionsService.getPalettes())

            animateSetup.addToOperations(animateSetup_op)
            animateSetup.save(failOnError: true)
        }
/*
  <operation name="Vector plot" ID="Plot_vector" default="true" output_template="zoom" service_action="Plot_vector" order="0103" private="false" category="visualization" isZoomable="true">
    <service>ferret</service>
    <response ID="PlotResp">
      <result type="image" ID="plot_image" streamable="true" mime_type="image/png" file_suffix=".png"/>
      <!-- <result type="ps" ID="plot_postscript" streamable="true" mime_type="application/postscript"
                                file_suffix=".ps"/> -->
      <result type="image" ID="ref_map" file_suffix=".png"/>
      <result type="map_scale" ID="map_scale" file_suffix=".xml"/>
      <result type="debug" ID="debug" file_suffix=".txt"/>
      <result type="cancel" ID="cancel" file_suffix=".txt"/>
    </response>
    <region>
      <intervals name="xy" type="Maps" title="Latitude-Longitude"/>

      <intervals name="xt" type="Hovmoller Plots" title="Longitude-Time"/>
      <intervals name="yt" type="Hovmoller Plots" title="Latitude-Time"/>

      <intervals name="yz" type="Depth Profiles" title="Latitude-Depth"/>
      <intervals name="xz" type="Depth Profiles" title="Longitude-Depth"/>

      <intervals name="zt" type="Hovmoller Plots" title="Depth-Time"/>
    </region>
    <grid_types>
      <grid_type name="vector"/>
    </grid_types>
    <optiondef IDREF="Options_Vector_7"/>
  </operation>


    Repeat this for all the other views as shown in the intervals section above


 */
        Product vector = Product.findByName("Plot_vector")
        if ( !vector ) {
            vector = new Product([name: "Plot_vector", title: "Vector Plot", ui_group: "Maps", view: "xy", data_view: "xy", geometry: GeometryType.VECTOR, hidden: false, product_order: "000001"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector.addToOperations(vector_op)
            vector.save(failOnError: true)
        }

        Product vector_xt = Product.findByName("Plot_vector_xt")
        if ( !vector_xt ) {
            vector_xt = new Product([name: "Plot_vector_xt", title: "Vector Plot, Longitude Time", ui_group: "Hovmöller Diagram", view: "xt", data_view: "xt", geometry: GeometryType.VECTOR, hidden: false, product_order: "000001"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector_xt.addToOperations(vector_op)
            vector_xt.save(failOnError: true)
        }

        Product vector_yt = Product.findByName("Plot_vector_yt")
        if ( !vector_yt ) {
            vector_yt = new Product([name: "Plot_vector_yt", title: "Vector Plot, Latitude Time", ui_group: "Hovmöller Diagram", view: "yt", data_view: "yt", geometry: GeometryType.VECTOR, hidden: false, product_order: "000002"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector_yt.addToOperations(vector_op)
            vector_yt.save(failOnError: true)
        }

        Product vector_yz = Product.findByName("Plot_vector_yz")
        if ( !vector_yz ) {
            vector_yz = new Product([name: "Plot_vector_yz", title: "Vector Plot, Latitude Depth", ui_group: "Depth Profile", view: "yz", data_view: "yz", geometry: GeometryType.VECTOR, hidden: false, product_order: "000002"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector_yz.addToOperations(vector_op)
            vector_yz.save(failOnError: true)
        }

        Product vector_xz = Product.findByName("Plot_vector_xz")
        if ( !vector_xz ) {
            vector_xz = new Product([name: "Plot_vector_xz", title: "Vector Plot, Longitude Depth", ui_group: "Depth Profile", view: "xz", data_view: "xz", geometry: GeometryType.VECTOR, hidden: false, product_order: "000001"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector_xz.addToOperations(vector_op)
            vector_xz.save(failOnError: true)
        }

        Product vector_zt = Product.findByName("Plot_vector_zt")
        if ( !vector_zt ) {
            vector_zt = new Product([name: "Plot_vector_zt", title: "Vector Plot, Depth Time", ui_group: "Depth Time", view: "zt", data_view: "zt", geometry: GeometryType.VECTOR, hidden: false, product_order: "000001"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector_zt.addToOperations(vector_op)
            vector_zt.save(failOnError: true)
        }
        /*
  <operation ID="Animation_2D_XY_vector" default="true" name="Animation" output_template="output_animation" service_action="Data_Extract_Frames" order="9999" category="animation">
    <service>ferret</service>
    <response ID="Data_Extract_Frames_Response">
      <result type="xml" ID="ferret_listing" streamable="true" mime_type="text/xml" file_suffix=".xml"/>
      <result type="debug" ID="debug" file_suffix=".txt"/>
    </response>
    <region>
      <intervals name="xyt"/>
      <intervals name="xy"/>
    </region>
    <grid_types>
      <grid_type name="vector"/>
    </grid_types>
    <optiondef IDREF="Options_Vector"/>
  </operation>

         */

        Product vectorAnim = Product.findByName("Animation_2D_XY_vector")
        if ( !vectorAnim ) {
            vectorAnim = new Product([name: "Animation_2D_XY_vector", title: "Animate Vector", ui_group: "none", view: "xy", data_view: "xy", geometry: GeometryType.VECTOR, hidden: true, product_order: "999999"])
            Operation vecAnimOp = new Operation([output_template: "plot_zoom", service_action: "Data_Extract_Frames", type: "ferret"])
            vecAnimOp.setResultSet(resultsService.getAnimateSetupResults())
            vecAnimOp.addToTextOptions(optionsService.getVector_length())
            vecAnimOp.addToTextOptions(optionsService.getVector_subsampling())
            vecAnimOp.addToMenuOptions(optionsService.getVector_style())
            vectorAnim.addToOperations(vecAnimOp)
            vectorAnim.save(failOnError: true)
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

                site = new Site([title: "Example LAS Site from Initial Installation"])
                // No site configured, so build the default site.

                // Turn off toast message by default.
                site.setToast(false)

                // Default link to to LAS documentation
                site.setInfoUrl("https://ferret.pmel.noaa.gov/LAS/")

                FerretEnvironment ferretEnvironment = FerretEnvironment.first()
                if (!ferretEnvironment) {
                    return
                }

                def dsets = ferretEnvironment.fer_dsets

                def coads
                def levitus
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
                                levitus = "$datadir" + File.separator + "data" + File.separator + "levitus_climatology.cdf"
                            }
                        }
                    } else {
                        coads = "$dsets" + File.separator + "data" + File.separator + "coads_climatology.cdf"
                        ocean_atlas = "$dsets" + File.separator + "data" + File.separator + "ocean_atlas_subset.nc"
                        levitus = "$dsets" + File.separator + "data" + File.separator + "levitus_climatology.cdf"
                    }
                }
                if (coads) {
                    log.debug("Ingesting COADS")
                    // Use the ferret shorthand for the default data set
                    def coadshash = IngestService.getDigest("coads_climatology")
                    Dataset coadsDS = Dataset.findByHash(coadshash)
                    if (!coadsDS) {
                        coadsDS = ingestService.ingest(coadshash, coads)
                        coadsDS.setTitle("COADS")
                        coadsDS.setStatus(Dataset.INGEST_FINISHED)
                        Variable v = coadsDS.getVariables().get(0);
                        v.addToVariableProperties(new VariableProperty([type: "ferret", name: "time_step", value: "3"]))
                        coadsDS.addToDatasetProperties(new DatasetProperty([type: "ferret", name: "time_step", value: "4"]))
                        Vector vector = new Vector();
                        vector.setGeometry(GeometryType.VECTOR)
                        vector.setTitle("Ocean Currents")
                        Variable ucomp = coadsDS.getVariables().find{it.name == "UWND"};
                        Variable vcomp = coadsDS.getVariables().find{it.name == "VWND"}
                        vector.setHash(ucomp.getHash() + "_" + vcomp.getHash())
                        vector.setName(ucomp.getName() + " and " + vcomp.getName())
                        vector.setU(ucomp)
                        vector.setV(vcomp)
                        coadsDS.addToVectors(vector)
                        coadsDS.save(flush: true)
                        elasticSearchService.index(coadsDS)
                    }
                    if (coadsDS) {
                        site.addToDatasets(coadsDS)
                    }
                }
                if (ocean_atlas) {
                    log.debug("Ingesting the ocean atlas subset.")
                    def oahash = IngestService.getDigest("ocean_atlas_subset")
                    Dataset ocean_atlasDS = Dataset.findByHash(oahash)
                    if (!ocean_atlasDS) {
                        ocean_atlasDS = ingestService.ingest(oahash, ocean_atlas)
                        ocean_atlasDS.setTitle("Ocean Atlas Subset")
                        ocean_atlasDS.setStatus(Dataset.INGEST_FINISHED)
                        ocean_atlasDS.save(flush: true)
                        elasticSearchService.index(ocean_atlasDS)
                    }
                    if (ocean_atlasDS) {
                        site.addToDatasets(ocean_atlasDS)
                    }
                }
                if ( levitus ) {
                    log.debug("Ingesting Levitus climatology")
                    def levhash = IngestService.getDigest("levitus_climatology.cdf")
                    Dataset levitusDS = Dataset.findByHash(levhash)
                    if ( !levitusDS ) {
                        levitusDS = ingestService.ingest(levhash, levitus)
                        levitusDS.setTitle("Levitus Ocean Climatology")
                        levitusDS.setStatus(Dataset.INGEST_FINISHED)
                        levitusDS.save(flush: true)
                        elasticSearchService.index(levitusDS)
                    }
                    if ( levitusDS ) {
                        site.addToDatasets(levitusDS)
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

//                TODO removed for gov't shutdown... log.debug("Ingesting example Timeseries DSG from ERDDAP")
//                def ts = "http://ferret.pmel.noaa.gov/engineering/erddap/tabledap/15min_w20_fdd7_a060"
//                List<AddProperty> properties = new ArrayList<>()
//                AddProperty hours = new AddProperty([name: "hours", value: ".25"])
//                properties.add(hours)
//                AddProperty display_hi = new AddProperty([name: "display_hi", value: "2018-02-20T00:00:00.000Z"])
//                properties.add(display_hi)
//                AddProperty display_lo = new AddProperty(([name: "display_lo", value: "2018-02-05T00:00:00.000Z"]))
//                properties.add(display_lo)
//                def dsgDataset = Dataset.findByHash(IngestService.getDigest(ts))
//                if ( !dsgDataset ) {
//                    dsgDataset = ingestService.ingestFromErddap(ts, properties)
//                    dsgDataset.setStatus(Dataset.INGEST_FINISHED)
//                    dsgDataset.save(flush: true)
//                    site.addToDatasets(dsgDataset)
//                }

//                log.debug("Ingesting UAF THREDDS server")
//                //def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalog.xml"
//                def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/ecowatch.ncddc.noaa.gov/thredds/oceanNomads/aggs/catalog_g_ncom_aggs.xml"
//                // def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/woa13/catalog.xml"
//                def erddap = "http://upwell.pfeg.noaa.gov/erddap/"
//                def uafDataset = Dataset.findByHash(IngestService.getDigest(uaf))
//                if ( ! uafDataset ) {
//                    uafDataset = ingestService.ingestFromThredds(uaf, erddap)
//                    uafDataset.setStatus(Dataset.INGEST_FINISHED)
//                    uafDataset.save(flush: true)
//                    site.addToDatasets(uafDataset)
//                }

//                ingestService.cleanup(site)

                def n = "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QA/vekm/3day"
                Dataset wind = ingestService.ingest(null, n)
                wind.setStatus(Dataset.INGEST_FINISHED)
                wind.save()
                site.addToDatasets(wind)

                site.save(failOnError: true)
            }
        }
    }
}
