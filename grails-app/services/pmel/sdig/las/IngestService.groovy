package pmel.sdig.las

import com.google.gson.*
import grails.gorm.transactions.Transactional
import grails.plugins.elasticsearch.ElasticSearchService
import grails.web.context.ServletContextHolder
import opendap.dap.AttributeTable
import opendap.dap.DAS
import org.apache.http.HttpException
import org.apache.http.client.utils.URIBuilder
import org.joda.time.*
import org.joda.time.chrono.GregorianChronology
import org.joda.time.format.*
import pmel.sdig.las.*
import pmel.sdig.las.type.GeometryType
import thredds.catalog.*
import ucar.nc2.Attribute
import ucar.nc2.constants.FeatureType
import ucar.nc2.dataset.CoordinateAxis
import ucar.nc2.dataset.CoordinateAxis1D
import ucar.nc2.dataset.CoordinateAxis1DTime
import ucar.nc2.dataset.CoordinateAxis2D
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.GridDataset
import ucar.nc2.dt.GridDatatype
import ucar.nc2.ft.FeatureDatasetFactoryManager
import ucar.nc2.time.CalendarDate
import ucar.nc2.time.CalendarDateRange
import ucar.nc2.time.CalendarDateUnit
import ucar.nc2.units.TimeUnit

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DecimalFormat

@Transactional
class IngestService {

    IngestStatusService ingestStatusService;
    DateTimeService dateTimeService

    ElasticSearchService elasticSearchService

    def servletContext = ServletContextHolder.servletContext

    LASProxy lasProxy = new LASProxy()
    PeriodFormatter pf = ISOPeriodFormat.standard()

    Dataset processRequset(AddRequest addRequest) {

        if ( addRequest.getType().equals("netcdf") ) {
            return ingest(null, addRequest.getUrl())
        } else if ( addRequest.getType().equals("dsg") ) {
            return ingestDSG(addRequest)
        }
    }

    Dataset ingestDSG(AddRequest addRequest) {


        List<AddProperty> properties = addRequest.getAddProperties()


        def url = addRequest.getUrl()

        Dataset dataset = ingestFromErddap(url, properties)

        dataset

    }
    String[] getMinMax(JsonObject bounds, String name) {
        JsonArray rows = (JsonArray) ((JsonObject) (bounds.get("table"))).get("rows")
        JsonArray names = (JsonArray) ((JsonObject) (bounds.get("table"))).get("columnNames")
        int index = -1
        for (int i = 0; i < names.size(); i++) {
            if ( names.get(i).getAsString().equals(name) ) {
                index = i
            }
        }
        JsonArray row1 = (JsonArray) rows.get(0)
        JsonArray row2 = (JsonArray) rows.get(1)

        String min = ((JsonElement) row1.get(index)).getAsString()
        String max = ((JsonElement) row2.get(index)).getAsString()
        String[] minmax = new String[2]
        minmax[0] = min
        minmax[1] = max
        return minmax
    }

    def addVariablesAndSaveFromThredds(String url, String parentHash, String erddap, boolean full) {


        Dataset dataset = Dataset.findByUrl(url)
        // If this is being done by the background process, it is possible that a user already requested this data set be loaded.
        log.debug("dataset found" + url)
            try {
                log.debug("Loading the catalog for" +url)
                ingestStatusService.saveProgress(parentHash, "Loading the THREDDS catalog for these variables.")
                Dataset temp = ingestFromThredds(url, parentHash, erddap, full)
                log.debug("Finished loading variables for " + url)

                // There will be a layer that represents the catalog at the top with the variables in a data set one level down.

                if (temp.getDatasets() && temp.getDatasets().size() == 1) {
                    Dataset temp2 = temp.getDatasets().get(0)
                    if (temp2 && temp2.getVariables()) {
                        log.debug(temp2.getVariables().size() + " vairables found " + dataset.getUrl())
                        dataset.setVariables(temp2.getVariables())
                        dataset.setStatus(Dataset.INGEST_FINISHED)
                        dataset.save(failOnError: true, flush: true)
                        elasticSearchService.index(dataset)
                    } else {
                        if ( temp2.getStatus() == Dataset.INGEST_FAILED ) {
                            dataset.setStatus(Dataset.INGEST_FAILED)
                            dataset.save(failOnError: true, flush: true)
                        }
                    }
                } else {
                    log.debug("No variables found for " + dataset.getUrl())
                    dataset.setStatus(Dataset.INGEST_FAILED)
                    dataset.save(failOnError: true, flush: true)
                }
            } catch (Exception e) {
                log.debug("Ingest failed " + e.getMessage())
                dataset.setStatus(Dataset.INGEST_FAILED)
                dataset.save(failOnError: true, flush:true)
            }

    }
    Dataset ingestFromThredds(String url, String parentHash, String erddap, boolean full) {

        log.debug("Starting ingest of " + url)
        dateTimeService.init()
        InvCatalogFactory factory = new InvCatalogFactory("default", false);
        String tdsid;
        String urlwithid;
        if ( url.contains("#") ) {
            urlwithid = url.substring(0, url.indexOf("#"))
            tdsid = url.substring(url.indexOf("#") + 1, url.length() )
            if ( !tdsid.equals("null") ) {
                urlwithid = urlwithid + "?dataset=" + tdsid;
            }
        } else {
            urlwithid = url;
        }
        ingestStatusService.saveProgress(parentHash, "Reading the catalog from the remote server.")
        InvCatalog catalog = (InvCatalog) factory.readXML(urlwithid);
        StringBuilder buff = new StringBuilder();
        boolean show = false
        if ( log.debugEnabled ) {
            show = true
        }
        if (!catalog.check(buff, show)) {
            log.error("Invalid catalog <" + url + ">\n" + buff.toString());
            return null
        }
        if ( erddap == null ) { // Just a thredds catalog, no supporting ERDDAP
            ingestStatusService.saveProgress(parentHash, "Catalog read. Looking for data sources.")
            return createDatasetFromCatalog(catalog, parentHash, full);
        } else {
            return createDatasetFromUAF(catalog, erddap);
        }
        log.debug("Finished ingest of " + url)
    }
    Dataset createDatasetFromUAF(InvCatalog catalog, String erddap) {
        Dataset dataset = new Dataset()
        String cname = catalog.getName();
        if ( cname.equals("THREDDS Server Default Catalog : You must change this to fit your server!"))
            cname = "Data Catalog";
        if ( !cname ) {
            dataset.setTitle(catalog.getUriString())
        } else {
            dataset.setTitle(cname)
        }
        dataset.setUrl(catalog.getUriString())
        dataset.setHash(getDigest(catalog.getUriString()))
        def children = catalog.getDatasets();
        for ( int i = 0; i < children.size(); i++ ) {
            InvDataset invDataset = (InvDataset) children.get(i)
            if ( !invDataset.getName().toLowerCase().equals("tds quality rubric")) {
                List<Dataset> childDatasets = processUAFDataset(invDataset, erddap)
                for (int j = 0; j < childDatasets.size(); j++) {
                    Dataset child = childDatasets.get(j)
                    dataset.addToDatasets(child)
                }
            }
        }
        dataset
    }
    List<Dataset> processUAFDataset(InvDataset invDataset, String erddap) {

        List<Dataset> all = new ArrayList<Dataset>()
        if (invDataset.hasAccess() && invDataset.getAccess(ServiceType.OPENDAP) != null) {
            all.addAll(createFromUAFDataset(invDataset, erddap))
        }
        if ( invDataset.hasNestedDatasets() ) {
            List<InvDataset> children = invDataset.getDatasets()
            InvDataset rubric = children.get(children.size() - 1)
            if ( rubric.getName().equals("") ) {
                children.remove(rubric)
            }

            Dataset childDataset = new Dataset([name: invDataset.getName(), title: invDataset.getName(), hash: getDigest(invDataset.getName() + invDataset.getCatalogUrl())])
            for (int i = 0; i < children.size(); i++) {
                InvDataset child = children.get(i)
                List<Dataset> kids = processUAFDataset(child, erddap)
                for (int j = 0; j < kids.size(); j++) {
                    childDataset.addToDatasets(kids.get(j))
                }
            }
            all.add(childDataset)
        }
        all
    }

