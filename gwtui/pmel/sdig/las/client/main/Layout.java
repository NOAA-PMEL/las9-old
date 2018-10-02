package pmel.sdig.las.client.main;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.addins.client.window.MaterialWindow;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.ProgressType;
import gwt.material.design.client.events.SideNavClosedEvent;
import gwt.material.design.client.events.SideNavOpenedEvent;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialCollapsibleItem;
import gwt.material.design.client.ui.MaterialCollection;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialDropDown;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialListBox;
import gwt.material.design.client.ui.MaterialNavBar;
import gwt.material.design.client.ui.MaterialNavBrand;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialPreLoader;
import gwt.material.design.client.ui.MaterialProgress;
import gwt.material.design.client.ui.MaterialRange;
import gwt.material.design.client.ui.MaterialRow;
import gwt.material.design.client.ui.MaterialSideNavPush;
import gwt.material.design.client.ui.MaterialSwitch;
import gwt.material.design.client.ui.MaterialTextBox;
import gwt.material.design.client.ui.html.Option;
import pmel.sdig.las.client.event.BreadcrumbSelect;
import pmel.sdig.las.client.event.Download;
import pmel.sdig.las.client.event.FeatureModifiedEvent;
import pmel.sdig.las.client.event.PanelCount;
import pmel.sdig.las.client.event.AnimateAction;
import pmel.sdig.las.client.event.ShowValues;
import pmel.sdig.las.client.map.OLMapWidget;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.client.util.Constants;
import pmel.sdig.las.client.widget.AxisWidget;
import pmel.sdig.las.client.widget.Breadcrumb;
import pmel.sdig.las.client.widget.ComparePanel;
import pmel.sdig.las.client.widget.DataItem;
import pmel.sdig.las.client.widget.DateTimeWidget;
import pmel.sdig.las.client.widget.ImagePanel;
import pmel.sdig.las.client.widget.MenuOptionsWidget;
import pmel.sdig.las.client.widget.ProductButtonList;
import pmel.sdig.las.client.widget.ResultsPanel;
import pmel.sdig.las.client.widget.TextOptionsWidget;
import pmel.sdig.las.client.widget.VariableConstraintWidget;
import pmel.sdig.las.client.widget.YesNoOptionsWidget;
import pmel.sdig.las.shared.autobean.Analysis;
import pmel.sdig.las.shared.autobean.AnalysisAxis;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.RequestProperty;
import pmel.sdig.las.shared.autobean.Variable;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by rhs on 9/8/15.
 */
public class Layout extends Composite {

    @UiField
    MaterialNavBrand brand;
    @UiField
    MaterialCollection datasets;
    @UiField
    MaterialButton update;

    @UiField
    MaterialButton plotsButton;
    @UiField
    MaterialDropDown plotsDropdown;

    @UiField
    MaterialLink animate;
    @UiField
    MaterialWindow animateWindow;

    @UiField
    MaterialButton overButton;
    @UiField
    MaterialButton analysisButton;

    @UiField
    ResultsPanel panel1;
    @UiField
    ComparePanel panel2;
    @UiField
    MaterialColumn panel3;
    @UiField
    MaterialColumn panel4;
    @UiField
    ImagePanel panel5;
    @UiField
    ResultsPanel panel8; // The correlation viewer...

    @UiField
    MaterialNavBar navbar;
    @UiField
    MaterialPanel mapPanel;
    @UiField
    MaterialCollapsibleItem dataItem;

    @UiField
    MaterialPanel animationDateTimePanel;
    @UiField
    MaterialPanel animationControls;
    @UiField
    MaterialLabel speedLabel;
    @UiField
    MaterialRange flipSpeed;
    @UiField
    MaterialButton stop;
    @UiField
    MaterialRow prevnext;
    @UiField
    MaterialButton next;
    @UiField
    MaterialButton prev;
    @UiField
    MaterialButton animateSubmit;
    @UiField
    MaterialPanel submitPanel;
    @UiField
    MaterialLabel frameCount;
    @UiField
    MaterialLabel animateHelp;
    @UiField
    MaterialTextBox time_step;
    @UiField
    MaterialProgress animateProgress;

