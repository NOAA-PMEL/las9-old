package pmel.sdig.las.client.main;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import google.visualization.AnnotationChart;
import google.visualization.AnnotationChartOptions;
import google.visualization.DataTable;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialToast;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.Resource;
import org.fusesource.restygwt.client.RestService;
import org.fusesource.restygwt.client.RestServiceProxy;
import org.fusesource.restygwt.client.TextCallback;
import org.gwttime.time.DateTime;
import org.gwttime.time.DateTimeZone;
import org.gwttime.time.format.DateTimeFormatter;
import org.gwttime.time.format.ISODateTimeFormat;
import org.moxieapps.gwt.highcharts.client.Axis;
import org.moxieapps.gwt.highcharts.client.BaseChart;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Series;
import pmel.sdig.las.client.event.BreadcrumbSelect;
import pmel.sdig.las.client.event.DateChange;
import pmel.sdig.las.client.event.FeatureModifiedEvent;
import pmel.sdig.las.client.event.MapChangeEvent;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.event.PanelCount;
import pmel.sdig.las.client.event.PlotOptionChange;
import pmel.sdig.las.client.event.PlotOptionChangeHandler;
import pmel.sdig.las.client.event.ProductSelected;
import pmel.sdig.las.client.event.ProductSelectedHandler;
import pmel.sdig.las.client.map.OLMapWidget;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.client.util.Constants;
import pmel.sdig.las.client.widget.AxisWidget;
import pmel.sdig.las.client.widget.Breadcrumb;
import pmel.sdig.las.client.widget.DateTimeWidget;
import pmel.sdig.las.client.widget.MenuOptionsWidget;
import pmel.sdig.las.client.widget.ProductButton;
import pmel.sdig.las.client.widget.ProductButtonList;
import pmel.sdig.las.shared.autobean.Config;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.LASRequest;
import pmel.sdig.las.shared.autobean.Operation;
import pmel.sdig.las.shared.autobean.Product;
import pmel.sdig.las.shared.autobean.ProductResults;
import pmel.sdig.las.shared.autobean.RequestProperty;
import pmel.sdig.las.shared.autobean.Site;
import pmel.sdig.las.shared.autobean.TimeAxis;
import pmel.sdig.las.shared.autobean.Variable;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static pmel.sdig.las.client.util.Constants.navWidth;

/**
 * Created by rhs on 9/8/15.
 */
public class UI implements EntryPoint {

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    Resource siteResource = new Resource(Constants.siteJson);
    SiteService siteService = GWT.create(SiteService.class);

    Resource datasetResource = new Resource(Constants.datasetJson);
    DatasetService datasetService = GWT.create(DatasetService.class);

    Resource configResource = new Resource(Constants.configJson);
    ConfigService configService = GWT.create(ConfigService.class);

    Resource productResource = new Resource(Constants.productCreate);
    ProductService productService = GWT.create(ProductService.class);

    Resource datatableResource = new Resource(Constants.datatable);
    DatatableService datatableService = GWT.create(DatatableService.class);

    ProductButtonList products = new ProductButtonList();

    State state = new State();

    Layout layout = new Layout();
    OLMapWidget refMap;
    DateTimeWidget dateTimeWidget = new DateTimeWidget();

    AxisWidget zAxisWidget = new AxisWidget();


    String tile_server;
    String tile_layer;

    // The currently selected variable.
    Variable variable;

    DateTimeFormatter iso;

    Queue<LASRequest> requestQueue = new LinkedList<LASRequest>();

