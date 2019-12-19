package pmel.sdig.las.client.main;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import google.visualization.AnnotationChart;
import google.visualization.AnnotationChartOptions;
import google.visualization.DataTable;
import google.visualization.LineChart;
import google.visualization.LineChartOptions;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialRadioButton;
import gwt.material.design.client.ui.MaterialRow;
import gwt.material.design.client.ui.MaterialToast;
import gwt.material.design.client.ui.html.Option;
import org.fusesource.restygwt.client.Attribute;
import org.fusesource.restygwt.client.JsonEncoderDecoder;
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
import pmel.sdig.las.client.event.AnimateAction;
import pmel.sdig.las.client.event.AnimateActionHandler;
import pmel.sdig.las.client.event.AutoColors;
import pmel.sdig.las.client.event.AutoColorsHandler;
import pmel.sdig.las.client.event.BreadcrumbSelect;
import pmel.sdig.las.client.event.DateChange;
import pmel.sdig.las.client.event.Download;
import pmel.sdig.las.client.event.DownloadHandler;
import pmel.sdig.las.client.event.FeatureModifiedEvent;
import pmel.sdig.las.client.event.Info;
import pmel.sdig.las.client.event.InfoHandler;
import pmel.sdig.las.client.event.MapChangeEvent;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.event.PanelControlOpen;
import pmel.sdig.las.client.event.PanelCount;
import pmel.sdig.las.client.event.PlotOptionChange;
import pmel.sdig.las.client.event.PlotOptionChangeHandler;
import pmel.sdig.las.client.event.ProductSelected;
import pmel.sdig.las.client.event.ProductSelectedHandler;
import pmel.sdig.las.client.event.Search;
import pmel.sdig.las.client.event.SearchHandler;
import pmel.sdig.las.client.event.ShowValues;
import pmel.sdig.las.client.event.ShowValuesHandler;
import pmel.sdig.las.client.map.MapSelectionChangeListener;
import pmel.sdig.las.client.map.OLMapWidget;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.client.util.Constants;
import pmel.sdig.las.client.util.Util;
import pmel.sdig.las.client.widget.AxisWidget;
import pmel.sdig.las.client.widget.Breadcrumb;
import pmel.sdig.las.client.widget.ComparePanel;
import pmel.sdig.las.client.widget.DatasetInfo;
import pmel.sdig.las.client.widget.DateTimeWidget;
import pmel.sdig.las.client.widget.MenuOptionsWidget;
import pmel.sdig.las.client.widget.ProductButton;
import pmel.sdig.las.client.widget.ProductButtonList;
import pmel.sdig.las.client.widget.TextOptionsWidget;
import pmel.sdig.las.client.widget.VariableConstraintWidget;
import pmel.sdig.las.client.widget.VariableInfo;
import pmel.sdig.las.client.widget.YesNoOptionsWidget;
import pmel.sdig.las.shared.autobean.Analysis;
import pmel.sdig.las.shared.autobean.AnalysisAxis;
import pmel.sdig.las.shared.autobean.Animation;
import pmel.sdig.las.shared.autobean.AxesSet;
import pmel.sdig.las.shared.autobean.ConfigSet;
import pmel.sdig.las.shared.autobean.DataConstraint;
import pmel.sdig.las.shared.autobean.DataQualifier;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.DatasetProperty;
import pmel.sdig.las.shared.autobean.LASRequest;
import pmel.sdig.las.shared.autobean.MapScale;
import pmel.sdig.las.shared.autobean.Operation;
import pmel.sdig.las.shared.autobean.Product;
import pmel.sdig.las.shared.autobean.Region;
import pmel.sdig.las.shared.autobean.RequestProperty;
import pmel.sdig.las.shared.autobean.Result;
import pmel.sdig.las.shared.autobean.ResultSet;
import pmel.sdig.las.shared.autobean.SearchRequest;
import pmel.sdig.las.shared.autobean.SearchResults;
import pmel.sdig.las.shared.autobean.Site;
import pmel.sdig.las.shared.autobean.TimeAxis;
import pmel.sdig.las.shared.autobean.Variable;
import pmel.sdig.las.shared.autobean.VariableProperty;
import pmel.sdig.las.shared.autobean.Vector;
import pmel.sdig.las.shared.autobean.VerticalAxis;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static pmel.sdig.las.client.util.Constants.GRID;
import static pmel.sdig.las.client.util.Constants.navWidth;

/**
 * Created by rhs on 9/8/15.
 */
public class UI implements EntryPoint {

    int loop = 0;

    boolean toast = false;

    NumberFormat dFormat = NumberFormat.getFormat("########.##");

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    Resource siteResource = new Resource(Constants.siteJson);
    SiteService siteService = GWT.create(SiteService.class);

    Resource datasetResource = new Resource(Constants.datasetJson);
    DatasetService datasetService = GWT.create(DatasetService.class);

    Resource infoResource = new Resource(Constants.thumbinfo);
    InfoService infoService = GWT.create(InfoService.class);

    Resource configResource = new Resource(Constants.configJson);
    ConfigService configService = GWT.create(ConfigService.class);

    Resource productResource = new Resource(Constants.productCreate);
    ProductService productService = GWT.create(ProductService.class);

    Resource datatableResource = new Resource(Constants.datatable);
    DatatableService datatableService = GWT.create(DatatableService.class);

    Resource erddapDataResource = new Resource(Constants.erddapDataRequest);
    ErddapDataService erddapDataService = GWT.create(ErddapDataService.class);

    Resource productsByIntervalResource = new Resource(Constants.productsByInterval);
    ProductsByIntervalService pbis = GWT.create(ProductsByIntervalService.class);

    Resource searchResource = new Resource(Constants.search);
    SearchService searchService = GWT.create(SearchService.class);

    Resource regionResource = new Resource(Constants.regions);
    RegionService regionService = GWT.create(RegionService.class);

    ProductButtonList products = new ProductButtonList();

    State state = new State();

    Layout layout = new Layout();

    OLMapWidget refMap;
    DateTimeWidget dateTimeWidget = new DateTimeWidget();
    AxisWidget zAxisWidget = new AxisWidget();

    DateTimeWidget animateDateTimeWidget = new DateTimeWidget();

    OLMapWidget downloadMap;
    DateTimeWidget downloadDateTime = new DateTimeWidget();
    AxisWidget downloadZaxisWidget = new AxisWidget();

    OLMapWidget correlationMap;
    DateTimeWidget correlationDateTime = new DateTimeWidget();
    AxisWidget correlationZaxisWidget = new AxisWidget();

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
    Vector newVector;
    List<Variable> variables = new ArrayList<>();
    // Since vector op is just a list of components, keep track of whether it's a vector or not.
    boolean isVector = false;
    // The ConfigSet is all the configurations of products for all the geometry and axes combinations in the data set
    ConfigSet configSet;

    // "Page" number of data sets with variables
    int offset = 0;

    String tile_server;
    String tile_layer;

    DateTimeFormatter iso;

    Queue<LASRequest> requestQueue = new LinkedList<LASRequest>();

    Animation animation;
    boolean animateCancel = false;

    public interface LASRequestCodec extends JsonEncoderDecoder<LASRequest> {}
    LASRequestCodec requestCodec = GWT.create(LASRequestCodec.class);

    LASRequest historyRequest = null;
    LASRequest historyRequestPanel2 = null;

    // Used for both history and for loading a variable from the description page.
    String historyVariable = null;

    // Display the info page of the first data set
    boolean info = true;

    // Incoming data set URL
    String xDataURL;

