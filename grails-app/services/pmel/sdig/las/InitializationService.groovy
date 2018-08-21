package pmel.sdig.las

import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import pmel.sdig.las.*
import pmel.sdig.las.type.GeometryType

import javax.servlet.ServletContext

class InitializationService {

    IngestService ingestService
    OptionsService optionsService
    ServletContext servletContext

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
        if ( !ferret.validate() ) {
            ferret.errors.each {
                log.debug(it)
            }
        }

        def tempFile = new File(ferret.tempDir);
        if ( !tempFile.exists() ) {
            tempFile.mkdirs()
        }
        ferret.addToArguments(new Argument([value: "-cimport sys; import pyferret; (errval, errmsg) = pyferret.init(sys.argv[1:], True)"]))
        ferret.addToArguments(new Argument([value: "-nodisplay"]))
        ferret.addToArguments(new Argument([value: "-script"]))
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
        //TODO this is debug I think this is going to be right --->
        //FileWriter configFileWriter = new FileWriter(new File("../content/ftds/catalog.xml"))
        FileWriter configFileWriter = new FileWriter(new File("/home/rhs/tomcat/content/ftds/catalog.xml"))  //TODO... DEBUG
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
        //FileWriter configFileWriter = new FileWriter(new File("../webapps/las#thredds/WEB-INF/classes/resources/iosp/FerretConfig.xml"))
        // TODO this is debug loction --->
        FileWriter configFileWriter = new FileWriter(new File("/home/rhs/tomcat/webapps/las#thredds/WEB-INF/classes/FerretConfig.xml"))
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