    List<Dataset> createFromUAFDataset(InvDataset invDataset, String erddap) {
        List<Dataset> rankDatasets = new ArrayList<Dataset>()
        // Either one with variables or one data sets holding the different rank variables?
        if ( erddap.endsWith("/") ) {
            erddap = erddap.substring(0, erddap.lastIndexOf("/"))
        }
        String searchUafErddap = erddap + "/search/index.json?page=1&itemsPerPage=1000&searchFor=";
        String metadataUafErddap = erddap + "/info/";  // + ID like "noaa_esrl_3ff0_1c43_88d7" + "/index.json"
        if ( invDataset.hasAccess() && invDataset.getAccess(ServiceType.OPENDAP) != null ) {
            String idurl = invDataset.getAccess(ServiceType.OPENDAP).getStandardUrlName();
            String url = idurl;
            if (url.startsWith("https://")) {
                url = url.replaceAll("https://", "");
            } else if (url.startsWith("http://")) {
                url = url.replace("http://", "");
            }

            log.debug("Processing UAF THREDDS dataset: " + invDataset.getAccess(ServiceType.OPENDAP).getStandardUrlName() + " from " + invDataset.getParentCatalog().getUriString())



            LASProxy lasProxy = new LASProxy();
            JsonParser jsonParser = new JsonParser();
            String indexJSON = null;
            try {
                indexJSON = lasProxy.executeGetMethodAndReturnResult(searchUafErddap + url);
            } catch (HttpException e) {
                log.debug("Failed on " + searchUafErddap + url);
            } catch (IOException e) {
                log.debug("Failed on " + searchUafErddap + url);
            }

            if (indexJSON != null) {
                JsonObject indexJO = jsonParser.parse(indexJSON).getAsJsonObject();
                JsonObject table = indexJO.get("table").getAsJsonObject();

                JsonArray names = table.getAsJsonArray("columnNames");
                int idIndex = 0;
                for (int i = 0; i < names.size(); i++) {
                    String name = names.get(i).getAsString();
                    if (name.equals("Dataset ID")) {
                        idIndex = i;
                    }
                }



                JsonArray rows = table.getAsJsonArray("rows");
                log.debug("ERDDAP Dataset from " + url + " has " + rows.size() + " rows.");
                for (int i = 0; i < rows.size(); i++) {
                    // Everything in a row is on the same grid (we'll make it one data set)
                    Dataset dataset = new Dataset()
                    dataset.setHash(getDigest(url+i))
                    TimeAxis timeAxis = new TimeAxis()
                    timeAxis.setName("time")
                    timeAxis.setTitle("Time")
                    Period p0;
                    // Will get set below if attribute exists
                    timeAxis.setCalendar("gregorian")
                    GeoAxisX geoAxisX = new GeoAxisX()
                    geoAxisX.setName("longitude")
                    geoAxisX.setTitle("Longitude")
                    geoAxisX.setType("x")
                    GeoAxisY geoAxisY = new GeoAxisY()
                    geoAxisY.setName("latitude")
                    geoAxisY.setTitle("Latitude")
                    geoAxisY.setType("y")
                    VerticalAxis verticalAxis = new VerticalAxis()
                    verticalAxis.setName("depth")
                    verticalAxis.setTitle("Depth")
                    verticalAxis.setType("z")
                    // May get changed below
                    verticalAxis.setPositive("down")

                    JsonArray first = rows.get(i).getAsJsonArray();
                    JsonElement idE = first.get(idIndex);
                    String erddapDatasetId = idE.getAsString();
                    log.debug("ERDDAP Dataset ID  " + erddapDatasetId + " from " + url + " processing row = " + i);
                    String metadataJSONString;

                    //EREDDAP splits a data source into separate data sets according to the variable's rank.
                    // XYT data sets in one, XYZT in another. And one with just the time axis and one with the time axis and other random time stuff like
                    // the calendar_components variable in http://ferret.pmel.noaa.gov/pmel/thredds/dodsC/ct_flux

                    // The only ones we're interested in have the WMS bit set. If WMS is a possibility then LAS is a possibility.

                    String wms = first.get(4).getAsString();
                    if (wms != null && !wms.isEmpty()) {

                        // TODO deal with z axis
                        try {
                            metadataJSONString = lasProxy.executeGetMethodAndReturnResult(metadataUafErddap + erddapDatasetId + "/index.json");
                            if (metadataJSONString != null) {

                                JsonObject metadata = jsonParser.parse(metadataJSONString).getAsJsonObject();
                                JsonObject metadata_table = metadata.get("table").getAsJsonObject();

                                // Assuming the positions in the array are always the same.
                                // Risky?
                                int typeIndex = 0;

                                // TODO use the title string, or combine data sets on different axes from the same data source.
                                JsonArray metadata_rows = metadata_table.getAsJsonArray("rows");

                                for (int mi = 0; mi < metadata_rows.size(); mi++) {
                                    JsonArray metaRow = metadata_rows.get(mi).getAsJsonArray();
                                    String metaType = metaRow.get(typeIndex).getAsString();
                                    if (metaType.equalsIgnoreCase("dimension")) {
                                        String dimName = metaRow.get(1).getAsString();
                                        // Time size
                                        if (dimName.equals("time")) {
                                            String info = metaRow.get(4).getAsString();
                                            String[] majorParts = info.split(",");
                                            String[] parts = majorParts[0].split("=");
                                            timeAxis.setSize(Long.valueOf(parts[1]).longValue());

                                            // Time step has to be derived from the average spacing which involves parsing out the values and deciding the units to use.
                                            /*
                                    Grab the first one.

                                    if it is days around 30 use it as monthsd
                                    if it is days less than 27 use days
                                    if it is hours use hours

                                     */


                                            int size = (int)(timeAxis.getSize())
                                            if (size > 1) {
                                                String[] deltaParts = majorParts[2].split("=");
                                                String[] timeParts = deltaParts[1].split(" ");
                                                if (timeParts[0].contains("infinity")) {
                                                    log.debug("Problem with the time axis.")
                                                    return null;
                                                } else if (timeParts[1].contains("year")) {
                                                    timeAxis.setUnits("year");
                                                    // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                    p0 = new Period(1, 0, 0, 0, 0, 0, 0, 0)
                                                } else if (timeParts[1].contains("days")) {
                                                    // Make a number out of the days;
                                                    int days = Integer.valueOf(timeParts[0]).intValue();
                                                    if (days < 27) {
                                                        timeAxis.setUnits("day");
                                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                        p0 = new Period(0, 0, 0, days, 0, 0, 0, 0)
                                                    } else if (days >= 27 && days < 33) {
                                                        timeAxis.setUnits("month");
                                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                        p0 = new Period(0, 1, 0, 0, 0, 0, 0, 0)
                                                    } else if (days >= 88 && days < 93) {
                                                        timeAxis.setUnits("month");
                                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                        p0 = new Period(0, 3, 0, 0, 0, 0, 0, 0)
                                                    } else if (days >= 175 && days < 188) {
                                                        timeAxis.setUnits("month");
                                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                        p0 = new Period(0, 6, 0, 0, 0, 0, 0, 0)
                                                    } else if (days > 357) {
                                                        timeAxis.setUnits("year");
                                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                        p0 = new Period(1, 0, 0, 0, 0, 0, 0, 0)
                                                    }

                                                } else if (timeParts[0].contains("h")) {
                                                    String step = timeParts[0].replace("h", "");
                                                    timeAxis.setUnits("hour");
                                                    // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                    p0 = new Period(0, 0, 0, 0, Integer.valueOf(step).intValue(), 0, 0, 0)
                                                }
                                            } else {
                                                String[] valueParts = majorParts[1].split("=");
                                                NameValuePair nv = new NameValuePair()
                                                nv.setName(valueParts[1])
                                                nv.setValue(valueParts[1])
                                                timeAxis.addToNameValuePairs(nv);
                                                timeAxis.setUnits("none")
                                                p0 = new Period(1, 0, 0, 0, 0, 0, 0, 0)
                                                timeAxis.setDelta(pf.print(p0))
                                            }
                                            // Lon size
                                        } else if (dimName.equals("longitude")) {
                                            // Found a lon axis
                                            String info = metaRow.get(4).getAsString();
                                            String[] majorParts = info.split(",");
                                            String[] parts = majorParts[0].split("=");
                                            geoAxisX.setSize(Long.valueOf(parts[1]).longValue());
                                            if (majorParts.length == 3) {
                                                parts = majorParts[2].split("=");
                                                geoAxisX.setDelta(Double.valueOf(parts[1]).doubleValue());
                                            }
                                            //
                                            // Lat size
                                        } else if (dimName.equals("latitude")) {
                                            String info = metaRow.get(4).getAsString();
                                            String[] majorParts = info.split(",");
                                            String[] parts = majorParts[0].split("=");
                                            geoAxisY.setSize(Long.valueOf(parts[1]).longValue());
                                            if (majorParts.length == 3) {
                                                parts = majorParts[2].split("=");
                                                geoAxisY.setDelta(Double.valueOf(parts[1]).doubleValue());
                                            }
                                        } else if (dimName.equals("depth")) {
                                            String zstring = lasProxy.executeGetMethodAndReturnResult(erddap + "/griddap/" + erddapDatasetId + ".json?depth");
                                            JsonObject zobject = jsonParser.parse(zstring).getAsJsonObject();
                                            JsonObject ztable = zobject.get("table").getAsJsonObject();
                                            JsonArray zarray = ztable.getAsJsonArray("rows");

                                            for (int zi = 0; zi < zarray.size(); zi++) {
                                                // TODO positive, regular, delta, etc maybe we don't care
                                                Zvalue zValue = new Zvalue();
                                                zValue.setZ(zarray.get(zi).getAsDouble())
                                                verticalAxis.addToZvalues(zValue)
                                            }
                                        }
                                    } else if (metaType.equalsIgnoreCase("attribute")) {
                                        String metaVar = metaRow.get(1).getAsString();
                                        // See if it's a attribute for a variable. Guaranteed to have encountered the variable before its attributes.
                                        Variable atvar
                                        if (dataset.variables) {
                                            atvar = (Variable) dataset.variables.find { Variable variable ->
                                                variable.getName().equals(metaVar);
                                            }
                                        }
                                        if (metaVar.equals("NC_GLOBAL")) {
                                            String metaName = metaRow.get(2).getAsString();
                                            // Time start
                                            if (metaName.equals("time_coverage_start")) {
                                                timeAxis.setStart(metaRow.get(4).getAsString());
                                                if ( timeAxis.getStart().contains("0000") ) {
                                                    timeAxis.setClimatology(true)
                                                } else {
                                                    timeAxis.setClimatology(false)

                                                }
                                                // Time end
                                            } else if (metaName.equals("time_coverage_end")) {
                                                timeAxis.setEnd(metaRow.get(4).getAsString());
                                                // Lon start
                                            } else if (metaName.equals("Westernmost_Easting")) {
                                                geoAxisX.setMin(metaRow.get(4).getAsDouble());
                                                // Lon end
                                            } else if (metaName.equals("Easternmost_Easting")) {
                                                geoAxisX.setMax(metaRow.get(4).getAsDouble());
                                                // Lon step
                                            } else if (metaName.equals("geospatial_lon_resolution")) {
                                                geoAxisX.setDelta(metaRow.get(4).getAsDouble());
                                                // Lat start
                                            } else if (metaName.equals("geospatial_lat_min")) {
                                                geoAxisY.setMin(metaRow.get(4).getAsDouble());
                                                // Lat end
                                            } else if (metaName.equals("geospatial_lat_max")) {
                                                geoAxisY.setMax(metaRow.get(4).getAsDouble());
                                                // Lat step
                                            } else if (metaName.equals("geospatial_lat_resolution")) {
                                                geoAxisY.setDelta(metaRow.get(4).getAsDouble());
                                            } else if (metaName.equals("title")) {
                                                dataset.setTitle(metaRow.get(4).getAsString())
                                            }
                                        } else if ( metaVar.equals("time") ) {
                                            String metaName = metaRow.get(2).getAsString();
                                            if (metaName.equals("calendar")) {
                                                timeAxis.setCalendar(metaRow.get(4).getAsString());
                                            } else if (metaName.equals("units")) {
                                                timeAxis.setUnitsString(metaRow.get(4).getAsString());
                                            }
                                        } else if (metaVar.equals("longitude")) {
                                            String metaName = metaRow.get(2).getAsString();
                                            if (metaName.equals("units")) {
                                                geoAxisX.setUnits(metaRow.get(4).getAsString());
                                            }
                                        } else if (metaVar.equals("latitude")) {
                                            String metaName = metaRow.get(2).getAsString();
                                            if (metaName.equals("units")) {
                                                geoAxisY.setUnits(metaRow.get(4).getAsString());
                                            }
                                        } else if (metaVar.equals("depth")) {
                                            String metaName = metaRow.get(2).getAsString();
                                            if (metaName.equals("units")) {
                                                verticalAxis.setUnits(metaRow.get(4).getAsString());
                                            } else if ( metaName.equals("positive") ) {
                                                verticalAxis.setPositive(metaRow.get(4).getAsString());
                                            }
                                        } else if (atvar) {
                                            String metaName = metaRow.get(2).getAsString();
                                            if (metaName.equals("units")) {
                                                atvar.setUnits(metaRow.get(4).getAsString());
                                            } else if (metaName.equals("long_name")) {
                                                atvar.setTitle(metaRow.get(4).getAsString());
                                            }
                                        }
                                    } else if (metaType.equals("variable")) {
                                        Variable variable = new Variable();
                                        variable.setHash(getDigest(idurl + "#" + metaRow.get(1).getAsString()));
                                        variable.setName(metaRow.get(1).getAsString());
                                        // Gets reset if there is a long_name attribute
                                        variable.setTitle(metaRow.get(1).getAsString())
                                        variable.setUrl(idurl + "#" + metaRow.get(1).getAsString());
                                        dataset.addToVariables(variable)
                                    }

                                }

                            }
                        } catch (Exception e) {
                            log.error(e.getLocalizedMessage())
                        }
                    }
                    if ( dataset.getVariables() ) {
                        dataset.variables.each { Variable variable ->
                            String intervals = ""
                            if (geoAxisX.getMax()) {
                                GeoAxisX vx = new GeoAxisX(geoAxisX.properties)
                                variable.setGeoAxisX(vx)
                                vx.setVariable(variable)
                                intervals = intervals + "x"
                            }
                            if (geoAxisY.getMax()) {
                                GeoAxisY vy = new GeoAxisY(geoAxisY.properties)
                                variable.setGeoAxisY(vy)
                                vy.setVariable(variable)
                                intervals = intervals + "y"
                            }
                            if (verticalAxis.getZvalues()) {
                                VerticalAxis vv = new VerticalAxis(verticalAxis.properties)
                                variable.setVerticalAxis(vv)
                                vv.setVariable(variable)
                                intervals = intervals + "z"
                            }
                            if (timeAxis.getStart()) {
                                TimeAxis vt = new TimeAxis(timeAxis.properties)
                                CalendarDateUnit cdu = CalendarDateUnit.of(vt.getCalendar(), vt.getUnitsString())
                                DateTime t0 = dateTimeService.dateTimeFromIso(vt.getStart())
                                DateTime tN = dateTimeService.dateTimeFromIso(vt.getEnd())
                                // Bob's times are always "seconds since" so divide milli's by 1000.
                                Period pTotal = getPeriod(cdu, (t0.getMillis()/1000.0d), (tN.getMillis()/1000.0d))
                                vt.setPeriod(pf.print(pTotal))
                                vt.setDelta(pf.print(p0))
                                variable.setTimeAxis(vt)
                                vt.setVariable(variable)
                                intervals = intervals + "t"
                            }
                            variable.setIntervals(intervals)
                            variable.setGeometry(GeometryType.GRID)
                            variable.save(failOnError: true)
                        }

                        dataset.save(failOnError: true)
                        rankDatasets.add(dataset)
                    }
                }
            }
        }
        rankDatasets
    }

