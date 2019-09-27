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
import com.google.visualization.datasource.datatable.value.Value
import com.google.visualization.datasource.datatable.value.ValueType
import com.google.visualization.datasource.render.JsonRenderer
import grails.converters.JSON
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import org.grails.web.json.JSONElement
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import pmel.sdig.las.tabledap.DataRow
import pmel.sdig.las.tabledap.DataRowComparator
import pmel.sdig.las.type.GeometryType
import ucar.ma2.Array
import ucar.ma2.ArrayBoolean
import ucar.ma2.ArrayByte
import ucar.ma2.ArrayChar
import ucar.ma2.ArrayDouble
import ucar.ma2.ArrayFloat
import ucar.ma2.ArrayInt
import ucar.ma2.ArrayLong
import ucar.ma2.ArrayShort
import ucar.ma2.ArrayString
import ucar.ma2.InvalidRangeException
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.Attribute
import ucar.nc2.Dimension
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFileWriter
import ucar.nc2.time.CalendarDate
import ucar.nc2.time.CalendarDateUnit
import ucar.unidata.geoloc.LatLonPoint
import ucar.unidata.geoloc.LatLonPointImpl

import java.nio.charset.StandardCharsets

class ProductController {
//    static scaffold = Product
    AsyncFerretService asyncFerretService
    FerretService ferretService
    ProductService productService
    LASProxy lasProxy = new LASProxy()
    JsonParser jsonParser = new JsonParser()
    ResultsService resultsService
    DateTimeService dateTimeService
    ErddapService erddapService
    def summary () {
        def webAppDirectory = request.getSession().getServletContext().getRealPath("")
        // Until such time as "-" file names are allowed, deal with it with a link named without the "-"
        // i.e. webapp instead of web-app
        webAppDirectory = webAppDirectory.replaceAll("-", "");

        if (!webAppDirectory.endsWith(File.separator)) {
            webAppDirectory = webAppDirectory + File.separator;
        }
        FerretEnvironment fe = FerretEnvironment.first()
        String variable_url
        def data = fe.getFer_data().tokenize().each{
            String testpath = it+File.separator+"etopo20.cdf"
            File f = new File(testpath)
            if ( f.exists() ) variable_url = f.getPath()
        }
        if ( variable_url ) {
            String variable_name = "ROSE"
            String variable_title = "Relief of the Surface of the Earth"
            String variable_units = "M"
            String hash = "rose20_full"

            StringBuffer jnl = new StringBuffer()

            jnl.append("DEFINE SYMBOL data_0_dataset_name = World Map\n")
            jnl.append("DEFINE SYMBOL data_0_dataset_url = ${variable_url}\n")
            jnl.append("DEFINE SYMBOL data_0_grid_type = regular\n")
            jnl.append("DEFINE SYMBOL data_0_name = ${variable_name}\n")
            jnl.append("DEFINE SYMBOL data_0_ID = ${variable_name}\n")
            jnl.append("DEFINE SYMBOL data_0_region = region_0\n")
            jnl.append("DEFINE SYMBOL data_0_title = ${variable_title}\n")
            jnl.append("DEFINE SYMBOL data_0_units = ${variable_units}\n")
            jnl.append("DEFINE SYMBOL data_0_url = ${variable_url}\n")
            jnl.append("DEFINE SYMBOL data_0_var = ${variable_name}\n")

            jnl.append("DEFINE SYMBOL region_0_x_hi = 0\n")
            jnl.append("DEFINE SYMBOL region_0_x_lo = 360\n")

            jnl.append("DEFINE SYMBOL region_0_y_hi = 90\n")
            jnl.append("DEFINE SYMBOL region_0_y_lo = -90\n")

            jnl.append("DEFINE SYMBOL data_count = 1\n")
            jnl.append("DEFINE SYMBOL ferret_annotations = file\n")
            jnl.append("DEFINE SYMBOL ferret_service_action = Plot_2D_XY\n")
            jnl.append("DEFINE SYMBOL operation_name = Plot_2D_XY\n")

            jnl.append("DEFINE SYMBOL ferret_size = .85\n")
            jnl.append("DEFINE SYMBOL ferret_view = xy\n")
            jnl.append("DEFINE SYMBOL las_debug = false\n")
            jnl.append("DEFINE SYMBOL las_output_type = xml\n")
            jnl.append("DEFINE SYMBOL operation_ID = Plot_2D_XY\n")
            jnl.append("DEFINE SYMBOL operation_key = ${hash}\n")
            jnl.append("DEFINE SYMBOL operation_service = ferret\n")


            jnl.append("DEFINE SYMBOL ferret_service_action = Plot_2D_XY\n")
            jnl.append("DEFINE SYMBOL operation_name = Plot_2D_XY\n")

            // TODO this has to come from the config
            jnl.append("DEFINE SYMBOL product_server_ps_timeout = 3600\n")
            jnl.append("DEFINE SYMBOL product_server_ui_timeout = 10\n")
            jnl.append("DEFINE SYMBOL product_server_use_cache = true\n")
            //ha ha jnl.append("DEFINE SYMBOL product_server_version = 7.3")]

            jnl.append("DEFINE SYMBOL ferret_fill_levels = 60c\n")
            jnl.append("DEFINE SYMBOL ferret_memsize = 128\n")
            jnl.append("DEFINE SYMBOL ferret_contour_levels = vc\n")
            jnl.append("DEFINE SYMBOL ferret_palette = topo_osmc_blue_brown\n")
            jnl.append("DEFINE SYMBOL ferret_land_type = none\n")


            //TODO one for each variable
            // TODO check the value for null before applying

            ResultSet resultSet = resultsService.getThumbnailResults()
            def cache = true
            def cache_filename
            resultSet.results.each { Result result ->
                // All we care about is the plot
                result.url = "output${File.separator}${hash}_${result.name}${result.suffix}"
                result.filename = "${webAppDirectory}output${File.separator}${hash}_${result.name}${result.suffix}"
                // The plot file is the only cache result we care about
                if (result.name == "plot_image") {
                    File file = new File(result.filename)
                    cache = cache && file.exists()
                    if (cache) {
                        cache_filename = result.getFilename()
                    }
                }
            }

            if (cache) {
                render file: cache_filename, contentType: 'image/png'
            } else {
                for (int i = 0; i < resultSet.getResults().size(); i++) {

                    def result = resultSet.getResults().get(i)

                    jnl.append("DEFINE SYMBOL result_${result.name}_ID = ${result.name}\n")
                    if (result.type == "image") {
                        jnl.append("DEFINE SYMBOL result_${result.name}_filename = ${webAppDirectory}output${File.separator}${hash}_${result.name}_base_${result.suffix}\n")
                    } else {
                        jnl.append("DEFINE SYMBOL result_${result.name}_filename = ${webAppDirectory}output${File.separator}${hash}_${result.name}${result.suffix}\n")
                    }
                    jnl.append("DEFINE SYMBOL result_${result.name}_type = ${result.type}\n")


                }
                jnl.append("go Plot_2D_XY\n")

                def datasets = Dataset.findAllByVariableChildren(true)
                for (int i = 0; i < datasets.size(); i++) {
                    def dataset = datasets.get(i)
                    if (dataset.getVariables() && dataset.getVariables().size() > 0) {
                        def variable = dataset.getVariables().get(0)
                        def xaxis = variable.getGeoAxisX()
                        def yaxis = variable.getGeoAxisY()


                        def x = xaxis.getMin() + "," + xaxis.getMin() + "," + xaxis.getMax() + "," + xaxis.getMax()
                        def y = yaxis.getMin() + "," + yaxis.getMax() + "," + yaxis.getMax() + "," + yaxis.getMin()
                        jnl.append("LET xoutline = YSEQUENCE({${x}})\n")
                        jnl.append("LET youtline = YSEQUENCE({${y}})\n")
                        jnl.append("POLYGON/OVER/MODULO/LINE/COLOR=${ferretService.getFerretColorValue(i)}/THICK=2/TITLE=\"${dataset.title}\" xoutline, youtline\n")

                    }
                }

                jnl.append("FRAME/FORMAT=PNG/FILE=\"${webAppDirectory}output${File.separator}${hash}_plot_image.png\"\n");

                def ferretResult = ferretService.runScript(jnl)
                def error = ferretResult["error"];
                // TODO error image???
                if (error) {
                    log.error(ferretResult["message"]);
                    render file: "/tmp/error.png", contentType: 'image/png'
                } else {
                    ResultSet allResults = new ResultSet()
                    ferretService.addResults(resultSet, allResults, "Plot_2D_XY")
                    Result r = allResults.results.find { it.name == "plot_image" }
                    render file: r.getFilename(), contentType: 'image/png'
                }


            }
        } else {
            render file: "/tmp/error.png", contentType: 'image/png'
        }
    }
    def make() {

        Ferret ferret = Ferret.first();
        // TODO are we really going to have multiple sites? Each request will have to ID it's site. Easy enough...
        Site site = Site.first()
        def base = site.getBase()
        if ( !base ) {
            base = request.requestURL.toString()
            base = base.substring(0, base.indexOf("product/make"))
        }

        def requestJSON = request.JSON

        def reason;

        def hash = IngestService.getDigest(requestJSON.toString());

        log.debug(requestJSON.toString())

        def lasRequest = new LASRequest(requestJSON);


        Product product = Product.findByName(lasRequest.operation)
        def productName = product.getName()
        String view = product.getView()
        List<RequestProperty> properties = lasRequest.getRequestProperties();
        List<Analysis> analysis = lasRequest.getAnalysis()

        /*
        Loop through all of the operations. Accumulate the results from each operation into a global ResultsSet
         */

        ResultSet allResults = new ResultSet()
        allResults.setProduct(product.getName())
        allResults.setTargetPanel(lasRequest.getTargetPanel())
        def operations = product.getOperations()

        def webAppDirectory = request.getSession().getServletContext().getRealPath("")
        // Until such time as "-" file names are allowed, deal with it with a link named without the "-"
        // i.e. webapp instead of web-app
        webAppDirectory = webAppDirectory.replaceAll("-", "");

        if (!webAppDirectory.endsWith(File.separator)) {
            webAppDirectory = webAppDirectory + File.separator;
        }


        boolean cache = true;
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i)
            ResultSet resultSet = operation.getResultSet()
            resultSet.results.each { Result result ->
                result.url = "output${File.separator}${hash}_${result.name}${result.suffix}"
                result.filename = "${webAppDirectory}output${File.separator}${hash}_${result.name}${result.suffix}"
                File file = new File(result.filename)
                cache = cache && file.exists()
            }
        }

        if ( cache ) {
            for (int i = 0; i < operations.size(); i++) {
                Operation operation = operations.get(i)
                if ( operation.getType() == "ferret") {
                    def resultSet = product.operations.get(i).resultSet
                    ferretService.addResults(resultSet, allResults, product.getName())
                }
            }
        } else {


            List<String> chainedUrls = new ArrayList<String>();
            List<String> datasetHashes = lasRequest.getDatasetHashes()
            List<String> variableHashes = lasRequest.getVariableHashes()
            List<DataConstraint> constraints = lasRequest.getDataConstraints();


            for (int o = 0; o < operations.size(); o++) {
                Operation operation = operations.get(o)


                if (operation.getType() == "ferret") {

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

                    Dataset dataset = null
                    Variable variable = null

                    // There is one data set has entry and one variable hash entry for each variable in the request
                    // even if the variables are from the same data set.
                    for (int h = 0; h < datasetHashes.size(); h++) {
                        // Apply the analysis to the variable URL
                        dataset = Dataset.findByHash(datasetHashes.get(h))
                        variable = dataset.variables.find { Variable v -> v.hash == variableHashes.get(h) }

                        String variable_url;

                        // TODO all variables are in one file. This might ought not be an array
                        if ( chainedUrls.size() > 0 ) {
                            variable_url = chainedUrls.get(0)
                        } else {
                            variable_url = variable.getUrl()
                        }

                        String variable_name = variable.getName()
                        String variable_title = variable.getTitle()
                        String varable_hash = variable.getHash()
                        String dataset_hash = dataset.getHash()


                        def analysis_axes = new ArrayList<String>();
                        // TODO other variables?
                        // TODO for now only analysis on first variable, but don't really know what this should look like
                        if (h == 0 && analysis != null && analysis.get(h) != null) {

                            Analysis a = analysis.get(h)

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

                            variable_url = base + "thredds/dodsC/las/" + dataset_hash + "/" + varable_hash + "/" + sp.getName()

                        }

                        def tabledap = dataset.getDatasetPropertyGroup("tabledap_access")
                        tabledap.each {
                            jnl.append("DEFINE SYMBOL " + it.type + "_" + h + "_" + it.name + " = " + it.value + "\n")
                        }

                        if ( constraints ) {
                            for (int cidx = 0; cidx < constraints.size(); cidx++) {
                                DataConstraint c = constraints.get(cidx);
                                jnl.append("DEFINE SYMBOL constraint_${cidx}_lhs = ${c.lhs}\n")
                                jnl.append("DEFINE SYMBOL constraint_${cidx}_type = ${c.type}\n")
                                jnl.append("DEFINE SYMBOL constraint_${cidx}_op = ${c.op}\n")
                                jnl.append("DEFINE SYMBOL constraint_${cidx}_rhs = ${c.rhs}\n")
                            }
                        }

                        // TODO merge variable, dataset and global properties

                        // TODO GRID vs regular

                        jnl.append("DEFINE SYMBOL data_${h}_dataset_name = ${dataset.title}\n")
                        jnl.append("DEFINE SYMBOL data_${h}_dataset_url = ${variable_url}\n")
                        jnl.append("DEFINE SYMBOL data_${h}_grid_type = regular\n")
                        jnl.append("DEFINE SYMBOL data_${h}_name = ${variable_name}\n")
                        jnl.append("DEFINE SYMBOL data_${h}_ID = ${variable_name}\n")
                        jnl.append("DEFINE SYMBOL data_${h}_region = region_0\n")
                        jnl.append("DEFINE SYMBOL data_${h}_title = ${variable_title}\n")
                        if (variable.units) jnl.append("DEFINE SYMBOL data_${h}_units = ${variable.units}\n")
                        jnl.append("DEFINE SYMBOL data_${h}_url = ${variable_url}\n")
                        jnl.append("DEFINE SYMBOL data_${h}_var = ${variable_name}\n")

                        // TODO stop sending nulls maybe
                        // Is there an axesset?
                        if ( lasRequest.getAxesSets().size() > h ) {
                            // Does it have non-null values?
                            if (!analysis_axes.contains("t") && lasRequest.getAxesSets().get(h).getThi() && lasRequest.getAxesSets().get(h).getTlo()) {
                                jnl.append("DEFINE SYMBOL region_${h}_t_hi = ${lasRequest.getAxesSets().get(h).getThi()}\n")
                                jnl.append("DEFINE SYMBOL region_${h}_t_lo = ${lasRequest.getAxesSets().get(h).getTlo()}\n")
                            }
                            if (!analysis_axes.contains("x")) {
                                jnl.append("DEFINE SYMBOL region_${h}_x_hi = ${lasRequest.getAxesSets().get(h).getXhi()}\n")
                                jnl.append("DEFINE SYMBOL region_${h}_x_lo = ${lasRequest.getAxesSets().get(h).getXlo()}\n")
                            }
                            if (!analysis_axes.contains("y")) {
                                jnl.append("DEFINE SYMBOL region_${h}_y_hi = ${lasRequest.getAxesSets().get(h).getYhi()}\n")
                                jnl.append("DEFINE SYMBOL region_${h}_y_lo = ${lasRequest.getAxesSets().get(h).getYlo()}\n")
                            }
                            if (!analysis_axes.contains("z")) {
                                if (lasRequest.getAxesSets().get(h).getZlo()) jnl.append("DEFINE SYMBOL region_${h}_z_lo = ${lasRequest.getAxesSets().get(h).getZlo()}\n")
                                if (lasRequest.getAxesSets().get(h).getZhi()) jnl.append("DEFINE SYMBOL region_${h}_z_hi = ${lasRequest.getAxesSets().get(h).getZhi()}\n")
                            }
                        }

//                        if (lasRequest.getAxesSets().get(1) != null) {
//
//
//                            if (!lasRequest.getAxesSets().get(1).getThi().equals("null")) jnl.append("DEFINE SYMBOL region_1_t_hi = ${lasRequest.getAxesSets().get(1).getThi()}\n")
//                            if (!lasRequest.getAxesSets().get(1).getTlo().equals("null")) jnl.append("DEFINE SYMBOL region_1_t_lo = ${lasRequest.getAxesSets().get(1).getTlo()}\n")
//                            // TODO value null
//                            // FIXME maybe don't send null from client
//                            if (lasRequest.getAxesSets().get(1).getXhi()) jnl.append("DEFINE SYMBOL region_1_x_hi = ${lasRequest.getAxesSets().get(1).getXhi()}\n")
//                            if (lasRequest.getAxesSets().get(1).getXlo()) jnl.append("DEFINE SYMBOL region_1_x_lo = ${lasRequest.getAxesSets().get(1).getXlo()}\n")
//
//                            if (lasRequest.getAxesSets().get(1).getYhi()) jnl.append("DEFINE SYMBOL region_1_y_hi = ${lasRequest.getAxesSets().get(1).getYhi()}\n")
//                            if (lasRequest.getAxesSets().get(1).getYlo()) jnl.append("DEFINE SYMBOL region_1_y_lo = ${lasRequest.getAxesSets().get(1).getYlo()}\n")
//
//                            if (lasRequest.getAxesSets().get(1).getZlo()) jnl.append("DEFINE SYMBOL region_1_z_lo = ${lasRequest.getAxesSets().get(1).getZlo()}\n")
//                            if (lasRequest.getAxesSets().get(1).getZhi()) jnl.append("DEFINE SYMBOL region_1_z_hi = ${lasRequest.getAxesSets().get(1).getZhi()}\n")
//
//                        }

                    }
                    jnl.append("DEFINE SYMBOL data_count = ${datasetHashes.size()}\n")
                    jnl.append("DEFINE SYMBOL ferret_annotations = file\n")
//        jnl.append("DEFINE SYMBOL ferret_fill_type = fill\n")
//        jnl.append("DEFINE SYMBOL ferret_image_format = gif\n")
//        jnl.append("DEFINE SYMBOL ferret_land_type = shade\n")
                    jnl.append("DEFINE SYMBOL ferret_service_action = ${product.operations.get(0).service_action}\n")
                    jnl.append("DEFINE SYMBOL ferret_size = 1.0\n")
                    jnl.append("DEFINE SYMBOL ferret_view = " + view + "\n")
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


                    for (int i = 0; i < properties.size(); i++) {
                        RequestProperty p = properties.get(i)
                        jnl.append("DEFINE SYMBOL " + p.getType() + "_" + p.getName() + " = " + p.getValue() + "\n")
                    }



                    def resultSet = product.operations.get(o).resultSet

                    resultSet.results.each { Result result ->

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


                    }


                    jnl.append("go ${operation.service_action}\n")

                    def ferretResult = ferretService.runScript(jnl)
                    def error = ferretResult["error"];
                    if (error) {
                        log.error(ferretResult["message"]);
                        def errorMessage = [error: ferretResult["message"], targetPanel: lasRequest.getTargetPanel()]
                        render errorMessage as JSON
                    }

                    ferretService.addResults(resultSet, allResults, product.getName())


                } else if (operation.type == "erddap") {

                    log.debug("Making erddap product."); // debug

                    // TODO for now assume there is only one data set on an ERDDAP operation
                    Dataset dataset = Dataset.findByHash(datasetHashes.get(0))

                    try {
                        String chainedURL = erddapService.makeNetcdfFile(lasRequest, productName, operation, dataset)
                        chainedUrls.add(chainedURL)
                    } catch (Exception e) {
                        def errorMessage = [error: e.getMessage(), targetPanel: lasRequest.getTargetPanel()]
                        render errorMessage as JSON
                    }

                    // TODO now what? with the file

                } // operation type

            } // loop on operations

        } // cacheHit

        log.debug("Finished product request, rendering response...")
        JSON.use("deep") {
            render allResults as JSON
        }

    }


    def erddapDataRequest() {

        def requestJSON = request.JSON
        // TODO This is the cache key. Must make caching aware of data requests.
        def hash = IngestService.getDigest(requestJSON.toString());
        def reason = ""
        def lasRequest = new LASRequest(requestJSON);
        List<RequestProperty> requestProperties = lasRequest.getRequestProperties();
        if ( requestProperties ) {
            requestProperties.each {
                if ( it.type == "dashboard" && it.name == "request_type" ) {
                    reason = it.value
                }
            }
        }
        Dataset dataset = Dataset.findByHash(lasRequest.getDatasetHashes().get(0))
        def url = dataset.getUrl()+".json?";
        String vars = ""
        for (int i = 0; i < lasRequest.getVariableHashes().size(); i++) {
            def vhash = lasRequest.getVariableHashes().get(i);
            Variable variable = dataset.variables.find {Variable v -> v.hash == vhash}
            vars = vars + variable.getName();
            if ( i < lasRequest.getVariableHashes().size() - 1 ) {
                vars = vars + ","
            }
        }

        String constraint = ""
        String tlo = lasRequest.getAxesSets().get(0).getTlo();
        String thi = lasRequest.getAxesSets().get(0).getThi();

        if ( tlo ) {
            if ( !constraint.isEmpty() ) constraint = constraint + "&"
            constraint = constraint + "time>=" + tlo;
        }
        if ( thi ) {
            if ( !constraint.isEmpty() ) constraint = constraint + "&"
            constraint = constraint + "time<=" + thi
        }


        // TODO use names from data set in constraints????

        String xlo = lasRequest.getAxesSets().get(0).getXlo();
        String xhi = lasRequest.getAxesSets().get(0).getXhi();

        if ( xlo ) {
            if ( !constraint.isEmpty() ) constraint = constraint + "&"
            constraint = constraint + "longitude>=" + xlo;
        }
        if ( xhi ) {
            if ( !constraint.isEmpty() ) constraint = constraint + "&"
            constraint = constraint + "longitude<=" + xhi
        }

        String ylo = lasRequest.getAxesSets().get(0).getYlo();
        String yhi = lasRequest.getAxesSets().get(0).getYhi();

        if ( ylo ) {
            if ( !constraint.isEmpty() ) constraint = constraint + "&"
            constraint = constraint + "latitude>=" + ylo;
        }
        if ( yhi ) {
            if ( !constraint.isEmpty() ) constraint = constraint + "&"
            constraint = constraint + "latitude<=" + yhi
        }

        List<DataQualifier> qualifierList = lasRequest.getDataQualifiers();
        for (int i = 0; i < qualifierList.size(); i++) {
            DataQualifier dq = qualifierList.get(i);
            if ( dq.isDistinct() ) {
                constraint = constraint+ "&distinct()"
            } else if ( !dq.getType().isEmpty() ) {
                constraint = constraint + "&" + dq.getType() + "("
                List<String> vs = dq.getVariables()
                for (int j = 0; j < vs.size(); j++) {
                    constraint = constraint + vs.get(j)
                    if ( j < vs.size() - 1 )
                        constraint = constraint + ","
                }
                constraint = constraint = + ")"
            }

        }

        url = url + URLEncoder.encode(vars + "&" + constraint, StandardCharsets.UTF_8.name())
        try {
            log.info(reason)
            log.info(url)
            String data = lasProxy.executeGetMethodAndReturnResult(url);
            render data;
        } catch (Exception e) {
            throw e;
        }

    }
    def datatable () {
        /*


        Make something that looks like this:



        {
          "cols": [
                   {"id":"","label":"Topping","pattern":"","type":"string"},
                   {"id":"","label":"Slices","pattern":"","type":"number"}
                  ],
          "rows": [
                   {"c":[{"v":"Mushrooms","f":null},{"v":3,"f":null}]},
                   {"c":[{"v":"Onions","f":null},{"v":1,"f":null}]},
                   {"c":[{"v":"Olives","f":null},{"v":1,"f":null}]},
                   {"c":[{"v":"Zucchini","f":null},{"v":1,"f":null}]},
                   {"c":[{"v":"Pepperoni","f":null},{"v":2,"f":null}]}
                  ]
        }


       The data table code below is not working. When rendering rows with null values it mak


         */


        DateTimeFormatter iso = ISODateTimeFormat.dateTimeNoMillis();
        def requestJSON = request.JSON
        // This is the cache key. Must make caching aware of data requests.
        def hash = IngestService.getDigest(requestJSON.toString());

        // A request is either an LAS request for which the ERDDAP URL must be formed, or
        // it is a request that contains the ERDDAP URL as a query parameter called "url"
        def url = null
        def reason = ""
        if (requestJSON) {
            def lasRequest = new LASRequest(requestJSON);

            List<RequestProperty> requestProperties = lasRequest.getRequestProperties();
            if ( requestProperties ) {
                requestProperties.each {
                    if ( it.type == "dashboard" && it.name == "request_type" ) {
                        reason = it.value
                    }
                }
            }

            Dataset dataset = Dataset.findByHash(lasRequest.getDatasetHashes().get(0))
            def varNames = ""
            lasRequest.getVariableHashes().each { String vhash ->
                Variable variable = dataset.variables.find { Variable v -> v.hash == vhash }
                if ( !varNames.isEmpty() ) varNames = varNames + ",";
                varNames = varNames + variable.name
            }

            def latname = dataset.getDatasetPropertyValue("tabledap_access","latitude");
            def lonname = dataset.getDatasetPropertyValue("tabledap_access","longitude");
            def zname = dataset.getDatasetPropertyValue("tabledap_access","altitude")

            String constraint = ""
            String tlo = lasRequest.getAxesSets().get(0).getTlo();
            String thi = lasRequest.getAxesSets().get(0).getThi();

            if ( tlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "time>=" + tlo;
            }
            if ( thi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "time<=" + thi
            }

            String xlo = lasRequest.getAxesSets().get(0).getXlo()
            String xhi = lasRequest.getAxesSets().get(0).getXhi()

            if ( xlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + lonname + ">=" + xlo;
            }
            if ( xhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + lonname + "<=" + xhi
            }

            String ylo = lasRequest.getAxesSets().get(0).getYlo()
            String yhi = lasRequest.getAxesSets().get(0).getYhi()

            if ( ylo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + latname + ">=" + ylo;
            }
            if ( yhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + latname + "<=" + yhi
            }

            String zlo = lasRequest.getAxesSets().get(0).getZlo()
            String zhi = lasRequest.getAxesSets().get(0).getZhi()

            if ( zlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + zname + ">=" + zlo;
            }
            if ( zhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + zname + "<=" + zhi
            }

            if ( !constraint.isEmpty() ) constraint = "&" + constraint



            List constraintElements = lasRequest.getDataConstraints()

            // For now we will not use the decimated data set when there is any constraint applied to the request.
            // In the future we may need to distinguish between a sub-set variable constraint and a variable constraint.
            // The two types below should be enough to tell the difference.

            if ( constraintElements && constraintElements.size() > 0 ) {

                Iterator cIt = constraintElements.iterator();
                while (cIt.hasNext()) {
                    def dc = (DataConstraint) cIt.next();
                    String lhsString = dc.getLhs()
                    String opString = dc.getOp()
                    String rhsString = dc.getRhs()
                    String tType = dc.getType()
                    if (tType.equals("variable")) {
                        constraint = constraint + "&" + dc.getAsString();  //op is now <, <=, ...
                        // Gather lt and gt constraint so see if modulo variable treatment is required.
//
//                        TODO what to do about this
//
//                        if (modulo_vars.contains(lhsString) && (opString.equals("lt") || opString.equals("le"))) {
//                            constrained_modulo_vars_lt.put(lhsString, constraint);
//                        }
//                        if (modulo_vars.contains(lhsString) && (opString.equals("gt") || opString.equals("ge"))) {
//                            constrained_modulo_vars_gt.put(lhsString, constraint);
//                        }

                    } else if (tType.equals("text")) {
                        constraint = constraint + "&" + dc.getAsERDDAPString()  //op is now <, <=, ...
                    }
                }
            }

            // TODO Add data qualifiers (distinct and orderByMax in this case).
            List<DataQualifier> qualifierList = lasRequest.getDataQualifiers();
            if ( qualifierList ) {
                for (int i = 0; i < qualifierList.size(); i++) {
                    DataQualifier dq = qualifierList.get(i)
                    if (dq.isDistinct()) {
                        constraint = constraint + "&distinct()"
                    } else if (!dq.getType().isEmpty()) {
                        constraint = constraint + "&" + dq.getType() + "(\""
                        List<String> vs = dq.getVariables()
                        for (int j = 0; j < vs.size(); j++) {
                            constraint = constraint + vs.get(j)
                            if (j < vs.size() - 1)
                                constraint = constraint + ","
                        }
                        constraint = constraint + "\")"
                    }

                }
            }

            url = dataset.getUrl() + ".json?" + URLEncoder.encode(varNames + constraint, StandardCharsets.UTF_8.name());
        } else {
            url = params.url
        }
        if (url) {

            log.info(reason);
            log.info(url);

            String jsonText = lasProxy.executeGetMethodAndReturnResult(url)
            JsonElement json = jsonParser.parse(jsonText)
            def table = json.getAsJsonObject().get("table")
            JsonArray names = table.get("columnNames").asJsonArray
            JsonArray types = table.get("columnTypes").asJsonArray
            JSONObject datatable = new JSONObject();
            JSONArray cols = new JSONArray();
            datatable.accumulate("cols", cols)
            for (int i = 0; i < names.size(); i++) {
                JSONObject col = new JSONObject();
                col.accumulate("id", "");
                col.accumulate("pattern", "")
                String name = names.get(i).asString
                col.accumulate("label", name)
                String type = types.get(i).asString
                ValueType chartColumnType = null;
                if (type.equals("String") && !name.toLowerCase().equals("time")) {
                    col.accumulate("type", "string")
                } else if (type.equals("String") && name.toLowerCase().equals("time")) {
                    col.accumulate("type", "datetime")
                } else if (type.equals("float")) {
                    col.accumulate("type", "number")
                } else if (type.equals("double")) {
                    col.accumulate("type", "number")
                }
                cols.add(col)
            }


            def rows = table.get("rows")
            JSONArray jsonRows = new JSONArray();
            datatable.accumulate("rows", jsonRows)
            rows.each { JsonArray row ->
                JSONObject tableRow = new JSONObject()
                jsonRows.add(tableRow)
                for (int i = 0; i < row.size(); i++) {
                    JSONObject rowValues = new JSONObject()
                    tableRow.accumulate("c", rowValues)
                    String name = names.get(i).asString
                    String type = types.get(i).asString
                    if (type.equals("String") && !name.toLowerCase().equals("time")) {
                        rowValues.accumulate("v", row.get(i).asString)
                        rowValues.accumulate("f", null)
                    } else if (type.equals("String") && name.toLowerCase().equals("time")) {
                        String dtstring = row.get(i).asString
                        DateTime dt = dateTimeService.dateTimeFromIso(dtstring);
                        int year = dt.getYear()
                        int month = dt.getMonthOfYear() - 1
                        int day = dt.getDayOfMonth()
                        rowValues.accumulate("v", "Date("+ year + "," +  month + "," + day + "," + dt.getHourOfDay() + "," + dt.getMinuteOfHour() + "," + dt.getSecondOfMinute() + "," + dt.getMillisOfSecond() +")")
                        rowValues.accumulate("f", null)
                    } else if (type.equals("float")) {
                        if (row.get(i).isJsonNull()) {
                            rowValues.accumulate("v", null)
                            rowValues.accumulate("f", null)
                        } else {
                            rowValues.accumulate("v", row.get(i).asDouble)
                            rowValues.accumulate("f", null)
                        }
                    } else if (type.equals("double")) {
                        JsonElement je = row.get(i);
                        if (je == null || je.isJsonNull()) {
                            rowValues.accumulate("v", null)
                            rowValues.accumulate("f", null)
                        } else {
                            rowValues.accumulate("v", je.asFloat)
                            rowValues.accumulate("f", null)
                        }
                    } else {
                        log.info("Cell value of unknown type encountered: " + type)
                    }
                }
            }

            render datatable
        } else {
            throw new DataSourceException(ReasonType.INVALID_REQUEST, "url parameter not provided");
        }


    }


}
