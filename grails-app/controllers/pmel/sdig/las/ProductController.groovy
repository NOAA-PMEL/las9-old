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
        Variable variable = dataset.variables.find {Variable v -> v.hash == lasRequest.getVariableHashes().get(0)}
        Product product = Product.findByName(lasRequest.operation)
        List<RequestProperty> properties = lasRequest.getRequestProperties();

        // TODO compound -- Should be a loop on operations.

        def temp = ferret.getTempDir();
        StringBuffer jnl = new StringBuffer()


        /*
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

        // TODO for each data set hash and variasble hash, print the data symbols

        // same with properties


        // TODO merge variable, dataset and global properties
        jnl.append("DEFINE SYMBOL data_0_dataset_name = ${dataset.title}\n")
        jnl.append("DEFINE SYMBOL data_0_dataset_url = ${variable.url}\n")
        // TODO GRID vs regular
        jnl.append("DEFINE SYMBOL data_0_grid_type = regular\n")
        jnl.append("DEFINE SYMBOL data_0_name = ${variable.name}\n")
        // TODO region by variable in list
        jnl.append("DEFINE SYMBOL data_0_region = region_0\n")
        jnl.append("DEFINE SYMBOL data_0_title = ${variable.title}\n")
        if (variable.units) jnl.append("DEFINE SYMBOL data_0_units = ${variable.units}\n")
        jnl.append("DEFINE SYMBOL data_0_url = ${variable.url}\n")
        jnl.append("DEFINE SYMBOL data_0_var = ${variable.name}\n")
        jnl.append("DEFINE SYMBOL data_count = ${lasRequest.datasetHashes.size()}\n")
        jnl.append("DEFINE SYMBOL ferret_annotations = file\n")
        jnl.append("DEFINE SYMBOL ferret_fill_type = fill\n")
        jnl.append("DEFINE SYMBOL ferret_image_format = gif\n")
        jnl.append("DEFINE SYMBOL ferret_land_type = shade\n")
        jnl.append("DEFINE SYMBOL ferret_service_action = ${product.operations.get(0).service_action}\n")
        jnl.append("DEFINE SYMBOL ferret_size = 1.0\n")
        jnl.append("DEFINE SYMBOL ferret_view = xy\n")
        jnl.append("DEFINE SYMBOL las_debug = false\n")
        jnl.append("DEFINE SYMBOL las_output_type = xml\n")
        jnl.append("DEFINE SYMBOL operation_ID = ${product.name}\n")
        jnl.append("DEFINE SYMBOL operation_key = ${hash}\n")
        jnl.append("DEFINE SYMBOL operation_name = ${product.name}\n")
        jnl.append("DEFINE SYMBOL operation_service = ferret\n")
        jnl.append("DEFINE SYMBOL operation_service_action = ${product.operations.get(0).service_action}\n")


        jnl.append("DEFINE SYMBOL product_server_ps_timeout = 3600\n")
        jnl.append("DEFINE SYMBOL product_server_ui_timeout = 10\n")
        jnl.append("DEFINE SYMBOL product_server_use_cache = true\n")
        //ha ha jnl.append("DEFINE SYMBOL product_server_version = 7.3")
        //TODO one for each variable
        jnl.append("DEFINE SYMBOL region_0_t_hi = ${lasRequest.thi}\n")
        jnl.append("DEFINE SYMBOL region_0_t_lo = ${lasRequest.tlo}\n")
        jnl.append("DEFINE SYMBOL region_0_x_hi = ${lasRequest.xhi}\n")
        jnl.append("DEFINE SYMBOL region_0_x_lo = ${lasRequest.xlo}\n")
        jnl.append("DEFINE SYMBOL region_0_y_hi = ${lasRequest.yhi}\n")
        jnl.append("DEFINE SYMBOL region_0_y_lo = ${lasRequest.ylo}\n")

        if ( lasRequest.zlo ) jnl.append("DEFINE SYMBOL region_0_z_lo = ${lasRequest.zlo}\n")
        if ( lasRequest.zhi ) jnl.append("DEFINE SYMBOL region_0_z_hi = ${lasRequest.zhi}\n")

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
            String tlo = lasRequest.getTlo();
            String thi = lasRequest.getThi();

            if ( tlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "time>=" + tlo;
            }
            if ( thi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "time<=" + thi
            }

            String xlo = lasRequest.getXlo()
            String xhi = lasRequest.getXhi()

            if ( xlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "longitude>=" + xlo;
            }
            if ( xhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "longitude<=" + xhi
            }

            String ylo = lasRequest.getYlo()
            String yhi = lasRequest.getYhi()

            if ( ylo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "latitude>=" + ylo;
            }
            if ( yhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "latitude<=" + yhi
            }

            String zlo = lasRequest.getZlo()
            String zhi = lasRequest.getZhi()

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