    public void onModuleLoad() {

        iso = ISODateTimeFormat.dateTimeNoMillis();

        Element wmsserver = DOM.getElementById("wms-server");
        if ( wmsserver != null )
            tile_server = wmsserver.getPropertyString("content");
        Element wmslayer = DOM.getElementById("wms-layer");
        if ( wmslayer != null )
            tile_layer = wmslayer.getPropertyString("content");


        refMap = new OLMapWidget("128px", "256px", tile_server, tile_layer);
        layout.setMap(refMap);

        ((RestServiceProxy)siteService).setResource(siteResource);
        // Call for the site now
        layout.showDataProgress();
        siteService.getSite("1.json", siteCallback);

        // Get ready to call for the config when a variable is selected
        ((RestServiceProxy)configService).setResource(configResource);

        ((RestServiceProxy)productService).setResource(productResource);

        ((RestServiceProxy)datatableService).setResource(datatableResource);

        ((RestServiceProxy)datasetService).setResource(datasetResource);

        eventBus.addHandler(PlotOptionChange.TYPE, new PlotOptionChangeHandler() {
            @Override
            public void onPlotOptionChange(PlotOptionChange event) {
                layout.setUpdate(Color.RED);
            }
        });
        eventBus.addHandler(ProductSelected.TYPE, new ProductSelectedHandler() {
            @Override
            public void onProductSelected(ProductSelected event) {
                ProductButton pb = (ProductButton) event.getSource();
                setProduct(pb.getProduct());
                layout.setUpdate(Color.RED);
            }
        });

        eventBus.addHandler(PanelCount.TYPE, new PanelCount.Handler() {
            @Override
            public void onPanelCountChange(PanelCount event) {
                int count = event.getCount();
                MaterialToast.fireToast("Setting panel count to "+count);

                // This is a hack for now.
                layout.panel1.getOutputPanel().setPanelCount(count);
                layout.panel1.scale();
            }
        });
        eventBus.addHandler(BreadcrumbSelect.TYPE, new BreadcrumbSelect.Handler() {
            @Override
            public void onBreadcrumbSelect(BreadcrumbSelect event) {
                Object selected = event.getSelected();
                if ( selected != null ) {
                    layout.removeBreadcrumbs(selected, 1);
                    if (selected instanceof Dataset) {
                        Dataset dataset = (Dataset) selected;
                        datasetService.getDataset(dataset.getId()+".json", datasetCallback);
                    } else if (selected instanceof Variable) {
                        Variable variable = (Variable) event.getSelected();
                        configService.getConfig(variable.getId(), configCallback);
                    }
                } else {
                    // This was the home button...

                    siteService.getSite("1.json", siteCallback);
                    layout.removeBreadcrumbs(1);
                    layout.clearDatasets();
                    layout.showDataProgress();

                }
            }
        });
        eventBus.addHandler(NavSelect.TYPE, new NavSelect.Handler() {
            @Override
            public void onNavSelect(NavSelect event) {

                Object selected = event.getSelected();
                final Breadcrumb bc = new Breadcrumb(selected, layout.getBreadcrumbCount(1) > 0);
                bc.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        eventBus.fireEventFromSource(new BreadcrumbSelect(selected, 1), bc);
                        // Breadcrumbs are in the collapsible header, so stop to prevent the crumb from opening the panel.
                        event.stopPropagation();
                    }
                });
                if ( selected instanceof Dataset ) {
                    Dataset dataset = (Dataset) selected;
                    layout.clearDatasets();
                    layout.showDataProgress();
                    datasetService.getDataset(dataset.getId()+".json", datasetCallback);
                    List<Variable> variables = dataset.getVariables();
                    if ( variables != null && variables.size() > 0 ) {
                        Collections.sort(variables);
                        for (int i = 0; i < variables.size(); i++) {
                            layout.addSelection(variables.get(i));
                        }
                    }
                } else if ( selected instanceof Variable ) {
                    Variable variable = (Variable) event.getSelected();
                    layout.showProgress();
                    configService.getConfig(variable.getId(), configCallback);
                }
                layout.addBreadcrumb(bc, 1);
            }
        });
        eventBus.addHandler(DateChange.TYPE, new DateChange.Handler() {
            @Override
            public void onDateChange(DateChange event) {
                layout.setUpdate(Color.RED);
            }
        });

        eventBus.addHandler(MapChangeEvent.TYPE, new MapChangeEvent.Handler() {
            @Override
            public void onMapSelectionChange(MapChangeEvent event) {
                layout.setUpdate(Color.RED);
                if ( event.getSource() instanceof PushButton) {
                    PushButton b = (PushButton) event.getSource();
                    if ( b.getTitle() != null && b.getTitle().equals("Reset Map") ) {
                        layout.panel1.getOutputPanel().clearOverlay();
                    }
                }
            }
        });

        eventBus.addHandler(FeatureModifiedEvent.TYPE, new FeatureModifiedEvent.Handler() {
            @Override
            public void onFeatureModified(FeatureModifiedEvent event) {
                layout.setUpdate(Color.RED);
            }
        });
        eventBus.addHandler(ClickEvent.getType(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (event.getSource() instanceof MaterialIcon) {
                    MaterialIcon b = (MaterialIcon) event.getSource();
                    if ( b.getTitle().equals("Open the controls.") ) {
                        layout.controlsWindow.open();
                    }
                    event.stopPropagation();
                }

                if (event.getSource() instanceof MaterialButton) {
                    MaterialButton b = (MaterialButton) event.getSource();
                    if (b.getText().toLowerCase().equals("update") ) {
                        Product p = products.getSelectedProduct();
                        update(p);
                    }

                }

            }
        });


        Mouse mouse = new Mouse();
        layout.addMouse(1, mouse);
