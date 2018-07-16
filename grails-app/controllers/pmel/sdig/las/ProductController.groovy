package pmel.sdig.las

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.visualization.datasource.base.DataSourceException
import com.google.visualization.datasource.base.ReasonType
import com.google.visualization.datasource.datatable.ColumnDescription
import com.google.visualization.datasource.datatable.DataTable
import com.google.visualization.datasource.datatable.TableRow
import com.google.visualization.datasource.datatable.value.DateTimeValue
import com.google.visualization.datasource.datatable.value.NumberValue
import com.google.visualization.datasource.datatable.value.ValueType
import com.google.visualization.datasource.render.JsonRenderer
import grails.converters.JSON
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat

import java.nio.charset.StandardCharsets

class ProductController {
    static scaffold = Product
    def FerretService ferretService
    def ProductService productService
    LASProxy lasProxy = new LASProxy()
    JsonParser jsonParser = new JsonParser()
    def make() {

        Ferret ferret = Ferret.first();

        def requestJSON = request.JSON

        def hash = IngestService.getDigest(requestJSON.toString());

        log.debug(requestJSON.toString())

        def lasRequest = new LASRequest(requestJSON);

        Dataset dataset = Dataset.findByHash(lasRequest.getDatasetHashes().get(0))
        Variable variable = dataset.variables.find { Variable v -> v.hash == lasRequest.getVariableHashes().get(0) }
        Product product = Product.findByName(lasRequest.operation)
        String view = product.getView()
        List<RequestProperty> properties = lasRequest.getRequestProperties();
        List<Analysis> analysis = lasRequest.getAnalysis()

        // TODO compound -- Should be a loop on operations.

        StringBuffer jnl = new StringBuffer()

        /** All symbols for an XY plot...
         DEFINE SYMBOL data_0_ID = airt
         DEFINE SYMBOL data_0_dataset_ID = coads_climatology_cdf
         DEFINE SYMBOL data_0_dataset_name = COADS climatology
         DEFINE SYMBOL data_0_dataset_url = file:coads_climatology
         DEFINE SYMBOL data_0_dsid = coads_climatology_cdf
         DEFINE SYMBOL data_0_ftds_url = http://gazelle.weathertopconsulting.com:8282/thredds/dodsC/las/coads_climatology_cdf/data_coads_climatology.jnl
         DEFINE SYMBOL data_0_grid_type = regular
         DEFINE SYMBOL data_0_intervals = xyt
         DEFINE SYMBOL data_0_name = AIR TEMPERATURE
         DEFINE SYMBOL data_0_points = xyt
         DEFINE SYMBOL data_0_region = region_0
         DEFINE SYMBOL data_0_title = AIR TEMPERATURE
         DEFINE SYMBOL data_0_units = DEG C
         DEFINE SYMBOL data_0_url = coads_climatology
         DEFINE SYMBOL data_0_var = airt
         DEFINE SYMBOL data_0_xpath = /lasdata/datasets/coads_climatology_cdf/variables/airt
         DEFINE SYMBOL data_count = 1
         DEFINE SYMBOL ferret_annotations = file
         DEFINE SYMBOL ferret_fill_type = fill
         DEFINE SYMBOL ferret_image_format = gif
         DEFINE SYMBOL ferret_land_type = shade
         DEFINE SYMBOL ferret_service_action = Plot_2D_XY
         DEFINE SYMBOL ferret_size = .8333
         DEFINE SYMBOL ferret_view = xy
         DEFINE SYMBOL las_debug = false
         DEFINE SYMBOL las_output_type = xml
         DEFINE SYMBOL operation_ID = Plot_2D_XY_zoom
         DEFINE SYMBOL operation_key = B070A6828DCD95F39BB5D58F17277A50
         DEFINE SYMBOL operation_name = Color plot
         DEFINE SYMBOL operation_service = ferret
         DEFINE SYMBOL operation_service_action = Plot_2D_XY
         DEFINE SYMBOL product_server_clean_age = 168
         DEFINE SYMBOL product_server_clean_interval = 24
         DEFINE SYMBOL product_server_clean_time = 00:01
         DEFINE SYMBOL product_server_clean_units = hour
         DEFINE SYMBOL product_server_default_catid = ocean_atlas_subset
         DEFINE SYMBOL product_server_default_dsid = ocean_atlas_subset
         DEFINE SYMBOL product_server_default_operation = Plot_2D_XY_zoom
         DEFINE SYMBOL product_server_default_option = Options_2D_image_contour_xy_7
         DEFINE SYMBOL product_server_default_varid = TEMP-ocean_atlas_subset
         DEFINE SYMBOL product_server_default_view = xy
         DEFINE SYMBOL product_server_ps_timeout = 3600
         DEFINE SYMBOL product_server_ui_timeout = 10
         DEFINE SYMBOL product_server_use_cache = true
         DEFINE SYMBOL product_server_version = 7.3
         DEFINE SYMBOL region_0_t_hi = 15-Jan
         DEFINE SYMBOL region_0_t_lo = 15-Jan
         DEFINE SYMBOL region_0_x_hi = 360
         DEFINE SYMBOL region_0_x_lo = 0
         DEFINE SYMBOL region_0_y_hi = 90
         DEFINE SYMBOL region_0_y_lo = -90
         DEFINE SYMBOL result_annotations_ID = annotations
         DEFINE SYMBOL result_annotations_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_annotations.xml
         DEFINE SYMBOL result_annotations_type = annotations
         DEFINE SYMBOL result_cancel_ID = cancel
         DEFINE SYMBOL result_cancel_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_cancel.txt
         DEFINE SYMBOL result_cancel_type = cancel
         DEFINE SYMBOL result_count = 11
         DEFINE SYMBOL result_debug_ID = debug
         DEFINE SYMBOL result_debug_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_debug.txt
         DEFINE SYMBOL result_debug_type = debug
         DEFINE SYMBOL result_map_scale_ID = map_scale
         DEFINE SYMBOL result_map_scale_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_map_scale.xml
         DEFINE SYMBOL result_map_scale_type = map_scale
         DEFINE SYMBOL result_plot_image_ID = plot_image
         DEFINE SYMBOL result_plot_image_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_plot_image.png
         DEFINE SYMBOL result_plot_image_type = image
         DEFINE SYMBOL result_plot_pdf_ID = plot_pdf
         DEFINE SYMBOL result_plot_pdf_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_plot_pdf.pdf
         DEFINE SYMBOL result_plot_pdf_type = pdf
         DEFINE SYMBOL result_plot_ps_ID = plot_ps
         DEFINE SYMBOL result_plot_ps_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_plot_ps.ps
         DEFINE SYMBOL result_plot_ps_type = ps
         DEFINE SYMBOL result_plot_svg_ID = plot_svg
         DEFINE SYMBOL result_plot_svg_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_plot_svg.svg
         DEFINE SYMBOL result_plot_svg_type = svg
         DEFINE SYMBOL result_ref_map_ID = ref_map
         DEFINE SYMBOL result_ref_map_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_ref_map.png
         DEFINE SYMBOL result_ref_map_type = image
         DEFINE SYMBOL result_rss_ID = rss
         DEFINE SYMBOL result_rss_filename = /home/rhs/tomcat/webapps/struts2/output/50C7105E454C06D199E6B62844E08B67_rss.rss
         DEFINE SYMBOL result_rss_type = rss
         */

        /** Symbols for a difference..
         *
         ! Symbols from the server
         DEFINE SYMBOL data_0_ID = TEMP-ocean_atlas_subset
         DEFINE SYMBOL data_0_dataset_ID = ocean_atlas_subset
         DEFINE SYMBOL data_0_dataset_name = Subset of World Ocean Atlas monthly 1994 Monthly Means
         DEFINE SYMBOL data_0_dataset_url = ocean_atlas_subset
         DEFINE SYMBOL data_0_dsid = ocean_atlas_subset
         DEFINE SYMBOL data_0_ftds_url = http://dunkel.pmel.noaa.gov:8920/thredds/dodsC/las/ocean_atlas_subset/data_ocean_atlas_subset.jnl
         DEFINE SYMBOL data_0_grid_type = regular
         DEFINE SYMBOL data_0_intervals = xyzt
         DEFINE SYMBOL data_0_name = Temperature
         DEFINE SYMBOL data_0_points = xyzt
         DEFINE SYMBOL data_0_region = region_0
         DEFINE SYMBOL data_0_title = Temperature
         DEFINE SYMBOL data_0_units = Deg C
         DEFINE SYMBOL data_0_url = http://dunkel.pmel.noaa.gov:8920/thredds/dodsC/las/ocean_atlas_subset/data_ocean_atlas_subset.jnl
         DEFINE SYMBOL data_0_var = TEMP
         DEFINE SYMBOL data_1_ID = TEMP-ocean_atlas_subset
         DEFINE SYMBOL data_1_dataset_ID = ocean_atlas_subset
         DEFINE SYMBOL data_1_dataset_name = Subset of World Ocean Atlas monthly 1994 Monthly Means
         DEFINE SYMBOL data_1_dataset_url = ocean_atlas_subset
         DEFINE SYMBOL data_1_dsid = ocean_atlas_subset
         DEFINE SYMBOL data_1_ftds_url = http://dunkel.pmel.noaa.gov:8920/thredds/dodsC/las/ocean_atlas_subset/data_ocean_atlas_subset.jnl
         DEFINE SYMBOL data_1_grid_type = regular
         DEFINE SYMBOL data_1_intervals = xyzt
         DEFINE SYMBOL data_1_name = Temperature
         DEFINE SYMBOL data_1_points = xyzt
         DEFINE SYMBOL data_1_region = region_1
         DEFINE SYMBOL data_1_title = Temperature
         DEFINE SYMBOL data_1_units = Deg C
         DEFINE SYMBOL data_1_url = http://dunkel.pmel.noaa.gov:8920/thredds/dodsC/las/ocean_atlas_subset/data_ocean_atlas_subset.jnl
         DEFINE SYMBOL data_1_var = TEMP
         DEFINE SYMBOL data_1_xpath = /lasdata/datasets/ocean_atlas_subset/variables/TEMP-ocean_atlas_subset
         DEFINE SYMBOL data_count = 2
         DEFINE SYMBOL ferret_annotations = file
         DEFINE SYMBOL ferret_fill_type = fill
         DEFINE SYMBOL ferret_image_format = gif
         DEFINE SYMBOL ferret_land_type = contour
         DEFINE SYMBOL ferret_service_action = Compare_Plot
         DEFINE SYMBOL ferret_size = .8333
         DEFINE SYMBOL ferret_view = xy
         DEFINE SYMBOL las_debug = false
         DEFINE SYMBOL las_output_type = xml
         DEFINE SYMBOL operation_ID = Compare_Plot
         DEFINE SYMBOL operation_key = D1F0FFE15252EDFC5563D822A193F274
         DEFINE SYMBOL operation_name = Difference plot
         DEFINE SYMBOL operation_service = ferret
         DEFINE SYMBOL operation_service_action = Compare_Plot
         DEFINE SYMBOL product_server_clean_age = 168
         DEFINE SYMBOL product_server_clean_interval = 24
         DEFINE SYMBOL product_server_clean_time = 00:01
         DEFINE SYMBOL product_server_clean_units = hour
         DEFINE SYMBOL product_server_default_catid = ocean_atlas_subset
         DEFINE SYMBOL product_server_default_dsid = ocean_atlas_subset
         DEFINE SYMBOL product_server_default_operation = Plot_2D_XY_zoom
         DEFINE SYMBOL product_server_default_option = Options_2D_image_contour_xy_7
         DEFINE SYMBOL product_server_default_varid = TEMP-ocean_atlas_subset
         DEFINE SYMBOL product_server_default_view = xy
         DEFINE SYMBOL product_server_ps_timeout = 3600
         DEFINE SYMBOL product_server_ui_timeout = 20
         DEFINE SYMBOL product_server_use_cache = true
         DEFINE SYMBOL product_server_version = 8.4
         DEFINE SYMBOL region_0_t_hi = 16-Jan-0001 00:00
         DEFINE SYMBOL region_0_t_lo = 16-Jan-0001 00:00
         DEFINE SYMBOL region_0_x_hi = 378.5
         DEFINE SYMBOL region_0_x_lo = 20.5
         DEFINE SYMBOL region_0_y_hi = 88.5
         DEFINE SYMBOL region_0_y_lo = -89.5
         DEFINE SYMBOL region_0_z_hi = 0
         DEFINE SYMBOL region_0_z_lo = 0
         DEFINE SYMBOL region_1_t_hi = 16-Feb-0001 00:00
         DEFINE SYMBOL region_1_t_lo = 16-Feb-0001 00:00
         DEFINE SYMBOL region_1_z_hi = 0
         DEFINE SYMBOL region_1_z_lo = 0
         DEFINE SYMBOL result_annotations_ID = annotations
         DEFINE SYMBOL result_annotations_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_annotations.xml
         DEFINE SYMBOL result_annotations_type = annotations
         DEFINE SYMBOL result_cancel_ID = cancel
         DEFINE SYMBOL result_cancel_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_cancel.txt
         DEFINE SYMBOL result_cancel_type = cancel
         DEFINE SYMBOL result_count = 11
         DEFINE SYMBOL result_debug_ID = debug
         DEFINE SYMBOL result_debug_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_debug.txt
         DEFINE SYMBOL result_debug_type = debug
         DEFINE SYMBOL result_map_scale_ID = map_scale
         DEFINE SYMBOL result_map_scale_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_map_scale.map_scale
         DEFINE SYMBOL result_map_scale_type = map_scale
         DEFINE SYMBOL result_plot_image_ID = plot_image
         DEFINE SYMBOL result_plot_image_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_plot_image.png
         DEFINE SYMBOL result_plot_image_type = image
         DEFINE SYMBOL result_plot_pdf_ID = plot_pdf
         DEFINE SYMBOL result_plot_pdf_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_plot_pdf.pdf
         DEFINE SYMBOL result_plot_pdf_type = pdf
         DEFINE SYMBOL result_plot_ps_ID = plot_ps
         DEFINE SYMBOL result_plot_ps_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_plot_ps.ps
         DEFINE SYMBOL result_plot_ps_type = ps
         DEFINE SYMBOL result_plot_svg_ID = plot_svg
         DEFINE SYMBOL result_plot_svg_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_plot_svg.svg
         DEFINE SYMBOL result_plot_svg_type = svg
         DEFINE SYMBOL result_ref_map_ID = ref_map
         DEFINE SYMBOL result_ref_map_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_ref_map.png
         DEFINE SYMBOL result_ref_map_type = image
         DEFINE SYMBOL result_rss_ID = rss
         DEFINE SYMBOL result_rss_filename = /home/users/rhs/tomcat/webapps/las/output/8B3C55B5A1F07D023C0D3A941D14BE71_rss.rss
         DEFINE SYMBOL result_rss_type = rss
         GO ($operation_service_action)

         */

        // TODO for each data set hash and variasble hash, print the data symbols
        // TODO same with properties

        // Apply the analysis to the variable URL
        String variable_url = variable.getUrl()
        String variable_title = variable.getTitle()
        String variable_name = variable.getName()
        String varable_hash = variable.getHash()
        String dataset_hash = dataset.getHash()

        def analysis_axes = ""
        // TODO other variables?
        if (analysis != null && analysis.get(0) != null) {

            Analysis a = analysis.get(0)

            String type = a.getTransformation();

            // Make dataset specific directory
            String dir = ferret.tempDir + File.separator + "dynamic" + File.separator + dataset_hash + File.separator + varable_hash
            File ftds_dir = new File(dir)
            if (!ftds_dir.exists()) {
                ftds_dir.mkdirs()
            }


            analysis_axes = a.getAxes()
            List<AnalysisAxis> axes = a.getAnalysisAxes()


            StringBuffer ftds_jnl = new StringBuffer()

            ftds_jnl.append("use \"" + variable_url + "\";\n");

            for (int i = 0; i < axes.size(); i++) {
                AnalysisAxis ax = axes.get(i)
                String axisType = ax.getType()
                String hi = ax.getHi()
                String lo = ax.getLo()
                String axisString = axisType + "="
                if (axisType.equals("t")) {
                    axisString = axisString + "\"" + lo + "\":\"" + hi + "\"";
                } else {
                    axisString = axisString + "" + lo + ":" + hi + "";
                }
                ftds_jnl.append("let/d=1 " + variable.getName() + "_transformed = " + variable.getName() + "[d=1," + axisString + "@" + type + "];\n")
                variable_title = variable.getName() + "[d=1," + axisString.replace("\"", "") + "@" + type + "]"
                variable_name = variable.getName() + "_transformed"

            }
            ftds_jnl.append("SET ATT/LIKE=" + variable.getName() + " " + variable.getName() + "_transformed ;\n")

            File sp = File.createTempFile("ftds_" + a.hash() + "_", ".jnl", ftds_dir);
            sp.withWriter { out ->
                out.writeLine(ftds_jnl.toString().stripIndent())
            }

            // TODO Assign the new title
            // TODO Redefine the URL that will be used, data_set_hash/variable_hash/jnl_hash.nc
            //variable_url = "http://bilbo.weathertopconsulting.com:8482/las/thredds/dodsC/las/" + dataset_hash + "/" + varable_hash + "/" + sp.getName()
            variable_url = "http://dunkel.pmel.noaa.gov:8920/las/thredds/dodsC/las/" + dataset_hash + "/" + varable_hash + "/" + sp.getName()


        }


        // TODO merge variable, dataset and global properties
        jnl.append("DEFINE SYMBOL data_0_dataset_name = ${dataset.title}\n")
        jnl.append("DEFINE SYMBOL data_0_dataset_url = ${variable_url}\n")
        // TODO GRID vs regular
        jnl.append("DEFINE SYMBOL data_0_grid_type = regular\n")
        jnl.append("DEFINE SYMBOL data_0_name = ${variable_name}\n")
        jnl.append("DEFINE SYMBOL data_0_region = region_0\n")
        jnl.append("DEFINE SYMBOL data_0_title = ${variable_title}\n")
        if (variable.units) jnl.append("DEFINE SYMBOL data_0_units = ${variable.units}\n")
        jnl.append("DEFINE SYMBOL data_0_url = ${variable_url}\n")
        jnl.append("DEFINE SYMBOL data_0_var = ${variable_name}\n")

        if ( lasRequest.getDatasetHashes().size() == 2 ) {
            Dataset d2 = Dataset.findByHash(lasRequest.getDatasetHashes().get(1))
            Variable v2 = dataset.variables.find { Variable v -> v.hash == lasRequest.getVariableHashes().get(1) }
            jnl.append("DEFINE SYMBOL data_1_dataset_name = ${d2.title}\n")
            jnl.append("DEFINE SYMBOL data_1_dataset_url = ${v2.url}\n")
            // TODO GRID vs regular
            jnl.append("DEFINE SYMBOL data_1_grid_type = regular\n")
            jnl.append("DEFINE SYMBOL data_1_name = ${v2.name}\n")
            jnl.append("DEFINE SYMBOL data_1_region = region_1\n")
            jnl.append("DEFINE SYMBOL data_1_title = ${v2.title}\n")
            if (variable.units) jnl.append("DEFINE SYMBOL data_0_units = ${v2.units}\n")
            jnl.append("DEFINE SYMBOL data_1_url = ${v2.url}\n")
            jnl.append("DEFINE SYMBOL data_1_var = ${v2.name}\n")
        }

        jnl.append("DEFINE SYMBOL data_count = ${lasRequest.datasetHashes.size()}\n")
        jnl.append("DEFINE SYMBOL ferret_annotations = file\n")
        jnl.append("DEFINE SYMBOL ferret_fill_type = fill\n")
        jnl.append("DEFINE SYMBOL ferret_image_format = gif\n")
        jnl.append("DEFINE SYMBOL ferret_land_type = shade\n")
        jnl.append("DEFINE SYMBOL ferret_service_action = ${product.operations.get(0).service_action}\n")
        jnl.append("DEFINE SYMBOL ferret_size = 1.0\n")
        jnl.append("DEFINE SYMBOL ferret_view = "+view +"\n")
        jnl.append("DEFINE SYMBOL las_debug = false\n")
        jnl.append("DEFINE SYMBOL las_output_type = xml\n")
        jnl.append("DEFINE SYMBOL operation_ID = ${product.name}\n")
        jnl.append("DEFINE SYMBOL operation_key = ${hash}\n")
        jnl.append("DEFINE SYMBOL operation_name = ${product.name}\n")
        jnl.append("DEFINE SYMBOL operation_service = ferret\n")
        jnl.append("DEFINE SYMBOL operation_service_action = ${product.operations.get(0).service_action}\n")

        // TODO this has to come from the config
        jnl.append("DEFINE SYMBOL product_server_ps_timeout = 3600\n")
        jnl.append("DEFINE SYMBOL product_server_ui_timeout = 10\n")
        jnl.append("DEFINE SYMBOL product_server_use_cache = true\n")
        //ha ha jnl.append("DEFINE SYMBOL product_server_version = 7.3")
        //TODO one for each variable
        // TODO check the value for null before applying


        if (!analysis_axes.contains("t")) {
            jnl.append("DEFINE SYMBOL region_0_t_hi = ${lasRequest.getAxesSet1().getThi()}\n")
            jnl.append("DEFINE SYMBOL region_0_t_lo = ${lasRequest.getAxesSet1().getTlo()}\n")
        }
        if (!analysis_axes.contains("x")) {
            jnl.append("DEFINE SYMBOL region_0_x_hi = ${lasRequest.getAxesSet1().getXhi()}\n")
            jnl.append("DEFINE SYMBOL region_0_x_lo = ${lasRequest.getAxesSet1().getXlo()}\n")
        }
        if ( !analysis_axes.contains("y") ) {
            jnl.append("DEFINE SYMBOL region_0_y_hi = ${lasRequest.getAxesSet1().getYhi()}\n")
            jnl.append("DEFINE SYMBOL region_0_y_lo = ${lasRequest.getAxesSet1().getYlo()}\n")
        }
        if ( !analysis_axes.contains("z") ) {
            if (lasRequest.getAxesSet1().getZlo()) jnl.append("DEFINE SYMBOL region_0_z_lo = ${lasRequest.getAxesSet1().getZlo()}\n")
            if (lasRequest.getAxesSet1().getZhi()) jnl.append("DEFINE SYMBOL region_0_z_hi = ${lasRequest.getAxesSet1().getZhi()}\n")
        }

        if ( lasRequest.getAxesSet2() != null ) {

            if (lasRequest.getAxesSet2().getThi()) jnl.append("DEFINE SYMBOL region_1_t_hi = ${lasRequest.getAxesSet2().getThi()}\n")
            if (lasRequest.getAxesSet2().getTlo()) jnl.append("DEFINE SYMBOL region_1_t_lo = ${lasRequest.getAxesSet2().getTlo()}\n")

            if (lasRequest.getAxesSet2().getXhi()) jnl.append("DEFINE SYMBOL region_1_x_hi = ${lasRequest.getAxesSet2().getXhi()}\n")
            if (lasRequest.getAxesSet2().getXlo()) jnl.append("DEFINE SYMBOL region_1_x_lo = ${lasRequest.getAxesSet2().getXlo()}\n")

            if (lasRequest.getAxesSet2().getYhi()) jnl.append("DEFINE SYMBOL region_1_y_hi = ${lasRequest.getAxesSet2().getYhi()}\n")
            if (lasRequest.getAxesSet2().getYlo()) jnl.append("DEFINE SYMBOL region_1_y_lo = ${lasRequest.getAxesSet2().getYlo()}\n")

            if (lasRequest.getAxesSet2().getZlo()) jnl.append("DEFINE SYMBOL region_1_z_lo = ${lasRequest.getAxesSet2().getZlo()}\n")
            if (lasRequest.getAxesSet2().getZhi()) jnl.append("DEFINE SYMBOL region_1_z_hi = ${lasRequest.getAxesSet2().getZhi()}\n")

        }


        for (int i = 0; i < properties.size(); i++) {
            RequestProperty p = properties.get(i)
            jnl.append("DEFINE SYMBOL "+p.getType()+"_"+p.getName()+" = "+p.getValue()+"\n")
        }

        def webAppDirectory = request.getSession().getServletContext().getRealPath("")
        // Until such time as "-" file names are allowed, deal with it with a link named without the "-"
        // i.e. webapp instead of web-app
        webAppDirectory = webAppDirectory.replaceAll("-", "");

        if ( !webAppDirectory.endsWith(File.separator) ) {
            webAppDirectory = webAppDirectory + File.separator;
        }


        def resultSet = product.operations.get(0).resultSet

        def mapScaleFile

        resultSet.results.each{Result result ->

            /*
    String name
	String type
    String mime_type
	String suffix
    String url
    String filename
             */

            jnl.append("DEFINE SYMBOL result_${result.name}_ID = ${result.name}\n")
            jnl.append("DEFINE SYMBOL result_${result.name}_filename = ${webAppDirectory}output${File.separator}${hash}_${result.name}${result.suffix}\n")
            jnl.append("DEFINE SYMBOL result_${result.name}_type = ${result.type}\n")

            result.url = "output${File.separator}${hash}_${result.name}${result.suffix}"
            result.filename = "${webAppDirectory}output${File.separator}${hash}_${result.name}${result.suffix}"

        }


        jnl.append("go ${product.operations.get(0).service_action}\n")

        def ferretResult = ferretService.runScript(jnl)
        def error = ferretResult["error"];
        if ( error ) {
            log.error(ferretResult["message"]);
            return null
        }

        def mapScaleResult = resultSet.results.find{Result result -> result.name == "map_scale"}
        def annotationsResult = resultSet.results.find{Result result -> result.name == "annotations"}

        def mapScale = productService.makeMapScale(mapScaleResult.filename)

        def annotations = productService.makeAnnotations(annotationsResult.filename);


        def productResults = [resultSet: resultSet, mapScale: mapScale, targetPanel: lasRequest.getTargetPanel(), annotationGroups: annotations]

        JSON.use("deep") {
            render productResults as JSON
        }

    }