    private String fixName(String url) {
        def main
        try {
            URIBuilder builder = new URIBuilder(url)
            String host = builder.getHost()
            def parts = host.tokenize(".")
            main = main + host
            if (parts.size() >= 3) {
                main = "TDS Data from " + parts.get(parts.size() - 3) + "." + parts.get(parts.size() - 2) + "." + parts.get(parts.size() - 1)
            }
        } catch (Exception e) {
            main = "TDS Data"
        }
        main
    }
    Dataset createDatasetFromCatalog(InvCatalog catalog, String parentHash, boolean full) {
        Dataset dataset = new Dataset()
        if (catalog.getName()) {
            if ( catalog.getName().toLowerCase().contains("you must change") ) {
                String name = fixName(catalog.getUriString())
                dataset.setTitle(name)
            } else {
                dataset.setTitle(catalog.getName())
            }
        } else {
            String name = fixName(catalog.getUriString())
            dataset.setTitle(name)
        }
        dataset.setUrl(catalog.getUriString())
        dataset.setHash(getDigest(catalog.getUriString()))

        List<InvDataset> children = catalog.getDatasets();


        if ( children.size() == 2 && !children.get(0).hasAccess() && children.get(1).getFullName().toLowerCase().contains("rubric") ) {
            InvDataset onlyChild = children.get(0);
            String name = onlyChild.getName();
            if ( name == null ) {
                name = "TDS Data"
            }
            dataset.setTitle(name)
            children = onlyChild.getDatasets()
        }
        for (int i = 0; i < children.size(); i++) {

            InvDataset nextChild = (InvDataset) children.get(i)
            if (!nextChild.getName().toLowerCase().contains("tds quality") && !nextChild.getName().contains("automated cleaning process")) {
                Dataset child = processDataset(nextChild, parentHash, full, dataset)
                dataset.addToDatasets(child)
            }
        }
        dataset
    }
    Dataset processDataset(InvDataset invDataset, String parentHash, boolean full, Dataset parent) {

        List<InvDataset> invDatasetList = invDataset.getDatasets();
        List<InvDataset> remove = new ArrayList<>()
        boolean access = invDataset.getAccess(ServiceType.OPENDAP) != null;
        for (int i = 0; i < invDatasetList.size(); i++) {
            InvDataset child = invDatasetList.get(i)
            InvAccess a = child.getAccess(ServiceType.OPENDAP)
            if ( a != null) access = true;
            if ( child.getName().contains("automated cleaning process") || child.getName().toLowerCase().contains("tds quality") ) {
                remove.add(child)
            }
        }
        invDatasetList.removeAll(remove)

        // FIXME
        String title = invDataset.getName()
        if (title.toLowerCase().contains("you must change")) {
            title = fixName(invDataset.getCatalogUrl())
        }
        Dataset saveToDataset = new Dataset(title: "Failed reading data set", url: "http://", hash: getDigest(Math.random().toString()), variableChildren: false)
        Dataset d = saveToDataset
        try {
            d = new Dataset(title: title, url: invDataset.getCatalogUrl(), hash: getDigest(invDataset.getCatalogUrl()), variableChildren: false)
            saveToDataset = d
            if ((invDatasetList.size() == 1 && !access) ||
                    (invDatasetList.size() > 0 && invDataset.getName().equals(invDatasetList.get(0).getName())) ||
                    (invDatasetList.size() > 0 && invDataset.getName().toLowerCase().contains("top dataset"))) {
                // If skipping, save next level to parent
                saveToDataset = parent
                // And refuse the current
                d = null;
            } else {
                if (invDataset.hasAccess() && invDataset.getAccess(ServiceType.OPENDAP) != null) {
                    if (full) {
                        try {
                            d = ingest(parentHash, invDataset.getAccess(ServiceType.OPENDAP).getStandardUrlName())
                        } catch (Exception e) {
                            log.debug("We failed..." + e.getMessage())
                        }
                        d.variableChildren = true
                        d.geometry = GeometryType.GRID
                        if ( d.getStatus() != Dataset.INGEST_FAILED)
                            d.setStatus(Dataset.INGEST_FINISHED)
                    } else {
                        d.variableChildren = true
                        d.geometry = GeometryType.GRID
                        d.setStatus(Dataset.INGEST_NOT_STARTED)
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Exception processing data set. Message = " + e.getMessage() + " Trying to go on with nested data ses.")
        }
        List<InvDataset> kids = invDataset.getDatasets();
        for (int i = 0; i < kids.size(); i++) {
            InvDataset kid = kids.get(i)
            if (!kid.getName().toLowerCase().contains("quality rubric") && !kid.getName().contains("automated cleaning process")) {
                Dataset dkid = processDataset(kid, parentHash, full, saveToDataset)
                if ( dkid != null ) {
                    saveToDataset.addToDatasets(dkid)
                }
            }
        }
        d
    }
    Dataset ingestFromErddap(String url, List<AddProperty> properties) {

        AddProperty plots = properties.find{it.name=="mapandplot"}

        def default_supplied = false
        def default_value

        if ( plots ) {
            default_supplied = true
            default_value = plots.getValue()
        }

        AddProperty hour = properties.find{it.name=="hours"}
        def hours_value = null
        double hours_step
        if ( hour ) {
            hours_value = hour.getValue()
            hours_step = Double.valueOf(hours_value)
        }
        def id = url.substring(url.lastIndexOf("/")+1, url.length() - 1)

        int timeout = 400  // units of seconds

        DAS das = new DAS()
        InputStream input
        List<String> subsetNames = new ArrayList<String>()
        Map<String, AttributeTable> idVar = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> timeVar = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> latVar = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> lonVar = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> zVar = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> data = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> subsets = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> monthOfYear = new HashMap<String, AttributeTable>()

        String TRAJECTORY = "cdm_trajectory_variables";
        String PROFILE = "cdm_profile_variables";
        String TIMESERIES = "cdm_timeseries_variables";
        String POINT = "cdm_point_variables";

        DecimalFormat df = new DecimalFormat("#.##");
        DecimalFormat decimalFormat = new DecimalFormat("###############.###############");

        DateTimeFormatter hoursfmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
        DateTimeFormatter shortFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy").withChronology(GregorianChronology.getInstance(DateTimeZone.UTC)).withZone(DateTimeZone.UTC);
        DateTimeFormatter mediumFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm").withChronology(GregorianChronology.getInstance(DateTimeZone.UTC)).withZone(DateTimeZone.UTC);

        InputStream stream = null
        JsonStreamParser jp = null


        boolean isTrajectoryProfile = false
        boolean isProfile = false
        boolean isPoint = false
        boolean isTrajectory = false
        boolean isTimeseries = false

        def display  = null

        def axesToSkip = []

        try {


            def skip = null

            def hash = getDigest(url)

            Dataset dataset = new Dataset([url: url, hash: hash])

            DateTime date = new DateTime()
            log.info("Processing: " + url + " at "+date.toString() )


            input = lasProxy.executeGetMethodAndReturnStream(url+".das", null, timeout)
            das.parse(input)
            AttributeTable global = das.getAttributeTable("NC_GLOBAL")
            opendap.dap.Attribute cdm_trajectory_variables_attribute = global.getAttribute(TRAJECTORY)
            opendap.dap.Attribute cdm_profile_variables_attribute = global.getAttribute(PROFILE)
            opendap.dap.Attribute cdm_timeseries_variables_attribute = global.getAttribute(TIMESERIES)
            opendap.dap.Attribute cdm_data_type = global.getAttribute("cdm_data_type")
            opendap.dap.Attribute altitude_proxy = global.getAttribute("altitude_proxy")
            String grid_type = cdm_data_type.getValueAt(0).toLowerCase(Locale.ENGLISH)
            opendap.dap.Attribute subset_names = null
            opendap.dap.Attribute title_attribute = global.getAttribute("title")
            if ( title_attribute == null ) {
                title_attribute = global.getAttribute("dataset_title")
            }
            String title = "No title global attribute"
            if ( title_attribute != null ) {
                Iterator<String> titleIt = title_attribute.getValuesIterator()
                title = titleIt.next()
            }
            AttributeTable variableAttributes = das.getAttributeTable("s")
            if ( ( (cdm_data_type != null && grid_type.equalsIgnoreCase(CdmDatatype.POINT) ) || cdm_profile_variables_attribute !=null || cdm_trajectory_variables_attribute != null || cdm_timeseries_variables_attribute != null ) && variableAttributes != null ) {
                if (grid_type.equals(CdmDatatype.TRAJECTORYPROFILE)) {
                    isTrajectoryProfile = true
                } else {
                    if (cdm_trajectory_variables_attribute != null && cdm_profile_variables_attribute != null) {
                        isTrajectoryProfile = true
                    } else if (cdm_trajectory_variables_attribute != null) {
                        subset_names = cdm_trajectory_variables_attribute
                        isTrajectory = true
                    } else if (cdm_profile_variables_attribute != null) {
                        subset_names = cdm_profile_variables_attribute
                        isProfile = true
                    } else if (cdm_timeseries_variables_attribute != null) {
                        subset_names = cdm_timeseries_variables_attribute
                        isTimeseries = true
                    } else if (grid_type.equalsIgnoreCase(CdmDatatype.POINT)) {
                        subset_names = null
                        isPoint = true
                    }
                }
                if (subset_names != null) {
                    Iterator<String> subset_variables_attribute_values = subset_names.getValuesIterator()
                    if (subset_variables_attribute_values.hasNext()) {
                        // Work with the first value...  Attributes like ranges can have multiple values...
                        String subset_variable_value = subset_variables_attribute_values.next()
                        String[] subset_variables = subset_variable_value.split(",")
                        for (int i = 0; i < subset_variables.length; i++) {
                            String tv = subset_variables[i].trim()
                            if (!tv.equals("")) {
                                subsetNames.add(tv)
                            }
                        }
                    } else {
                        System.err.println("No CDM trajectory, profile or timeseries variables found in the cdm_trajectory_variables, cdm_profile_variables or cdm_timeseries_variables global attribute.")
                    }
                }
                // Collect the subset names...

                // Classify all of the variables...

                Enumeration names = variableAttributes.getNames()
                if (!names.hasMoreElements()) {
                    log.debug("No variables found in this data collection.")
                } else {
                    // We found some variables, so set the flag
                    dataset.setVariableChildren(true);
                }
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement()
                    AttributeTable var = variableAttributes.getAttribute(name).getContainer()
                    if (subsetNames.contains(name)) {
                        if (var.hasAttribute("cf_role") && (
                                var.getAttribute("cf_role").getValueAt(0).equals("trajectory_id") ||
                                        var.getAttribute("cf_role").getValueAt(0).equals("profile_id") ||
                                        var.getAttribute("cf_role").getValueAt(0).equals("timeseries_id")
                        )) {
                            idVar.put(name, var)
                        } else {
                            if (!subsets.containsKey(name)) {
                                subsets.put(name, var)
                            }
                        }
                    } else if (var.hasAttribute("cf_role") && (
                            var.getAttribute("cf_role").getValueAt(0).equals("trajectory_id") ||
                                    var.getAttribute("cf_role").getValueAt(0).equals("profile_id") ||
                                    var.getAttribute("cf_role").getValueAt(0).equals("timeseries_id")
                    )) {
                        idVar.put(name, var)
                        if (!subsets.containsKey(name)) {
                            subsets.put(name, var)
                        }
                    }
                    // Look at the attributes and classify any variable as either time, lat, lon, z or a data variable.
                    if (var.hasAttribute("_CoordinateAxisType")) {
                        String type = var.getAttribute("_CoordinateAxisType").getValueAt(0)
                        if (type.toLowerCase(Locale.ENGLISH).equals("time")) {
                            timeVar.put(name, var)
                        } else if (type.toLowerCase(Locale.ENGLISH).equals("lon")) {
                            lonVar.put(name, var)
                        } else if (type.toLowerCase(Locale.ENGLISH).equals("lat")) {
                            latVar.put(name, var)
                        } else if (type.toLowerCase(Locale.ENGLISH).equals("height")) {
                            zVar.put(name, var)
                        }
                    } else {
                        if (name.toLowerCase(Locale.ENGLISH).contains("tmonth")) {
                            monthOfYear.put(name, var)
                        }
                        boolean skipCheck = false
                        if (skip != null) {
                            skipCheck = Arrays.asList(skip).contains(name)
                        }
                        if (!data.containsKey(name) && !subsets.containsKey(name) && !idVar.containsKey(name) && !skipCheck) {
                            data.put(name, var)
                        }
                    }


                }
                // DEBUG what we've got so far:
                if (!idVar.keySet().isEmpty()) {
                    String name = idVar.keySet().iterator().next()
                    log.debug(grid_type + " ID variable:")
                    log.debug("\t " + name)
                }
                log.debug("Subset variables:")

                for (Iterator subIt = subsets.keySet().iterator(); subIt.hasNext();) {
                    String key = (String) subIt.next()
                    log.debug("\t " + key)
                }
                if (!timeVar.keySet().isEmpty()) {
                    String name = timeVar.keySet().iterator().next()
                    log.debug("Time variable:")
                    log.debug("\t " + name)
                }
                if (!lonVar.keySet().isEmpty()) {
                    String name = lonVar.keySet().iterator().next()
                    log.debug("Lon variable:")
                    log.debug("\t " + name)
                }
                if (!latVar.keySet().isEmpty()) {
                    String name = latVar.keySet().iterator().next()
                    log.debug("Lat variable:")
                    log.debug("\t " + name)
                }
                if (!zVar.keySet().isEmpty()) {
                    String name = zVar.keySet().iterator().next()
                    log.debug("Z variable:")
                    log.debug("\t " + name)
                }
                if (!monthOfYear.keySet().isEmpty()) {
                    String name = monthOfYear.keySet().iterator().next()
                    log.debug("Month of year variable:")
                    log.debug("\t " + name)
                }

                log.debug("Data variables:")

                for (Iterator subIt = data.keySet().iterator(); subIt.hasNext();) {
                    String key = (String) subIt.next()
                    log.debug("\t " + key)
                }


                dataset.setTitle(title)
                DatasetProperty property = new DatasetProperty([type: "ferret", name: "data_format", value: "csv"])
                dataset.addToDatasetProperties(property)


                String dsgIDVariablename = null
                if (!idVar.keySet().isEmpty()) {
                    dsgIDVariablename = idVar.keySet().iterator().next()
                }

                // Get the ISO Metadata
                String isourl = url + ".iso19115"
                stream = null

                IsoMetadata meta = new IsoMetadata()
                stream = lasProxy.executeGetMethodAndReturnStream(isourl, null, timeout)
                if (stream != null) {
                    JDOMUtils.XML2JDOM(new InputStreamReader(stream), meta)
                    meta.init()
                }

                /*
                With an ERDDAP DSG data set, the "grid" which is just the maximum lon/lat/time/depth extents is the
                same for every variable. Though it's wasteful for storage, we're going to build the axes and save
                a copy in each varible.
                 */

                TimeAxis timeAxis = new TimeAxis()
                GeoAxisX geoAxisX = new GeoAxisX()
                GeoAxisY geoAxisY = new GeoAxisY()
                geoAxisY.setType("y")
                VerticalAxis zAxis = new VerticalAxis()
                zAxis.setType("z")

                if (!timeVar.keySet().isEmpty()) {
                    String name = timeVar.keySet().iterator().next()
                    timeAxis.setName(name)
                    AttributeTable var = timeVar.get(name)
                    DatasetProperty tp = new DatasetProperty([type: "tabledap_access", name: "time", value: name])
                    dataset.addToDatasetProperties(tp)

                    opendap.dap.Attribute cala = var.getAttribute("calendar")
                    String calendar = "standard"
                    if (cala != null) {
                        calendar = cala.getValueAt(0)
                    }
                    timeAxis.setCalendar(calendar)
                    if (display != null && !display[0].equals("minimal")) {
                        if (display.length == 1) {
                            timeAxis.setDisplay_lo(display[0])
                        } else if (display.length == 2) {
                            String t0text = display[0]
                            String t1text = display[1]
                            DateTime dt0
                            DateTime dt1
                            try {
                                dt0 = shortFerretForm.parseDateTime(t0text)
                            } catch (Exception e) {
                                try {
                                    dt0 = mediumFerretForm.parseDateTime(t0text)
                                } catch (Exception e1) {
                                    dt0 = null
                                }
                            }
                            try {
                                dt1 = shortFerretForm.parseDateTime(t1text)
                            } catch (Exception e) {
                                try {
                                    dt1 = mediumFerretForm.parseDateTime(t1text)
                                } catch (Exception e1) {
                                    dt1 = null
                                }
                            }
                            if (dt1 != null && dt0 != null) {
                                if (dt0.isBefore(dt1)) {
                                    timeAxis.setDisplay_lo(t0text)
                                    timeAxis.setDisplay_hi(t1text)
                                } else {
                                    timeAxis.setDisplay_lo(t1text)
                                    timeAxis.setDisplay_hi(t0text)
                                }
                            }
                        }
                    }
                    // TODO pass in the whehter data are to be minutes
                    def minutes = false
                    def hours = false

                    if ( hours_value ) {
                        hours = true
                    }

                    if (minutes) {
                        timeAxis.setUnits("minutes")
                    } else if (hours) {
                        timeAxis.setUnits("hours")
                    } else {
                        timeAxis.setUnits("days")
                    }
                    opendap.dap.Attribute ua = var.getAttribute("units")
                    if (ua != null) {
                        String units = ua.getValueAt(0)
                        DatasetProperty tu = new DatasetProperty([type: "tabledap_access", name: "time_units", value: units])
                        dataset.addToDatasetProperties(tu)
                    }

                    if (!axesToSkip.contains("t")) {

                        String start = meta.getTlo()
                        String end = meta.getThi()

                        if (start == null || end == null) {
                            throw new Exception("Time metadata not found.")
                        }

                        // This should be time strings in ISO Format

                        Chronology chrono = GregorianChronology.getInstance(DateTimeZone.UTC)
                        DateTimeFormatter iso = ISODateTimeFormat.dateTimeParser().withChronology(chrono).withZone(DateTimeZone.UTC)

                        DateTime dtstart = iso.parseDateTime(start)
                        DateTime dtend = iso.parseDateTime(end)

                        int days = Days.daysBetween(dtstart.withTimeAtStartOfDay(), dtend.withTimeAtStartOfDay()).getDays()
                        Period span = new Period(0, 0, 0, days, 0, 0, 0, 0)
                        timeAxis.setPeriod(pf.print(span))
                        timeAxis.setPosition("middle")

                        if (hours || minutes) {
                            timeAxis.setStart(hoursfmt.print(dtstart))
                        } else {
                            timeAxis.setStart(hoursfmt.print(dtstart.withTimeAtStartOfDay()))
                        }

                        AddProperty dlo = properties.find{it.name=="display_lo"}
                        if ( dlo ) timeAxis.setDisplay_lo(dlo.getValue())
                        AddProperty dhi = properties.find{it.name=="display_hi"}
                        if ( dhi ) timeAxis.setDisplay_hi(dhi.getValue())

                        // Fudge
                        days = days + 1
                        Period period;
                        if (minutes) {
                            // Days are now minutes :-)
                            days = days * 24 * 60
                            period = new Period(0, 0, 0, 0, 0, days, 0, 0)
                        } else if (hours) {
                            // Days are now hours :-)
                            days = (int) (days * 24 * Math.rint(1.0d / hours_step))
                            period = new Period(0, 0, 0, 0, days, 0, 0, 0)
                        } else {
                            period = new Period(0, 0, 0, days, 0, 0, 0, 0)
                        }

                        timeAxis.setSize(Long.valueOf(days))
                        timeAxis.setDelta(pf.print(period))
                        timeAxis.setStart(start)
                        timeAxis.setEnd(end)
                        //TODO should be long_name, yeah ?-)
                        timeAxis.setTitle("Time")

                        // If we're scanning a catalog set the display dates so the entire data set is not requested with the first plot.
                        //TODO You'll need to fix this
                        def auto_display = false
                        if (auto_display) {
                            timeAxis.setDisplay_lo(mediumFerretForm.print(dtstart))
                            timeAxis.setDisplay_hi(mediumFerretForm.print(dtstart.plusDays(1)))
                        }
                        //TODO big  ton more stuff needs to be set in the TimeAxis object.

                    } else {
                        // Beats me
                    }

                }
                if (!lonVar.keySet().isEmpty()) {
                    String name = lonVar.keySet().iterator().next()
                    geoAxisX.setName(name)
                    geoAxisX.setType("x")
                    DatasetProperty xp = new DatasetProperty([type: "tabledap_access", name: "longitude", value: name])
                    dataset.addToDatasetProperties(xp)
                    AttributeTable var = lonVar.get(name)
                    opendap.dap.Attribute ua = var.getAttribute("units")
                    if (ua != null) {
                        String units = ua.getValueAt(0)
                        geoAxisX.setUnits(units)
                    }
                    opendap.dap.Attribute ln = var.getAttribute("long_name")
                    if ( ln != null ) {
                        String long_name = ln.getValueAt(0)
                        geoAxisX.setTitle(long_name)
                    } else {
                        geoAxisX.setTitle("Longitude")
                    }
                    if (!axesToSkip.contains("x")) {

                        String start = meta.getXlo()
                        String end = meta.getXhi()
                        double dmin = -180.0d
                        double dmax = 180.0d
                        DatasetProperty lonDomain
                        if (Math.abs(Double.valueOf(start)) > 180.0d || Math.abs(Double.valueOf(end)) > 180.0d) {
                            lonDomain = new DatasetProperty([type: "tabledap_access", name: "lon_domain", value: "0:360"])
                            dmin = 0.0d
                            dmax = 360.0d
                        } else {
                            lonDomain = new DatasetProperty([type: "tabledap_access", name: "lon_domain", value: "-180:180"])
                        }
                        dataset.addToDatasetProperties(lonDomain)
                        double dstart = Double.valueOf(start)
                        double dend = Double.valueOf(end)
                        double size = dend - dstart

                        // Fudge it up if the interval is really small...

                        long fsize = 3l
                        if (size < 355.0) {
                            double fudge = size * 0.15
                            if (size < 1.0d) {
                                fudge = 0.25
                            }
                            dstart = dstart - fudge

                            if (dstart < dmin) {
                                dstart = dmin
                            }
                            dend = dend + fudge
                            if (dend > dmax) {
                                dend = dmax
                            }

                            double c = Math.ceil(dend - dstart)
                            size = (long) c + 1
                        }
                        double step = (dend - dstart) / (Double.valueOf(fsize) - 1.0d)
                        geoAxisX.setSize(fsize)
                        geoAxisX.setMin(dstart)
                        geoAxisX.setMax(dend)
                        geoAxisX.setDelta(step)


                    } else {
                        // Don't know about this.
                    }

                }
                if (!latVar.keySet().isEmpty()) {

                    String name = latVar.keySet().iterator().next()
                    geoAxisY.setName(name)
                    geoAxisY.setType("y")
                    DatasetProperty latName = new DatasetProperty([type: "tabledap_access", name: "latitude", value: name])
                    AttributeTable var = latVar.get(name)
                    opendap.dap.Attribute ua = var.getAttribute("units")
                    if (ua != null) {
                        String units = ua.getValueAt(0)
                        geoAxisY.setUnits(units)
                    }
                    opendap.dap.Attribute ln = var.getAttribute("long_name")
                    if ( ln != null ) {
                        String long_name = ln.getValueAt(0)
                        geoAxisY.setTitle(long_name)
                    } else {
                        geoAxisY.setTitle("Latitude")
                    }
                    if (!axesToSkip.contains("y")) {

                        String start = meta.getYlo()
                        String end = meta.getYhi()
                        double dstart = Double.valueOf(start)
                        double dend = Double.valueOf(end)
                        double size = dend - dstart
                        long fsize = 3l
                        if (size < 85.0) {
                            double fudge = size * 0.15
                            if (size < 1.0d) {
                                fudge = 0.25
                            }
                            dstart = dstart - fudge
                            if (dstart < -90.0d) {
                                dstart = -90.0d
                            }
                            dend = dend + fudge
                            if (dend > 90.0d) {
                                dend = 90.0d
                            }
                            double c = Math.ceil(dend - dstart)
                            fsize = String.valueOf((long) c + 1)
                        }
                        double step = (dend - dstart) / (Double.valueOf(fsize) - 1.0d)
                        geoAxisY.setMin(dstart)
                        geoAxisY.setMax(dend)
                        geoAxisY.setDelta(step)
                        geoAxisY.setSize(fsize)
                    } else {
                        //TODO what to do when we can't fix the axis?
                    }

                }
                /*
                 * For profiles, grab the depth and make 10 equal levels.
                 *
                 *
                 */
                // TODO look for the cdm_alititude_proxy attribute do a query since there won't be metadata.
                if (!zVar.keySet().isEmpty()) {
                    String name = zVar.keySet().iterator().next()
                    DatasetProperty alt = new DatasetProperty([type: "tabledap_access", name: "altitude", value: name])
                    dataset.addToDatasetProperties(alt)
                    AttributeTable var = zVar.get(name)
                    opendap.dap.Attribute ua = var.getAttribute("units")
                    if (ua != null) {
                        String units = ua.getValueAt(0)
                        zAxis.setUnits(units)
                    }
                    // TODO this is old code. needs vertical axis object
                    if (!axesToSkip.contains("z")) {

//                        String start = meta.getZlo()
//                        String end = meta.getZhi()
//                        if (start == null || end == null || altitude_proxy != null) {
//                            // If it was a proxy, there's no metadata.
//                            // Pull the range from the data.
//                            stream = null
//                            jp = null
//                            String zquery = ""
//
//                            String nanDistinct = "&" + name + "!=NaN&distinct()"
//                            if (zquery.length() > 0) {
//                                zquery = zquery + ","
//                            }
//                            zquery = zquery + name + "&orderByMinMax(\"" + name + "\")"
//                            String zurl = url + id + ".json?" + URLEncoder.encode(zquery, "UTF-8")
//                            stream = null
//
//                            stream = lasProxy.executeGetMethodAndReturnStream(zurl, null, timeout)
//
//
//                            if (stream != null) {
//                                jp = new JsonStreamParser(new InputStreamReader(stream))
//                                JsonObject bounds = (JsonObject) jp.next()
//                                String[] zminmax = getMinMax(bounds, name)
//                                stream.close()
//
//                                start = zminmax[0]
//                                end = zminmax[1]
//                            }
//                        }
//                        if (start != null && end != null) {
//                            double size = Double.valueOf(end) - Double.valueOf(start)
//                            double step = size / 10.arb.setStart(start)
//                            arb.setStep(df.format(step))
//                            arb.setSize("10")
//                            ab.setArange(arb)
//                        } else {
//                            //TODO something needed here:?
//                        }
                    } else {

                    }

                }

                Variable idvb = new Variable()

                if (dsgIDVariablename != null) {

                    AttributeTable idvar = idVar.get(dsgIDVariablename)
                    idvb.setName(dsgIDVariablename)
                    idvb.setUrl(url+"#"+dsgIDVariablename)
                    idvb.setHash(getDigest(idvb.getUrl()))
                    opendap.dap.Attribute ln = idvar.getAttribute("long_name")
                    if (ln != null) {
                        String longname = ln.getValueAt(0)
                        idvb.setTitle(longname)
                    } else {
                        idvb.setTitle(dsgIDVariablename)
                    }

                    VariableAttribute cby = new VariableAttribute([name: "color_by", value: "true"])
                    VariableAttribute cid = new VariableAttribute([name: grid_type.toLowerCase(Locale.ENGLISH) + "_id", value: "true"])
                    idvb.addToVariableAttributes(cby)
                    idvb.addToVariableAttributes(cid)
                    idvb.setGeometry(grid_type)
                    // Axis and intervals
                    GeoAxisX gx = new GeoAxisX(geoAxisX.properties)
                    gx.setVariable(idvb)
                    idvb.setGeoAxisX(gx)

                    GeoAxisY gy = new GeoAxisY(geoAxisY.properties)
                    gy.setVariable(idvb)
                    idvb.setGeoAxisY(gy)
                    def intervals = "xy"
                    if ( !zVar.keySet().isEmpty() ) {
                        VerticalAxis za = new VerticalAxis(zAxis.properties)
                        za.setVariable(idvb)
                        idvb.setVerticalAxis(za)
                        intervals = intervals + "z"
                    }

                    TimeAxis ta = new TimeAxis(timeAxis.properties)
                    ta.setVariable(idvb)
                    idvb.setTimeAxis(ta)

                    intervals = intervals + "t"
                    idvb.setIntervals(intervals)

                    dataset.addToVariables(idvb)
                }
//TODO this shas to do with the constaintt objec wich we mujst define and populate
//                if ( isTrajectory ) {
//                    idcg.setAttribute("name", "Individual Trajectory(ies)")
//                }
//                if ( isTrajectoryProfile ) {
//                    idcg.setAttribute("name", "Trajectory Profiles(s)")
//                }
//                if ( isProfile ) {
//                    idcg.setAttribute("name", "Individual Profile(s)")
//                }
//                if ( isTimeseries ) {
//                    idcg.setAttribute("name", "Individual Station(s)")
//                }
//                if ( isPoint ) {
//                    idcg.setAttribute("name", "Points")
//                }
//                idcg.setAttribute("type", "selection")

//                Element idc = new Element("constraint")
//                idc.setAttribute("name","Select By")
//                if ( dsgIDVariablename != null ) {
//                    Element idv = new Element("variable")
//                    idv.setAttribute("IDREF", dsgIDVariablename+"-"+id)
//                    Element idkey = new Element("key")
//                    idkey.setText(dsgIDVariablename)
//                    idc.addContent(idv)
//                    idc.addContent(idkey)
//                    idcg.addContent(idc)
//                    cons.addContent(idcg)
//                }
//
//                Element subsetcg = new Element("constraint_group")
//                subsetcg.setAttribute("type", "subset")
//                subsetcg.setAttribute("name", "by Metadata")


                String lonn = lonVar.keySet().iterator().next()
                String latn = latVar.keySet().iterator().next()

                // Before using them, remove latn and lonn
                subsets.remove(latn)
                subsets.remove(lonn)

                if (subsets.keySet().size() > 0) {
                    for (Iterator subsetIt = subsets.keySet().iterator(); subsetIt.hasNext();) {

                        String name = (String) subsetIt.next()
                        AttributeTable var = subsets.get(name)

                        Variable vb = new Variable()
                        vb.setName(name)
                        vb.setGeometry(grid_type)
                        opendap.dap.Attribute ln = var.getAttribute("long_name")
                        if (ln != null) {
                            String longname = ln.getValueAt(0)
                            vb.setTitle(longname)
                        } else {
                            vb.setTitle(name)
                        }
                        vb.setUrl(url+"#"+name)
                        vb.setHash(getDigest(vb.getUrl()))
                        vb.setUnits("text")
                        vb.addToVariableAttributes(new VariableAttribute([name: "subset_variable", value: "true"]))
                        vb.addToVariableAttributes(new VariableAttribute([name: "geometry", value: grid_type.toLowerCase(Locale.ENGLISH)]))

                        GeoAxisX gx = new GeoAxisX(geoAxisX.properties)
                        gx.setVariable(vb)
                        vb.setGeoAxisX(gx)

                        GeoAxisY gy = new GeoAxisY(geoAxisY.properties)
                        gy.setVariable(vb)
                        vb.setGeoAxisY(gy)

                        def intervals = "xy"
                        if ( !zVar.keySet().isEmpty() ) {
                            VerticalAxis za = new VerticalAxis(zAxis.properties)
                            za.setVariable(vb)
                            vb.setVerticalAxis(za)
                            intervals = intervals + "z"
                        }

                        TimeAxis ta = new TimeAxis(timeAxis.properties)
                        ta.setVariable(vb)
                        vb.setTimeAxis(ta)

                        intervals = intervals + "t"
                        vb.setIntervals(intervals)
                        dataset.addToVariables(vb)
//TODO this is the subset variable constraint
//                        Element c = new Element("constraint")
//                        c.setAttribute("type", "subset")
//                        c.setAttribute("widget", "list")
//                        Element v = new Element("variable")
//                        v.setAttribute("IDREF", name+"-"+id)
//                        Element key = new Element("key")
//                        key.setText(name)
//                        c.addContent(v)
//                        c.addContent(key)
//                        subsetcg.addContent(c)
                    }
//                    cons.addContent(subsetcg)
                }

                int i = 0
                // Make the prop-prop list before adding in lat,lon and time.
                StringBuilder allv = new StringBuilder()
                for (Iterator subIt = data.keySet().iterator(); subIt.hasNext();) {
                    String key = (String) subIt.next()
                    allv.append(key)
                    if (subIt.hasNext()) allv.append(",")
                }

                // Z name zn is used below as well...

                String zn = null
                if (!zVar.keySet().isEmpty()) {
                    zn = zVar.keySet().iterator().next()
                }

                dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "all_variables", value: allv.toString()]))
                /*
                 * There is a page that will show thumbnails of property-property plots.
                 *
                 * It takes 3 pieces of metadata. First is the list of variables that will show up in the banner for a particular ID.
                 *
                     <thumbnails>
                 *
                 * THESE are ERDDAP variable names.
                          <metadata>expocode,vessel_name,investigators,qc_flag</metadata>
                 *
                 * Next is the list of plot paris:
                 *
                 *          <variable_pairs>
                               <!-- NO WHITESPACE AROUND THE COMMA -->
                               <!-- x-axis followed by y-axis variable. -->
                               <!-- LAS IDs -->

                               longitude-socatV3_c6c1_d431_8194,latitude-socatV3_c6c1_d431_8194
                               time-socatV3_c6c1_d431_8194,day_of_year-socatV3_c6c1_d431_8194
                               time-socatV3_c6c1_d431_8194,temp-socatV3_c6c1_d431_8194
                               time-socatV3_c6c1_d431_8194,Temperature_equi-socatV3_c6c1_d431_8194

                           </variable_paris>

                 * Finally, is just a flat list of every variable needed to make all the plots so there can be one data pull from ERDDAP.

                           <!-- The names of the variables needed to make all of the thumbnail plots so the netcdf file can be as minimal as possible.
                                Do not list latitude,longitude,depth,time,expocode
                                as these are handled by LAS internally
                           -->
                           <variable_names>day_of_year,temp,Temperature_equi</variable_names>

                 *
                 * The default set that we will construct will be lat vs lon and time vs all other varaibles.
                 */

                if (dsgIDVariablename != null) {
                    dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "metadata", value: dsgIDVariablename]))
                }

                StringBuilder pairs = new StringBuilder()
                StringBuilder vnames = new StringBuilder()
                pairs.append("\n")
                pairs.append(lonn + "-" + id + "," + latn + "-" + id + "\n")
                String timen = timeVar.keySet().iterator().next()



                List<String> data_variable_ids = new ArrayList()
                for (Iterator subIt = data.keySet().iterator(); subIt.hasNext();) {
                    String key = (String) subIt.next()
                    vnames.append(key)
                    if (subIt.hasNext()) {
                        vnames.append(",")
                    }
                    if (CdmDatatype.TRAJECTORY.contains(grid_type)) {
                        pairs.append(timen + "-" + id + "," + key + "-" + id + "\n")
                        pairs.append(key + "-" + id + "," + latn + "-" + id + "\n")
                        pairs.append(lonn + "-" + id + "," + key + "-" + id + "\n")
                    } else if (CdmDatatype.PROFILE.contains(grid_type) && zn != null) {
                        pairs.append(key + "-" + id + "," + zn + "-" + id + "\n")
                    } else if (CdmDatatype.TIMESERIES.contains(grid_type)) {
                        pairs.append(timen + "-" + id + "," + key + "-" + id + "\n")
                    } else if (CdmDatatype.POINT.contains(grid_type)) {
                        if (zn != null && !zn.equals("")) {
                            pairs.append(key + "-" + id + "," + zn + "-" + id + "\n")
                        }
                        pairs.append(key + "-" + id + "," + latn + "-" + id + "\n")
                        pairs.append(lonn + "-" + id + "," + key + "-" + id + "\n")
                    }
                    data_variable_ids.add(key + "-" + id)
                }

                // Pair up every data variable with every other.
                // Filter these in the UI to only use variables paired with current selection.
                StringBuilder data_pairs = new StringBuilder()

                for (int index = 0; index < data_variable_ids.size(); index++) {
                    for (int jindex = index; jindex < data_variable_ids.size(); jindex++) {
                        if (index != jindex) {
                            data_pairs.append(data_variable_ids.get(index) + "," + data_variable_ids.get(jindex) + "\n")
                        }
                    }
                }


                pairs.append("\n")
                dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "coordinate_pairs", value: pairs.toString()]))
                if (data_pairs.length() > 0) {
                    dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "variable_pairs", value: data_pairs.toString()]))
                }
                dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "variable_names", value: vnames.toString()]))

                // Add lat, lon and time to the data variable for output to the dataset

                String vn = lonVar.keySet().iterator().next()
                if (!data.containsKey(vn)) {
                    data.put(vn, lonVar.get(vn))
                }
                vn = latVar.keySet().iterator().next()
                if (!data.containsKey(vn)) {
                    data.put(vn, latVar.get(vn))
                }
                vn = timeVar.keySet().iterator().next()
                if (!data.containsKey(vn)) {
                    data.put(vn, timeVar.get(vn))
                }
                if (zn != null && !data.containsKey(zn)) {
                    data.put(zn, zVar.get(zn))
                }
                for (Iterator dataIt = data.keySet().iterator(); dataIt.hasNext();) {
                    String name = (String) dataIt.next()
                    // May already be done because it's a sub set variable??
                    boolean dummy = false
                    if (!subsets.containsKey(name)) {
                        if (!dummy && !name.toLowerCase(Locale.ENGLISH).contains("time") && !name.toLowerCase(Locale.ENGLISH).contains("lat") && !name.toLowerCase(Locale.ENGLISH).contains("lon") && !name.toLowerCase(Locale.ENGLISH).contains("depth")) {
                            dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "dummy", value: name]))
                            dummy = true
                        }
                        i++
                        AttributeTable var = data.get(name)
                        Variable vb = new Variable()
                        vb.setName(name)
                        vb.setUrl(url+"#"+name)
                        vb.setHash(getDigest(vb.getUrl()))
                        vb.setGeometry(grid_type)
                        opendap.dap.Attribute ua = var.getAttribute("units")
                        if (ua != null) {
                            String units = ua.getValueAt(0)
                            vb.setUnits(units)
                        } else {
                            vb.setUnits("none")
                        }
                        opendap.dap.Attribute ln = var.getAttribute("long_name")
                        if (ln != null) {
                            String longname = ln.getValueAt(0)
                            vb.setTitle(longname)
                        } else {
                            vb.setTitle(name)
                        }
                        GeoAxisX gx = new GeoAxisX(geoAxisX.properties)
                        gx.setVariable(vb)
                        vb.setGeoAxisX(gx)
                        GeoAxisY gy = new GeoAxisY(geoAxisY.properties)
                        gy.setVariable(vb)
                        vb.setGeoAxisY(gy)
                        def intervals = "xy"
                        if (!zVar.keySet().isEmpty()) {
                            VerticalAxis za = new VerticalAxis(zAxis.properties)
                            za.setVariable(vb)
                            vb.setVerticalAxis(za)
                            intervals = intervals + "z"
                        }
                        intervals = intervals + "t"
                        TimeAxis ta = new TimeAxis(timeAxis.properties)
                        ta.setVariable(vb)
                        vb.setTimeAxis(ta)
                        vb.setIntervals(intervals)
                        vb.addToVariableAttributes(new VariableAttribute([name: "grid_type", value: grid_type.toLowerCase(Locale.ENGLISH)]))
                        dataset.addToVariables(vb)
                    }

                }

                dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "table_variables", value: dsgIDVariablename]))

                // add any variable properties.
                for (Iterator varid = dataset.variables.iterator(); varid.hasNext();) {
                    Variable variableb = (Variable) varid.next()
                    // TODO Some variable properties nee do be pass in
//                    if ( varproperties != null && varproperties.length > 0 ) {
//                        for (int p = 0 p < varproperties.length p++) {
//                            // Split n-1 times so any ":" after the third remain
//                            String[] parts = varproperties[p].split(":", 4)
//                            if ( variableb.getUrl().endsWith(parts[0]) ) {
//                                variableb.setProperty(parts[1], parts[2], parts[3])
//                            }
//                        }
//                    }
                }

                // Add all the tabledap_access properties

                //TODO "Profile"
                if (dsgIDVariablename != null) {
                    dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: grid_type.toLowerCase(Locale.ENGLISH) + "_id", value: dsgIDVariablename]))
                }
                dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "server", value: "TableDAP " + grid_type.toLowerCase(Locale.ENGLISH)]))
                dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "title", value: title]))
                dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "id", value: id]))


                if (!default_supplied) {
                    dataset.addToDatasetProperties(new DatasetProperty([type: "ui", name: "default", value: "file:ui.xml#" + grid_type]))
                } else {
                    def dv
                    if ( default_value.contains("only") ) {
                        dv = grid_type+"_only"
                    } else {
                        dv = grid_type
                    }
                    dataset.addToDatasetProperties(new DatasetProperty([type: "ui", name: "default", value: "file:ui.xml#" + dv]))
                }

                //TODO the rest of the contrains logic