//
        layout.setDateTime(dateTimeWidget);
        layout.addVerticalAxis(zAxisWidget);
        RootPanel.get("load").getElement().setInnerHTML("");
        RootPanel.get("las_main").add(layout);
//
//
//        layout.show();

    }
    public class Mouse {
        public void applyNeeded() {
            layout.setUpdate(Color.RED);
        }

        public void setZ(double zlo, double zhi) {
             zAxisWidget.setNearestLo(zlo);
             zAxisWidget.setNearestHi(zhi);
        }

        public void updateLat(double ylo, double yhi) {
            refMap.setCurrentSelection(ylo, yhi, refMap.getXlo(), refMap.getXhi());
        }

        public void updateLon(double xlo, double xhi) {
            refMap.setCurrentSelection(refMap.getYlo(), refMap.getYhi(), xlo, xhi);
        }

        public void updateMap(double ylo, double yhi, double xlo, double xhi) {
            refMap.setCurrentSelection(ylo, yhi, xlo, xhi);
        }

        public void updateTime(double tlo, double thi, String time_origin, String unitsString, String calendar) {
            dateTimeWidget.setLoByDouble(tlo, time_origin, unitsString, calendar);
            dateTimeWidget.setHiByDouble(thi, time_origin, unitsString, calendar);
        }
    }


    public interface DatasetService extends RestService {
        @GET
        @Path("/{id}") // String so it can have ".json" suffix
        public void getDataset(@PathParam("id") String id, MethodCallback<Dataset> datasetCallback);
    }
    public interface SiteService extends RestService {
        @GET
        @Path("/{id}")
        public void getSite(@PathParam("id") String id, MethodCallback<Site> siteCallback);
    }
    public interface ConfigService extends RestService {
        @GET
        @Path("/{id}")
        public void getConfig(@PathParam("id") long id, MethodCallback<Config> configCallback);
    }
    public interface ProductService extends RestService {
        @POST
        public void getProduct(LASRequest lasRequest, MethodCallback<ProductResults> productRequestCallback);
    }
    public interface DatatableService extends RestService {
        @POST
        public void datatable(LASRequest datatableRequest, TextCallback data);
    }
    TextCallback makeChartCallback = new TextCallback() {

        @Override
        public void onFailure(Method method, Throwable exception) {
            Window.alert("Request failed: "+exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, String response) {

            JSONValue jsonV = JSONParser.parseStrict(response);

            JSONObject jsonO = jsonV.isObject();

            JSONObject table = (JSONObject) jsonO.get("table");
            JSONArray names = (JSONArray) table.get("columnNames");
            JSONArray rows = (JSONArray) table.get("rows");

            Chart chart = new Chart()
                    .setType(Series.Type.SPLINE)
                    .setChartTitleText(variable.getTitle())
                    .setMarginRight(10)
                    .setZoomType(BaseChart.ZoomType.X);

            chart.getXAxis().setType(Axis.Type.DATE_TIME);
            Series series = chart.createSeries()
                    .setName(variable.getUnits());
            for (int i = 0; i < rows.size(); i++ ) {
                JSONArray aRow = (JSONArray) rows.get(i);
                JSONValue d = aRow.get(5);
                JSONValue o = aRow.get(6);
                if ( !d.toString().equals("null") ) {
                    String dateTimeString = d.toString();
                    dateTimeString = dateTimeString.replace("\"", "");
                    DateTime dt = iso.parseDateTime(dateTimeString).withZone(DateTimeZone.UTC);
                    if (!o.toString().equals("null")) {
                        series.addPoint(dt.getMillis(), o.isNumber().doubleValue());
                    } else {
                        series.addPoint(dt.getMillis(), null);
                    }
                }
            }


            chart.addSeries(series);

//            layout.resultsPanel01.getChart().clear();
//            layout.resultsPanel01.getChart().add(chart);
        }
    };
    TextCallback makeGoogleChartFromJSON = new TextCallback() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            layout.hideProgress();
            layout.setUpdate(Color.WHITE);
            Window.alert("Request failed: "+exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, String response) {
            layout.hideProgress();
            layout.setUpdate(Color.WHITE);
            layout.panel1.openAnnotations();

            DataTable dataTable = new DataTable(response);

            AnnotationChartOptions options = new AnnotationChartOptions();
            options.setDisplayAnnotations(true);
            layout.panel1.getChart().setWidth((Window.getClientWidth() - navWidth)+"px");
            layout.panel1.getChart().setHeight((Window.getClientHeight() - navWidth)+"px");
            layout.panel1.getChart().setMargin(0.0d);

            AnnotationChart annChart = new AnnotationChart(layout.panel1.getChart().getElement());
            annChart.draw(dataTable, options);

        }
    };
    TextCallback makeChartFromJSON = new TextCallback() {

        @Override
        public void onFailure(Method method, Throwable exception) {
            Window.alert("Request failed: "+exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, String response) {

            JSONValue jsonV = JSONParser.parseStrict(response);

            JSONArray jsonO = jsonV.isArray();

            Chart chart = new Chart()
                    .setType(Series.Type.SPLINE)
                    .setChartTitleText(variable.getTitle())
                    .setMarginRight(10)
                    .setZoomType(BaseChart.ZoomType.X);

            chart.getXAxis().setType(Axis.Type.DATE_TIME);
            Series series = chart.createSeries()
                    .setName(variable.getTitle() + " " +variable.getUnits());

            for( int i=0; i < jsonO.size(); i++ ) {
                JSONArray a = (JSONArray) jsonO.get(i);
                JSONValue one = a.get(0);
                JSONValue two = a.get(1);
                if ( two.isNull() != null )  {
                    series.addPoint(one.isNumber().doubleValue(), null);
                } else {
                    series.addPoint(a.get(0).isNumber().doubleValue(), two.isNumber().doubleValue());
                }
            }

            chart.addSeries(series);

//            layout.resultsPanel01.getChart().clear();
//            layout.resultsPanel01.getChart().add(chart);
        }
    };
    MethodCallback<ProductResults> productRequestCallback = new MethodCallback<ProductResults>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            layout.setUpdate(Color.WHITE);
            layout.hideProgress();
            Window.alert("Request failed: "+exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, ProductResults results) {
            layout.setUpdate(Color.WHITE);
            layout.hideProgress();
            state.getPanelState(1).setProductResults(results);
            layout.setState(1, state);
        }
    };
    MethodCallback<Dataset> datasetCallback = new MethodCallback<Dataset>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            layout.hideDataProgress();
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, Dataset dataset) {
            layout.hideDataProgress();
            layout.clearDatasets();
            if ( dataset.getDatasets().size() > 0 ) {
                List<Dataset> datasets = dataset.getDatasets();
                Collections.sort(datasets);
                for (int i = 0; i < datasets.size(); i++) {
                    layout.addSelection(datasets.get(i));
                }
            }
            if ( dataset.getVariables().size() > 0 ) {
                List<Variable> variables = dataset.getVariables();
                Collections.sort(variables);
                for (int i = 0; i < variables.size(); i++) {
                    layout.addSelection(variables.get(i));
                }
            }
        }
    };
    MethodCallback<Site> siteCallback = new MethodCallback<Site>() {

        public void onSuccess(Method method, Site site) {
            layout.hideDataProgress();
            layout.clearDatasets();
            layout.setBrand(site.getTitle());
            if ( site.getDatasets().size() > 0 ) {
                List<Dataset> datasets = site.getDatasets();
                Collections.sort(datasets);
                for (int i = 0; i < datasets.size(); i++) {
                    layout.addSelection(datasets.get(i));
                }

            }

        }

        public void onFailure(Method method, Throwable exception) {
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
            layout.hideDataProgress();
        }
    };
    MethodCallback<Config> configCallback = new MethodCallback<Config>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, Config config) {
            // Apply the config

            variable = config.getVariable();

            TimeAxis tAxis = variable.getTimeAxis();

            dateTimeWidget.init(tAxis, false);
            if ( variable.getVerticalAxis() != null ) {
                zAxisWidget.init(variable.getVerticalAxis());
            }

            refMap.setDataExtent(variable.getGeoAxisY().getMin(), variable.getGeoAxisY().getMax(), variable.getGeoAxisX().getMin(), variable.getGeoAxisX().getMax(), variable.getGeoAxisX().getDelta());
            List<Product> productsList = config.getProducts();
//
            products.init(productsList);
            Product p = products.getSelectedProduct();
            setProduct(p);

            layout.addProducts(products);

            String display_hi = tAxis.getDisplay_hi();
            String display_lo = tAxis.getDisplay_lo();

            if ( display_hi != null ) {
                dateTimeWidget.setHi(display_hi);
            }
            if ( display_lo != null ) {
                dateTimeWidget.setLo(display_lo);
            }

            if ( p != null ) {

                update(p);

            }

        }
    };
    private void update(Product p) {
        layout.showProgress();
        if ( p.isClientPlot() ) {

            if ( variable.getGeometry().equals(Constants.TIMESERIES) ) {

                layout.panel1.getOutputPanel().setVisible(false);
                layout.panel1.getChart().setVisible(true);
                makeDataRequest(variable, p);

            }


        } else {
            queueRequest(1);
            layout.panel1.getChart().setVisible(false);
            layout.panel1.getOutputPanel().setVisible(true);
            layout.panel1.clearAnnotations();
            processQueue();
        }
    }
    private void makeDataRequest(Variable variable, Product p) {
        LASRequest timeseriesReqeust = makeRequest(1);
        if ( p.getTitle().equals("Timeseries") ) {
//            erddapDataService.erddapTimeseries(timeseriesReqeust, makeChartFromJSON);

            // TODO hack for using two different chart clients
        } else if ( p.getTitle().equals("Timeseries Plot") ) {
            datatableService.datatable(timeseriesReqeust, makeGoogleChartFromJSON);
            layout.panel1.clearAnnotations();
            layout.panel1.addAnnotation(new MaterialLabel("Dataset: " +layout.getDataset(1).getTitle()));
            layout.panel1.addAnnotation(new MaterialLabel("Variable: " +variable.getTitle()));
            layout.panel1.addAnnotation(new MaterialLabel("Time: " + timeseriesReqeust.getTlo()+" : " + timeseriesReqeust.getThi() ));
            if ( variable.getVerticalAxis() != null ) {
                layout.panel1.addAnnotation(new MaterialLabel("Depth: " + timeseriesReqeust.getZlo() + " : " + timeseriesReqeust.getZhi()));
            }
            layout.panel1.addAnnotation(new MaterialLabel("Latitude: " + timeseriesReqeust.getYlo()+" : " + timeseriesReqeust.getYlo() ));
            layout.panel1.addAnnotation(new MaterialLabel("Longitue: " + timeseriesReqeust.getXlo()+" : " + timeseriesReqeust.getXhi() ));
        }

    }
    private LASRequest makeRequest(int panel) {
        LASRequest lasRequest = new LASRequest();

        lasRequest.setTargetPanel(panel);

        lasRequest.setOperation(products.getSelected());

        lasRequest.setXlo(String.valueOf(refMap.getXlo()));
        lasRequest.setXhi(String.valueOf(refMap.getXhi()));

        lasRequest.setYlo(String.valueOf(refMap.getYlo()));
        lasRequest.setYhi(String.valueOf(refMap.getYhi()));

        // TODO ask ferret to accept ISO Strings
        if ( products.getSelectedProduct().isClientPlot() ) {
            lasRequest.setTlo(dateTimeWidget.getISODateLo());
            if (dateTimeWidget.isRange()) {
                lasRequest.setThi(dateTimeWidget.getISODateHi());
            } else {
                lasRequest.setThi(dateTimeWidget.getISODateLo());
            }
        } else {
            lasRequest.setTlo(dateTimeWidget.getFerretDateLo());
            if (dateTimeWidget.isRange()) {
                lasRequest.setThi(dateTimeWidget.getFerretDateHi());
            } else {
                lasRequest.setThi(dateTimeWidget.getFerretDateLo());
            }
        }

        if ( variable.getVerticalAxis() != null ) {
            lasRequest.setZlo(zAxisWidget.getLo());
            if (zAxisWidget.isRange()) {
                lasRequest.setZhi(zAxisWidget.getHi());
            } else {
                lasRequest.setZhi(zAxisWidget.getLo());
            }
        }

        List<RequestProperty> properties = layout.getPlotOptions();
        lasRequest.setRequestProperties(properties);

        List<String> dhashes = new ArrayList<String>();
        String dhash1 = layout.getDataset(panel).getHash();
        if ( dhash1 != null ) {
            dhashes.add(dhash1);
        }
        // TODO get second data set

        List<String> vhashes = new ArrayList<String>();
        String vhash1 = layout.getVariableHash(panel);
        if ( vhash1 != null ) {
            vhashes.add(vhash1);
        }

        lasRequest.setDatasetHashes(dhashes);
        lasRequest.setVariableHashes(vhashes);
        return lasRequest;
    }
    private void queueRequest(int panel) {

        LASRequest lasRequest = makeRequest(panel);

        state.getPanelState(panel).setLasRequest(lasRequest);
        requestQueue.add(lasRequest);
    }
    private void processQueue() {

        if ( !requestQueue.isEmpty() ) {
            LASRequest lasRequest = requestQueue.remove();
            state.getPanelState(lasRequest.getTargetPanel()).setLasRequest(lasRequest);
            productService.getProduct(lasRequest, productRequestCallback);
        }
    }
    private void setProduct(Product p) {

        String view = "xy";

        if ( p != null ) {
            view = p.getView();
        }


        refMap.setTool(view);

        if ( view.contains("t") || p.getData_view().contains("t") ) {
            dateTimeWidget.setRange(true);
        }

        if ( variable.getVerticalAxis() != null ) {
            zAxisWidget.init(variable.getVerticalAxis());
            layout.showVertialAxis();
            if ( view.contains("z") ) {
                zAxisWidget.setRange(true);
            } else {
                zAxisWidget.setRange(false);
            }
        } else {
            layout.hideVerticalAxis();
        }

        List<Operation> operations = p.getOperations();
        layout.clearOptions();
        for (int i = 0; i < operations.size(); i++) {
            Operation o = operations.get(i);
            layout.addOptions(new MenuOptionsWidget(o.getMenuOptions()));
            o.getTextOptions();
            o.getYesNoOptions();
        }
    }

}