    @UiField
    MaterialLink showValuesButton;
    @UiField
    MaterialWindow showValuesWindow;

    @UiField
    MaterialWindow downloadWindow;
    @UiField
    MaterialLink saveAsButton;
    @UiField
    MaterialCollection downloadDatasets;
    @UiField
    MaterialDropDown formatsDropDown;
    @UiField
    MaterialButton formatsButton;
    @UiField
    MaterialPanel downloadMapPanel;
    @UiField
    MaterialPanel downloadDateTimePanel;
    @UiField
    MaterialPanel downloadZaxisPanel;
    @UiField
    MaterialButton downloadButton;
    @UiField
    MaterialLink downloadLink;
    @UiField
    MaterialPreLoader downloadLoader;
    @UiField
    MaterialLabel downloadError;

    @UiField
    MaterialLink correlationLink;
    @UiField
    MaterialWindow correlationWindow;
    @UiField
    MaterialButton correlationUpdate;
    @UiField
    MaterialListBox xVariableListBox;
    @UiField
    MaterialListBox yVariableListBox;
    @UiField
    MaterialListBox cVariableListBox;
    @UiField
    MaterialPanel correlationDateTimePanel;
    @UiField
    MaterialPanel correlationMapPanel;
    @UiField
    MaterialPanel correlationZaxisPanel;
    Variable ySelectedVariable;
    Variable xSelectedVariable;
    Variable cSelectedVariable;
    @UiField
    VariableConstraintWidget xVariableConstraint;
    @UiField
    VariableConstraintWidget yVariableConstraint;
    @UiField
    MaterialCollapsibleItem correlationAxisItem;
    @UiField
    MaterialCollapsibleItem correlationConstraintsItem;
    @UiField
    MaterialListBox variableConstraintListBox;
    @UiField
    MaterialSwitch colorByOn;
    @UiField
    MaterialPanel variableConstraints;
    @UiField
    MaterialProgress correlationProgress;


    @UiField
    MaterialPanel dateTimePanel;
    @UiField
    MaterialPanel zaxisPanel;

    @UiField
    MaterialIcon home;

    @UiField
    MaterialIcon back;

    @UiField
    MaterialPanel products;
    @UiField
    MaterialPanel options;

    @UiField
    MaterialSwitch analysisSwitch;

    @UiField
    MaterialSideNavPush sideNav;

    @UiField
    MaterialRow outputRow01;

    @UiField
    MaterialRow outputRow02;

    Widget root;

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    boolean menuopen = true;

    int kern = 15;

    NumberFormat seconds = NumberFormat.getFormat("#.#");


    interface LayoutUiBinder extends UiBinder<Widget, Layout> {     }
    private static LayoutUiBinder ourUiBinder = GWT.create(LayoutUiBinder.class);

