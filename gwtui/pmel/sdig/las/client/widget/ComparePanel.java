package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.addins.client.window.MaterialWindow;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.IconPosition;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.constants.ProgressType;
import gwt.material.design.client.ui.MaterialCollapsible;
import gwt.material.design.client.ui.MaterialCollapsibleBody;
import gwt.material.design.client.ui.MaterialCollapsibleHeader;
import gwt.material.design.client.ui.MaterialCollapsibleItem;
import gwt.material.design.client.ui.MaterialCollection;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialModal;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialSwitch;
import gwt.material.design.client.ui.html.Div;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.event.PanelControlOpen;
import pmel.sdig.las.client.event.PlotOptionChange;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.map.OLMapWidget;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.client.util.Constants;
import pmel.sdig.las.shared.autobean.Annotation;
import pmel.sdig.las.shared.autobean.AnnotationGroup;
import pmel.sdig.las.shared.autobean.Config;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.Site;
import pmel.sdig.las.shared.autobean.TimeAxis;
import pmel.sdig.las.shared.autobean.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by rhs on 1/6/17.
 */
public class ComparePanel extends Composite {

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    @UiField
    MaterialColumn panel;

    @UiField
    OutputPanel outputPanel;

    @UiField
    Div chart;

    @UiField
    MaterialCollapsibleHeader breadcrumbs;

    @UiField
    MaterialCollapsibleBody annotations;


    @UiField
    MaterialPanel annotationPanel;

    @UiField
    MaterialCollapsible annotationsCollapse;

    @UiField
    Breadcrumb home;

    @UiField
    MaterialSwitch difference;

    Dataset dataset;
    Variable variable;
    double xlo;
    double xhi;
    double ylo;
    double yhi;
    String zlo;
    String zhi;
    String tlo;
    String thi;

    int index;

    @UiField
    MaterialWindow window;
    @UiField
    MaterialCollection datasets;
    @UiField
    MaterialCollapsibleItem dataItem;
    @UiField
    MaterialPanel mapPanel;
    @UiField
    MaterialPanel dateTimePanel;
    @UiField
    MaterialPanel zaxisPanel;

    MaterialIcon gear = new MaterialIcon(IconType.SETTINGS);
    List<Breadcrumb> holdBreadcrumbs;

    String tile_server;
    String tile_layer;

    OLMapWidget refMap;
    public DateTimeWidget dateTimeWidget = new DateTimeWidget();
    AxisWidget zAxisWidget = new AxisWidget();

    String view;

    interface ComparePanelUiBinder extends UiBinder<MaterialColumn, ComparePanel> {
    }

    private static ComparePanelUiBinder ourUiBinder = GWT.create(ComparePanelUiBinder.class);

    public ComparePanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

        // Initialize the local axes widgets for to set axes orthogonal to the view...
        Element wmsserver = DOM.getElementById("wms-server");
        if ( wmsserver != null )
            tile_server = wmsserver.getPropertyString("content");
        Element wmslayer = DOM.getElementById("wms-layer");
        if ( wmslayer != null )
            tile_layer = wmslayer.getPropertyString("content");

        refMap = new OLMapWidget("128px", "256px", tile_server, tile_layer);

        mapPanel.add(refMap);
        dateTimePanel.add(dateTimeWidget);
        zaxisPanel.add(zAxisWidget);