//                if ( !monthOfYear.keySet().isEmpty() ) {
//                    String name = monthOfYear.keySet().iterator().next()
//                    String mid = name+"-"+id
//                    Element season = new Element("constraint_group")
//                    season.setAttribute("type", "season")
//
//                    season.setAttribute("name", "by Season")
//                    Element con = new Element("constraint")
//                    con.setAttribute("widget", "month")
//                    Element variable = new Element("variable")
//                    variable.setAttribute("IDREF", mid)
//                    Element key = new Element("key")
//                    key.setText(name)
//                    con.addContent(key)
//                    con.addContent(variable)
//                    season.addContent(con)
//                    cons.addContent(season)
//                }

//                Element vrcg = new Element("constraint_group")
//                vrcg.setAttribute("type", "variable")
//                vrcg.setAttribute("name", "by Variable")
//                cons.addContent(vrcg)
//
//                Element valcg = new Element("constraint_group")
//                valcg.setAttribute("type", "valid")
//                valcg.setAttribute("name", "by Valid Data")
//                cons.addContent(valcg)
//
//
//                Element d = db.toXml()
//                d.addContent(cons)
//                datasetsE.addContent(d)


            }
            dataset
        } catch (Exception e) {
            log.error("Exception adding data set. "+e.getMessage())
            //TODO return an error
        } finally {
            if ( stream != null ) {
                try {
                    stream.close()
                } catch (IOException e) {
                    System.err.println("Error closing stream.  "+e.getMessage())
                }
            }
        }


    }
    Dataset ingest(String parentHash, String url) {

        // Set the status of the parent for each variable using the parentHash key.

        // Is it a netCDF data source?

        def hash = getDigest(url)
        def dataset = new Dataset([url: url, hash: hash])
        if (!parentHash) parentHash = dataset.getHash();

        // TODO catch exepctions and keep going...

        Formatter error = new Formatter()

        GridDataset gridDs
        try {
            ingestStatusService.saveProgress(parentHash, "Reading the OPeNDAP data source for the variables.")
            gridDs = (GridDataset) FeatureDatasetFactoryManager.open(FeatureType.GRID, url, null, error)
        } catch (IOException e) {
            dataset.setMessage(e.getMessage())
            dataset.setStatus(Dataset.INGEST_FAILED)
            return dataset
        }

        if ( gridDs != null) {

            log.debug("Grid data set found ... ")
            List<Attribute> globals = gridDs.getGlobalAttributes()
            // Get the DRS information


            Map<String, String> drsParams = new HashMap<String, String>()

            String title = url
            for (Iterator iterator = globals.iterator(); iterator.hasNext(); ) {
                Attribute attribute = (Attribute) iterator.next()
                if ( attribute.getShortName().equals("title") ) {
                    title = attribute.getStringValue()
                } else if ( attribute.getShortName().equals("dataset_title") ) {
                    title = attribute.getStringValue();
                }
                drsParams.put(attribute.getShortName(), attribute.getStringValue())
            }

            dataset.setTitle(title)

            List<GridDatatype> grids = gridDs.getGrids()
            String m = grids.size() + " variables were found. Processing..."
            ingestStatusService.saveProgress(parentHash, m)

            for (int i = 0; i < grids.size(); i++) {

                GridDatatype gridDatatype = (GridDatatype) grids.get(i);

                // The variable basics
                String vname = gridDatatype.getShortName()

                log.debug("Processing variable " + vname + " ...")
                //TODO do I need to get the attributes and find the long_name myself?

                String vtitle = gridDatatype.getDescription()
                if (vtitle == null) {
                    vtitle = vname
                }
                String vhash = getDigest(url + ":" + gridDatatype.getDescription())

                // Set the variable name in the DRS params...
                drsParams.put("shortname", gridDatatype.getShortName())

                // Build the DRS
//                DataReferenceSyntax drs = new DataReferenceSyntax()
//                // Map constructor does not work when called from Java!?!
//                drs.setActivity(drsParams.get("activity"))
//                drs.setData_structure(drsParams.get("data_structure"))
//                drs.setEnsemble(drsParams.get("ensemble"))
//                drs.setExperiment_id(drsParams.get("experiement_id"))
//                drs.setFrequency(drsParams.get("frequency"))
//                drs.setInstitute_id(drsParams.get("institute_id"))
//                drs.setInstrument(drsParams.get("instrument"))
//                drs.setModel_id(drsParams.get("model_id"))
//                drs.setObs_project(drsParams.get("obs_project"))
//                drs.setProduct(drsParams.get("product"))
//                drs.setRealm(drsParams.get("realm"))
//                drs.setSource_id(drsParams.get("source_id"))
//                drs.setShortname(drsParams.get("shortname"))

                GridCoordSystem gcs = gridDatatype.getCoordinateSystem()

                String units = gridDatatype.getUnitsString()
                // Axes are next...
                long tIndex = -1
                long zIndex = -1
                TimeAxis tAxis = null
                if (gcs.hasTimeAxis()) {

                    if (gcs.hasTimeAxis1D()) {
                        log.debug("1D time axis found ... ")
                        CoordinateAxis1DTime time = gcs.getTimeAxis1D()
                        CalendarDateRange range = time.getCalendarDateRange()

                        // Get the basics
                        String start = range.getStart().toString()
                        String end = range.getEnd().toString()
                        long size = time.getSize()
                        tIndex = size / 2
                        if (tIndex <= 0) tIndex = 1
                        String timeunits = time.getUnitsString()
                        Attribute cal = time.findAttribute("calendar")
                        String calendar = "standard"
                        if (cal != null) {
                            calendar = cal.getStringValue(0)
                        }
                        String shortname = time.getShortName()
                        String timetitle = time.getFullName()

                        // Figure out the delta (as a period string) and where the time is marked (beginning, middle, or end of the period
                        double[] tb1 = time.getBound1()
                        double[] tb2 = time.getBound2()
                        double[] times = time.getCoordValues()

                        CalendarDateUnit cdu = CalendarDateUnit.of(calendar, timeunits)
                        Period p0 = null
                        String position0 = getPosition(times[0], tb1[0], tb2[0])
                        boolean regular = true
                        boolean constant_position = true
                        regular = time.isRegular()
                        tAxis = new TimeAxis()

                        TimeUnit tu = time.getTimeResolution()
                        double du = tu.getValue()
                        String u = tu.getUnitString()

                        if ( times.length > 1 ) {
                            if (regular) {
                                // TODO sec, week, year?
                                if (u.contains("hour")) {
                                    for (int d = 0; d < 27; d++) {
                                        if (du < 23.5 * d && du < 23.5 * d + 1) {
                                            // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                            p0 = new Period(0, 0, 0, d, 0, 0, 0, 0)
                                        }
                                    }
                                    if (p0 == null) {
                                        if (du > 28 * 24 && du < 33 * 24) {
                                            p0 = new Period(0, 1, 0, 0, 0, 0, 0, 0)
                                        }
                                    }

                                } else if (u.contains("day")) {
                                    if (du < 1) {
                                        int hours = du * 24.0d
                                        p0 = new Period(0, 0, 0, 0, hours, 0, 0, 0)
                                    } else {
                                        p0 = new Period(0, 0, 0, du, 0, 0, 0, 0)
                                    }

                                }
                            } else {
                                p0 = getPeriod(cdu, times[0], times[1])
                                int hours = p0.getHours()
                                int days = p0.getDays()
                                int months = p0.getMonths()
                                int years = p0.getYears()
                                if (days >= 28) {
                                    p0 = new Period(0, 1, 0, 0, 0, 0, 0, 0)
                                } else if (hours == 0 && days > 0) {
                                    p0 = new Period(0, 0, 0, days, 0, 0, 0, 0)
                                } else if (hours > 0) {
                                    p0 = new Period(0, 0, 0, 0, hours, 0, 0, 0)
                                }
                            }

                            Period period = getPeriod(cdu, times[0], times[times.length - 1])

                            log.debug("Setting delta " + pf.print(p0))
                            if (p0 != null) {
                                tAxis.setDelta(pf.print(p0))
                            } else {
                                tAxis.setDelta("P1D")
                            }
                            log.debug("Setting data set period to " + pf.print(period))
                            if (period != null) {
                                tAxis.setPeriod(pf.print(period))
                            }

                        } else if ( times.length ) {
                            tAxis.setDelta(pf.print(Period.ZERO));
                            tAxis.setPeriod(pf.print(Period.ZERO))
                        }


                        tAxis.setStart(start)
                        tAxis.setEnd(end)
                        if (start.contains("0000") && end.contains("0000")) {
                            tAxis.setClimatology(true)
                        } else {
                            tAxis.setClimatology(false)
                        }
                        tAxis.setSize(size)
                        tAxis.setUnits(units)
                        tAxis.setCalendar(calendar)
                        tAxis.setTitle(title)
                        tAxis.setName(shortname)

                        if (constant_position) {
                            tAxis.setPosition(position0)
                        }


                    } else {
                        // TODO 2D Time Axis
                    }
                }

                CoordinateAxis xca = gcs.getXHorizAxis()
                GeoAxisX xAxis = null
                if (xca instanceof CoordinateAxis1D) {
                    CoordinateAxis1D x = (CoordinateAxis1D) xca
                    xAxis = new GeoAxisX()
                    xAxis.setType("x")
                    xAxis.setTitle(x.getFullName())
                    xAxis.setName(x.getShortName())
                    xAxis.setUnits(x.getUnitsString())
                    xAxis.setRegular(x.isRegular())
                    if (x.isRegular()) {
                        xAxis.setDelta(x.getIncrement())
                    }
                    xAxis.setMin(x.getMinValue())
                    xAxis.setMax(x.getMaxValue())
                    xAxis.setSize(x.getSize())
                    xAxis.setDimensions(1)

                } else if (xca instanceof CoordinateAxis2D) {
                    CoordinateAxis2D x = (CoordinateAxis2D) xca
                    xAxis = new GeoAxisX()
                    xAxis.setType("x")
                    xAxis.setTitle(x.getFullName())
                    xAxis.setName(x.getShortName())
                    xAxis.setUnits(x.getUnitsString())
                    xAxis.setRegular(false)
                    xAxis.setDimensions(2)
                    xAxis.setMin(x.getMinValue())
                    xAxis.setMax(x.getMaxValue())
                    xAxis.setSize(x.getSize())
                }
                GeoAxisY yAxis = null
                CoordinateAxis yca = gcs.getYHorizAxis()
                if (yca instanceof CoordinateAxis1D) {
                    CoordinateAxis1D y = (CoordinateAxis1D) yca
                    yAxis = new GeoAxisY()
                    yAxis.setType("y")
                    yAxis.setTitle(y.getFullName())
                    yAxis.setName(y.getShortName())
                    yAxis.setUnits(y.getUnitsString())
                    yAxis.setRegular(y.isRegular())
                    if (y.isRegular()) {
                        yAxis.setDelta(y.getIncrement())
                    }
                    yAxis.setMin(y.getMinValue())
                    yAxis.setMax(y.getMaxValue())
                    yAxis.setSize(y.getSize())
                    yAxis.setDimensions(1)

                } else {
                    CoordinateAxis2D y = (CoordinateAxis2D) yca
                    yAxis = new GeoAxisY()
                    yAxis.setType("y")
                    yAxis.setTitle(y.getFullName())
                    yAxis.setName(y.getShortName())
                    yAxis.setUnits(y.getUnitsString())
                    yAxis.setRegular(false)
                    yAxis.setMin(y.getMinValue())
                    yAxis.setMax(y.getMaxValue())
                    yAxis.setSize(y.getSize())
                    yAxis.setDimensions(2)
                }
                CoordinateAxis1D z = gcs.getVerticalAxis()
                VerticalAxis zAxis = null
                if (z != null) {
                    // Use the first z. It's probably more interesting.
                    zIndex = 1
                    zAxis = new VerticalAxis()
                    zAxis.setSize(z.getSize())
                    zAxis.setType("z")
                    zAxis.setTitle(z.getFullName())
                    zAxis.setName(z.getShortName())
                    zAxis.setMin(z.getMinValue())
                    zAxis.setMax(z.getMaxValue())
                    zAxis.setRegular(z.isRegular())
                    zAxis.setUnits(z.getUnitsString())
                    if (zAxis.isRegular()) {
                        zAxis.setDelta(z.getIncrement())
                    }
                    double[] v = z.getCoordValues()
                    List<Zvalue> values = new ArrayList<Zvalue>()
                    for (int j = 0; j < v.length; j++) {
                        Zvalue zv = new Zvalue()
                        zv.setZ(v[j])
                        values.add(zv)
                    }
                    zAxis.setZV(values)
                    zAxis.setPositive(z.getPositive())

                }
// The make stats code in-line
//                def ferret = Ferret.first()
//                def palette = "blue_darkorange"
//                if ( vtitle.toLowerCase().contains("temp") ) {
//                    palette = "blue_darkred"
//                } else if ( vtitle.toLowerCase().contains("precip") ) {
//                    palette = "brown_blue"
//                }
//
//                int tsize
//                if ( tAxis ) {
//                    tsize = tAxis.size
//                } else {
//                    tsize = 1
//                }
//                int min = Math.min(12, tsize)
//                // DEBUG
//                //TODO remove this debug
//                min = 1
//                Stats stats = null
//                for ( int tcounter = 0 tcounter < min tcounter++ ) {
//                    StringBuilder jnl = new StringBuilder()
//                    StringBuilder timeRange = new StringBuilder()
//                    StringBuilder levelRange = new StringBuilder()
//
//                    def xcount = xAxis.size
//                    def ycount = yAxis.size
//
//                    def xstride = 1
//                    if ( xcount >= 1000 && xcount < 3000 ) {
//                        xstride = 5
//                    } else if ( xcount >=3000 && xcount < 5000 ) {
//                        xstride = 10
//                    } else if ( xcount >= 5000 ) {
//                        xstride = 50
//                    }
//
//                    def ystride = 1
//                    if ( ycount >= 1000 && ycount < 3000 ) {
//                        ystride = 5
//                    } else if ( ycount >=3000 && ycount < 5000 ) {
//                        ystride = 10
//                    } else if ( ycount >= 5000){
//                        ystride = 50
//                    }
//                    if ( tAxis ) {
//                        int tindex = tcounter + 1
//
//                        timeRange.append("l=\""+tindex+"\"")
//                    }
//
//                    if ( zAxis ) {
//                        // Always do the first level.
//                        levelRange.append("/k=1")
//                    }
//
//                    def relativeStatsBase = dataset.hash + File.separator + vname
//                    File base = new File(ferret.tempDir + File.separator + relativeStatsBase)
//                    if ( !base.exists() ) {
//                        base.mkdirs()
//                    }
//                    def relativeStatsPath = relativeStatsBase + File.separator + "stats.txt"
//                    def relativeColorBar = relativeStatsBase
//                    jnl.append("DEFINE SYMBOL DATA = ${url}\n")
//                    if ( vname.contains("-") ) {
//                        jnl.append("DEFINE SYMBOL PARAMETER = '$vname'\n")
//                    } else {
//                        jnl.append("DEFINE SYMBOL PARAMETER = $vname\n")
//                    }
//                    if ( timeRange.length() > 0 || levelRange.length() > 0 ) {
//                        jnl.append("DEFINE SYMBOL RANGES=${timeRange.toString()}${levelRange.toString()}\n")
//                    }
//                    jnl.append("DEFINE SYMBOL OUTPUT = ${relativeStatsPath}\n")
//                    jnl.append("LET XSTRIDE = ${xstride}\n")
//                    jnl.append("LET YSTRIDE = ${ystride}\n")
//
//                    jnl.append("go stats\n")
//
//                    File statsFile = new File(ferret.tempDir + File.separator + relativeStatsPath)
//
//
//
//                    def ferretResult = ferretService.runScript(jnl.toString())
//                    if ( ferretResult["error"] == false ) {
//                        Stats statsTemp = new Stats(ferret.tempDir + File.separator + relativeStatsPath)
//                        statsTemp.setPalette(palette)
//                        if ( !stats ) {
//                            stats = statsTemp
//                        } else {
//                            if ( statsTemp.histogram_lev_min < stats.histogram_lev_min) {
//                                stats.histogram_lev_min = statsTemp.histogram_lev_min
//                            }
//                            if ( statsTemp.histogram_lev_max > stats.histogram_lev_max ) {
//                                stats.histogram_lev_max = statsTemp.histogram_lev_max
//                            }
//                        }
//
//                    } else {
//                        log.error(ferretResult["message"])
//                        return null
//                    }
//                    statsFile.delete()
//
//                }
//
//
//                stats.computeDel()
// End of make stats in-line

                String intervals = ""
                if (xAxis) {
                    intervals = intervals + "x"
                }
                if (yAxis) {
                    intervals = intervals + "y"
                }
                if (zAxis) {
                    intervals = intervals + "z"
                }
                if (tAxis) {
                    intervals = intervals + "t"
                }

                Variable variable = new Variable()
                variable.setUrl(url)
                variable.setName(vname)
                variable.setHash(vhash)
                variable.setTitle(vtitle)
                variable.setGeometry(GeometryType.GRID)
                variable.setIntervals(intervals)
                variable.setUnits(units)

                variable.setGeoAxisX(xAxis)
                xAxis.setVariable(variable)

                variable.setGeoAxisY(yAxis)
                yAxis.setVariable(variable)

                if (zAxis) {
                    variable.setVerticalAxis(zAxis)
                    zAxis.setVariable(variable)
                }
                if (tAxis) {
                    variable.setTimeAxis(tAxis)
                    tAxis.setVariable(variable)
                }

//                if ( variable.validate() ) {
//                    variable.save(failOnError: true)
//                } else {
//                    variable.errors.allErrors.each {
//                        print it
//                    }
//                }

                log.debug("Adding " + variable.getTitle() + " to data set")
                dataset.addToVariables(variable)
                dataset.variableChildren = true;
                dataset.geometry = GeometryType.GRID

                int done = i + 1;
                String m2 = done + "variables out of a total of " + grids.size() + " have been processed."
                ingestStatusService.saveProgress(parentHash, m2)

            }

            if ( !dataset.validate() ) {
                dataset.errors.each {
                    log.debug(it.toString())
                }
            }

        } else {
            // Is it a THREDDS catalog?
        }
        dataset
        // Is it an ESGF catalog or data set?
    }
    // Sometimes hierarchies from THREDDS servers end up with several levels
    // of children with only one child at each level. This makes for a bunch
    // of miserable clicking.
    // This method will remove any intermediate data sets with only one child
    def cleanup() {
        List<Dataset> datasets = Dataset.findAllVariableChildren()
        for (int i = 0; i < datasets.size(); i++) {
            Dataset dataset = datasets.get(i)
            collapse(dataset)
        }

    }
    def collapse(Dataset dataset) {
        Dataset parent = dataset.getParent()
        if ( parent ) {
            Dataset grandparent = parent.getParent()
            if (grandparent) {
                if (grandparent.getDatasets().size() == 1 && parent.getDatasets().size() == 1) {
                    // Parent is superfluous
                    parent.removeFromDatasets(dataset)
                    grandparent.removeFromDatasets(parent)
                    dataset.setParent(grandparent)
                    grandparent.addToDatasets(dataset)
                    log.debug("Removing: " + parent.getTitle() + " with id " + parent.getId() )
                    parent.delete()
                    // Are we at the top yet?
                    if (grandparent.getParent()) {
                        if (grandparent.getParent().getParent()) {
                            // The parent is dead, continue from the grandparent
                            collapse(grandparent)
                        }
                    }
                } else {
                    // If there is more hierarchy above continue
                    if (parent.getParent()) {
                        if (parent.getParent().getParent()) {
                            collapse(parent)
                        }
                    }
                }
            }
        }
    }
    public void addVariablesToAll() {

        List<Dataset> needIngest = Dataset.withCriteria{
            eq("variableChildren", true)
            isEmpty("variables")
        }

        for (int i = 0; i < needIngest.size(); i++) {
            Dataset d = needIngest.get(i)
            log.debug("Adding variables to " + d.getUrl() + " which has variableChildren = " + d.variableChildren)
            addVariablesAndSaveFromThredds(d.getUrl(), d.getHash(), null, true)
        }
    }
    private static Period getPeriod(CalendarDateUnit cdu, double t0, double t1) {
        CalendarDate cdt0 = cdu.makeCalendarDate(t0)
        CalendarDate cdt1 = cdu.makeCalendarDate(t1)
        DateTime dt0 = new DateTime(cdt0.getMillis()).withZone(DateTimeZone.UTC)
        DateTime dt1 = new DateTime(cdt1.getMillis()).withZone(DateTimeZone.UTC)

        return new Period(dt0, dt1)
    }
    private static String getPosition(double t, double tb1, double tb2) {
        String position = null
        double c1 = tb1 - t
        double ca1 = Math.abs(c1)

        double delta = 0.00001d

        if ( c1 < delta ) {
            position = "beginning"
        }

        double c2 = t - tb2
        double ca2 = Math.abs(c2)

        if ( ca2 < delta ) {
            position = "end"
        }
        if ( Math.abs(( tb1 + ((tb2 - tb1)/2.0d) ) - t) < delta ) {
            position = "middle"
        }
        return position
    }
    public static String getDigest(String url) {
        MessageDigest md
        StringBuffer sb = new StringBuffer()
        try {
            md = MessageDigest.getInstance("MD5")
            md.update(url.getBytes())
            byte[] digest = md.digest()
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff))
            }
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            System.err.println(e.getMessage())
        }
        return sb.toString()


    }


}