    public Layout() {
        root = ourUiBinder.createAndBindUi(this);
        initWidget(root);
        panel2.setIndex(2);

        colorByOn.addValueChangeHandler(colorByOnChange);
        analysisSwitch.addValueChangeHandler(analysisSwitchChange);
        outputRow01.setPaddingLeft(Constants.navWidth);
        outputRow02.setPaddingLeft(Constants.navWidth);
        sideNav.addOpenedHandler(new SideNavOpenedEvent.SideNavOpenedHandler() {
            @Override
            public void onSideNavOpened(SideNavOpenedEvent sideNavOpenedEvent) {
                outputRow01.setPaddingLeft(Constants.navWidth);
                outputRow02.setPaddingLeft(Constants.navWidth);
                scale(Constants.navWidth);
            }
        });
        sideNav.addClosedHandler(new SideNavClosedEvent.SideNavClosedHandler() {
            @Override
            public void onSideNavClosed(SideNavClosedEvent sideNavOpenedEvent) {
                outputRow01.setPaddingLeft(4);
                outputRow02.setPaddingLeft(4);
                scale(8);
            }
        });
        animateWindow.addCloseHandler(new CloseHandler<Boolean>() {
            @Override
            public void onClose(CloseEvent<Boolean> closeEvent) {
                eventBus.fireEventFromSource(new AnimateAction(true, false, false), animateWindow);
                // Reset animation controls to the "Going" state...
                prevnext.setDisplay(Display.NONE);
                stop.setText("Stop");
                animateProgress.setDisplay(Display.NONE);
                hideProgress();
            }
        });
        correlationWindow.addCloseHandler(new CloseHandler<Boolean>() {
            @Override
            public void onClose(CloseEvent<Boolean> closeEvent) {
                correlationProgress.setDisplay(Display.NONE);
                xVariableListBox.clear();
                yVariableListBox.clear();
                cVariableListBox.clear();
                variableConstraintListBox.clear();
                variableConstraints.clear();
            }
        });
    }
    public void scale(int navWidth) {
        State state = panel1.getOutputPanel().getState();
        int panelCount = 1;
        if ( state != null ) {
            panelCount = state.getPanelCount();
        }
        if ( panelCount == 1 ) {
            panel1.scale(navWidth);;
        } else if ( panelCount == 2 ) {
            panel1.scale(navWidth);
            panel2.scale(navWidth);
        } else if ( panelCount == 4 ) {
            panel1.scale(navWidth);
            panel2.scale(navWidth);
            // TODO the other 4 panels
//                    panel3.scale(navWidth);
//                    panel4.scale(navWidth);
        }
    }
    public Analysis getAnalysis() {
        if ( analysisSwitch.getValue() ) {
            Analysis analysis = new Analysis();
            String axes = "";
            String type = analysisButton.getText();
            String over = overButton.getText();
            if ( !type.equals("Computer") && !over.equals("Over")) {
                analysis.setTransformation(type);
                if ( over.equals("Area") ) {
                    AnalysisAxis x = new AnalysisAxis();
                    x.setType("x");
                    AnalysisAxis y = new AnalysisAxis();
                    y.setType("y");
                    List<AnalysisAxis> ax = new ArrayList<>();
                    ax.add(x);
                    ax.add(y);
                    analysis.setAnalysisAxes(ax);
                    analysis.setAxes("xy");
                } else if ( over.equals("Longitude") ) {
                    AnalysisAxis x = new AnalysisAxis();
                    x.setType("x");
                    List<AnalysisAxis> ax = new ArrayList<>();
                    ax.add(x);
                    analysis.setAnalysisAxes(ax);
                    analysis.setAxes("x");
                } else if ( over.equals("Latitude") ) {
                    AnalysisAxis y = new AnalysisAxis();
                    y.setType("y");
                    List<AnalysisAxis> ax = new ArrayList<>();
                    ax.add(y);
                    analysis.setAnalysisAxes(ax);
                    analysis.setAxes("y");
                } else if ( over.equals("Time") ) {
                    AnalysisAxis t = new AnalysisAxis();
                    t.setType("t");
                    List<AnalysisAxis> ax = new ArrayList<>();
                    ax.add(t);
                    analysis.setAnalysisAxes(ax);
                    analysis.setAxes("t");
                }

            } else {
                return null;
            }
            return analysis;
        } else {
            return null;
        }
    }
    public void setBrand(String title) {
        brand.setText(title);
    }

    public void clearDatasets() {
        datasets.clear();
    }
    public void addMap(OLMapWidget map) {
//        mapPanelBody.add(map);
    }
    //    public void showMap() {
//        mapCollapse.setIn(true);
//    }
    public void showDateTime() {
        dateTimePanel.setVisible(true);
    }
    public void hideDateTime() {
        dateTimePanel.setVisible(false);
    }
    public void showVertialAxis() {
        zaxisPanel.setDisplay(Display.BLOCK);
    }
    public void hideVerticalAxis() {
        zaxisPanel.setDisplay(Display.NONE);
    }

    public void addSelection(Object selection) {

        DataItem dataItem = new DataItem(selection);
        dataItem.addNavSelectClickHandler();
        datasets.add(dataItem);

    }