    def erddapDataRequest() {

        def requestJSON = request.JSON
        // This is the cache key. Must make caching aware of data requests.
        def hash = IngestService.getDigest(requestJSON.toString());

        def lasRequest = new LASRequest(requestJSON);
        Dataset dataset = Dataset.findByHash(lasRequest.getDatasetHashes().get(0))
        Variable variable = dataset.variables.find {Variable v -> v.hash == lasRequest.getVariableHashes().get(0)}
        def url = dataset.getUrl()+".json";
        def data = new URL(url).getText()

        render data;

    }
    def datatable() {

        DateTimeFormatter iso = ISODateTimeFormat.dateTimeNoMillis();
        def requestJSON = request.JSON
        // This is the cache key. Must make caching aware of data requests.
        def hash = IngestService.getDigest(requestJSON.toString());

        // A request is either an LAS request for which the ERDDAP URL must be formed, or
        // it is a request that contains the ERDDAP URL as a query parameter called "url"
        def url = null
        if (requestJSON) {
            def lasRequest = new LASRequest(requestJSON);

            Dataset dataset = Dataset.findByHash(lasRequest.getDatasetHashes().get(0))
            def varNames = ""
            lasRequest.getVariableHashes().each { String vhash ->
                Variable variable = dataset.variables.find { Variable v -> v.hash == vhash }
                varNames = varNames + "," + variable.name
            }

            String constraint = ""
            String tlo = lasRequest.getAxesSet1().getTlo();
            String thi = lasRequest.getAxesSet1().getThi();

            if ( tlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "time>=" + tlo;
            }
            if ( thi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "time<=" + thi
            }

            String xlo = lasRequest.getAxesSet1().getXlo()
            String xhi = lasRequest.getAxesSet1().getXhi()

            if ( xlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "longitude>=" + xlo;
            }
            if ( xhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "longitude<=" + xhi
            }

            String ylo = lasRequest.getAxesSet1().getYlo()
            String yhi = lasRequest.getAxesSet1().getYhi()

            if ( ylo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "latitude>=" + ylo;
            }
            if ( yhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "latitude<=" + yhi
            }

            String zlo = lasRequest.getAxesSet1().getZlo()
            String zhi = lasRequest.getAxesSet1().getZhi()

            if ( zlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "depth>=" + zlo;
            }
            if ( zhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "latitude<=" + zhi
            }

            if ( !constraint.isEmpty() ) constraint = "&" + constraint

            url = dataset.getUrl() + ".json?" + URLEncoder.encode("time" + varNames + constraint, StandardCharsets.UTF_8.name());
        } else {
            url = params.url
        }
        if (url) {

            String jsonText = lasProxy.executeGetMethodAndReturnResult(url)
            JsonElement json = jsonParser.parse(jsonText)
            def table = json.getAsJsonObject().get("table")
            JsonArray names = table.get("columnNames").asJsonArray
            JsonArray types = table.get("columnTypes").asJsonArray

            DataTable dataTable = new DataTable();
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i).asString
                String type = types.get(i).asString
                ValueType chartColumnType = null;
                if (type.equals("String") && !name.toLowerCase().equals("time")) {
                    chartColumnType = ValueType.TEXT
                } else if (type.equals("String") && name.toLowerCase().equals("time")) {
                    chartColumnType = ValueType.DATETIME
                } else if (type.equals("float")) {
                    chartColumnType = ValueType.NUMBER
                } else if (type.equals("double")) {
                    chartColumnType = ValueType.NUMBER
                }
                ColumnDescription columnDescription = new ColumnDescription(name, chartColumnType, name)
                dataTable.addColumn(columnDescription)
            }


