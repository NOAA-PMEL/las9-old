package pmel.sdig.las.client.main;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
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
import pmel.sdig.las.client.event.AddVariable;
import pmel.sdig.las.client.event.AddVariableHandler;
import pmel.sdig.las.client.event.BreadcrumbSelect;
import pmel.sdig.las.client.event.DateChange;
import pmel.sdig.las.client.event.FeatureModifiedEvent;
import pmel.sdig.las.client.event.MapChangeEvent;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.event.PanelControlOpen;
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
import pmel.sdig.las.client.widget.ComparePanel;
import pmel.sdig.las.client.widget.DateTimeWidget;
import pmel.sdig.las.client.widget.MenuOptionsWidget;
import pmel.sdig.las.client.widget.ProductButton;
import pmel.sdig.las.client.widget.ProductButtonList;
import pmel.sdig.las.client.widget.TextOptionsWidget;
import pmel.sdig.las.client.widget.YesNoOptionsWidget;
import pmel.sdig.las.shared.autobean.Analysis;
import pmel.sdig.las.shared.autobean.AnalysisAxis;
import pmel.sdig.las.shared.autobean.ConfigSet;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.LASRequest;
import pmel.sdig.las.shared.autobean.Operation;
import pmel.sdig.las.shared.autobean.Product;
import pmel.sdig.las.shared.autobean.ProductResults;
import pmel.sdig.las.shared.autobean.Region;
import pmel.sdig.las.shared.autobean.RequestProperty;
import pmel.sdig.las.shared.autobean.Site;
import pmel.sdig.las.shared.autobean.TimeAxis;
import pmel.sdig.las.shared.autobean.Variable;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
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

    Resource productsByIntervalResource = new Resource(Constants.productsByInterval);
    ProductsByIntervalService pbis = GWT.create(ProductsByIntervalService.class);


    ProductButtonList products = new ProductButtonList();

    State state = new State();

    Layout layout = new Layout();

    OLMapWidget refMap;
    DateTimeWidget dateTimeWidget = new DateTimeWidget();

    AxisWidget zAxisWidget = new AxisWidget();

    // These are the variables that contain the current state
    double xlo;
    double xhi;
    double ylo;
    double yhi;
    String zlo;
    String zhi;
    String tlo;
    String thi;
    Dataset dataset = null;
    Variable newVariable;
    List<Variable> variables = new ArrayList<>();
    // The ConfigSet is all the configurations of products for all the geometry and axes combinations in the data set
    ConfigSet configSet;


    String tile_server;
    String tile_layer;

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
        MaterialToast.fireToast("Getting initial site information.");
        siteService.getSite("1.json", siteCallback);

        // Get ready to call for the config when a variable is selected
        ((RestServiceProxy)configService).setResource(configResource);

        ((RestServiceProxy)productService).setResource(productResource);

        ((RestServiceProxy)datatableService).setResource(datatableResource);

        ((RestServiceProxy)datasetService).setResource(datasetResource);

        // Server-side computation of which products apply to a set of intervals,
        // call after analysis on or off, rather than compute it in the client
        ((RestServiceProxy)pbis).setResource(productsByIntervalResource);


        eventBus.addHandler(PlotOptionChange.TYPE, new PlotOptionChangeHandler() {
            @Override
            public void onPlotOptionChange(PlotOptionChange event) {
                layout.setUpdate(Color.RED);
            }
        });
        eventBus.addHandler(AddVariable.TYPE, new AddVariableHandler() {
            @Override
            public void onAddVariable(AddVariable event) {
                layout.setUpdate(Color.RED);
                if ( event.isAdd() ) {
                    Variable v = event.getVariable();
                    variables.add(v);
                    if ( variables.size() > 1 ) {
                        if ( !layout.plotsButton.getTitle().contains("1") ) {
                            layout.plotsButton.setText("Plot 1 Only with Mul");
                            layout.setPanels("Plot 1");
                            eventBus.fireEventFromSource(new PanelCount(1), layout.plotsDropdown);
                        }
                        layout.plotsDropdown.setEnabled(false);
                        layout.plotsButton.setEnabled(false);

                    }
                } else {
                    Variable v = event.getVariable();
                    int removei = -1;
                    for (int i = 0; i < variables.size(); i++) {
                        Variable iv = variables.get(i);
                        if ( iv.getName().equals(v.getName()) && iv.getTitle().equals(v.getTitle())) {
                            removei = i;
                        }
                    }
                    if ( removei >= 0 ) {
                        variables.remove(removei);
                    }
                    if ( variables.size() == 1 ) {
                        layout.plotsDropdown.setEnabled(true);
                        layout.plotsButton.setEnabled(true);
                    }
                }
            }
        });
        eventBus.addHandler(AnalysisActive.TYPE, new AnalysisActiveHandler() {
            @Override
            public void onAnalysisActive(AnalysisActive event) {
                if ( event.isActive() ) {
                    String type = event.getType();
                    String over = event.getOver();
                    if ( !type.equals("Compute") && !over.equals("Over")) {
                        // Analysis is active, take first variable.
                        String intervals = variables.get(0).getIntervals();
                        if ( over.equals("Area") ) {
                            refMap.setTool("xy");
                            intervals.replace("xy", "");
                        } else if ( over.equals("Longitude") ) {
                            refMap.setTool("x");
                            intervals.replace("x", "");
                        } else if ( over.equals("Latitude") ) {
                            refMap.setTool("y");
                            intervals.replace("y", "");
                        } else if ( over.equals("Time") ) {
                            dateTimeWidget.setRange(true);
                            intervals.replace("t","");
                        }
                        pbis.getProductsByInterval(variables.get(0).getGeometry(), intervals, pbisCallback);
                    }
                } else {
                    // set operations and axes back to full set.
                }
            }
        });
        eventBus.addHandler(ProductSelected.TYPE, new ProductSelectedHandler() {
            @Override
            public void onProductSelected(ProductSelected event) {
                ProductButton pb = (ProductButton) event.getSource();
                Product p = pb.getProduct();

                if ( p.getMaxArgs() > 1 ) {
                    layout.toDatasetChecks();
                } else {
                    layout.plotsDropdown.setEnabled(true);
                    layout.plotsButton.setEnabled(true);
                    // Potentially going from a multi-variable situation to a single variable
                    // so clear it and re-add the selected variable.
                    variables.clear();
                    layout.toDatasetRadios();
                    newVariable = layout.getSelectedVariable();
                    if ( newVariable != null ) {
                        variables.add(newVariable);
                        layout.addBreadcrumb(new Breadcrumb(newVariable, false), 1);
                    }
                }

                if ( state.getPanelCount() == 2 ) {
                    layout.panel2.initializeAxes(p.getView(), newVariable);
                    layout.panel2.setMapSelection(refMap.getYlo(), refMap.getYhi(), refMap.getXlo(), refMap.getXhi());
                    if ( variables.get(0).getVerticalAxis() != null ) {
                        layout.panel2.setZlo(zAxisWidget.getLo());
                        layout.panel2.setZhi(zAxisWidget.getHi());
                    }
                    layout.panel2.setFerretDateLo(dateTimeWidget.getFerretDateLo());
                    layout.panel2.setFerretDateHi(dateTimeWidget.getFerretDateHi());
                }
                setProduct(pb.getProduct());
                layout.setUpdate(Color.RED);
            }
        });
        eventBus.addHandler(PanelCount.TYPE, new PanelCount.Handler() {
            @Override
            public void onPanelCountChange(PanelCount event) {
                int count = event.getCount();
                state.setPanelCount(count);
                layout.panel1.scale();
                if ( count == 2 ) {

                    // This action will set the variable and initialize the axes.
                    layout.panel2.setVariable(variables.get(0));
                    // Initialize the Axes and Hide axes that are in the view
                    layout.panel2.initializeAxes(products.getSelectedProduct().getView(), variables.get(0));

                    // set them to match the left nav
                    layout.panel2.setMapSelection(refMap.getYlo(), refMap.getYhi(), refMap.getXlo(), refMap.getXhi());

                    // Use the first
                    if ( variables.get(0).getVerticalAxis() != null ) {
                        layout.panel2.setZlo(zAxisWidget.getLo());
                        layout.panel2.setZhi(zAxisWidget.getHi());
                    }
                    layout.panel2.setFerretDateLo(dateTimeWidget.getFerretDateLo());
                    layout.panel2.setFerretDateHi(dateTimeWidget.getFerretDateHi());

                    if ( variables.get(0).getGeometry().equals(Constants.GRID) ) {
                        // TODO all panels
                        layout.panel2.enableDifference(true);
                    }

                    queueRequest(2);
                    processQueue();
                }
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
                        newVariable = variable;
                        applyConfig();
                        // configService.getConfig(variable.getId(), configCallback);
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
                int index = event.getTargetPanel();

                final Breadcrumb bc = new Breadcrumb(selected, layout.getBreadcrumbCount(index) > 0);
                bc.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        eventBus.fireEventFromSource(new BreadcrumbSelect(selected, index), bc);
                        // Breadcrumbs are in the collapsible header, so stop to prevent the crumb from opening the panel.
                        event.stopPropagation();
                    }
                });


                if ( index == 1 ) {

                    if (selected instanceof Dataset) {
                        Dataset dataset = (Dataset) selected;
                        layout.clearDatasets();
                        layout.showDataProgress();
                        MaterialToast.fireToast("Getting information for " + dataset.getTitle());
                        datasetService.getDataset(dataset.getId() + ".json", datasetCallback);
                    } else if (selected instanceof Variable) {

                        newVariable = (Variable) event.getSelected();
                        // save the current widget state

                        if ( variables.size() > 0 ) {
                            xlo = refMap.getXlo();
                            xhi = refMap.getXhi();
                            ylo = refMap.getYlo();
                            yhi = refMap.getYhi();
                            if (variables.get(0).getVerticalAxis() != null) {
                                zlo = zAxisWidget.getLo();
                                zhi = zAxisWidget.getHi();
                            } else {
                                zlo = null;
                                zhi = null;
                            }
                            if (variables.get(0).getTimeAxis() != null) {
                                tlo = dateTimeWidget.getISODateLo();
                                thi = dateTimeWidget.getISODateHi();
                            } else {
                                tlo = null;
                                thi = null;
                            }
                        }

                        applyConfig();
                        //configService.getConfig(newVariable.getId(), configCallback);
                    }
                    layout.addBreadcrumb(bc, 1);

                } else if ( index == 2 ) {
                    layout.panel2.addBreadcrumb(bc);
                    if (selected instanceof Dataset) {
                        Dataset dataset = (Dataset) selected;
                        datasetService.getDataset(dataset.getId() + ".json", layout.panel2.datasetCallback);
                    } else if (selected instanceof Variable) {
                        Variable variable = (Variable) event.getSelected();
                        layout.panel2.switchVariables(variable);
                        layout.setUpdate(Color.RED);
                    }
                }
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
        eventBus.addHandler(PanelControlOpen.TYPE, new PanelControlOpen.Handler() {
            @Override
            public void onPanelControlOpen(PanelControlOpen event) {
                ComparePanel panel = (ComparePanel) event.getSource();
                if ( event.getTargetPanel() == 2 ) {
                    siteService.getSite("1.json", panel.siteCallback);
                }
            }
        });
        eventBus.addHandler(ClickEvent.getType(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {

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

        layout.panel1.getOutputPanel().setTitle(Constants.PANEL01);
        layout.panel1.setIndex(1);
        layout.panel2.getOutputPanel().setTitle(Constants.PANEL02);
        layout.panel2.setIndex(2);


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


    public interface ProductsByIntervalService extends RestService {
        @GET
        public void getProductsByInterval(@QueryParam("grid") String grid, @QueryParam("intervals") String intervals, MethodCallback<List<Product>> productsByIntervalCallback);
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
        public void getConfig(@PathParam("id") long id, MethodCallback<ConfigSet> configCallback);
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
                    .setChartTitleText(variables.get(0).getTitle())
                    .setMarginRight(10)
                    .setZoomType(BaseChart.ZoomType.X);

            chart.getXAxis().setType(Axis.Type.DATE_TIME);
            Series series = chart.createSeries().setName(variables.get(0).getUnits());
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
    RequestCallback makeGoogleChartFromDataTable = new RequestCallback() {

        @Override
        public void onResponseReceived(Request request, Response response) {
            layout.hideProgress();
            String jsonText = response.getText();
            DataTable dataTable = new DataTable(jsonText);

            AnnotationChartOptions options = new AnnotationChartOptions();
            options.setDisplayAnnotations(true);
            layout.panel1.getChart().setWidth((Window.getClientWidth() - navWidth)+"px");
            layout.panel1.getChart().setHeight((Window.getClientHeight() - navWidth)+"px");
            layout.panel1.getChart().setMargin(0.0d);

            AnnotationChart annChart = new AnnotationChart(layout.panel1.getChart().getElement());
            annChart.draw(dataTable, options);

        }

        @Override
        public void onError(Request request, Throwable exception) {

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
    MethodCallback<ProductResults> productRequestCallback = new MethodCallback<ProductResults>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            layout.setUpdate(Color.WHITE);
            layout.hideProgress();
            Window.alert("Request failed: "+exception.getMessage());
            processQueue();
        }

        @Override
        public void onSuccess(Method method, ProductResults results) {
            layout.setUpdate(Color.WHITE);
            layout.hideProgress();
            int tp = results.getTargetPanel();
            state.getPanelState(tp).setProductResults(results);
            layout.setState(tp, state);
            if ( tp == 2 ) {
                layout.panel2.scale();
            }
            processQueue();
        }
    };
    /**
     * Used when setting an analysis option cause the view to change so the list of operations changes.
     */
    MethodCallback<List<Product>> pbisCallback = new MethodCallback<List<Product>>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            Window.alert("Unable to get products for this analysis configuration.");
        }

        @Override
        public void onSuccess(Method method, List<Product> productsList) {
            // Duplicate from configCallback. Method??
            products.init(productsList);
            Product p = products.getSelectedProduct();
            setProduct(p);
            layout.setProducts(products);
        }
    };
    MethodCallback<Dataset> datasetCallback = new MethodCallback<Dataset>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            layout.hideDataProgress();
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, Dataset returnedDataset) {

            layout.clearDatasets();
            dataset = returnedDataset;
            if ( dataset.getDatasets().size() > 0 ) {
                layout.hideDataProgress();
                List<Dataset> datasets = dataset.getDatasets();
                Collections.sort(datasets);
                for (int i = 0; i < datasets.size(); i++) {
                    layout.addSelection(datasets.get(i));
                }
            }
            if ( dataset.getVariables().size() > 0 ) {
                configService.getConfig(dataset.getId(), configCallback);
                List<Variable> variables = dataset.getVariables();
                Collections.sort(variables);
                for (int i = 0; i < variables.size(); i++) {
                    layout.addSelection(variables.get(i));
                }
                MaterialToast.fireToast("Getting products for these variables.");
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
    MethodCallback<ConfigSet> configCallback = new MethodCallback<ConfigSet>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, ConfigSet config) {
            layout.hideDataProgress();
            configSet = config;
            // Apply the config ... after the variable is selected...
            // applyConfig();

        }
    };
    private void applyConfig() {

        TimeAxis tAxis = newVariable.getTimeAxis();

        dateTimeWidget.init(tAxis, false);
        if ( newVariable.getVerticalAxis() != null ) {
            zAxisWidget.init(newVariable.getVerticalAxis());
        }

        refMap.setDataExtent(newVariable.getGeoAxisY().getMin(), newVariable.getGeoAxisY().getMax(), newVariable.getGeoAxisX().getMin(), newVariable.getGeoAxisX().getMax(), newVariable.getGeoAxisX().getDelta());
        List<Product> productsList = configSet.getConfig().get(newVariable.getGeometry()+"_"+newVariable.getIntervals()).getProducts();
        List<Region> regions = configSet.getConfig().get(newVariable.getGeometry()+"_"+newVariable.getIntervals()).getRegions();

        refMap.setRegions(regions);
//
        products.init(productsList);
        Product p = products.getSelectedProduct();
        // If the map has the same shape try the old settings. Otherwize it will deafult for the variable.
        boolean useCurrentMapSettings = p.getView().equals(refMap.getTool());
        setProduct(p);

        layout.setProducts(products);

        String display_hi = tAxis.getDisplay_hi();
        String display_lo = tAxis.getDisplay_lo();

        if ( display_hi != null ) {
            dateTimeWidget.setHi(display_hi);
        }
        if ( display_lo != null ) {
            dateTimeWidget.setLo(display_lo);
        }

        // State has been set to work with new variable,
        // now restore previous settings if possible if a previous variable exists

        if ( variables.size() > 0 ) {
            if ( useCurrentMapSettings ) {
                refMap.setCurrentSelection(ylo, yhi, xlo, xhi);
            }
            dateTimeWidget.setLo(tlo);
            dateTimeWidget.setHi(thi);
            if (variables.get(0).getVerticalAxis() != null) {
                zAxisWidget.setLo(zlo);
                zAxisWidget.setHi(zhi);
            }
        }

        variables.clear();
        variables.add(newVariable);



        if ( p != null ) {

            update(p);

        }
    }
    private void update(Product p) {
        layout.showProgress();
        if ( p.isClientPlot() ) {

            if ( variables.get(0).getGeometry().equals(Constants.TIMESERIES) ) {

                layout.panel1.getOutputPanel().setVisible(false);
                layout.panel1.getChart().setVisible(true);
                makeDataRequest(variables, p);

            }


        } else {
            for (int i = 1; i <= state.getPanelCount(); i++) {
                queueRequest(i);
                if ( i == 1 ) {
                    layout.panel1.getChart().setVisible(false);
                    layout.panel1.getOutputPanel().setVisible(true);
                    layout.panel1.clearAnnotations();
                } else if ( i == 2 ) {
                    layout.panel2.getChart().setVisible(false);
                    layout.panel2.getOutputPanel().setVisible(true);
                    layout.panel2.clearAnnotations();
                }
            }
            processQueue();
        }
    }
    private void makeDataRequest(List<Variable> variables1, Product p) {
        LASRequest timeseriesReqeust = makeRequest(1);
        if ( p.getTitle().equals("Timeseries") ) {
//            erddapDataService.erddapTimeseries(timeseriesReqeust, makeChartFromJSON);

            // TODO hack for using two different chart clients
        } else if ( p.getTitle().equals("Timeseries Plot") ) {
            // Always get the data table from LAS to avoid same origin problem
            // TODO add smarts to controller to stream diret any .dataTable ERDDAPs
            datatableService.datatable(timeseriesReqeust, makeGoogleChartFromJSON);

            layout.panel1.clearAnnotations();
            layout.panel1.addAnnotation(new MaterialLabel("Dataset: " +dataset.getTitle()));
            layout.panel1.addAnnotation(new MaterialLabel("Variable: " +variables1.get(0).getTitle()));
            layout.panel1.addAnnotation(new MaterialLabel("Time: " + timeseriesReqeust.getAxesSet1().getTlo()+" : " + timeseriesReqeust.getAxesSet1().getThi() ));
            if ( variables.get(0).getVerticalAxis() != null ) {
                layout.panel1.addAnnotation(new MaterialLabel("Depth: " + timeseriesReqeust.getAxesSet1().getZlo() + " : " + timeseriesReqeust.getAxesSet1().getZhi()));
            }
            layout.panel1.addAnnotation(new MaterialLabel("Latitude: " + timeseriesReqeust.getAxesSet1().getYlo()+" : " + timeseriesReqeust.getAxesSet1().getYlo() ));
            layout.panel1.addAnnotation(new MaterialLabel("Longitue: " + timeseriesReqeust.getAxesSet1().getXlo()+" : " + timeseriesReqeust.getAxesSet1().getXhi() ));
        }

    }
    private LASRequest makeRequest(int panel) {
        LASRequest lasRequest = new LASRequest();

        lasRequest.setTargetPanel(panel);

        lasRequest.setOperation(products.getSelected());
        Analysis analysis = null;
        // Only apply analysis from the layou to panel 1
        if ( panel == 1 ) {
            analysis = layout.getAnalysis();
            if (analysis != null) {
                List<AnalysisAxis> axes = analysis.getAnalysisAxes();
                for (int i = 0; i < axes.size(); i++) {
                    AnalysisAxis a = axes.get(i);
                    if (a.getType().equals("x")) {
                        a.setLo(refMap.getXloFormatted());
                        a.setHi(refMap.getXhiFormatted());
                    } else if (a.getType().equals("y")) {
                        a.setLo(refMap.getYloFormatted());
                        a.setHi(refMap.getYhiFormatted());
                    } else if (a.getType().equals("z")) {
                        a.setLo(zAxisWidget.getLo());
                        a.setHi(zAxisWidget.getHi());
                    } else if (a.getType().equals("t")) {
                        a.setLo(dateTimeWidget.getFerretDateLo());
                        a.setHi(dateTimeWidget.getFerretDateHi());
                    }
                }
            }
            // This is a list to allow for the eventual implementation of custom analysis for each panel
            List<Analysis> transforms = new ArrayList<>();
            transforms.add(analysis);
            lasRequest.setAnalysis(transforms);
        }

        // If the plot is for the first panel, then all of the axes are set from the controls on the left nav

        if (!products.getSelectedProduct().isClientPlot()) {
            if (analysis == null || (analysis != null && !analysis.getAxes().contains("x"))) {
                lasRequest.getAxesSet1().setXlo(String.valueOf(refMap.getXlo()));
                lasRequest.getAxesSet1().setXhi(String.valueOf(refMap.getXhi()));
            }

            if (analysis == null || (analysis != null && !analysis.getAxes().contains("y"))) {
                lasRequest.getAxesSet1().setYlo(String.valueOf(refMap.getYlo()));
                lasRequest.getAxesSet1().setYhi(String.valueOf(refMap.getYhi()));
            }
        } else {
            // If it's a timeseries plot, fudge the xy to the bounding box LAS has for the location???
            if ( products.getSelectedProduct().getName().toLowerCase().contains("timeseries") ) {
                    lasRequest.getAxesSet1().setXlo(String.valueOf(refMap.getDataExtent()[2]));
                    lasRequest.getAxesSet1().setXhi(String.valueOf(refMap.getDataExtent()[3]));

                    lasRequest.getAxesSet1().setYlo(String.valueOf(refMap.getDataExtent()[0]));
                    lasRequest.getAxesSet1().setYhi(String.valueOf(refMap.getDataExtent()[1]));

            }
        }

        // TODO check analysis axes first

        // TODO ask ferret to accept ISO Strings
        if (products.getSelectedProduct().isClientPlot()) {
            lasRequest.getAxesSet1().setTlo(dateTimeWidget.getISODateLo());
            if (dateTimeWidget.isRange()) {
                lasRequest.getAxesSet1().setThi(dateTimeWidget.getISODateHi());
            } else {
                lasRequest.getAxesSet1().setThi(dateTimeWidget.getISODateLo());
            }
        } else {
            if ( analysis == null || ( analysis != null && !analysis.getAxes().contains("t") ) ) {
                lasRequest.getAxesSet1().setTlo(dateTimeWidget.getFerretDateLo());
                if (dateTimeWidget.isRange()) {
                    lasRequest.getAxesSet1().setThi(dateTimeWidget.getFerretDateHi());
                } else {
                    lasRequest.getAxesSet1().setThi(dateTimeWidget.getFerretDateLo());
                }
            }
        }

        if (variables.get(0).getVerticalAxis() != null) {
            if ( analysis == null || ( analysis != null && !analysis.getAxes().contains("z") ) ) {
                lasRequest.getAxesSet1().setZlo(zAxisWidget.getLo());
                if (zAxisWidget.isRange()) {
                    lasRequest.getAxesSet1().setZhi(zAxisWidget.getHi());
                } else {
                    lasRequest.getAxesSet1().setZhi(zAxisWidget.getLo());
                }
            }
        }

        // Replace any axes setting orthogonal to the view for any panel other than 1

        String view = products.getSelectedProduct().getView();

        if ( panel != 1 ) {

            ComparePanel comparePanel = null;
            if (panel == 2) {
                comparePanel = layout.panel2;
            }
            // The map is always has the right hi and lo no matter the view

            // If it's a difference, the nav values stay in AxesSet1 and the
            // ortho values go in AxesSet2
            if (!view.contains("x") && !comparePanel.isDifference()) {
                lasRequest.getAxesSet1().setXlo(String.valueOf(comparePanel.getXlo()));
                lasRequest.getAxesSet1().setXhi(String.valueOf(comparePanel.getXhi()));
            } else if (!view.contains("x") && comparePanel.isDifference()) {
                lasRequest.getAxesSet2().setXlo(String.valueOf(comparePanel.getXlo()));
                lasRequest.getAxesSet2().setXhi(String.valueOf(comparePanel.getXhi()));
            }
            if (!view.contains("y") && !comparePanel.isDifference()) {
                lasRequest.getAxesSet1().setYlo(String.valueOf(comparePanel.getYlo()));
                lasRequest.getAxesSet1().setYhi(String.valueOf(comparePanel.getYhi()));
            } else if (!view.contains("y") && comparePanel.isDifference()) {
                lasRequest.getAxesSet2().setYlo(String.valueOf(comparePanel.getYlo()));
                lasRequest.getAxesSet2().setYhi(String.valueOf(comparePanel.getYhi()));
            }
            if (!view.contains("z") && !comparePanel.isDifference()) {
                if (comparePanel.getVariable().getVerticalAxis() != null) {
                    lasRequest.getAxesSet1().setZlo(comparePanel.getZlo());
                    if (comparePanel.isZRange()) {
                        lasRequest.getAxesSet1().setZhi(comparePanel.getZhi());
                    } else {
                        lasRequest.getAxesSet1().setZhi(comparePanel.getZlo());
                    }
                }
            } else if (!view.contains("z") && comparePanel.isDifference()) {
                if (comparePanel.getVariable().getVerticalAxis() != null) {
                    lasRequest.getAxesSet2().setZlo(comparePanel.getZlo());
                    if (comparePanel.isZRange()) {
                        lasRequest.getAxesSet2().setZhi(comparePanel.getZhi());
                    } else {
                        lasRequest.getAxesSet2().setZhi(comparePanel.getZlo());
                    }
                }
            }
            if (!view.contains("t") && !comparePanel.isDifference()) {
                lasRequest.getAxesSet1().setTlo(comparePanel.getFerretDateLo());
                if (comparePanel.dateTimeWidget.isRange()) {
                    lasRequest.getAxesSet1().setThi(comparePanel.getFerretDateHi());
                } else {
                    lasRequest.getAxesSet1().setThi(comparePanel.getFerretDateLo());
                }
            } else if (!view.contains("t") && comparePanel.isDifference()) {
                lasRequest.getAxesSet2().setTlo(comparePanel.getFerretDateLo());
                if (comparePanel.dateTimeWidget.isRange()) {
                    lasRequest.getAxesSet2().setThi(comparePanel.getFerretDateHi());
                } else {
                    lasRequest.getAxesSet2().setThi(comparePanel.getFerretDateLo());
                }
            }
        }


        List<RequestProperty> properties = layout.getPlotOptions();
        lasRequest.setRequestProperties(properties);

        setHashes(panel, lasRequest);
        if ( layout.isDifference(panel) ) {
            // TODO thought these required a different script, but apparently not...
            if ( view.equals("xy") ) {
                lasRequest.setOperation("Compare_Plot");
            } else if ( view.equals("t") ) {
                lasRequest.setOperation("Compare_Plot_T");
            } else if ( view.equals("x") ) {
                lasRequest.setOperation("Compare_Plot_X");
            } else if ( view.equals("y") ) {
                lasRequest.setOperation("Compare_Plot_Y");
            }
        }


        return lasRequest;
    }
    private void setHashes(int panel, LASRequest lasRequest) {
        List<String> dhashes = new ArrayList<String>();
        List<String> vhashes = new ArrayList<String>();
        if (panel == 1) {
            String dhash1 = dataset.getHash();
            // For now it's all one data set, but we want to repeat the data set hash
            for (int i = 0; i < variables.size(); i++) {
                // repeat the data set hash...
                if (dhash1 != null) {
                    dhashes.add(dhash1);
                }
                // Add the variable hash
                Variable variable = variables.get(i);
                String vhash1 = variable.getHash();
                if (vhash1 != null) {
                    vhashes.add(vhash1);
                }
            }
        } else if (panel == 2 ){
            String dhash1 = dataset.getHash();
            if (dhash1 != null) {
                dhashes.add(dhash1);
            }
            for (int i = 0; i < variables.size(); i++) {
                Variable variable = variables.get(i);
                String vhash1 = variable.getHash();
                if (vhash1 != null) {
                    vhashes.add(vhash1);
                }
            }
            if ( layout.isDifference(panel) ) {

                dhashes.add(layout.panel2.getDataset().getHash());
                vhashes.add(layout.panel2.getVariable().getHash());
                // Extra mumbo jumbo to have list becuase later custom analysis for panels.
            }
        }
        if ( variables.size() > 1 ) {
            List<RequestProperty> p = lasRequest.getRequestProperties();
            RequestProperty rp = new RequestProperty();
            rp.setType("ferret");
            rp.setName("data_count");
            rp.setValue(String.valueOf(variables.size()));
            if ( p == null ) {
                p = new ArrayList<>();
            }
            p.add(rp);
            lasRequest.setRequestProperties(p);
        }
        lasRequest.setDatasetHashes(dhashes);
        lasRequest.setVariableHashes(vhashes);
    }
    private void queueRequest(int panel) {
        LASRequest lasRequest = makeRequest(panel);
        state.getPanelState(panel).setLasRequest(lasRequest);
        requestQueue.add(lasRequest);
    }

    private void processQueue() {
        if ( !requestQueue.isEmpty() ) {
            layout.showProgress();
            LASRequest lasRequest = requestQueue.remove();
            state.getPanelState(lasRequest.getTargetPanel()).setLasRequest(lasRequest);
            productService.getProduct(lasRequest, productRequestCallback);
            MaterialToast.fireToast("Requesting product...");
        }
    }
    // Use the incoming variable to select the product
    // since we want to delay setting the member variable
    // until after we have reset the axes to the state
    // for the previous variable.
    private void setProduct(Product p) {

        String view = "xy";

        if ( p != null ) {
            view = p.getView();
        }

        refMap.setTool(view);

        if ( newVariable.getTimeAxis() != null ) {
            layout.showDateTime();
            if ( view.contains("t") || p.getData_view().contains("t") ) {
                dateTimeWidget.setRange(true);
            } else {
                dateTimeWidget.setRange(false);
            }
        } else {
            layout.hideDateTime();
        }

        if ( newVariable.getVerticalAxis() != null ) {
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
            layout.addOptions(new TextOptionsWidget(o.getTextOptions()));
            layout.addOptions(new YesNoOptionsWidget(o.getYesNoOptions()));
        }
    }
}