    public void toDatasetChecks() {
        for (int i = 0; i < datasets.getWidgetCount(); i++) {
            Widget w = datasets.getWidget(i);
            if ( w instanceof DataItem ) {
                DataItem d = (DataItem) w;
                d.toCheck();
            }

        }
    }
    public void toDatasetRadios() {
        boolean checked = false;
        int index = 0;
        for (int i = 0; i < datasets.getWidgetCount(); i++) {
            Widget w = datasets.getWidget(i);
            if ( w instanceof DataItem ) {
                DataItem d = (DataItem) w;
                if (d.isChecked()) {
                    if ( !checked ) {
                        index = i;
                        checked = true;
                    }
                }
                d.toRadio();
            }
        }
        DataItem s = (DataItem) datasets.getWidget(index);
        s.setRadioSelected();
    }
    public Variable getSelectedVariable() {
        for (int i = 0; i < datasets.getWidgetCount(); i++) {
            DataItem di = (DataItem) datasets.getWidget(i);
            Object s = di.getSelection();
            if ( s instanceof Variable && di.getRadioSelected() ) {
                return (Variable) s;
            }
        }
        return null;
    }
    //
    public void addBreadcrumb(Breadcrumb breadcrumb, int panel) {
        if ( panel == 1 ) {
            panel1.addBreadcrumb(breadcrumb);
        } else if ( panel == 2 ) {
            panel2.addBreadcrumb(breadcrumb);
        }
    }

    public void removeBreadcrumbs(Object selected, int targetPanel) {
        int index = 0;
        if ( targetPanel == 1 ) {
            List<Breadcrumb> crumbs = panel1.getBreadcrumbs();
            for (int i = 0; i < crumbs.size(); i++) {
                if (crumbs.get(i).getSelected() != null && crumbs.get(i).getSelected().equals(selected)) {
                    index = i;
                }
            }
            //TODO The end value is too big. What should it be?
            int end = crumbs.size() - index;
            for (int i = 1; i < end; i++) {
                int removeIndex = panel1.getBreadcrumbContainer().getWidgetCount() - 1;
                // Start just beyond the select crumb (index+1) and remove every crumb after that...
                Widget w = panel1.getBreadcrumbContainer().getWidget(removeIndex);
                if (w instanceof Breadcrumb) {
                    panel1.getBreadcrumbContainer().remove(removeIndex);
                }
            }
        }
    }
    public int getBreadcrumbCount(int targetPanel) {
        if ( targetPanel == 1 ) {
            return panel1.getBreadcrumbs().size();
        } else if ( targetPanel == 2 ){ // Other panels
            return panel2.getBreadcrumbs().size();
        } else if ( targetPanel == 3 ){
//            return panel3.getBreadcrumbs().size();
        } else if ( targetPanel == 4 ){
//            return panel4.getBreadcrumbs().size();
        }
        return -1;
    }