    public void onModuleLoad() {

        iso = ISODateTimeFormat.dateTimeNoMillis();

        String initialHistory = getAnchor();
        xDataURL = Util.getParameterString("data_url");

        animateDateTimeWidget.setTitle("Time Range for Animation");
        downloadDateTime.setTitle("Time Range for Data Download.");

        Element wmsserver = DOM.getElementById("wms-server");
        if ( wmsserver != null )
            tile_server = wmsserver.getPropertyString("content");
        Element wmslayer = DOM.getElementById("wms-layer");
        if ( wmslayer != null )
            tile_layer = wmslayer.getPropertyString("content");

        refMap = new OLMapWidget("128px", "256px", tile_server, tile_layer);
        downloadMap = new OLMapWidget("128px", "256px", tile_server, tile_layer);
        correlationMap = new OLMapWidget("128px", "256px", tile_server, tile_layer);
        layout.setMap(refMap);

        refMap.setMapListener(mapListener);
        layout.sideNav.addMouseDownHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent mouseDownEvent) {
                refMap.resizeMap();
            }
        });
        layout.setAnimateTimeWidget(animateDateTimeWidget);

        layout.downloadDateTimePanel.add(downloadDateTime);
        layout.downloadMapPanel.add(downloadMap);
        layout.downloadZaxisPanel.add(downloadZaxisWidget);

        layout.correlationDateTimePanel.add(correlationDateTime);
        layout.correlationMapPanel.add(correlationMap);
        layout.correlationZaxisPanel.add(correlationZaxisWidget);

        ((RestServiceProxy)siteService).setResource(siteResource);

        // Get ready to call for the config when a variable is selected
        ((RestServiceProxy)configService).setResource(configResource);

        ((RestServiceProxy)productService).setResource(productResource);

        ((RestServiceProxy)datatableService).setResource(datatableResource);

        ((RestServiceProxy)datasetService).setResource(datasetResource);

        ((RestServiceProxy)infoService).setResource(infoResource);

        ((RestServiceProxy)regionService).setResource(regionResource);

        // Server-side computation of which products apply to a set of intervals,
        // call after analysis on or off, rather than compute it in the client
        ((RestServiceProxy)pbis).setResource(productsByIntervalResource);

        ((RestServiceProxy) searchService).setResource(searchResource);

        ((RestServiceProxy) erddapDataService).setResource(erddapDataResource);

        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> event) {
                String historyToken = event.getValue();
                popHistory(historyToken);
            }
        });

        eventBus.addHandler(ChangeConstraint.TYPE, new ChangeConstraintHandler() {
            @Override
            public void onAddConstraint(ChangeConstraint event) {
                layout.setUpdate(Constants.UPDATE_NEEDED);
                if ( event.getAction().equals("add") ) {
                    DataConstraintLink dcl = new DataConstraintLink(event.getType(), event.getLhs(), event.getOp(), event.getRhs());
                    layout.addActiveConstraint(dcl);
                } else if ( event.getAction().equals("remove") ) {
                    DataConstraintLink link = (DataConstraintLink) event.getSource();
                    layout.removeActiveConstraint(link);
                }
            }
        });
        eventBus.addHandler(SubsetValues.TYPE, new SubsetValuesHandler() {
            @Override
            public void onSubsetValues(SubsetValues event) {
                Variable variable = event.getVariable();
                List<String> dhashes = new ArrayList<>();
                List<String> vhashes = new ArrayList<>();
                dhashes.add(dataset.getHash());
                vhashes.add(variable.getHash());
                LASRequest requestValues = new LASRequest();
                requestValues.setDatasetHashes(dhashes);
                requestValues.setVariableHashes(vhashes);
                DataQualifier distinct = new DataQualifier();
                distinct.setDistinct(true);
                requestValues.addDataQualifier(distinct);
                erddapDataService.getData(requestValues, constraintValuesCallback);
            }
        });
        eventBus.addHandler(Search.TYPE, new SearchHandler() {
            @Override
            public void onSearch(Search event) {
                Object source = event.getSource();
                SearchRequest sr = event.getSearchRequest();
                String query = sr.getQuery();
                List<DatasetProperty> dpl = sr.getDatasetProperties();
                List<VariableProperty> vpl = sr.getVariableProperties();
                // check all for null before toasting
                if ( query != null || dpl != null || vpl != null ) {
                    if ( source instanceof ComparePanel) {
                        searchService.getSearchResults(sr, layout.panel2.searchCallback);
                    } else {
                        searchService.getSearchResults(sr, searchCallback);
                    }
                } else {
                    layout.hideDataProgress();
                    MaterialToast.fireToast("Please specify at least one search term.");
                }
            }
        });
        eventBus.addHandler(PlotOptionChange.TYPE, new PlotOptionChangeHandler() {
            @Override
            public void onPlotOptionChange(PlotOptionChange event) {
                layout.setUpdate(Constants.UPDATE_NEEDED);
            }
        });
        eventBus.addHandler(AddVariable.TYPE, new AddVariableHandler() {
            @Override
            public void onAddVariable(AddVariable event) {

                layout.setUpdate(Constants.UPDATE_NEEDED);
                if ( event.isAdd() ) {
                    Variable v = event.getVariable();
                    variables.add(v);
                    if ( variables.size() > 1 ) {
                        if ( !layout.plotsDropdown.getValue().contains("1") ) {
                            layout.setPanels(1);
                            eventBus.fireEventFromSource(new PanelCount(1), layout.plotsDropdown);
                        }
                        layout.plotsDropdown.setEnabled(false);
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
                    turnOnAnalysis(type, over);
                } else {
                    turnOffAnalysis();
                }
            }
        });
        eventBus.addHandler(AnimateAction.TYPE, new AnimateActionHandler() {
            @Override
            public void onSetupAnimate(AnimateAction event) {
                if ( event.isCancel() ) {
                    animateCancel = true;
                    state.setAnimating(false);
                } else if (event.isSubmit() ) {
                    LASRequest lasRequest;
                    if ( isVector ) {
                        lasRequest = makeRequest(5, "Animation_2D_XY_vector");
                    } else {
                        lasRequest = makeRequest(5, "Animation_2D_XY");
                    }
                    state.getPanelState(5).setLasRequest(lasRequest);
                    state.getPanelState(5).setFrameIndex(0);
                    state.getPanelState(5).clearFrames();
                    state.setAnimating(true);
                    requestQueue.add(lasRequest);
                    layout.panel5.setImage("images/44frgm.gif");
                    processQueue();
                } else if ( event.isOpen() ) {
                    animateCancel = false;
                    state.getPanelState(5).setFrameIndex(0);
                    state.getPanelState(5).clearFrames();
                    animation = null;
                    layout.panel5.clearAnnotations();
                    layout.frameCount.setText("0 frames downloaded.");
                    layout.time_step.setText("1");

                    String dts = dataset.getProperty("ferret", "time_step");
                    if ( dts != null ) {
                        layout.time_step.setText(dts);
                    }

                    if ( isVector ) {
                        // TODO need a collection of properties
                        Vector v = layout.getSelectedVector();
                    } else {
                        // TODO look at attributes, variableattributes and variableproperties. Why all three?
                        Variable v = layout.getSelectedVariable();
                        String ts = v.getProperty("ferret", "time_step");
                        if (ts != null) {
                            layout.time_step.setText(ts);
                        }
                    }

                    layout.animateSubmit.setText("Submit");
                    layout.panel5.setImage("images/animation_arrow.png");
                    String l = "Animating " + dataset.getTitle() + " " + variables.get(0).getTitle();
                    layout.panel5.setLabel(l);
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
                    // Potentially going from a multi-variable situation to a single variable
                    // so clear it and re-add the selected variable.
                    variables.clear();
                    layout.toDatasetRadios(); // TODO this is not working
                    newVariable = layout.getSelectedVariable();
                    if ( newVariable != null ) {
                        variables.add(newVariable);
                        layout.addBreadcrumb(new Breadcrumb(newVariable, false), 1);
                    } else {
                        newVector = layout.getSelectedVector();
                        variables.add(newVector.getU());
                        variables.add(newVector.getV());
                        if ( newVector.getW() != null ) {
                            variables.add(newVector.getW());
                        }
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
                setUpForProduct(pb.getProduct());
                layout.setUpdate(Constants.UPDATE_NEEDED);
            }
        });
        eventBus.addHandler(PanelCount.TYPE, new PanelCount.Handler() {
            @Override
            public void onPanelCountChange(PanelCount event) {
                int count = event.getCount();
                state.setPanelCount(count);
                layout.panel1.scale();
                layout.setPanels(count);
                if ( count == 2 ) {
                    if ( historyRequestPanel2 == null ) {
                        setUpPanel(2, variables.get(0));
                    } else {
                        String op = historyRequestPanel2.getOperation();
                        String hash = historyRequestPanel2.getDatasetHashes().get(0);
                        if ( op.equals("Compare_Plot") ) {
                            layout.panel2.setDifference(true);
                            if ( historyRequestPanel2.getDatasetHashes().size() == 2 ) {
                                hash = historyRequestPanel2.getDatasetHashes().get(1);
                            }

                        }
                        datasetService.getDataset( hash + ".json", panel2DatasetCallback);
                    }

                }
            }
        });
        eventBus.addHandler(BreadcrumbSelect.TYPE, new BreadcrumbSelect.Handler() {
            @Override
            public void onBreadcrumbSelect(BreadcrumbSelect event) {
                int panel = event.getTargetPanel();
                Object selected = event.getSelected();
                if (panel == 1) {
                    if (selected != null) {
                        layout.removeBreadcrumbs(selected, 1);
                        if (selected instanceof Dataset) {
                            Dataset dataset = (Dataset) selected;
                            datasetService.getDataset(dataset.getId() + ".json", datasetCallback);
                        } else if (selected instanceof Variable) {
                            Variable variable = (Variable) event.getSelected();
                            newVariable = variable;
                            applyConfig();
                        }
                    } else {
                        // This was the home button...

                        info = false;
                        siteService.getSite("1.json", siteCallback);
                        layout.panel1.getOutputPanel().clear();
                        layout.panel1.clearAnnotations();
                        layout.removeBreadcrumbs(1);
                        layout.advancedSearch.setDisplay(Display.BLOCK);
                        layout.clearDatasets();
                        layout.showDataProgress();


                    }
                } else {
                    if ( selected != null ) {
                        if ( selected instanceof Dataset ) {
                            Dataset p2ds = (Dataset) selected;
                            layout.removeBreadcrumbs(selected, panel);
                            if ( panel == 2 ) {
                                layout.panel2.openDatasets();
                                datasetService.getDataset(p2ds.getId() + ".json", layout.panel2.datasetCallback);
                            }
                        }
                    }
                }
            }
        });
        eventBus.addHandler(NavSelect.TYPE, new NavSelect.Handler() {
            @Override
            public void onNavSelect(NavSelect event) {

                Object selected = event.getSelected();
                int index = event.getTargetPanel();
                Object source = event.getSource();
                if ( source instanceof VariableInfo ) {
                    VariableInfo vi = (VariableInfo) source;
                    dataset = vi.getDataset();
                    layout.clearDatasets();
                    layout.showProgress();
                    Variable infoVariable = (Variable) selected;
                    // TODO, we're only getting the variable and we need the data set. Should be able to have both in this instance.
                    final Breadcrumb bc = new Breadcrumb(dataset, layout.getBreadcrumbCount(index) > 0);
                    bc.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            eventBus.fireEventFromSource(new BreadcrumbSelect(dataset, index), bc);
                            // Breadcrumbs are in the collapsible header, so stop to prevent the crumb from opening the panel.
                            event.stopPropagation();
                        }
                    });
                    layout.addBreadcrumb(bc, 1);
                    final Breadcrumb vbc = new Breadcrumb(selected, layout.getBreadcrumbCount(index) > 0);
                    bc.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            eventBus.fireEventFromSource(new BreadcrumbSelect(selected, index), vbc);
                            // Breadcrumbs are in the collapsible header, so stop to prevent the crumb from opening the panel.
                            event.stopPropagation();
                        }
                    });
                    layout.addBreadcrumb(vbc, 1);
                    List<Variable> returnedVariables = dataset.getVariables();
                    Collections.sort(returnedVariables);
                    for (int i = 0; i < returnedVariables.size(); i++) {
                        layout.addSelection(returnedVariables.get(i));
                    }
                    List<Vector> returnedVectors = dataset.getVectors();
                    for (int i = 0; i < returnedVectors.size(); i++) {
                        Vector v = returnedVectors.get(i);
                        layout.addSelection(v);
                    }

                    historyVariable = infoVariable.getHash();
                    int selectedIndex = dataset.findVariableIndexByHash(infoVariable.getHash());

                    if (selectedIndex >= 0) {
                        layout.setSelectedVariable(selectedIndex);
                    }

                    configService.getConfig(dataset.getId(), configCallback);
                    return;
                }

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

                    layout.infoPanel.setDisplay(Display.NONE);
                    layout.infoHeader.setDisplay(Display.NONE);
                    layout.advancedSearch.setDisplay(Display.NONE);
                    layout.panel1.setVisible(true);
                    layout.panel1.clearAnnotations();
                    layout.panel1.clearPlot();
                    if (selected instanceof Dataset) {
                        Dataset dataset = (Dataset) selected;
                        layout.clearDatasets();
                        layout.showDataProgress();
                        if ( toast )
                            MaterialToast.fireToast("Getting information for " + dataset.getTitle());
                        datasetService.getDataset(dataset.getId() + ".json", datasetCallback);
                    } else if (selected instanceof Variable) {

                        newVector = null;
                        newVariable = (Variable) selected;
                        // save the current widget state

                        if ( variables.size() > 0 ) {
                            saveAxes();
                        }

                        applyConfig();

                    } else if ( selected instanceof Vector ) {

                        newVariable = null;
                        newVector = (Vector) selected;
                        if ( variables.size() > 0 ) {
                            saveAxes();
                        }
                        applyConfig();
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
                        layout.setUpdate(Constants.UPDATE_NEEDED);
                    }
                }
            }
        });
        eventBus.addHandler(DateChange.TYPE, new DateChange.Handler() {
            @Override
            public void onDateChange(DateChange event) {
                if (layout.animateWindow.isOpen()) {
                    if (animation != null) {
                        // Starting over...
                        animation = null;
                        eventBus.fireEventFromSource(new AnimationSpeed(0), animateDateTimeWidget);
                        state.getPanelState(5).clearFrames();
                        layout.animationControls.setDisplay(Display.NONE);
                        layout.frameCount.setText("0 frames downloaded.");
                        layout.animateSubmit.setText("Submit");
                        layout.animateHelp.setText(Constants.ANIMATE_SUBMIT);
                        layout.submitPanel.setDisplay(Display.BLOCK);
                    }
                } else {
                    layout.setUpdate(Constants.UPDATE_NEEDED);
                }
            }
        });

        eventBus.addHandler(MapChangeEvent.TYPE, new MapChangeEvent.Handler() {
            @Override
            public void onMapSelectionChange(MapChangeEvent event) {
                layout.setUpdate(Constants.UPDATE_NEEDED);
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
                layout.setUpdate(Constants.UPDATE_NEEDED);
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
        eventBus.addHandler(AnimationSpeed.TYPE, new AnimationSpeedHandler() {
            @Override
            public void onAnimationSpeed(AnimationSpeed event) {
                int speed = event.getSpeed();
                timer.cancel();
                if ( speed > 0 ) {
                    timer.scheduleRepeating(speed);
                } else {
                    // Stopping, take the loop counter back to be ready to move next or prev
                    loop--;
                }
            }
        });

        eventBus.addHandler(MoveAnimation.TYPE, new MoveAnimationHandler() {
            @Override
            public void onMoveAnimation(MoveAnimation event) {
                if ( event.getDirection() > 0 ) {
                    // The loop counter has already been advanced by 1
                    loop = loop + event.getDirection();
                    loop = loop%animation.getFrames().size();
                    nextStep();
                } else {
                    loop = loop + event.getDirection();
                    if ( loop < 0 ) {
                        loop = animation.getFrames().size() - 1;
                    } else {
                        loop = loop % animation.getFrames().size();
                    }
                    nextStep();
                }

            }
        });

        eventBus.addHandler(ShowValues.TYPE, new ShowValuesHandler() {
            @Override
            public void onShowValues(ShowValues event) {
                LASRequest lasRequest = makeRequest(6, "Data_Extract");
                requestQueue.add(lasRequest);
                processQueue();
            }
        });
        eventBus.addHandler(Download.TYPE, new DownloadHandler() {
            @Override
            public void onDownload(Download event) {
                Widget s = (Widget) event.getSource();
                if ( s instanceof MaterialButton) {
                    if ( layout.formatsButton.getText().toLowerCase().equals("netcdf") ) {
                        LASRequest lasRequest = makeRequest(7, "Data_Extract_netCDF");
                        requestQueue.add(lasRequest);
                    } else if ( layout.formatsButton.getText().toLowerCase().equals("csv") ) {
                        LASRequest lasRequest = makeRequest(7, "Data_Extract_CSV");
                        RequestProperty ff = new RequestProperty("ferret", "data_format", "csv");
                        lasRequest.addProperty(ff);
                        requestQueue.add(lasRequest);
                    } else if ( layout.formatsButton.getText().toLowerCase().equals("ascii") ) {
                        LASRequest lasRequest = makeRequest(7, "Data_Extract_File");
                        RequestProperty ff = new RequestProperty("ferret", "data_format", "tsv");
                        lasRequest.addProperty(ff);
                        requestQueue.add(lasRequest);
                    }
                    processQueue();
                } else {

                    String view = products.getSelectedProduct().getView();
                    if (event.isOpen()) {
                        if (view.equals("xy")) {
                            layout.downloadMapPanel.setDisplay(Display.NONE);
                        } else {
                            layout.downloadMapPanel.setDisplay(Display.BLOCK);
                        }
                        if (view.contains("z") || variables.get(0).getVerticalAxis() == null) {
                            layout.downloadZaxisPanel.setDisplay(Display.NONE);
                        } else {
                            layout.downloadZaxisPanel.setDisplay(Display.BLOCK);
                        }
                        if (view.contains("t")) {
                            layout.downloadDateTimePanel.setDisplay(Display.NONE);
                        } else {
                            layout.downloadDateTimePanel.setDisplay(Display.BLOCK);
                        }
                        if ( !products.getSelectedProduct().getView().contains("t") ) {
                            downloadDateTime.setLo(dateTimeWidget.getFerretDateLo());
                            downloadDateTime.setHi(dateTimeWidget.getFerretDateLo());
                        }
                        if ( !products.getSelectedProduct().getView().contains("z") ) {
                            downloadZaxisWidget.setLo(zAxisWidget.getLo());
                            downloadZaxisWidget.setHi(zAxisWidget.getLo());
                        }
                    }
                }
            }
        });

        eventBus.addHandler(AutoColors.TYPE, new AutoColorsHandler() {
            @Override
            public void onAutoColors(AutoColors event) {
                if ( event.isOn() ) {
                    // TODO all panels
                    layout.panel2.setLevels(layout.panel1.getLevels_string());
                } else {
                    layout.panel2.setLevels("");
                }
                layout.setUpdate(Constants.UPDATE_NEEDED);
            }
        });
        eventBus.addHandler(Info.TYPE, new InfoHandler() {
            @Override
            public void onInfo(Info event) {
                layout.showProgress();
                layout.infoPanel.setDisplay(Display.NONE);
                layout.infoHeader.setDisplay(Display.NONE);
                layout.advancedSearch.setDisplay(Display.NONE);
                long id = event.getId();
                layout.setInfoSelect(id);
                String rid = id + ".json";
                info = true;
                datasetService.getDataset(rid, infoCallback);
            }
        });
        eventBus.addHandler(Correlation.TYPE, new CorrelationHandler() {
            @Override
            public void onCorrelation(Correlation event) {
                // isOpen means this is the event that is opening the window, so set it up.
                if ( event.isOpen() ) {
                    correlationMap.setCurrentSelection(refMap.getYlo(), refMap.getYhi(), refMap.getXlo(), refMap.getXhi());
                    correlationDateTime.setLo(dateTimeWidget.getFerretDateLo());
                    correlationDateTime.setHi(dateTimeWidget.getFerretDateHi());
                    if (variables.get(0).getVerticalAxis() != null) {
                        correlationZaxisWidget.setDisplay(Display.BLOCK);
                        correlationZaxisWidget.setLo(zAxisWidget.getLo());
                        correlationZaxisWidget.setHi(zAxisWidget.getHi());
                    } else {
                        correlationZaxisWidget.setDisplay(Display.NONE);
                    }
                } else if ( event.isSetX() ) {
                    OptionElement o = (OptionElement) event.getSource();
                    layout.xSelectedVariable = dataset.findVariableByNameAndTitle(o.getValue(), o.getText());
                } else if ( event.isSetY() ) {
                    OptionElement o = (OptionElement) event.getSource();
                    layout.ySelectedVariable = dataset.findVariableByNameAndTitle(o.getValue(), o.getText());
                } else if ( event.isSetC() ) {
                    OptionElement o = (OptionElement) event.getSource();
                    layout.cSelectedVariable = dataset.findVariableByNameAndTitle(o.getValue(), o.getText());
                } else if ( event.isRemoveConstraint() ) {
                    VariableConstraintWidget vcw = (VariableConstraintWidget) event.getSource();
                    if ( vcw.isActive() ) {
                        layout.setUpdate(Constants.UPDATE_NEEDED);
                    }
                    OptionElement o = layout.variableConstraintListBox.getOptionElement(layout.variableConstraintListBox.getIndex(vcw.getName()));
                    o.setDisabled(false);
                    layout.variableConstraintListBox.reload();
                    boolean gone = layout.variableConstraints.remove(vcw);
                }

                // Fires on any event except setting the variable
                if ( !event.isSetX() && !event.isSetY() && !event.isSetC() && !event.isRemoveConstraint()) {
                    layout.correlationProgress.setDisplay(Display.BLOCK);
                    // If not opening or changing variables, then just submit the product request to update the plot
                    LASRequest lasRequest = makeRequest(8, "prop_prop_plot");
                    requestQueue.add(lasRequest);
                    processQueue();
                }

            }
        });

        Mouse mouse = new Mouse();
        layout.addMouse(1, mouse);
        Mouse correlationMouse = new Mouse();
        layout.addMouse(8, correlationMouse);

        layout.setDateTime(dateTimeWidget);
        layout.addVerticalAxis(zAxisWidget);
        RootPanel.get("load").getElement().setInnerHTML("");
        RootPanel.get("las_main").add(layout);

        layout.panel1.getOutputPanel().setTitle(Constants.PANEL01);
        layout.panel1.setIndex(1);
        layout.panel2.getOutputPanel().setTitle(Constants.PANEL02);
        layout.panel2.setIndex(2);
        layout.panel5.setTitle(Constants.PANEL05);
        layout.panel5.setIndex(5);
        layout.panel8.getOutputPanel().setTitle(Constants.PANEL08);
        layout.panel8.setIndex(8);

        layout.animateWindow.addCloseHandler(new CloseHandler<Boolean>() {
            @Override
            public void onClose(CloseEvent<Boolean> closeEvent) {
                timer.cancel();
            }
        });

        // Call for the site now

        if ( initialHistory == null || initialHistory.isEmpty() ) {
            if ( toast )
                MaterialToast.fireToast("Getting initial site information.");
            siteService.getSite("1.json", siteCallback);
            layout.showDataProgress();

        } else {
            popHistory(initialHistory);
        }

    }
    private void saveAxes() {
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
    public class Mouse {
        public void applyNeeded() {
            if ( layout.animateWindow.isOpen() ) {

            } else if ( layout.correlationWindow.isOpen() ) {
                layout.correlationUpdate.setBackgroundColor(Constants.UPDATE_NEEDED);
            } else {
                layout.setUpdate(Constants.UPDATE_NEEDED);
            }
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
        public void updateConstraints(double world_startx, double world_endx, double world_starty, double world_endy) {
            layout.xVariableConstraint.setActive(true);
            layout.yVariableConstraint.setActive(true);
            if (world_startx <= world_endx) {

                layout.xVariableConstraint.setLo(dFormat.format(world_startx));
                layout.xVariableConstraint.setHi(dFormat.format(world_endx));

            } else {

                layout.xVariableConstraint.setLo(dFormat.format(world_endx));
                layout.xVariableConstraint.setHi(dFormat.format(world_startx));

            }

            if (world_starty <= world_endy) {

                layout.yVariableConstraint.setLo(dFormat.format(world_starty));
                layout.yVariableConstraint.setHi(dFormat.format(world_endy));

            } else {

                layout.yVariableConstraint.setLo(dFormat.format(world_endy));
                layout.yVariableConstraint.setHi(dFormat.format(world_starty));

            }
        }
    }


    public interface RegionService extends RestService {
        @GET
        public void getRegions(MethodCallback<List<Region>> regionCallback);
    }
    public interface SearchService extends RestService {
        @POST
        public void getSearchResults(SearchRequest search, MethodCallback<SearchResults> datasetCallback);
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
    public interface InfoService extends RestService {
        @GET
        public void getInfo(@QueryParam("offset") String offset, MethodCallback<List<Dataset>> browseCallback);
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
        public void getProduct(LASRequest lasRequest, MethodCallback<ResultSet> productRequestCallback);
    }
    public interface DatatableService extends RestService {
        @POST
        public void datatable(LASRequest datatableRequest, TextCallback data);
    }
    public interface ErddapDataService extends RestService {
        @POST
        public void getData(LASRequest erddapRequest, TextCallback data);
    }

    TextCallback constraintValuesCallback = new TextCallback() {
        @Override
        public void onFailure(Method method, Throwable throwable) {

        }

        @Override
        public void onSuccess(Method method, String s) {

            layout.possibleValues.clear();
            JSONValue jsonV = JSONParser.parseStrict(s);

            JSONObject jsonO = jsonV.isObject();

            JSONObject table = (JSONObject) jsonO.get("table");
            JSONArray names = (JSONArray) table.get("columnNames");
            JSONArray rows = (JSONArray) table.get("rows");
            for (int i = 0; i < rows.size(); i++ ) {
                JSONArray aRow = (JSONArray) rows.get(i);
                JSONValue value = aRow.get(0);
                JSONValue name = names.get(0);
                String svalue = value.toString().replaceAll("\"", "");
                String sname = name.toString().replaceAll("\"", "");


                final MaterialLink link = new MaterialLink();
                link.setText(value.toString());
                link.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        eventBus.fireEventFromSource(new ChangeConstraint("add", "variable", sname, "eq", svalue), link);
                    }
                });
                layout.possibleValues.add(link);
            }

        }
    };
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
            layout.setUpdate(Constants.UPDATE_NOT_NEEDED);
            Window.alert("Request failed: "+exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, String response) {
            layout.hideProgress();
            layout.setUpdate(Constants.UPDATE_NOT_NEEDED);
            layout.panel1.openAnnotations();

            DataTable dataTable = new DataTable(response);

//            AnnotationChartOptions options = new AnnotationChartOptions();
//            options.setDisplayAnnotations(true);
            layout.panel1.getChart().setWidth((Window.getClientWidth() - navWidth)+"px");
            layout.panel1.getChart().setHeight((Window.getClientHeight() - navWidth)+"px");
            layout.panel1.getChart().setMargin(0.0d);


//            AnnotationChart annChart = new AnnotationChart(layout.panel1.getChart().getElement());
//            annChart.draw(dataTable, options);

            LineChartOptions options = new LineChartOptions();
            LineChartOptions.Explorer explorer = new LineChartOptions.Explorer();
            explorer.setAxis("horizontal");
            options.setExplorer(explorer);
            LineChart lineChart = new LineChart(layout.panel1.getChart().getElement());
            lineChart.draw(dataTable, options);


        }
    };
    MethodCallback<ResultSet> productRequestCallback = new MethodCallback<ResultSet>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            layout.setUpdate(Constants.UPDATE_NOT_NEEDED);
            layout.hideProgress();
            layout.downloadLoader.setDisplay(Display.NONE);
            Window.alert("Request failed: "+exception.getMessage());
            processQueue();
        }

        @Override
        public void onSuccess(Method method, ResultSet results) {
            if ( products.getSelectedProduct().getView().equals("xy") ) {
                // Active the animate button for the xy view only for now
                // TODO animate in other dimensions
                layout.animate.setEnabled(true);
            } else {
                layout.animate.setEnabled(false);
            }
            layout.topMenuEnabled(true);
            layout.setUpdate(Constants.UPDATE_NOT_NEEDED);
            layout.hideProgress();
            int tp = results.getTargetPanel();

            if ( tp < 5 ) {
                layout.infoPanel.setDisplay(Display.NONE);
                layout.infoHeader.setDisplay(Display.NONE);
                layout.advancedSearch.setDisplay(Display.NONE);
                layout.panel1.setVisible(true);
                state.getPanelState(tp).setResultSet(results);
                layout.setState(tp, state);
                if ( historyRequest != null ) {
                    // First set the UI to multi-panel

                    if ( historyRequestPanel2 != null ) {
                        eventBus.fireEvent(new PanelCount(2));
                        layout.setPlotCount(2);
                    }
                    // TODO other panels
//                    } else if ( currentTokens.size() == 4 ) {
//                        eventBus.fireEvent(new PanelCount(4));
//                    }

                    historyRequest = null;
                    historyVariable = null;
                }
                if (tp == 2) {
                    layout.panel2.scale();
                    if ( historyRequestPanel2 != null ) {
                        historyRequestPanel2 = null; // We're finally done with the second panel setup and product request.
                    }
                    makeAnnotationsEven();
                    // TODO other panels... 5 is the animation window
                }


            } else if ( tp == 5 ) {
                // Only do things in panel 5 if the animate window is open... Otherwise just ignore them
                if ( layout.animateWindow.isOpen() ) {
                    if (results.getProduct().equals("Animation_2D_XY") || results.getProduct().equals("Animation_2D_XY_vector")) {
                        animation = results.getAnimation();
                        layout.frameCount.setText("0/" + animation.getFrames().size() + " frames downloaded.");
                        layout.animateSubmit.setText("Cancel");
                        layout.animateHelp.setText(Constants.ANIMATE_CANCEL);
                    } else {
                        state.getPanelState(5).setResultSet(results);
                        layout.setState(tp, state);
                        layout.panel5.setImage(state.getPanelState(5).getImageUrl());
                        state.getPanelState(5).addFrame(state.getPanelState(5).getResultSet());
                        layout.frameCount.setText(state.getPanelState(5).getCompletedFrames().size() + "/" + animation.getFrames().size() + " frames downloaded.");
                    }
                    if (state.getPanelState(5).getCompletedFrames().size() <= animation.getFrames().size()) {
                        int frame = state.getPanelState(5).getFrameIndex();
                        frame = frame % animation.getFrames().size();
                        state.getPanelState(5).setFrameIndex(frame);
                        String t = animation.getFrames().get(frame);
                        // The animation properties to make a consistent plot between time steps
                        String contour_levels = animation.getContour_levels();
                        String fill_levels = animation.getFill_levels();
                        String dep_axis_scale = animation.getDep_axis_scale();

                        LASRequest lasRequest = makeRequest(5, products.getSelected());
                        lasRequest.getAxesSets().get(0).setTlo(t);
                        lasRequest.getAxesSets().get(0).setThi(t);

                        if ( contour_levels != null ) {
                            RequestProperty cl = new RequestProperty("ferret", "contour_levels", contour_levels);
                            lasRequest.addProperty(cl);
                        }
                        if ( fill_levels != null ) {
                            RequestProperty fl = new RequestProperty("ferret", "fill_levels", fill_levels);
                            lasRequest.addProperty(fl);
                        }
                        if ( dep_axis_scale != null ) {
                            RequestProperty das = new RequestProperty("ferret","dep_axis_scale", dep_axis_scale);
                            lasRequest.addProperty(das);
                        }
                        state.getPanelState(5).setLasRequest(lasRequest);

                        if (state.getPanelState(5).getCompletedFrames().size() == animation.getFrames().size() || animateCancel) {
                            // Stop progress bar
                            layout.animateProgress.setDisplay(Display.NONE);
                            // Done gathering frames.

                            // Open controls

                            layout.submitPanel.setDisplay(Display.NONE);
                            layout.animationControls.setDisplay(Display.BLOCK);

                            // Animate

                            timer.scheduleRepeating(layout.getSpeed());

                        } else {

                            // Queue the request and advance the counters
                            requestQueue.add(lasRequest);
                            frame++;
                            state.getPanelState(5).setFrameIndex(frame);

                        }
                    }
                }
            } else if ( tp == 6 ) {

                String error = results.getError();
                if ( error != null && !error.isEmpty() ) {

                }
                layout.showValuesWindow.clear();
                String myfile  = "";
                Result outfile = results.getResultByTypeAndFile_type("ferret_listing", "text");
                if ( outfile != null ) {
                    myfile = outfile.getUrl();
                }

                Frame frame = new Frame(myfile);
                frame.setWidth("100%");
                frame.setHeight("98%");
                layout.showValuesWindow.add(frame);
            } else if ( tp == 7 ) {
                String error = results.getError();
                if ( error != null && !error.isEmpty() ) {
                    layout.downloadError.setDisplay(Display.BLOCK);
                    layout.downloadError.setText(error);
                } else {
                    Result output = results.getResultByType("netcdf");
                    if (output == null) {
                        output = results.getResultByTypeAndFile_type("ferret_listing", "text");
                    }
                    if (output == null) {
                        output = results.getResultByTypeAndFile_type("ferret_listing", "csv");
                    }
                    String myfile = "";
                    if (output != null) {
                        myfile = output.getUrl();
                    }

                    layout.downloadLink.setText("Request sucessful. Click to download...");
                    layout.downloadLoader.setDisplay(Display.NONE);
                    layout.downloadLink.setDisplay(Display.BLOCK);
                    layout.downloadLink.setTarget("_blank");
                    layout.downloadLink.setHref(myfile);
                }
            } else if ( tp == 8 ) {
                layout.correlationProgress.setDisplay(Display.NONE);
                // Show plot an annotations in panel 8
                state.getPanelState(tp).setResultSet(results);
                layout.setState(tp, state);
                MapScale ms = results.getMapScale();
                layout.xVariableConstraint.setActive(false);
                layout.yVariableConstraint.setActive(false);
                layout.xVariableConstraint.setName(ms.getAxis_horizontal());
                layout.yVariableConstraint.setName(ms.getAxis_vertical());
                layout.xVariableConstraint.setLo(dFormat.format(Double.valueOf(ms.getXxxAxisLowerLeft())));
                layout.xVariableConstraint.setHi(dFormat.format(Double.valueOf(ms.getXxxAxisUpperRight())));
                layout.yVariableConstraint.setLo(dFormat.format(Double.valueOf(ms.getYyyAxisLowerLeft())));
                layout.yVariableConstraint.setHi(dFormat.format(Double.valueOf(ms.getYyyAxisUpperRight())));
                layout.correlationAxisItem.collapse();
                layout.correlationConstraintsItem.expand();
            }
            processQueue();
        }
    };
    Timer timer = new Timer() {
        @Override
        public void run() {
            nextStep();
            loop++;
            // Loop on completed frames in case we canceled
            loop = loop%state.getPanelState(5).getCompletedFrames().size();
        }
    };
    private void nextStep() {
        state.getPanelState(5).setFrameIndex(loop);
        ResultSet completedFrame = state.getPanelState(5).getCompletedFrames().get(loop);
        layout.panel5.setImage(state.getPanelState(5).getImageUrl());
        state.getPanelState(5).setResultSet(completedFrame);
        layout.setState(5, state);

    }
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

            products.init(productsList);
            Product p = products.getSelectedProduct();
            setUpForProduct(p);
            // Could be setting up after turning it off so check
            if ( layout.isAnalysisActive() ) {
                String type = layout.getAnalysis().getTransformation();
                String over = layout.getAnalysis().getOver();
                if (!type.equals("Compute") && !over.equals("Over")) {
                    // Analysis is active, take first variable.
                    if (over.equals("Area")) {
                        refMap.setTool("xy");
                    } else if (over.equals("Longitude")) {
                        refMap.setTool("x");
                    } else if (over.equals("Latitude")) {
                        refMap.setTool("y");
                    } else if (over.equals("Z")) {
                        zAxisWidget.setRange(true);
                    } else if (over.equals("Time")) {
                        dateTimeWidget.setRange(true);
                    }
                }
            }
            layout.setProducts(products);
            layout.setUpdate(Constants.UPDATE_NEEDED);
            if (historyRequest != null ) {

                // Start with the first axis set (the main UI panel)
                String hxlo = null;
                String hxhi = null;
                String hylo = null;
                String hyhi = null;
                String hzlo = null;
                String hzhi = null;
                String htlo = null;
                String hthi = null;

                Analysis hisAnalysis = null;
                AxesSet hisAxesSet = null;
                List<Analysis> analysisList = historyRequest.getAnalysis();
                if ( analysisList != null && analysisList.size() > 0 ) {
                    hisAnalysis = analysisList.get(0);
                }

                List<AxesSet> sets =  historyRequest.getAxesSets();
                if ( sets != null && sets.size() > 0 ) {
                    hisAxesSet = sets.get(0);
                }

                if ( hisAnalysis != null && hisAxesSet != null ) {
                    // TODO is the list for each panel or multiple transformations?
                    layout.setAnalysisOver(hisAnalysis.getAnalysisAxes().get(0).getType());
                    layout.setAnalysisTransformation(hisAnalysis.getTransformation());
                    layout.setAnalysisActive(true);
                    turnOnAnalysis(hisAnalysis.getTransformation(), hisAnalysis.getOver());


                    if ( hisAnalysis.getAxes().contains("x") ) {
                        AnalysisAxis xax = hisAnalysis.getAnalysisAxis("x");
                        if ( xax != null ) {
                            hxlo = xax.getLo();
                            hxhi = xax.getHi();
                        }
                    } else {
                        hxlo = hisAxesSet.getXlo();
                        hxhi = hisAxesSet.getXhi();
                    }
                    if ( hisAnalysis.getAxes().contains("y") ) {
                        AnalysisAxis yax = hisAnalysis.getAnalysisAxis("y");
                        if ( yax != null ) {
                            hylo = yax.getLo();
                            hyhi = yax.getHi();
                        }
                    } else {
                        hylo = hisAxesSet.getYlo();
                        hyhi = hisAxesSet.getYhi();
                    }
                    if ( hisAnalysis.getAxes().contains("z") ) {
                        AnalysisAxis zax = hisAnalysis.getAnalysisAxis("z");
                        if ( zax != null ) {
                            hzlo = zax.getLo();
                            hzhi = zax.getHi();
                        }
                    } else {
                        hzlo = hisAxesSet.getZlo();
                        hzhi = hisAxesSet.getZhi();
                    }
                    if ( hisAnalysis.getAxes().contains("t") ) {
                        AnalysisAxis tax = hisAnalysis.getAnalysisAxis("t");
                        if ( tax != null ) {
                            htlo = tax.getLo();
                            hthi = tax.getHi();
                        }
                    } else {
                        htlo = hisAxesSet.getTlo();
                        hthi = hisAxesSet.getThi();
                    }
                    if ( hisAnalysis.getOver().equals("Area") ) {

                    }
                    refMap.setCurrentSelection(Double.valueOf(hylo).doubleValue(), Double.valueOf(hyhi).doubleValue(), Double.valueOf(hxlo).doubleValue(), Double.valueOf(hxhi).doubleValue());

                    if ( hthi != null && htlo != null  ) {
                        dateTimeWidget.setLo(htlo);
                        dateTimeWidget.setHi(hthi);
                    }
                    if (hzlo != null && hzhi != null ) {
                        zAxisWidget.setLo(hzlo);
                        zAxisWidget.setHi(hzhi);
                    }
                }

                update(p);
            }
        }
    };
    // N.B. there are two callbacks. This one deals with history events. Maybe should be renamed.
    MethodCallback<Dataset> panel2DatasetCallback = new MethodCallback<Dataset>() {
        @Override
        public void onFailure(Method method, Throwable throwable) {
            Window.alert("Failed to get data set to setup panel from history token.");
        }

        @Override
        public void onSuccess(Method method, Dataset dataset) {
            layout.panel2.clearBreadcrumbs();
            final Breadcrumb bc = new Breadcrumb(dataset, false);
            bc.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    eventBus.fireEventFromSource(new BreadcrumbSelect(dataset, 2), bc);
                    // Breadcrumbs are in the collapsible header, so stop to prevent the crumb from opening the panel.
                    event.stopPropagation();
                }
            });
            layout.panel2.addBreadcrumb(bc);
            layout.panel2.setDataset(dataset);
            String hash = historyRequestPanel2.getVariableHashes().get(0);
            if ( historyRequestPanel2.getVariableHashes().size() == 2 ) {
                hash = historyRequestPanel2.getVariableHashes().get(1);
            }
            Variable variable = dataset.findVariableByHash(hash);
            final Breadcrumb vbc = new Breadcrumb(variable, true);
            vbc.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    eventBus.fireEventFromSource(new BreadcrumbSelect(variable, 2), bc);
                    // Breadcrumbs are in the collapsible header, so stop to prevent the crumb from opening the panel.
                    event.stopPropagation();
                }
            });
            layout.panel2.addBreadcrumb(vbc);
            setUpPanel(2, variable);
        }
    };
    MethodCallback<Dataset> infoCallback = new MethodCallback<Dataset>() {
        @Override
        public void onFailure(Method method, Throwable throwable) {
            layout.hideProgress();
        }

        @Override
        public void onSuccess(Method method, Dataset infoDataset) {
            layout.hideProgress();
            layout.panel1.setVisible(false);
            layout.infoPanel.setDisplay(Display.BLOCK);
            layout.advancedSearch.setDisplay(Display.NONE);
            layout.animate.setEnabled(false);
            layout.topMenuEnabled(false);
            layout.infoPanel.clear();
            dataset = infoDataset;
            DatasetInfo di = new DatasetInfo(infoDataset);
            info = false;
            if ( infoDataset != null ) {
                List<Variable> returnedVariables = infoDataset.getVariables();
                Collections.sort(returnedVariables);
                for (int i = 0; i < returnedVariables.size(); i++) {
                    Variable variable = returnedVariables.get(i);
                    di.addVariable(variable);
                }
                layout.infoPanel.add(di);
            }
        }
    };
    MethodCallback <SearchResults> searchCallback = new MethodCallback<SearchResults>() {
        @Override
        public void onFailure(Method method, Throwable throwable) {
            layout.hideDataProgress();
        }

        @Override
        public void onSuccess(Method method, SearchResults searchResults) {
            layout.hideDataProgress();
            layout.clearDatasets();
            layout.panel1.setVisible(true);
            layout.infoPanel.setDisplay(Display.NONE);
            layout.infoHeader.setDisplay(Display.NONE);
            layout.infoPanel.clear();
            List<Dataset> searchDatasets = searchResults.getDatasetList();
            layout.advancedSearchTotal = searchResults.getTotal();
            int start = searchResults.getStart();
            int end = searchResults.getEnd();
            int total = end - start;
            if ( layout.advancedSearchTotal > total ) {
                if ( start > 0 ) {
                    layout.prevAdvancedSearch.setDisplay(Display.INLINE);
                } else if ( start ==0 ) {
                    layout.prevAdvancedSearch.setDisplay(Display.NONE);
                }
                layout.nextAdvancedSearch.setDisplay(Display.INLINE);
                layout.advancedSearchOffset = end;
            }
            if ( end >= layout.advancedSearchTotal ) {
                layout.nextAdvancedSearch.setDisplay(Display.NONE);
            } else {
                layout.nextAdvancedSearch.setDisplay(Display.INLINE);
            }
            if ( searchDatasets != null && searchDatasets.size() > 0 ) {
                for (int i = 0; i < searchDatasets.size(); i++) {
                    layout.addSelection(searchDatasets.get(i));
                }
                layout.dataItem.expand();
                if ( xDataURL != null && !xDataURL.isEmpty() ) {
                    xDataURL = null;
                }
            } else {
                MaterialToast.fireToast("No data sets found matching your search terms.");
                info = false;
                siteService.getSite("1.json", siteCallback);
                layout.removeBreadcrumbs(1);
            }
        }
    };
    MethodCallback<Dataset> datasetCallback = new MethodCallback<Dataset>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            layout.hideDataProgress();
            if ( layout.loadDialog.isOpen() )
                layout.loadDialog.close();
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, Dataset returnedDataset) {

            layout.clearDatasets();
            if (returnedDataset.getDatasets() != null) {
                dataset = returnedDataset;
                if (dataset.getDatasets().size() > 0) {
                    layout.hideDataProgress();
                    List<Dataset> datasets = dataset.getDatasets();
                    Collections.sort(datasets);
                    for (int i = 0; i < datasets.size(); i++) {
                        layout.addSelection(datasets.get(i));
                    }
                }

                if (dataset.hasVariableChildren() && dataset.getStatus() == null) {
                    Window.alert("Data set ingested with unknown status. May not display correctly.");
                }
                if (dataset.hasVariableChildren() && dataset.getStatus() != null && dataset.getStatus().equals(Dataset.INGEST_FINISHED)) {
                    if (layout.loadDialog.isOpen())
                        layout.loadDialog.close();
                    configService.getConfig(dataset.getId(), configCallback);
                    List<Variable> returnedVariables = dataset.getVariables();
                    Collections.sort(returnedVariables);
                    for (int i = 0; i < returnedVariables.size(); i++) {
                        layout.addSelection(returnedVariables.get(i));
                    }
                    List<Vector> returnedVectors = dataset.getVectors();
                    for (int i = 0; i < returnedVectors.size(); i++) {
                        Vector v = returnedVectors.get(i);
                        layout.addSelection(v);
                    }
                    if (toast)
                        MaterialToast.fireToast("Getting products for these variables.");
                } else if (dataset.hasVariableChildren() && (dataset.getStatus().equals(Dataset.INGEST_NOT_STARTED) || dataset.getStatus().equals(Dataset.INGEST_STARTED))) {
                    layout.setDatasetsMessage(dataset.getMessage());
                    layout.goBack();
                } else if (dataset.hasVariableChildren() && dataset.getStatus().equals(Dataset.INGEST_FAILED)) {
                    layout.setDatasetsMessage(Constants.INGEST_FAILED);
                    layout.goBack();
                }
            } else {
                layout.setDatasetsMessage(returnedDataset.getMessage());
            }
            layout.dataItem.expand();
        }
    };
    MethodCallback<Site> siteCallback = new MethodCallback<Site>() {

        public void onSuccess(Method method, Site site) {

            layout.hideDataProgress();
            layout.clearDatasets();
            layout.setBrand(site.getTitle());
            int w = 4;
            if (layout.sideNav.isOpen()) {
                w = navWidth;
            }
            layout.setBrandWidth(w);


            String t = site.getTotal() + " data sets in total.";
            layout.total.setText(t);

            String g = site.getGrids() + " data sets on grids.";
            layout.grids.setText(g);

            String d = site.getDiscrete() + " data sets on discrete geometries.";
            layout.discrete.setText(d);
            String traj = site.getTrajectory() + " trajectory data sets.";
            layout.trajectoryCount.setText(traj);
            String point = site.getPoint() + " point data sets.";
            layout.pointCount.setText(point);
            String profile = site.getProfile() + " profile data sets.";
            layout.profileCount.setText(profile);
            String timeseries = site.getTimeseries() + " timeseries data sets.";
            layout.timeseriesCount.setText(timeseries);

            // TODO not used at the moment. Was for ds and var titles etc. We now do those with autocomplete
            Map<String, List<String>> attrs = site.getAttributes();

            toast = site.isToast();
            if (site.getDatasets().size() > 0) {
                List<Dataset> datasets = site.getDatasets();
                Collections.sort(datasets);
                for (int i = 0; i < datasets.size(); i++) {
                    Dataset iterDataset = datasets.get(i);
                    layout.addSelection(iterDataset);
                }
            }

            if ( xDataURL != null && !xDataURL.isEmpty() ) {
                MaterialToast.fireToast("Searching for initial data set.");
                List<VariableProperty> vpl = new ArrayList<>();
                SearchRequest sr = new SearchRequest();
                sr.setCount(10);
                sr.setOffset(0);
                VariableProperty vp = new VariableProperty();
                vp.setType("search");
                vp.setName("url");
                vp.setValue(xDataURL);
                vpl.add(vp);
                sr.setVariableProperties(vpl);
                layout.startSearch(sr);
            } else {
                layout.navcollapsible.setActive(1, true);
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
            // Set up the first variable.
            // If there's more than one, set up the rest in applyConfig.
            if ( historyRequest != null ) {



                String hash = historyRequest.getVariableHashes().get(0);
                if (hash != null && !hash.isEmpty()) {
                    historyVariable = hash;
                }

                int selectedIndex = dataset.findVariableIndexByHash(historyVariable);

                if (selectedIndex >= 0) {
                    layout.setSelectedVariable(selectedIndex);
                    newVariable = layout.getSelectedVariable();
                    final Breadcrumb dbc = new Breadcrumb(dataset, false);
                    dbc.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            eventBus.fireEventFromSource(new BreadcrumbSelect(dataset, 1), dbc);
                            // Breadcrumbs are in the collapsible header, so stop to prevent the crumb from opening the panel.
                            event.stopPropagation();
                        }
                    });
                    layout.removeBreadcrumbs(1);
                    layout.addBreadcrumb(dbc, 1);
                    final Breadcrumb vbc = new Breadcrumb(newVariable, true);
                    vbc.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            eventBus.fireEventFromSource(new BreadcrumbSelect(newVariable, 1), vbc);
                            // Breadcrumbs are in the collapsible header, so stop to prevent the crumb from opening the panel.
                            event.stopPropagation();
                        }
                    });
                    layout.addBreadcrumb(vbc, 1);
                }
                historyVariable = null;
                applyConfig();
            } else if ( historyVariable != null ) {
                // It's been selected, now apply the config and plot.
                newVariable = layout.getSelectedVariable();
                applyConfig();
            }
        }
    };
    private void applyConfig() {

        TimeAxis tAxis = null;
        if (newVariable != null) {
            tAxis = newVariable.getTimeAxis();
        } else if (newVector != null) {
            tAxis = newVector.getU().getTimeAxis();
        }

        if (tAxis != null) {
            dateTimeWidget.init(tAxis, false);
            animateDateTimeWidget.init(tAxis, true);
            downloadDateTime.init(tAxis, true);
            correlationDateTime.init(tAxis, true);
        }
        VerticalAxis vAxis = null;
        if (newVariable != null) {
            vAxis = newVariable.getVerticalAxis();
        } else if (newVector != null) {
            vAxis = newVector.getU().getVerticalAxis();
        }
        if (vAxis != null) {
            zAxisWidget.init(vAxis);
            downloadZaxisWidget.init(vAxis);
            downloadZaxisWidget.setRange(true);
            correlationZaxisWidget.init(vAxis);
            correlationZaxisWidget.setRange(true);
            layout.allowZAverage(true);
        } else {
            layout.allowZAverage(false);
        }

        Variable useVariable = null;
        String geometry = "";
        if (newVariable != null) {
            useVariable = newVariable;
            geometry = useVariable.getGeometry();
        } else if (newVector != null) {
            useVariable = newVector.getU();
            geometry = newVector.getGeometry();
        }
        refMap.setDataExtent(useVariable.getGeoAxisY().getMin(), useVariable.getGeoAxisY().getMax(), useVariable.getGeoAxisX().getMin(), useVariable.getGeoAxisX().getMax(), useVariable.getGeoAxisX().getDelta());
        downloadMap.setDataExtent(useVariable.getGeoAxisY().getMin(), useVariable.getGeoAxisY().getMax(), useVariable.getGeoAxisX().getMin(), useVariable.getGeoAxisX().getMax(), useVariable.getGeoAxisX().getDelta());
        correlationMap.setDataExtent(useVariable.getGeoAxisY().getMin(), useVariable.getGeoAxisY().getMax(), useVariable.getGeoAxisX().getMin(), useVariable.getGeoAxisX().getMax(), useVariable.getGeoAxisX().getDelta());


        List<Product> productsList = configSet.getConfig().get(geometry + "_" + useVariable.getIntervals()).getProducts();
        List<Region> regions = configSet.getConfig().get(geometry + "_" + useVariable.getIntervals()).getRegions();


        refMap.setRegions(regions);
        downloadMap.setRegions(regions);
        correlationMap.setRegions(regions);


        Product previousProduct = products.getSelectedProduct();

        // Attempt to save the product when changing variables.
        // TODO test this!
        products.init(productsList);
        if (previousProduct != null && productsList.contains(previousProduct)) {
            products.setSelected(previousProduct.getName());
        }

        Product p = products.getSelectedProduct();
        // If the map has the same shape try the old settings. Otherwise it will default for the variable.
        boolean useCurrentMapSettings = p.getView().equals(refMap.getTool());
        setUpForProduct(p);

        layout.setProducts(products);

        if (tAxis != null) {
            String display_hi = tAxis.getDisplay_hi();
            String display_lo = tAxis.getDisplay_lo();

            if (display_hi != null) {
                if ( display_hi.equalsIgnoreCase("start") ) {
                    dateTimeWidget.setHi(tAxis.getStart());
                } else if ( display_hi.equalsIgnoreCase("end") ) {
                    dateTimeWidget.setHi(tAxis.getEnd());
                } else {
                    dateTimeWidget.setHi(display_hi);
                }
            }
            if (display_lo != null) {
                if ( display_lo.equalsIgnoreCase("start") ) {
                    dateTimeWidget.setLo(tAxis.getStart());
                } else if ( display_lo.equalsIgnoreCase("end") ) {
                    dateTimeWidget.setLo(tAxis.getEnd());
                } else {
                    dateTimeWidget.setLo(display_lo);
                }
            }
        }

        // State has been set to work with new variable,
        // now restore previous settings if possible if a previous variable exists
        // and it's not a request to go back in history (which dictates all the settings to get to previous state).

        if (variables.size() > 0 && historyRequest == null && historyVariable == null) {
            if (useCurrentMapSettings) {
                refMap.setCurrentSelection(ylo, yhi, xlo, xhi);
            }
            if (tAxis != null) {
                dateTimeWidget.setLo(tlo);
                dateTimeWidget.setHi(thi);
            }
            if (variables.get(0).getVerticalAxis() != null) {
                zAxisWidget.setLo(zlo);
                zAxisWidget.setHi(zhi);
            }
        }

        variables.clear();
        if (newVariable != null) {
            variables.add(newVariable);
            isVector = false;
        }
        if ( newVector != null ) {
            isVector = true;
            variables.add(newVector.getU());
            variables.add(newVector.getV());
            if ( newVector.getW() != null ) {
                variables.add(newVector.getW());
            }
        }

        // If this came in because of a history token, fix all the other stuff (plot type, plot properties, etc) then update.

        if ( historyRequest != null ) {

            List<Analysis> historyAnalysisList = historyRequest.getAnalysis();
            if ( historyAnalysisList != null && historyAnalysisList.size() > 0 && historyAnalysisList.get(0) != null) {
                Analysis a = historyAnalysisList.get(0);
                // TODO is the list for each panel or multiple transformations?
                layout.setAnalysisOver(a.getAnalysisAxes().get(0).getType());
                layout.setAnalysisTransformation(a.getTransformation());
                layout.setAnalysisActive(true);
                turnOnAnalysis(a.getTransformation(), a.getOver());
                return;
            }

            String op = historyRequest.getOperation();
            String historyOpName = historyRequest.getOperation();
            Product historyProduct = null;
            for (int i = 0; i < productsList.size(); i++) {
                Product product = productsList.get(i);
                if ( product.getName().equalsIgnoreCase(historyOpName) ) {
                    historyProduct = product;
                }
            }
            if ( historyProduct != null ) {
                layout.setProductByName(historyProduct.getName());
                setUpForProduct(historyProduct);
                p = historyProduct;
            }


            List<RequestProperty> props = historyRequest.getRequestProperties();
            for (int i = 0; i < props.size(); i++) {
                RequestProperty rp = props.get(i);
                layout.setProperty(rp);
            }

            String hr_xlo = historyRequest.getAxesSets().get(0).getXlo();
            String hr_xhi = historyRequest.getAxesSets().get(0).getXhi();

            String hr_ylo = historyRequest.getAxesSets().get(0).getYlo();
            String hr_yhi = historyRequest.getAxesSets().get(0).getYhi();

            if ( hr_xlo != null && hr_xhi != null && hr_ylo != null && hr_yhi != null ) {
                refMap.setCurrentSelection( Double.valueOf(hr_ylo).doubleValue(), Double.valueOf(hr_yhi).doubleValue(), Double.valueOf(hr_xlo).doubleValue(), Double.valueOf(hr_xhi).doubleValue());
            }

            String hr_tlo = historyRequest.getAxesSets().get(0).getTlo();
            String hr_thi = historyRequest.getAxesSets().get(0).getThi();
            if ( hr_tlo != null ) {
                dateTimeWidget.setLo(hr_tlo);
            }
            if ( hr_thi != null ) {
                dateTimeWidget.setHi(hr_thi);
            }

            String hr_zlo = historyRequest.getAxesSets().get(0).getZlo();
            String hr_zhi = historyRequest.getAxesSets().get(0).getZhi();
            if ( hr_zlo != null ) {
                zAxisWidget.setLo(hr_zlo);
            }
            if ( hr_zhi != null ) {
                zAxisWidget.setHi(hr_zhi);
            }

            if ( historyRequest.getVariableHashes().size() > 1 ) {
                layout.toDatasetChecks();

                for (int i = 1; i < historyRequest.getVariableHashes().size(); i++) {
                    String vhash = historyRequest.getVariableHashes().get(i);
                    int selectedIndex = dataset.findVariableIndexByHash(vhash);
                    if ( selectedIndex >= 0 ) {
                        Variable v = dataset.getVariables().get(selectedIndex);
                        variables.add(v);
                        layout.setSelectedVariable(selectedIndex);
                    }
                }
            }
        }

        if (p != null) update(p);

    }
    private void turnOnAnalysis(String type, String over) {
        if ( !type.equals("Compute") && !over.equals("Over")) {
            // Analysis is active, take first variable.
            String intervals = variables.get(0).getIntervals();
            if ( over.equals("Area") ) {
                intervals = intervals.replace("xy", "");
            } else if ( over.equals("Longitude") ) {
                intervals = intervals.replace("x", "");
            } else if ( over.equals("Latitude") ) {
                intervals = intervals.replace("y", "");
            } else if ( over.equals("Z") ) {
                intervals = intervals.replace("z", "");
            } else if ( over.equals("Time") ) {
                intervals = intervals.replace("t","");
            }
            pbis.getProductsByInterval(variables.get(0).getGeometry(), intervals, pbisCallback);
        }
    }
    private void turnOffAnalysis() {
        String intervals = variables.get(0).getIntervals();
        pbis.getProductsByInterval(variables.get(0).getGeometry(), intervals, pbisCallback);
    }
    private void update(Product p) {
        layout.showProgress();
        if (p.isClientPlot()) {

            if (variables.get(0).getGeometry().equals(Constants.TIMESERIES)) {

                layout.panel1.getOutputPanel().setVisible(false);
                layout.panel1.getChart().setVisible(true);
                makeDataRequest(variables, p);

            }


        } else {
            for (int i = 1; i <= state.getPanelCount(); i++) {

                LASRequest lasRequest = makeRequest(i, products.getSelected());
                state.getPanelState(i).setLasRequest(lasRequest);
                requestQueue.add(lasRequest);
                if (i == 1) {
                    layout.panel1.getChart().setVisible(false);
                    layout.panel1.getOutputPanel().setVisible(true);
                    layout.panel1.clearAnnotations();
                } else if (i == 2) {
                    layout.panel2.getChart().setVisible(false);
                    layout.panel2.getOutputPanel().setVisible(true);
                    layout.panel2.clearAnnotations();
                }
            }
            processQueue();
        }

    }
    private void makeDataRequest(List<Variable> variables1, Product p) {
        LASRequest timeseriesReqeust = makeRequest(1, p.getName());
        if ( p.getTitle().equals("Timeseries") ) {
//            erddapDataService.erddapTimeseries(timeseriesReqeust, makeChartFromJSON);

            // TODO hack for using two different chart clients
        } else if ( p.getTitle().equals("Timeseries Plot") ) {
            // Always get the data table from LAS to avoid same origin problem
            // TODO add smarts to controller to stream direct any .dataTable ERDDAPs
            datatableService.datatable(timeseriesReqeust, makeGoogleChartFromJSON);

            layout.panel1.clearAnnotations();
            layout.panel1.addAnnotation(new MaterialLabel("Dataset: " +dataset.getTitle()));
            layout.panel1.addAnnotation(new MaterialLabel("Variable: " +variables1.get(0).getTitle()));
            layout.panel1.addAnnotation(new MaterialLabel("Time: " + timeseriesReqeust.getAxesSets().get(0).getTlo()+" : " + timeseriesReqeust.getAxesSets().get(0).getThi() ));
            if ( variables.get(0).getVerticalAxis() != null ) {
                layout.panel1.addAnnotation(new MaterialLabel("Depth: " + timeseriesReqeust.getAxesSets().get(0).getZlo() + " : " + timeseriesReqeust.getAxesSets().get(0).getZhi()));
            }
            layout.panel1.addAnnotation(new MaterialLabel("Latitude: " + timeseriesReqeust.getAxesSets().get(0).getYlo()+" : " + timeseriesReqeust.getAxesSets().get(0).getYlo() ));
            layout.panel1.addAnnotation(new MaterialLabel("Longitue: " + timeseriesReqeust.getAxesSets().get(0).getXlo()+" : " + timeseriesReqeust.getAxesSets().get(0).getXhi() ));
        }

    }
    private LASRequest makeRequest(int panel, String productName) {
        LASRequest lasRequest = new LASRequest();

        lasRequest.setTargetPanel(panel);

        lasRequest.setOperation(productName);
        Analysis analysis = null;
        // Only apply analysis from the layou to panel 1
        if ( panel == 1 ) {
            analysis = layout.getAnalysis();
            if (analysis != null) {
                List<AnalysisAxis> axes = analysis.getAnalysisAxes();
                for (int i = 0; i < axes.size(); i++) {
                    AnalysisAxis a = axes.get(i);
                    if (a.getType().equals("x")) {
                        a.setLo(String.valueOf(refMap.getXlo()));
                        a.setHi(String.valueOf(refMap.getXhi()));
                    } else if (a.getType().equals("y")) {
                        a.setLo(String.valueOf(refMap.getYlo()));
                        a.setHi(String.valueOf(refMap.getYhi()));
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
        AxesSet a1 = new AxesSet();
        lasRequest.getAxesSets().add(a1);

        if (productName.toLowerCase().contains("timeseries")) {
            double[] exts = refMap.getDataExtent();
            lasRequest.getAxesSets().get(0).setXlo(String.valueOf(exts[2]));
            lasRequest.getAxesSets().get(0).setXhi(String.valueOf(exts[3]));

            lasRequest.getAxesSets().get(0).setYlo(String.valueOf(exts[0]));
            lasRequest.getAxesSets().get(0).setYhi(String.valueOf(exts[1]));
        } else {
            if (analysis == null || (analysis != null && !analysis.getAxes().contains("x"))) {
                lasRequest.getAxesSets().get(0).setXlo(String.valueOf(refMap.getXlo()));
                lasRequest.getAxesSets().get(0).setXhi(String.valueOf(refMap.getXhi()));
            }

            if (analysis == null || (analysis != null && !analysis.getAxes().contains("y"))) {
                lasRequest.getAxesSets().get(0).setYlo(String.valueOf(refMap.getYlo()));
                lasRequest.getAxesSets().get(0).setYhi(String.valueOf(refMap.getYhi()));
            }
        }

        if ( ! variables.get(0).getGeometry().equals(GRID) ) {
            // Apply any active constraints.
            lasRequest.setDataConstraints(layout.getActiveConstraints());
        }
        // TODO ask ferret to accept ISO Strings

        if (products.getSelectedProduct().isClientPlot()) {
            if ( products.getSelectedProduct().getTitle().toLowerCase().contains("timeseries") ) {
                Variable t = dataset.findVariableByNameAndTitle("time", "Time");
                if ( !variables.contains(t) ) {
                    variables.add(0, t);
                }
            }
            if (variables.get(0).getTimeAxis() != null) {
                lasRequest.getAxesSets().get(0).setTlo(dateTimeWidget.getISODateLo());
                if (dateTimeWidget.isRange()) {
                    lasRequest.getAxesSets().get(0).setThi(dateTimeWidget.getISODateHi());
                } else {
                    lasRequest.getAxesSets().get(0).setThi(dateTimeWidget.getISODateLo());
                }
            }
        } else {
            if (analysis == null || (analysis != null && !analysis.getAxes().contains("t"))) {
                if (variables.get(0).getTimeAxis() != null) {
                    lasRequest.getAxesSets().get(0).setTlo(dateTimeWidget.getFerretDateLo());
                    if (dateTimeWidget.isRange()) {
                        lasRequest.getAxesSets().get(0).setThi(dateTimeWidget.getFerretDateHi());
                    } else {
                        lasRequest.getAxesSets().get(0).setThi(dateTimeWidget.getFerretDateLo());
                    }
                }
            }
        }

        if (variables.get(0).getVerticalAxis() != null) {

            if (analysis == null || (analysis != null && !analysis.getAxes().contains("z"))) {
                lasRequest.getAxesSets().get(0).setZlo(zAxisWidget.getLo());
                if (zAxisWidget.isRange()) {
                    lasRequest.getAxesSets().get(0).setZhi(zAxisWidget.getHi());
                } else {
                    lasRequest.getAxesSets().get(0).setZhi(zAxisWidget.getLo());
                }
            }

        }

        // Replace any axes setting orthogonal to the view for any panel other than 1

        String view = products.getSelectedProduct().getView();

        if ( panel != 1 ) {

            ComparePanel comparePanel = null;
            if (panel == 2) {  // or 3 or 4 then do this work. Otherwise it's the main or animation panel...
                comparePanel = layout.panel2;

                // The map is always has the right hi and lo no matter the view

                AxesSet a2 = new AxesSet();
                lasRequest.getAxesSets().add(a2);
                // If it's a difference, the nav values stay in AxesSet1 and the
                // ortho values go in AxesSet2
                if (!view.contains("x") && !comparePanel.isDifference()) {
                    lasRequest.getAxesSets().get(0).setXlo(String.valueOf(comparePanel.getXlo()));
                    lasRequest.getAxesSets().get(0).setXhi(String.valueOf(comparePanel.getXhi()));
                } else if (!view.contains("x") && comparePanel.isDifference()) {
                    lasRequest.getAxesSets().get(1).setXlo(String.valueOf(comparePanel.getXlo()));
                    lasRequest.getAxesSets().get(1).setXhi(String.valueOf(comparePanel.getXhi()));
                }
                if (!view.contains("y") && !comparePanel.isDifference()) {
                    lasRequest.getAxesSets().get(0).setYlo(String.valueOf(comparePanel.getYlo()));
                    lasRequest.getAxesSets().get(0).setYhi(String.valueOf(comparePanel.getYhi()));
                } else if (!view.contains("y") && comparePanel.isDifference()) {
                    lasRequest.getAxesSets().get(1).setYlo(String.valueOf(comparePanel.getYlo()));
                    lasRequest.getAxesSets().get(1).setYhi(String.valueOf(comparePanel.getYhi()));
                }
                if (!view.contains("z") && !comparePanel.isDifference()) {
                    if (comparePanel.getVariable().getVerticalAxis() != null) {
                        lasRequest.getAxesSets().get(0).setZlo(comparePanel.getZlo());
                        if (comparePanel.isZRange()) {
                            lasRequest.getAxesSets().get(0).setZhi(comparePanel.getZhi());
                        } else {
                            lasRequest.getAxesSets().get(0).setZhi(comparePanel.getZlo());
                        }
                    }
                } else if (!view.contains("z") && comparePanel.isDifference()) {
                    if (comparePanel.getVariable().getVerticalAxis() != null) {
                        lasRequest.getAxesSets().get(1).setZlo(comparePanel.getZlo());
                        if (comparePanel.isZRange()) {
                            lasRequest.getAxesSets().get(1).setZhi(comparePanel.getZhi());
                        } else {
                            lasRequest.getAxesSets().get(1).setZhi(comparePanel.getZlo());
                        }
                    }
                }
                if (!view.contains("t") && !comparePanel.isDifference()) {
                    if ( comparePanel.getVariable().getTimeAxis() != null ) {
                        lasRequest.getAxesSets().get(0).setTlo(comparePanel.getFerretDateLo());
                        if (comparePanel.dateTimeWidget.isRange()) {
                            lasRequest.getAxesSets().get(0).setThi(comparePanel.getFerretDateHi());
                        } else {
                            lasRequest.getAxesSets().get(0).setThi(comparePanel.getFerretDateLo());
                        }
                    }
                } else if (!view.contains("t") && comparePanel.isDifference()) {
                    if ( comparePanel.getVariable().getTimeAxis() != null ) {
                        lasRequest.getAxesSets().get(1).setTlo(comparePanel.getFerretDateLo());
                        if (comparePanel.dateTimeWidget.isRange()) {
                            lasRequest.getAxesSets().get(1).setThi(comparePanel.getFerretDateHi());
                        } else {
                            lasRequest.getAxesSets().get(1).setThi(comparePanel.getFerretDateLo());
                        }
                    }
                }
            } else if ( panel == 5 ) {
                if ( productName.equals("Animation_2D_XY") || productName.equals("Animation_2D_XY_vector") ) {
                    lasRequest.getAxesSets().get(0).setTlo(animateDateTimeWidget.getFerretDateLo());
                    lasRequest.getAxesSets().get(0).setThi(animateDateTimeWidget.getFerretDateHi());
                    String time_step = layout.time_step.getText();
                    RequestProperty rp = new RequestProperty("ferret", "fill_type", "fill");
                    lasRequest.addProperty(rp);
                    if ( time_step != null && Integer.valueOf(time_step).intValue() > 0 ) {
                        RequestProperty p = new RequestProperty("ferret", "time_step", time_step);
                        lasRequest.addProperty(p);
                    }
                }
            } else if ( panel == 8 ) {

                // Use correlation window values initially set when window was opened,
                // could be reset by user.
                lasRequest.getAxesSets().get(0).setXlo(String.valueOf(correlationMap.getXlo()));
                lasRequest.getAxesSets().get(0).setXhi(String.valueOf(correlationMap.getXhi()));

                lasRequest.getAxesSets().get(0).setYlo(String.valueOf(correlationMap.getYlo()));
                lasRequest.getAxesSets().get(0).setYhi(String.valueOf(correlationMap.getYhi()));

                if ( variables.get(0).getVerticalAxis() != null ) {
                    lasRequest.getAxesSets().get(0).setZlo(correlationZaxisWidget.getLo());
                    lasRequest.getAxesSets().get(0).setZhi(correlationZaxisWidget.getHi());
                }

                lasRequest.getAxesSets().get(0).setTlo(correlationDateTime.getFerretDateLo());
                lasRequest.getAxesSets().get(0).setThi(correlationDateTime.getFerretDateHi());


                // Appply the active variable constraints
                if ( layout.xVariableConstraint.isActive() ) {
                    if ( layout.xVariableConstraint.getLo() != null && !layout.xVariableConstraint.getLo().isEmpty() ) {
                        DataConstraint xlo = new DataConstraint("variable", layout.xVariableConstraint.getName(), "gt", layout.xVariableConstraint.getLo());
                        lasRequest.addDataConstraint(xlo);
                    }
                    if ( layout.xVariableConstraint.getHi() != null && !layout.xVariableConstraint.getHi().isEmpty() ) {
                        DataConstraint xhi = new DataConstraint("variable", layout.xVariableConstraint.getName(), "lt", layout.xVariableConstraint.getHi());
                        lasRequest.addDataConstraint(xhi);
                    }

                }
                if ( layout.yVariableConstraint.isActive() ) {
                    if ( layout.yVariableConstraint.getLo() != null && !layout.yVariableConstraint.getLo().isEmpty() ) {
                        DataConstraint ylo = new DataConstraint("variable", layout.yVariableConstraint.getName(), "gt", layout.yVariableConstraint.getLo());
                        lasRequest.addDataConstraint(ylo);
                    }
                    if ( layout.yVariableConstraint.getHi() != null && !layout.yVariableConstraint.getHi().isEmpty() ) {
                        DataConstraint yhi = new DataConstraint("variable", layout.yVariableConstraint.getName(), "lt", layout.yVariableConstraint.getHi());
                        lasRequest.addDataConstraint(yhi);
                    }
                }
                for (int vcindx = 0; vcindx < layout.variableConstraints.getWidgetCount(); vcindx++) {
                    VariableConstraintWidget vcw = (VariableConstraintWidget) layout.variableConstraints.getWidget(vcindx);
                    if ( vcw.isActive() ) {
                        if ( vcw.getLo() != null && !vcw.getLo().isEmpty() ) {
                            DataConstraint clo = new DataConstraint("variable", vcw.getName(), "gt", vcw.getLo());
                            lasRequest.addDataConstraint(clo);
                        }
                        if ( vcw.getHi() != null && !vcw.getHi().isEmpty() ) {
                            DataConstraint chi = new DataConstraint("variable", vcw.getName(), "lt", vcw.getHi());
                            lasRequest.addDataConstraint(chi);
                        }
                    }
                }
            }
        }


        List<RequestProperty> properties = layout.getPlotOptions();
        List<RequestProperty> existingProperites = lasRequest.getRequestProperties();

        if ( existingProperites != null ) {
            properties.addAll(existingProperites);
        }


        // Replace panel 2 fill_levels or contour_levels with with auto levels if defined...

        String p2_levels = layout.panel2.getLevels();
        if ( panel == 2 && p2_levels != null && !p2_levels.isEmpty() ) {
            RequestProperty fill = null;
            RequestProperty contour = null;
            for (int i = 0; i < properties.size(); i++) {
                RequestProperty p = properties.get(i);
                if ( p.getName().equals("fill_levels") ) {
                    fill = p;
                } else if ( p.getName().equals("contour_levels") ) {
                    contour = p;
                }
            }
            if ( fill != null ) {
                fill.setValue(p2_levels);
            }
            if ( contour != null ) {
                contour.setValue(p2_levels);
            }
            if ( fill == null && contour == null ) {
                // Add 'em both and let the scripts sort it out. :-)
                RequestProperty fp = new RequestProperty();
                fp.setType("ferret");
                fp.setName("fill_levels");
                fp.setValue(p2_levels);
                properties.add(fp);

                RequestProperty cp = new RequestProperty();
                cp.setType("ferret");
                cp.setName("contour_levels");
                cp.setValue(p2_levels);
                properties.add(cp);
            }
        }

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
        // TODO for now both 1 and 5 are the same...
        if (panel == 1 || panel == 5 || panel == 6 || panel == 7) {
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

            if ( layout.isDifference(panel) ) {
                // Add the panel1 variable and the variable to be compared.
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
                dhashes.add(layout.panel2.getDataset().getHash());
                vhashes.add(layout.panel2.getVariable().getHash());
                // Extra mumbo jumbo to have list because later custom analysis for panels.
            } else {
                // Just add the panel variable
                dhashes.add(layout.panel2.getDataset().getHash());
                vhashes.add(layout.panel2.getVariable().getHash());
            }
        } else if ( panel == 8 ) {
            // X-Variable
            String dhash1 = dataset.getHash();
            if (dhash1 != null) {
                dhashes.add(dhash1);
            }
            String xhash = layout.xSelectedVariable.getHash();
            if ( xhash != null ) {
                vhashes.add(xhash);
            }
            // Y-Variable
            if (dhash1 != null) {
                dhashes.add(dhash1);
            }
            String yhash = layout.ySelectedVariable.getHash();
            if ( yhash != null ) {
                vhashes.add(yhash);
            }

            // ColorBy if active
            if ( layout.colorByOn.getValue()) {
                if (dhash1 != null) {
                    dhashes.add(dhash1);
                }
                String chash = layout.cSelectedVariable.getHash();
                if (chash != null) {
                    vhashes.add(chash);
                }
            }

        }
        if ( vhashes.size() >= 1 ) {
            List<RequestProperty> p = lasRequest.getRequestProperties();
            RequestProperty rp = new RequestProperty("ferret", "data_count", String.valueOf(vhashes.size()));
            lasRequest.addProperty(rp);
        }
        lasRequest.setDatasetHashes(dhashes);
        lasRequest.setVariableHashes(vhashes);
    }
//    private void queueRequest(int panel, String productID) {
//        LASRequest lasRequest = makeRequest(panel, productID);
//        state.getPanelState(panel).setLasRequest(lasRequest);
//        requestQueue.add(lasRequest);
//    }

    private void processQueue() {
        if ( !requestQueue.isEmpty() ) {
            layout.showProgress();
            LASRequest lasRequest = requestQueue.remove();
            state.getPanelState(lasRequest.getTargetPanel()).setLasRequest(lasRequest);
            pushHistory();
            productService.getProduct(lasRequest, productRequestCallback);
            if ( toast )
                MaterialToast.fireToast("Requesting "+ lasRequest.getOperation() + " ...");
        }
    }
    private void pushHistory() {

        String fullHistoryToken = "";

        LASRequest r1 = state.getPanelState(1).getLasRequest();

        if ( r1 != null ) {
            JSONValue jsonHistoryToken = requestCodec.encode(r1);
            fullHistoryToken = fullHistoryToken + jsonHistoryToken.toString();
        }

        LASRequest r2 = state.getPanelState(2).getLasRequest();

        if ( r2 != null ) {
            JSONValue jsonHistoryToken = requestCodec.encode(r2);
            fullHistoryToken = fullHistoryToken + "_ns_" + jsonHistoryToken.toString();
        }

        // TODO history for panel 3 and 4

        History.newItem(fullHistoryToken, false);
    }
    private void popHistory(String token) {

        token = URL.decode(token);
        List<String> currentTokens = tokenizeHistory(token);

        if ( currentTokens != null && currentTokens.size() > 0 ) {
            String panelOneRequest = currentTokens.get(0);
            historyRequest = requestCodec.decode(panelOneRequest);
            // Get data set and variables and set them.
            List<String> historyDatasets = historyRequest.getDatasetHashes();
            if (historyDatasets.size() > 0) {
                // Get the first one. If there's more than one they are likely the same.
                datasetService.getDataset(historyDatasets.get(0) + ".json", datasetCallback);
            }
        }
        if ( currentTokens != null && currentTokens.size() > 1 ) {
            String panelTwoRequest = currentTokens.get(1);
            historyRequestPanel2 = requestCodec.decode(panelTwoRequest);

        }

        // TODO Get Ferret properties
        // TODO Get analysis
        // TODO Get difference
    }
    private List<String> tokenizeHistory(String token) {
        List<String> tokens = new ArrayList<>();
        String[] t = token.split("_ns_");
        for (int i = 0; i < t.length; i++) {
            tokens.add(t[i]);
        }
        return tokens;
    }
    private void setUpPanel(int tp, Variable variable) {
        // This action will set the variable and initialize the axes.
        if ( tp == 2 ) {

            String pview = products.getSelectedProduct().getView();
            layout.panel2.setVariable(variable);
            // Initialize the Axes and Hide axes that are in the view
            layout.panel2.initializeAxes(pview, variable);

            // set them to match the left nav
            //TODO - this needs logic to get from the left nav and the history and decide which to use based on the
            // view of the plot

            double nxlo = refMap.getXlo();
            double nxhi = refMap.getXhi();
            double nylo = refMap.getYlo();
            double nyhi = refMap.getYhi();



            if ( historyRequestPanel2 != null ) {
                if ( historyRequestPanel2.getAxesSets() != null) {
                    AxesSet axesSet = historyRequestPanel2.getAxesSets().get(0);
                    if ( !pview.contains("x") ) {
                        nxlo = Double.valueOf(axesSet.getXlo());
                        nxhi = Double.valueOf(axesSet.getXhi());
                    }
                    if ( !pview.contains("y") ) {
                        nylo = Double.valueOf(axesSet.getYlo());
                        nyhi = Double.valueOf(axesSet.getYhi());
                    }
                }
            }

            layout.panel2.setMapSelection(nylo, nyhi, nxlo, nxhi);

            // Use the first
            if (variable.getVerticalAxis() != null) {


                // Z is in the view and the main variable has z, use it's values
                if ( variables.get(0).getVerticalAxis() != null && pview.contains("z") ) {
                    layout.panel2.setZlo(zAxisWidget.getLo());
                    layout.panel2.setZhi(zAxisWidget.getHi());
                } else {
                    // Otherwise, use the variable or the history
                    String v_zlo = null;
                    String v_zhi = null;
                    if ( variable.getVerticalAxis() != null ) {
                        v_zlo = String.valueOf(variable.getVerticalAxis().getMin());
                        v_zhi = v_zlo;

                    }
                    if (historyRequestPanel2 != null) {
                        if (historyRequestPanel2.getAxesSets() != null) {
                            AxesSet axesSet = historyRequestPanel2.getAxesSets().get(0);
                            v_zlo = axesSet.getZlo();
                            v_zhi = axesSet.getZhi();
                        }
                    }
                    if ( v_zlo != null ) {
                        layout.panel2.setZlo(v_zlo);
                    }
                    if ( v_zhi != null ) {
                        layout.panel2.setZhi(v_zhi);
                    }

                }
            }
            if ( variable.getTimeAxis() != null ) {
                layout.panel2.setFerretDateLo(dateTimeWidget.getFerretDateLo());
                layout.panel2.setFerretDateHi(dateTimeWidget.getFerretDateHi());
            }

            if (variable.getGeometry().equals(Constants.GRID)) {
                // TODO all panels
                layout.panel2.enableDifference(true);
            }
        }
        LASRequest lasRequest = makeRequest(tp, products.getSelected());
        state.getPanelState(tp).setLasRequest(lasRequest);
        requestQueue.add(lasRequest);
        processQueue();
    }

    // Use the incoming variable to select the product
    // since we want to delay setting the member variable
    // until after we have reset the axes to the state
    // for the previous variable.
    private void setUpForProduct(Product p) {

        String view = "xy";

        if ( p != null ) {
            view = p.getView();
        }



        Variable useVariable = null;
        if ( newVariable != null ) {
            useVariable = newVariable;
        }
        if ( newVector != null ) {
            useVariable = newVector.getU();
        }

        // Only do this if analysis is not actitve
        if ( !layout.isAnalysisActive() ) {
            // TODO Use data view maybe
            if ( useVariable.getGeometry().equals(GRID) ) {
                refMap.setTool(view);
                downloadMap.setTool(view);
                correlationMap.setTool(view);
                layout.constraints.setDisplay(Display.NONE);
            } else {
                refMap.setTool("xy");
                downloadMap.setTool("xy");
                correlationMap.setTool("xy");
                layout.constraints.setDisplay(Display.BLOCK);
                layout.subsetColumn.clear();
                layout.byVariable.clear();
                for (int i = 0; i < dataset.getVariables().size(); i++) {
                    Variable v = dataset.getVariables().get(i);

                    if ( v.isSubset() || v.isDsgId() ) {
                        layout.activeConstraints.clear();
                        MaterialRow r = new MaterialRow();
                        r.addStyleName("radioHeight");
                        MaterialRadioButton rb = new MaterialRadioButton();
                        rb.addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent clickEvent) {
                                eventBus.fireEventFromSource(new SubsetValues(v), rb);
                            }
                        });
                        rb.setName("byIdConstraint");
                        rb.setText(v.getTitle());
                        rb.setFormValue(v.getName());
                        r.add(rb);
                        layout.subsetColumn.add(r);
                    } else {
                        layout.byVariable.addItem(v.getName(), v.getTitle());
                    }
                }
            }
        }


        if (useVariable.getTimeAxis() != null) {

            layout.showDateTime();
            if (view.contains("t") || p.getData_view().contains("t")) {
                dateTimeWidget.setRange(true);
            } else {
                dateTimeWidget.setRange(false);
            }
        } else {
            layout.hideDateTime();
        }

        if (useVariable.getVerticalAxis() != null) {
            layout.showVertialAxis();
            if (view.contains("z") ) {
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
    private void autoScale() {

        // Use the values from the main panel to compute the level

        // Algorithm from range.F subroutine in Ferret source code

        double umin = layout.panel1.getDataMin();
        double umax = layout.panel1.getDataMax();
        int nints = 20;

        double temp = (umax - umin) / nints;
        if (temp <= 0.0000000001) {
            temp = umax;
        }

        double nt = Math.floor(Math.log(temp) / Math.log(10.));
        if (temp < 1.0) {
            nt = nt - 1;
        }
        double pow = Math.pow(10, nt);
        temp = temp / pow;

        double dint = 10.0 * pow;
        if (temp < Math.sqrt(2.0)) {
            dint = pow;
        } else {
            if (temp < Math.sqrt(10.0)) {
                dint = 2.0 * pow;
            } else {
                if (temp < Math.sqrt(50.0)) {
                    dint = 5.0 * pow;
                }
            }
        }

        double fm = umin / dint;
        double m = Math.floor(fm);
        if (m < 0) {
            m = m - 1;
        }
        double uminr = Math.round(1000000 * dint * m) / 1000000;

        fm = umax / dint;
        m = Math.floor(fm);
        if (m > 0) {
            m = m + 1;
        }
        double umaxr = Math.round(1000000 * dint * m) / 1000000;

        // END OF FERRET ALGORITHM

        // Only use 4 significant digits

        // Modify the optionTextField and submit the request
        String fill_levels = "(-inf)(" + uminr + "," + umaxr + "," + dint + ")(inf)";

        // These are pretty close to zero. I think the min/max did not come back
        // from the server, so stop
        if ((uminr + .00001 < .0001 && umaxr + .00001 < .0001) || umax < -9999999. && umin > 9999999.) {
            // Do all panels
            layout.panel2.setLevels(Constants.NO_MIN_MAX);
            layout.panel2.setAutoLevelsOn(false);
        } else {
            layout.panel2.setLevels(fill_levels);
        }
        layout.setUpdate(Constants.UPDATE_NEEDED);
    }
    public void setLevels(String levels) {
        layout.panel2.setLevels(levels);
    }
    private String getAnchor() {
        String url = Window.Location.getHref();
        if (url.contains("#")) {
            return url.substring(url.indexOf("#") + 1, url.length());
        } else {
            return "";
        }

    }
    MapSelectionChangeListener mapListener = new MapSelectionChangeListener() {
        @Override
        public void onFeatureChanged() {
            layout.setUpdate(Constants.UPDATE_NEEDED);
        }
    };
    private void makeAnnotationsEven() {

        int lines1 = layout.panel1.countAnnotations();
        int lines2 = layout.panel2.countAnnotations();

        if ( lines1 > lines2 ) {
            layout.panel2.padAnnotations(lines1 - lines2);
        }

        if ( lines2 > lines1 ) {
            layout.panel1.padAnnotations(lines2 - lines1);
        }
    }
}