    def createResults() {

        ResultSet results_debug_image_mapscale_annotations = new ResultSet([name: "results_debug_image_mapscale_annotations"])

        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "text", suffix: ".txt"])
        Result plot_image = new Result([name: "plot_image", mime_type: "image/png", type: "image", suffix: ".png"])
        Result map_scale = new Result([name: "map_scale", mime_type: "text/xml", type: "xml", suffix: ".xml"])
        Result annotations = new Result([name: "annotations", mime_type: "text/xml", type: "xml", suffix: ".xml"])

        results_debug_image_mapscale_annotations.addToResults(debug)
        results_debug_image_mapscale_annotations.addToResults(plot_image)
        results_debug_image_mapscale_annotations.addToResults(map_scale)
        results_debug_image_mapscale_annotations.addToResults(annotations)


        results_debug_image_mapscale_annotations.save(failOnError: true)

    }

    def createProducts() {

        createResults()

        /*

        This are all the line plots, xyzt

         */
        
        // Sort order set by the product_order string. I use first three digits to sort the groups, and the next three digits for the operation within the group.
        Product t_line_plot = Product.findByName("Time")
        if (!t_line_plot) {

            t_line_plot = new Product([name: "Time", title: "Time", view: "t", data_view: "t", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200001", minArgs: 1, maxArgs: 10])
            Operation operation_t_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D"])

            // inherit="#expression,#interpolate_data,#image_format,#size,#use_graticules,#margins,#deg_min_sec"/
            // inherit="#Options_Default_7,#line_or_sym,#trend_line,#line_color,#line_thickness,#dep_axis_scale"


            ResultSet results_t_line_plot = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_t_line_plot) {
                ResultSet toned = new ResultSet(results_t_line_plot.properties)
                operation_t_line_plot.setResultSet(toned)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

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
            Operation operation_z_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D"])

            ResultSet results_z_line_plot = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_z_line_plot) {
                ResultSet zoned = new ResultSet(results_z_line_plot.properties)
                operation_z_line_plot.setResultSet(zoned)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

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
            Operation operation_y_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D"])

            ResultSet results_y_line_plot = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_y_line_plot) {
                ResultSet yoned = new ResultSet(results_y_line_plot.properties)
                operation_y_line_plot.setResultSet(yoned)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

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
            Operation operation_x_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D"])

            ResultSet results_x_line_plot = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_x_line_plot) {
                ResultSet xoned = new ResultSet(results_x_line_plot.properties)
                operation_x_line_plot.setResultSet(xoned)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

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
            Operation operation_comparePlot = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot"])

            ResultSet results_compare_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_compare_plot_2d) {
                ResultSet twod = new ResultSet(results_compare_plot_2d.properties)
                operation_comparePlot.setResultSet(twod)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

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
        Operation operation_comparePlot_t = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot"])

        ResultSet results_compare_plot_t = ResultSet.findByName("results_debug_image_mapscale_annotations")
        if (results_compare_plot_t) {
            ResultSet twod = new ResultSet(results_compare_plot_t.properties)
            operation_comparePlot_t.setResultSet(twod)
        } else {
            log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
        }

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
            Operation operation_comparePlot_x = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot"])

            ResultSet results_compare_plot_x = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_compare_plot_x) {
                ResultSet twod = new ResultSet(results_compare_plot_x.properties)
                operation_comparePlot_x.setResultSet(twod)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

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
            Operation operation_comparePlot_y = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot"])

            ResultSet results_compare_plot_y = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_compare_plot_y) {
                ResultSet twod = new ResultSet(results_compare_plot_y.properties)
                operation_comparePlot_y.setResultSet(twod)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

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

            Operation operation_plot_2d_xy = new Operation([output_template: "xy_zoom", service_action: "Plot_2D_XY"])

            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_plot_2d) {
                ResultSet twod = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_xy.setResultSet(twod)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

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
            Operation operation_plot_2d_xz = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_xz.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

            operation_plot_2d_xz.addToMenuOptions(optionsService.getPalettes())

            plot_2d_xz.addToOperations(operation_plot_2d_xz)
            plot_2d_xz.save(failOnError: true)

        }

        Product plot_2d_yz = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "yz", "yz")

        if ( !plot_2d_yz) {
            plot_2d_yz = new Product([name: "Plot_2D_yz", title: "Latitude-z", ui_group: "Vertical Cross Sections", view: "yz", data_view: "yz", geometry: GeometryType.GRID, product_order: "300002"])
            Operation operation_plot_2d_yz = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_yz.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

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
            Operation operation_plot_2d_xt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_xt.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

            operation_plot_2d_xt.addToMenuOptions(optionsService.getPalettes())

            plot_2d_xt.addToOperations(operation_plot_2d_xt)
            plot_2d_xt.save(failOnError: true)

        }

        Product plot_2d_yt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "yt", "yt")

        if ( !plot_2d_yt) {
            plot_2d_yt = new Product([name: "Plot_2D_yt", title: "Latitude-time", ui_group: "Hovmöller Diagram", view: "yt", data_view: "yt", geometry: GeometryType.GRID, product_order: "400002"])
            Operation operation_plot_2d_yt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_yt.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

            operation_plot_2d_yt.addToMenuOptions(optionsService.getPalettes())

            plot_2d_yt.addToOperations(operation_plot_2d_yt)
            plot_2d_yt.save(failOnError: true)

        }

//        Product plot_2d_zt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "zt", "zt")
//
//        if ( !plot_2d_zt) {
//            plot_2d_zt = new Product([name: "Plot_2D_zt", title: "Z-time", ui_group: "Hovmöller Diagram", view: "zt", data_view: "zt", geometry: GeometryType.GRID, product_order: "400003"])
//            Operation operation_plot_2d_zt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
//            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
//            if ( results_plot_2d ) {
//                ResultSet rs = new ResultSet(results_plot_2d.properties)
//                operation_plot_2d_zt.setResultSet(rs)
//            } else {
//                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
//            }
//            MenuOption palettes = MenuOption.findByName("palette")
//            if (palettes) {
//                operation_plot_2d_zt.addToMenuOptions(palettes.properties)
//            } else {
//                log.error("Results sets not available. Did you use the results service menthod createOptions before calling createOperations?")
//            }
//            plot_2d_zt.addToOperations(operation_plot_2d_zt)
//            plot_2d_zt.save(failOnError: true)
//
//        }


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

                ingestService.cleanup(site)
                site.save(failOnError: true)
            }
        }
    }
}
