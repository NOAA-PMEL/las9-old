package pmel.sdig.las.client.main;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.addins.client.window.MaterialWindow;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.HeadingSize;
import gwt.material.design.client.constants.ProgressType;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialCardContent;
import gwt.material.design.client.ui.MaterialCollapsibleBody;
import gwt.material.design.client.ui.MaterialCollapsibleItem;
import gwt.material.design.client.ui.MaterialCollection;
import gwt.material.design.client.ui.MaterialCollectionItem;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialDropDown;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialLoader;
import gwt.material.design.client.ui.MaterialNavBar;
import gwt.material.design.client.ui.MaterialNavBrand;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialProgress;
import gwt.material.design.client.ui.MaterialRow;
import gwt.material.design.client.ui.html.Div;
import gwt.material.design.client.ui.html.Heading;
import pmel.sdig.las.client.event.BreadcrumbSelect;
import pmel.sdig.las.client.event.PanelCount;
import pmel.sdig.las.client.map.OLMapWidget;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.client.widget.AxisWidget;
import pmel.sdig.las.client.widget.Breadcrumb;
import pmel.sdig.las.client.widget.DataItem;
import pmel.sdig.las.client.widget.DateTimeWidget;
import pmel.sdig.las.client.widget.MenuOptionsWidget;
import pmel.sdig.las.client.widget.ProductButton;
import pmel.sdig.las.client.widget.ProductButtonList;
import pmel.sdig.las.client.widget.ResultsPanel;
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
    ResultsPanel panel1;
    @UiField
    MaterialColumn panel2;
    @UiField
    MaterialColumn panel3;
    @UiField
    MaterialColumn panel4;
    @UiField
    MaterialNavBar navbar;
    @UiField
    MaterialPanel mapPanel;
    @UiField
    MaterialCollapsibleItem dataItem;

    @UiField
    MaterialPanel dateTimePanel;
    @UiField
    MaterialPanel zaxisPanel;

    @UiField
    Breadcrumb home;

    @UiField
    MaterialIcon back;

    @UiField
    MaterialPanel products;
    @UiField
    MaterialPanel options;

    @UiField
    MaterialWindow controlsWindow;

    Widget root;

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    boolean menuopen = true;

    int kern = 15;

    interface LayoutUiBinder extends UiBinder<Widget, Layout> {     }
    private static LayoutUiBinder ourUiBinder = GWT.create(LayoutUiBinder.class);

    public Layout() {
        root = ourUiBinder.createAndBindUi(this);
        initWidget(root);
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
//    public void showDateTime() {
//        dateTime.setVisible(true);
//    }
    public void showVertialAxis() {
        zaxisPanel.setVisible(true);
    }
    public void hideVerticalAxis() {
        zaxisPanel.setVisible(false);
    }
//
//    public void hideDateTime() {
//        dateTime.setVisible(false);
//    }
//
    public void addSelection(Object selection) {

        DataItem dataItem = new DataItem(selection);
        datasets.add(dataItem);

    }
//
    public void addBreadcrumb(Breadcrumb breadcrumb, int panel) {
        // TODO this is probably right, but confusing. resultsPanel.getBreadcrumbTail(); ??
        int index = panel1.getBreadcrumbs().size();
        if ( index > 0 ) {
            Breadcrumb tail = (Breadcrumb) panel1.getBreadcrumbs().get(index - 1);
            Object tailObject = tail.getSelected();
            if ( tailObject instanceof Variable) {
                panel1.removeBreadcrumb(tail);
                panel1.addBreadcrumb(breadcrumb);
            } else {
                panel1.addBreadcrumb(breadcrumb);
            }
        } else {
            panel1.addBreadcrumb(breadcrumb);
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
        } else { // Other panels
            return 0;
        }
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
    public void addProducts(ProductButtonList productButtonList) {
        products.clear();
        products.add(productButtonList);
    }

    // For debugging for now
    public void clearProducts() {
        products.clear();
    }
    public void addProductButton(MaterialRow pb) {
        products.add(pb);
    }
    public void setUpdate(Color color) {
        update.setBackgroundColor(color);
    }
    public Dataset getDataset(int panel) {
        Dataset d = null;
        // Return the last one...
        List<Breadcrumb> crumbs = panel1.getBreadcrumbs();
        for (int i = 0; i < crumbs.size(); i++ ) {
            Breadcrumb crumb = (Breadcrumb) crumbs.get(i);
            if ( crumb.getSelected() instanceof Dataset) {
                d = (Dataset) crumb.getSelected();
            }
        }
        return d;
    }
    public String getVariableHash(int panel) {
        String hash = null;
        // Return the last one...
        List<Breadcrumb> crumbs = panel1.getBreadcrumbs();
        for (int i = 0; i < crumbs.size(); i++ ) {
            Breadcrumb crumb = (Breadcrumb) crumbs.get(i);
            if ( crumb.getSelected() instanceof Variable) {
                Variable d = (Variable) crumb.getSelected();
                hash = d.getHash();
            }
        }
        return hash;
    }
    public void setState(int panel, State state) {
        panel1.setState(state);
//        int main_height = Window.getClientHeight() - 100;
//        output_column_01.setHeight(main_height+"px");
    }
    public void addMouse(int panel, UI.Mouse mouse) {
        panel1.getOutputPanel().addMouse(mouse);
    }
    public List<RequestProperty> getPlotOptions() {
        List<RequestProperty> properties = new ArrayList<>();
        List<Widget> ow = options.getChildrenList();
        for (int i = 0; i < ow.size(); i++) {
            Widget w = ow.get(i);
            if ( w instanceof MenuOptionsWidget ) {
                MenuOptionsWidget mo = (MenuOptionsWidget) w;
                properties.addAll(mo.getOptions());
            }
        }
        return properties;
    }
    @UiHandler("update")
    public void update(ClickEvent clickEvent) {
        eventBus.fireEventFromSource(clickEvent, update);
    }


    @UiHandler("plotsDropdown")
    void onDropDown(SelectionEvent<Widget> selection) {
        String title = ((MaterialLink)selection.getSelectedItem()).getText();
        plotsButton.setText(title);
        int count = 1;
        if ( title.contains("1") ) {
            count = 1;
            panel1.setGrid("s10 m10 l10");
            panel2.setVisibility(Style.Visibility.HIDDEN);
            panel3.setVisibility(Style.Visibility.HIDDEN);
            panel4.setVisibility(Style.Visibility.HIDDEN);
        } else if ( title.contains("2") ) {
            count = 2;
            panel1.setGrid("s5 m5 l5");
            panel2.setVisibility(Style.Visibility.VISIBLE);
            panel3.setVisibility(Style.Visibility.HIDDEN);
            panel4.setVisibility(Style.Visibility.HIDDEN);
        } else if ( title.contains("4") ) {
            count = 4;
            panel1.setGrid("s5 m5 l5");
            panel2.setVisibility(Style.Visibility.VISIBLE);
            panel3.setVisibility(Style.Visibility.VISIBLE);
            panel4.setVisibility(Style.Visibility.VISIBLE);
        }

        eventBus.fireEventFromSource(new PanelCount(count), selection);
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
}