    /**
     * Remove them all except the home crumb
     * @param targetPanel
     */
    public void removeBreadcrumbs(int targetPanel){
        int count = panel1.getBreadcrumbs().size();
        for(int i = 0; i < count; i++ ) {
            panel1.getBreadcrumbContainer().remove(panel1.getBreadcrumbContainer().getWidgetCount()-1);
        }
    }
    public void addVerticalAxis(AxisWidget zAxisWidget) {
        zaxisPanel.clear();
        zaxisPanel.add(zAxisWidget);
    }
    public void setProducts(ProductButtonList productButtonList) {
        products.clear();
        products.add(productButtonList);
    }
    public void addProductButton(MaterialRow pb) {
        products.add(pb);
    }
    public void setUpdate(Color color) {
        if ( !animateWindow.isOpen() && !correlationWindow.isOpen() ) {
            update.setBackgroundColor(color);
        }
        if ( correlationWindow.isOpen() ) {
            correlationUpdate.setBackgroundColor(color);
        }
    }
    public void setState(int panel, State state) {
        if ( panel == 1 ) {
            panel1.setState(state);
        } else if ( panel == 2 ) {
            panel2.setState(state);
            // TODO the others, 5 is the animation window
        } else if ( panel == 5 ) {
            panel5.setState(state);
        } else if ( panel == 8 ) {
            panel8.setState(state);
        }
    }
    public Dataset getDataset(int panel) {
        // Dataset for penal 1 kept in UI class
        if ( panel == 2 ) {
            return panel2.getDataset();
        } else if ( panel == 3 ) {
            // TODO when we get around to implementing 3 and 4
//            return panel3.getDataset();
        } else if ( panel == 4 ) {
//            return panel4.getDataset();
        }
        return null;
    }
    public Variable getVariable(int panel) {
        // Variable for panel 1 kept in UI class
        if ( panel == 2 ) {
            return panel2.getVariable();
        } else if ( panel == 3 ) {
            // TODO when we get around to implementing 3 and 4
//            return panel3.getVariable();
        } else if ( panel == 4 ) {
//            return panel4.getVariable();
        }
        return null;
    }
    public void addMouse(int panel, UI.Mouse mouse) {
        if ( panel == 1 ) {
            panel1.getOutputPanel().addMouse(mouse);
        } else if ( panel == 8 ) {
            panel8.getOutputPanel().addMouse(mouse);
        }
    }
    public List<RequestProperty> getPlotOptions() {
        List<RequestProperty> properties = new ArrayList<>();
        List<Widget> ow = options.getChildrenList();
        for (int i = 0; i < ow.size(); i++) {
            Widget w = ow.get(i);
            if ( w instanceof MenuOptionsWidget ) {
                MenuOptionsWidget mo = (MenuOptionsWidget) w;
                properties.addAll(mo.getOptions());
            } else if ( w instanceof TextOptionsWidget ) {
                TextOptionsWidget to = (TextOptionsWidget) w;
                properties.addAll(to.getOptions());
            } else if ( w instanceof YesNoOptionsWidget ) {
                YesNoOptionsWidget yno = (YesNoOptionsWidget) w;
                properties.addAll(yno.getOptions());
            }
        }
        return properties;
    }

    public int setPanels(String title) {
        plotsButton.setText(title);
        int count = 1;
        if ( title.contains("1") ) {
            count = 1;
            panel1.setGrid("s12 m12 l12");
            panel2.setVisibility(false);
            panel3.setVisibility(Style.Visibility.HIDDEN);
            panel4.setVisibility(Style.Visibility.HIDDEN);
        } else if ( title.contains("2") ) {
            count = 2;
            panel1.setGrid("s6 m6 l6");
            panel2.setVisibility(true);
            // Initialize the second panel with the breadcrumbs from the first..
            List<Breadcrumb> b = panel1.getBreadcrumbs();
            for ( int i = 0; i < b.size(); i++ ) {
                Breadcrumb nb = new Breadcrumb();
                panel2.addBreadcrumb(new Breadcrumb(b.get(i).getSelected(), i != 0 ));
            }
            panel3.setVisibility(Style.Visibility.HIDDEN);
            panel4.setVisibility(Style.Visibility.HIDDEN);
        } else if ( title.contains("4") ) {
            count = 4;
            panel1.setGrid("s6 m6 l6");
            panel2.setVisibility(true);
            panel3.setVisibility(Style.Visibility.VISIBLE);
            panel4.setVisibility(Style.Visibility.VISIBLE);
        }
        return count;
    }
    public void setMap(OLMapWidget map) {
        mapPanel.add(map);
    }
    public void setDateTime(DateTimeWidget dateTime) {
        dateTimePanel.add(dateTime);
    }
    public void showDataProgress() {
        dataItem.setActive(true);
        dataItem.showProgress(ProgressType.INDETERMINATE);
    }
    public void hideDataProgress() {
        dataItem.hideProgress();
    }
    public void showProgress() {
        navbar.showProgress(ProgressType.INDETERMINATE);
    }
    public void hideProgress() {
        navbar.hideProgress();
    }
    public void clearOptions() {
        options.clear();;
    }
    public void addOptions(Widget widget) {
        options.add(widget);
    }
    public int getSpeed() {
        return flipSpeed.getValue();
    }
    public boolean isDifference(int panel) {
        if ( panel == 1 ) {
            // Should need to ask
            return false;
        } else if ( panel == 2 ) {
            // TODO othter panels
            return panel2.isDifference();
        }
        return false;
    }
    public void setAnimateTimeWidget(DateTimeWidget dtw) {
        animationDateTimePanel.add(dtw);
    }