        gear.setIconPosition(IconPosition.LEFT);
        gear.setDisplay(Display.INLINE);
        gear.setIconColor(Color.BLUE);
        gear.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dataItem.setActive(true);
                window.setLayoutPosition(Style.Position.ABSOLUTE);
                window.setLeft(gear.getAbsoluteLeft());
                window.setTop(gear.getAbsoluteTop());
                window.setWidth(Constants.navWidth+"px");
                window.open();
                holdBreadcrumbs = getBreadcrumbs();
                for (int i = 0; i < holdBreadcrumbs.size(); i++) {
                    removeBreadcrumb(holdBreadcrumbs.get(i));
                }
                // This event loads the site (beginning of the heirarchy.  Only fire if datasets is empty.
                if ( getDataItems().size() <= 0 ) {
                    dataItem.setActive(true);
                    dataItem.showProgress(ProgressType.INDETERMINATE);
                    eventBus.fireEventFromSource(new PanelControlOpen(index), ComparePanel.this);
                }
                event.stopPropagation();
            }
        });

        window.addCloseHandler(new CloseHandler() {
            @Override
            public void onClose(CloseEvent event) {
                Object currentEnd = getBreadcrumbs().get(getBreadcrumbs().size()-1).getSelected();
                if ( getBreadcrumbs().size() <= 0 || !(currentEnd instanceof Variable) ) {
                    // If it's not a variable, clear it out and put back what was before
                    for (int i = 0; i < getBreadcrumbs().size(); i++ ) {
                        removeBreadcrumb(getBreadcrumbs().get(i));
                    }
                    for (int i = 0; i < holdBreadcrumbs.size(); i++) {
                        addBreadcrumb(holdBreadcrumbs.get(i));
                    }
                }
            }
        });
        breadcrumbs.add(gear);
        home.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dataItem.showProgress(ProgressType.INDETERMINATE);
                eventBus.fireEventFromSource(new PanelControlOpen(index), ComparePanel.this);
                event.stopPropagation();
            }
        });
        difference.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                eventBus.fireEventFromSource(new PlotOptionChange(), event.getSource());
            }
        });
    }
    public MethodCallback<Site> siteCallback = new MethodCallback<Site>() {

        public void onSuccess(Method method, Site site) {
            datasets.clear();
            dataItem.hideProgress();
            if ( site.getDatasets().size() > 0 ) {
                List<Dataset> siteDatasets = site.getDatasets();
                Collections.sort(siteDatasets);
                for (int i = 0; i < siteDatasets.size(); i++) {
                    final Dataset d = siteDatasets.get(i);
                    DataItem dataItem = new DataItem(d);
                    dataItem.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            eventBus.fireEventFromSource(new NavSelect(d, index), dataItem);
                        }
                    });
                    datasets.add(dataItem);
                }

            }

        }
        public void onFailure(Method method, Throwable exception) {
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
            dataItem.hideProgress();
        }
    };
    private List<DataItem> getDataItems() {
        List<DataItem> dataItems = new ArrayList<>();
        for (int i = 0; i < datasets.getWidgetCount(); i++) {
            Widget w = datasets.getWidget(i);
            if ( w instanceof DataItem ) {
                dataItems.add((DataItem)w);
            }
        }
        return dataItems;
    }
    public MethodCallback<Dataset> datasetCallback = new MethodCallback<Dataset>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            dataItem.hideProgress();
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, Dataset newDataset) {
            dataItem.setActive(true);
            dataItem.hideProgress();
            datasets.clear();
            dataset = newDataset;
            if ( dataset.getDatasets().size() > 0 ) {
                List<Dataset> returnedDatasets = dataset.getDatasets();
                Collections.sort(returnedDatasets);
                for (int i = 0; i < returnedDatasets.size(); i++) {
                    Dataset d = returnedDatasets.get(i);
                    DataItem dataItem = new DataItem(d);
                    dataItem.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            eventBus.fireEventFromSource(new NavSelect(d, index), dataItem);
                        }
                    });
                    datasets.add(dataItem);
                }
            }
            if ( dataset.getVariables().size() > 0 ) {
                List<Variable> variables = dataset.getVariables();
                Collections.sort(variables);
                for (int i = 0; i < variables.size(); i++) {
                    Variable v = variables.get(i);
                    DataItem dataItem = new DataItem(v);
                    dataItem.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            eventBus.fireEventFromSource(new NavSelect(v, index), dataItem);
                        }
                    });
                    datasets.add(dataItem);
                }
            }
            dataItem.setActive(true);
            dataItem.hideProgress();
        }
    };
    public MethodCallback<Config> configCallback = new MethodCallback<Config>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, Config config) {
            // Apply the config

            if ( variable != null ) {
                // Save current state
                xlo = refMap.getXlo();
                xhi = refMap.getXhi();
                ylo = refMap.getYlo();
                yhi = refMap.getYhi();
                if (variable.getVerticalAxis() != null) {
                    zlo = zAxisWidget.getLo();
                    zhi = zAxisWidget.getHi();
                } else {
                    zlo = null;
                    zhi = null;
                }
                if (variable.getTimeAxis() != null) {
                    tlo = dateTimeWidget.getISODateLo();
                    thi = dateTimeWidget.getISODateHi();
                } else {
                    tlo = null;
                    thi = null;
                }
            }
            Variable newVariable = config.getVariable();
            initializeAxes(view, newVariable);

            // Put the values back if we've been here before
            if ( variable != null ) {
                refMap.setCurrentSelection(ylo, yhi, xlo, xhi);
                dateTimeWidget.setLo(tlo);
                dateTimeWidget.setHi(thi);
                if (variable.getVerticalAxis() != null) {
                    zAxisWidget.setLo(zlo);
                    zAxisWidget.setHi(zhi);
                }
            }
            variable = newVariable;

        }
    };
    public void setVisibility(boolean visibility) {
        this.setVisible(visibility);
    }
    public void clearAnnotations() {
        annotationPanel.clear();
    }
    public void addAnnotation(Widget w) {
        annotationPanel.add(w);
    }
    public void setState(State state) {
        outputPanel.setState(state);
        List<AnnotationGroup> groups = state.getPanelState(this.getTitle()).getProductResults().getAnnotationGroups();
        annotationPanel.clear();
        for (Iterator<AnnotationGroup> gIt = groups.iterator(); gIt.hasNext(); ) {
            AnnotationGroup ag = gIt.next();
            for (Iterator<Annotation> aIt = ag.getAnnotations().iterator(); aIt.hasNext(); ) {
                MaterialLabel l = new MaterialLabel();
                Annotation a = aIt.next();
                l.setText(a.getValue());
                annotationPanel.add(l);
            }
        }
        annotationsCollapse.open(1);
    }
    public void openAnnotations() {
        annotationsCollapse.open(1);
    }
    public OutputPanel getOutputPanel() {
        return outputPanel;
    }
    public Div getChart() { return chart; }

    public void addBreadcrumb(Breadcrumb b) {

        int index = getBreadcrumbs().size();
        if ( b.getSelected() instanceof Dataset ) {
            dataset = (Dataset) b.getSelected();
        }
        if ( index > 0 ) {
            Breadcrumb tail = (Breadcrumb) getBreadcrumbs().get(index - 1);
            Object tailObject = tail.getSelected();
            if ( tailObject instanceof Variable) {
                removeBreadcrumb(tail);
                breadcrumbs.add(b);
            } else {
                breadcrumbs.add(b);
            }
        } else {
            breadcrumbs.add(b);
        }
    }

    public void removeBreadcrumb(Breadcrumb tail) {
        breadcrumbs.remove(tail);
        List<Breadcrumb> remaining = getBreadcrumbs();
        if ( remaining.size() > 0 ) {
            Breadcrumb last = remaining.get(remaining.size() - 1);
            if ( last.getSelected() instanceof Dataset ) {
                dataset = (Dataset) last.getSelected();
            }
        }
    }

    public void setGrid(String grid) {
        panel.setGrid(grid);
    }

    public List<Breadcrumb> getBreadcrumbs() {

        List<Breadcrumb> crumbs = new ArrayList<Breadcrumb>();
        for (int i = 0; i < breadcrumbs.getWidgetCount(); i++) {
            Widget w = breadcrumbs.getWidget(i);
            if ( w instanceof Breadcrumb ) {
                crumbs.add((Breadcrumb) w);
            }
        }

        return crumbs;
    }
    public MaterialCollapsibleHeader getBreadcrumbContainer() {
        return breadcrumbs;
    }

    public void scale() {
        outputPanel.scale();
    }

    public void scale(int navSize) {
        outputPanel.scale(navSize);
    }

    public void open(int left, int top) {

    }

    public void initializeAxes(String view, Variable variable) {
        TimeAxis tAxis = variable.getTimeAxis();

        dateTimeWidget.init(tAxis, false);
        if ( variable.getVerticalAxis() != null ) {
            zAxisWidget.init(variable.getVerticalAxis());
        }

        refMap.setDataExtent(variable.getGeoAxisY().getMin(), variable.getGeoAxisY().getMax(), variable.getGeoAxisX().getMin(), variable.getGeoAxisX().getMax(), variable.getGeoAxisX().getDelta());

        String display_hi = tAxis.getDisplay_hi();
        String display_lo = tAxis.getDisplay_lo();

        if ( display_hi != null ) {
            dateTimeWidget.setHi(display_hi);
        }
        if ( display_lo != null ) {
            dateTimeWidget.setLo(display_lo);
        }
        hideViewAxes(view, variable);
    }
    public void hideViewAxes(String view, Variable variable) {
        this.view = view;
        mapPanel.setDisplay(Display.BLOCK);
        dateTimePanel.setDisplay(Display.BLOCK);
        zaxisPanel.setDisplay(Display.BLOCK);
        if ( view.contains("x") || view.contains("y") ) {
            mapPanel.setDisplay(Display.NONE);
        }
        if ( view.contains("z") || variable.getVerticalAxis() == null ) {
            zaxisPanel.setDisplay(Display.NONE);
        }
        if ( view.contains("t") ) {
            dateTimePanel.setDisplay(Display.NONE);
        }
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Variable getVariable() {
        return variable;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public double getXlo() {
        return refMap.getXlo();
    }
    public double getXhi() {
        return refMap.getXhi();
    }
    public double getYlo() {
        return refMap.getYlo();
    }
    public double getYhi() {
        return refMap.getYhi();
    }
    public String getZlo() {
        return zAxisWidget.getLo();
    }
    public String getZhi() {
        return zAxisWidget.getHi();
    }
    public boolean isZRange() {
        return zAxisWidget.isRange();
    }
    public void setFerretDateLo(String tlo) {
        dateTimeWidget.setLo(tlo);
    }
    public void setFerretDateHi(String hi) {
        dateTimeWidget.setHi(hi);
    }
    public void setMapSelection(double ylo, double yhi, double xlo, double xhi) {
        refMap.setCurrentSelection(ylo, yhi, xlo, yhi);
    }
    public void setZlo(String zlo) {
        zAxisWidget.setLo(zlo);
    }
    public void setZhi(String zhi) {
        zAxisWidget.setHi(zhi);
    }
    public String getFerretDateLo() {
        return dateTimeWidget.getFerretDateLo();
    }
    public String getFerretDateHi() {
        return dateTimeWidget.getFerretDateHi();
    }
    public void enableDifference(boolean b) {
        difference.setEnabled(b);
    }
    public boolean isDifference() {
        return difference.getValue();
    }
}