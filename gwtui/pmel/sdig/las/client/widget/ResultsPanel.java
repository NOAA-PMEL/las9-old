package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialCollapsible;
import gwt.material.design.client.ui.MaterialCollapsibleBody;
import gwt.material.design.client.ui.MaterialCollapsibleHeader;
import gwt.material.design.client.ui.MaterialCollapsibleItem;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialRow;
import gwt.material.design.client.ui.MaterialTab;
import gwt.material.design.client.ui.html.Div;
import pmel.sdig.las.client.event.BreadcrumbSelect;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.map.OLMapWidget;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.shared.autobean.Annotation;
import pmel.sdig.las.shared.autobean.AnnotationGroup;
import pmel.sdig.las.shared.autobean.LASRequest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by rhs on 1/6/17.
 */
public class ResultsPanel extends Composite {

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



    interface ResultsPanelUiBinder extends UiBinder<MaterialColumn, ResultsPanel> {
    }

    private static ResultsPanelUiBinder ourUiBinder = GWT.create(ResultsPanelUiBinder.class);

    public ResultsPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
//        eventBus.addHandler(ClickEvent.getType(), new ClickHandler() {
//
//            @Override
//            public void onClick(ClickEvent event) {
//                if ( event.getSource() instanceof Button ) {
//                    if ( ((Button) event.getSource()).getTitle().equals("Hide/Show Annotations")) {
//                        Button at = (Button) event.getSource();
//                        if ( at.getIcon().equals(IconType.CARET_DOWN) ) {
//                            at.setIcon(IconType.CARET_RIGHT);
//                            annotationsPanel.setVisible(false);
//                        } else if (at.getIcon().equals(IconType.CARET_RIGHT) ) {
//                            at.setIcon(IconType.CARET_DOWN);
//                            annotationsPanel.setVisible(true);
//                        }
//                    }
//                }
//            }
//        });
    }

    public void clearAnnotations() {
        annotationPanel.clear();
    }
    public void addAnnotation(Widget w) {
        annotationPanel.add(w);
    }
    public void setState(State state) {
        outputPanel.setState(state);
        List<AnnotationGroup> groups = state.getPanelState(1).getProductResults().getAnnotationGroups();
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
        breadcrumbs.add(b);
    }

    public void removeBreadcrumb(Breadcrumb tail) {
        breadcrumbs.remove(tail);
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



    public void setLayoutPosition(Style.Position postion) {
        panel.setLayoutPosition(postion);
    }
    public void setLeft(double left) {
        panel.setLeft(left);
    }
    public void setTop(double top) {
        panel.setTop(top);
    }
}