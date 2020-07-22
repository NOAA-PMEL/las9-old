package pmel.sdig.las

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.visualization.datasource.base.DataSourceException
import com.google.visualization.datasource.base.ReasonType
import com.google.visualization.datasource.datatable.value.ValueType
import grails.gorm.transactions.Transactional
import grails.util.Holders
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import static grails.async.Promises.task

@Transactional(readOnly = true)
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
        webAppDirectory = webAppDirectory.replaceAll("-", "")

        if (!webAppDirectory.endsWith(File.separator)) {
            webAppDirectory = webAppDirectory + File.separator
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
    def cancel() {

        Ferret ferret = Ferret.first();

        def base = ferret.getBase_url()
        if ( !base ) {
            base = request.requestURL.toString()
            base = base.substring(0, base.indexOf("product/make"))
        }

        def requestJSON = request.JSON

        log.debug(requestJSON.toString())

        def hash = IngestService.getDigest(requestJSON.toString());
        LASRequest lasRequest = new LASRequest(requestJSON);
        File outputFile = Holders.grailsApplication.mainContext.getResource("output").file
        String outputPath = outputFile.getAbsolutePath()

        def pulse = productService.checkPulse(hash, outputPath)
        File pfile = new File(pulse.getPulseFile())
        pfile.delete();
        File cancelFile = new File("${outputPath}${File.separator}${hash}_cancel.txt")
        cancelFile.write(requestJSON.toString())
        if ( pulse.getFerretScript() ) {
            def kill = 'kill -9 ' + pulse.getPid()
            def k = kill.execute()
            if ( k ) {
                cancelFile.delete();
            }
        }
        render "Product request canceled"
    }
    def make() {

        Ferret ferret = Ferret.first();

        def ferret_temp = ferret.tempDir

        def base = ferret.getBase_url()
        if ( !base ) {
            base = request.requestURL.toString()
            base = base.substring(0, base.indexOf("product/make"))
        }

        def requestJSON = request.JSON

        log.debug(requestJSON.toString())

        def hash = IngestService.getDigest(requestJSON.toString());
        LASRequest lasRequest = new LASRequest(requestJSON);
        File outputFile = Holders.grailsApplication.mainContext.getResource("output").file
        String outputPath = outputFile.getAbsolutePath()

        def pulse = productService.checkPulse(hash, outputPath)


        Product product = Product.findByName(lasRequest.operation, [fetch: [operations: 'eager']])

        def operations = product.getOperations()
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i)
            ResultSet rs = operation.getResultSet()
            List<Result> results = rs.getResults();
            for (int j = 0; j < results.size(); j++) {
                Result result = results.get(j)
                result.setUrl("output${File.separator}${hash}_${result.name}${result.suffix}")
                result.setFilename("${outputPath}${File.separator}${hash}_${result.name}${result.suffix}")
            }
        }

        def resultSet
        if ( pulse.hasPulse ) {
            // If it has a pulse, check it status and return the appropriate response
            if (pulse.getState().equals(PulseType.COMPLETED) ) {
                Map cacheMap = productService.cacheCheck(product, hash, outputPath)
                resultSet = cacheMap.resultSet
                resultSet.setTargetPanel(lasRequest.getTargetPanel())
                resultSet.setProduct(product.getName())
                if ( !cacheMap.cache ) {
                    // Cache was invalid, so start the job again
                    // TODO NOT DRY see below
                    // If the current request does not have a pulse, start of request and wait 20 seconds for it to finish
                    def p = task {
                        productService.doRequest(lasRequest, product, hash, ferret.getTempDir(), base, outputPath, ferret_temp);
                    }
                    try {
                        resultSet = p.get(10l, TimeUnit.SECONDS)
                        // End of request
                        log.debug("Finished product request, rendering response...")
                    } catch (TimeoutException e) {
                        resultSet = productService.pulseResult(lasRequest, hash, ferret.getTempDir(), base, outputPath, product)
                    }
                }
            } else if ( pulse.getState().equals(PulseType.ERROR) ) {
                resultSet = productService.errorResult(lasRequest, hash, ferret.getTempDir(), base, outputPath, product);
                File pulseFile = new File(pulse.getPulseFile())
                pulseFile.delete()
            } else {
                // If there is a download file and no ferret script update download, otherwise
                // update the ferret progress iff ferretScript
                String downloadFile = pulse.getDownloadFile()
                String ferretScript = pulse.getFerretScript()
                if ( downloadFile && !ferretScript ) {
                    productService.writePulse(hash, outputPath, "Downloading data from ERDDAP", ferretScript, downloadFile, null, PulseType.STARTED)
                } else if ( ferretScript ) {
                    def pinfo = productService.getProcessInfo(ferretScript)
                    productService.writePulse(hash, outputPath, "PyFerret process is running", ferretScript, null, pinfo, PulseType.STARTED)
                }
                resultSet = productService.pulseResult(lasRequest, hash, ferret.getTempDir(), base, outputPath, product)
            }
        } else {
            // If the current request does not have a pulse, start of request and wait 20 seconds for it to finish
            def p = task {
                productService.doRequest(lasRequest, product, hash, ferret.getTempDir(), base, outputPath, ferret_temp);
            }
            try {
                // DEBUG always get a response
                resultSet = p.get(20l, TimeUnit.SECONDS)
                // End of request
                log.debug("Finished product request, rendering response...")
            } catch (TimeoutException e) {
                String ferretScript = pulse.getFerretScript()
                if ( ferretScript ) {
                    def pinfo = productService.getProcessInfo(ferretScript)
                    productService.writePulse(hash, outputPath, "PyFerret process is running", ferretScript, null, pinfo, PulseType.STARTED)
                }
                resultSet = productService.pulseResult(lasRequest, hash, ferret.getTempDir(), base, outputPath, product);
            }
        }
        if (resultSet) {
            withFormat {
                json {
                    respond resultSet // uses the custom templates in views/resultset
                }
            }
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