            def rows = table.get("rows")
            rows.each { JsonArray row ->
                TableRow tableRow = new TableRow()
                for (int i = 0; i < row.size(); i++) {
                    String name = names.get(i).asString
                    String type = types.get(i).asString
                    if (type.equals("String") && !name.toLowerCase().equals("time")) {
                        tableRow.addCell(row.get(i).asString)
                    } else if (type.equals("String") && name.toLowerCase().equals("time")) {
                        DateTime dt = iso.parseDateTime(row.get(i).asString).withZone(DateTimeZone.UTC);
                        tableRow.addCell(new DateTimeValue(dt.getYear(), dt.getMonthOfYear()-1, dt.getDayOfMonth(), dt.getHourOfDay(), dt.getMinuteOfHour(), dt.getSecondOfMinute(), dt.getMillisOfSecond()))
                    } else if (type.equals("float")) {
                        if (row.get(i).isJsonNull()) {
                            tableRow.addCell(NumberValue.getNullValue())
                        } else {
                            tableRow.addCell(row.get(i).asDouble)
                        }
                    } else if (type.equals("double")) {
                        if (row.get(i).isJsonNull()) {
                            tableRow.addCell(NumberValue.getNullValue())
                        } else {
                            tableRow.addCell(row.get(i).asFloat)
                        }
                    }
                }

                dataTable.addRow(tableRow)

            }


            render JsonRenderer.renderDataTable(dataTable, true, false, false)
        } else {
            throw new DataSourceException(ReasonType.INVALID_REQUEST, "url parameter not provided");
        }
    }

}