    ValueChangeHandler addAndDisable = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent valueChangeEvent) {
            String value = (String) valueChangeEvent.getValue();
            variableConstraintListBox.setSelectedIndex(0);
            OptionElement oe = variableConstraintListBox.getOptionElement(variableConstraintListBox.getIndex(value));
            oe.setDisabled(true);
            variableConstraintListBox.reload();
            VariableConstraintWidget vcw = new VariableConstraintWidget();
            vcw.setName(value);
            vcw.showRemove();
            variableConstraints.add(vcw);
        }
    };
    ValueChangeHandler correlationChangeX = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent valueChangeEvent) {
            String value = (String) valueChangeEvent.getValue();
            OptionElement optionElement = xVariableListBox.getOptionElement(xVariableListBox.getIndex(value));
            setUpdate(Color.RED);
            eventBus.fireEventFromSource(new Correlation(false, true, false, false, false), optionElement);
        }
    };
    ValueChangeHandler correlationChangeY = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent valueChangeEvent) {
            String value = (String) valueChangeEvent.getValue();
            OptionElement optionElement = yVariableListBox.getOptionElement(yVariableListBox.getIndex(value));
            setUpdate(Color.RED);
            eventBus.fireEventFromSource(new Correlation(false, false, true, false, false), optionElement);
        }
    };
    ValueChangeHandler correlationChangeC = new ValueChangeHandler() {
        @Override
        public void onValueChange(ValueChangeEvent valueChangeEvent) {
            String value = (String) valueChangeEvent.getValue();
            OptionElement optionElement = cVariableListBox.getOptionElement(cVariableListBox.getIndex(value));
            setUpdate(Color.RED);
            eventBus.fireEventFromSource(new Correlation(false, false, false, true, false), optionElement);
        }
    };
    ValueChangeHandler<Boolean> analysisSwitchChange = new ValueChangeHandler<Boolean>() {
        @Override
        public void onValueChange(ValueChangeEvent<Boolean> event) {
            String over = overButton.getTitle();
            String type = analysisButton.getTitle();
            eventBus.fireEventFromSource(new AnalysisActive(type, over, event.getValue()), overButton);
        }
    };
    ValueChangeHandler<Boolean> colorByOnChange = new ValueChangeHandler<Boolean>() {
        @Override
        public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
            eventBus.fireEventFromSource(new FeatureModifiedEvent(0,0,0,0), colorByOn);
        }
    };
    @UiHandler("update")
    public void update(ClickEvent clickEvent) {
        eventBus.fireEventFromSource(clickEvent, update);
    }

    @UiHandler("plotsDropdown")
    void onDropDown(SelectionEvent<Widget> selection) {
        String title = ((MaterialLink)selection.getSelectedItem()).getText();
        int count = setPanels(title);
        eventBus.fireEventFromSource(new PanelCount(count), selection);
    }

    @UiHandler("formatsDropDown")
    void onFormatChange(SelectionEvent<Widget> event) {
        downloadLink.setDisplay(Display.NONE);
        String title = ((MaterialLink) event.getSelectedItem()).getText();
        formatsButton.setText(title);
    }
    @UiHandler("analysis")
    void onAnalysisDropDown(SelectionEvent<Widget> selection) {
        String title = ((MaterialLink)selection.getSelectedItem()).getText();
        analysisButton.setText(title);
        setUpdate(Color.RED);
        analysisSwitch.setValue(true);
        String over = overButton.getText();
        boolean active = analysisSwitch.getValue();
        eventBus.fireEventFromSource(new AnalysisActive(title, over, active), overButton);
    }

    @UiHandler("over")
    void onOverDropDown(SelectionEvent<Widget> selection) {
        String title = ((MaterialLink)selection.getSelectedItem()).getText();
        overButton.setText(title);
        setUpdate(Color.RED);
        analysisSwitch.setValue(true);
        String type = analysisButton.getText();
        boolean active = analysisSwitch.getValue();
        eventBus.fireEventFromSource(new AnalysisActive(type, title, active), overButton);
    }

    @UiHandler("correlationLink")
    public void onCorrelation(ClickEvent event) {
        correlationWindow.setLeft(30);
        correlationWindow.setTop(30);
        int w = Window.getClientWidth() - 60;
        int h = Window.getClientHeight() - 60;
        correlationWindow.setWidth(w+"px");
        correlationWindow.setHeight(h+"px");
        xVariableListBox.clear();
        yVariableListBox.clear();
        cVariableListBox.clear();
        variableConstraintListBox.clear();
        xVariableListBox.addValueChangeHandler(correlationChangeX);
        yVariableListBox.addValueChangeHandler(correlationChangeY);
        cVariableListBox.addValueChangeHandler(correlationChangeC);
        variableConstraintListBox.addValueChangeHandler(addAndDisable);
        for (int i = 0; i < datasets.getWidgetCount(); i++) {
            DataItem di = (DataItem) datasets.getWidget(i);
            Object s = di.getSelection();
            if ( s instanceof Variable ) {

                Variable v = (Variable)s;
                xVariableListBox.addItem(v.getName(), v.getTitle());;
                yVariableListBox.addItem(v.getName(), v.getTitle());
                cVariableListBox.addItem(v.getName(), v.getTitle());

                // Add all variables to the menu to add a varible constraint, but disable
                // variables in the plot

                variableConstraintListBox.addItem(v.getName(), v.getTitle());

                // TODO, need an event to set the defaults from config
                if ( i == 0 ) {
                    ySelectedVariable = v;
                    xSelectedVariable = v;
                    cSelectedVariable = v;
                    xVariableListBox.setSelectedIndex(i);
                    yVariableListBox.setSelectedIndex(i);
                    cVariableListBox.setSelectedIndex(i);
                    OptionElement o = variableConstraintListBox.getOptionElement(i);
                    o.setDisabled(true);
                } else if ( i == 1 ) {
                    xSelectedVariable = v;
                    cSelectedVariable = v;
                    xVariableListBox.setSelectedIndex(i);
                    cVariableListBox.setSelectedIndex(i);
                    OptionElement o = variableConstraintListBox.getOptionElement(i);
                    o.setDisabled(true);
                } else if ( i == 2 ) {
                    cSelectedVariable = v;
                    cVariableListBox.setSelectedIndex(i);
                }
            }
        }
        eventBus.fireEventFromSource(new Correlation(true, false, false, false, false), correlationLink);
        correlationWindow.open();
    }
    @UiHandler("correlationUpdate")
    public void onCorrelationUpdate(ClickEvent event) {
        eventBus.fireEventFromSource(new Correlation(false, false, false, false, false), correlationLink);
    }
    @UiHandler("saveAsButton")
    public void onSaveAs(ClickEvent event) {
        downloadDatasets.clear();
        downloadLoader.setDisplay(Display.NONE);
        downloadLink.setDisplay(Display.NONE);
        downloadError.setDisplay(Display.NONE);
        downloadLink.setHref("");
        downloadWindow.open();
        for (int i = 0; i < datasets.getWidgetCount(); i++) {
            DataItem d = (DataItem) datasets.getWidget(i);
            DataItem dd = new DataItem(d.getSelection());
            if ( d.getRadioSelected() ) {
                dd.setRadioSelected();
            }
            dd.toCheck();
            downloadDatasets.add(dd);
        }
        eventBus.fireEventFromSource(new Download(true), saveAsButton);
    }

    @UiHandler("animateSubmit")
    public void onAnimateSubmit(ClickEvent event) {
        MaterialButton button = (MaterialButton) event.getSource();
        if ( button.getText().equals("Submit") ) {
            animateProgress.setDisplay(Display.BLOCK);
            eventBus.fireEvent(new AnimateAction(false, false, true));
        } else {
            animateProgress.setDisplay(Display.NONE);
            eventBus.fireEvent(new AnimateAction(true, false, false));
        }
    }

    @UiHandler("downloadButton")
    public void onDownload(ClickEvent event) {
        downloadLoader.setDisplay(Display.BLOCK);
        eventBus.fireEventFromSource(new Download(true), downloadButton);
    }
    @UiHandler("animate")
    public void onAnimate(ClickEvent event) {

        animateWindow.setLayoutPosition(Style.Position.ABSOLUTE);
        animateWindow.setLeft(60);
        animateWindow.setTop(60);
        int w = Window.getClientWidth() - 120;
        int h = Window.getClientHeight() - 120;
        animateWindow.setWidth(w+"px");
        animateWindow.setHeight(h+"px");
        animationControls.setDisplay(Display.NONE);
        submitPanel.setDisplay(Display.BLOCK);
        panel5.setImage("images/animation_arrow.png");
        eventBus.fireEventFromSource(new AnimateAction(false, true, false), animate);
        animateWindow.open();
    }

    @UiHandler("showValuesButton")
    public void onShowValues(ClickEvent event) {
        showValuesWindow.setLayoutPosition(Style.Position.ABSOLUTE);
        showValuesWindow.setLeft(60);
        showValuesWindow.setTop(60);
        int w = Window.getClientWidth() - 120;
        int h = Window.getClientHeight() - 120;
        showValuesWindow.setWidth(w+"px");
        showValuesWindow.setHeight(h+"px");
        showValuesWindow.open();
        eventBus.fireEventFromSource(new ShowValues(), showValuesButton);
    }

    @UiHandler("home")
    public void onHome(ClickEvent event) {
        eventBus.fireEventFromSource(new BreadcrumbSelect(), home);
        event.stopPropagation();
    }
    @UiHandler("back")
    public void onBack(ClickEvent event) {
        if ( getBreadcrumbCount(1) > 0 ) {
            panel1.getBreadcrumbContainer().remove(panel1.getBreadcrumbContainer().getWidgetCount()-1);
        }
        if ( getBreadcrumbCount(1) > 0 ) {
            Breadcrumb bc = (Breadcrumb) panel1.getBreadcrumbContainer().getWidget(panel1.getBreadcrumbContainer().getWidgetCount()-1);
            eventBus.fireEventFromSource(new BreadcrumbSelect(bc.getSelected(), 1), bc);
            event.stopPropagation();
        } else {
            eventBus.fireEventFromSource(new BreadcrumbSelect(), home);
            event.stopPropagation();
        }
    }
    @UiHandler("next")
    public void onNext(ClickEvent event) {
        eventBus.fireEventFromSource(new MoveAnimation(1), next);
    }
    @UiHandler("prev")
    public void onPrev(ClickEvent event) {
        eventBus.fireEventFromSource(new MoveAnimation(-1), prev);
    }
    @UiHandler("stop")
    public void onStop(ClickEvent event) {
        if ( stop.getText().equals("Stop") ) {
            eventBus.fireEventFromSource(new AnimationSpeed(0), stop);
            prevnext.setDisplay(Display.BLOCK);
            stop.setText("Go");
        } else { // The button said go
            eventBus.fireEventFromSource(new AnimationSpeed(flipSpeed.getValue()), stop);
            prevnext.setDisplay(Display.NONE);
            stop.setText("Stop");
        }
    }
    @UiHandler("flipSpeed")
    public void onFaster(ChangeEvent event) {
        String secs = seconds.format(Double.valueOf(flipSpeed.getValue()).doubleValue()/1000.d);
        speedLabel.setText(secs+" seconds between images.");
        eventBus.fireEventFromSource(new AnimationSpeed(flipSpeed.getValue()), flipSpeed);
    }

}