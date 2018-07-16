package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import pmel.sdig.las.client.event.BreadcrumbSelect;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.shared.autobean.Annotation;
import pmel.sdig.las.shared.autobean.AnnotationGroup;

import java.util.Iterator;
import java.util.List;

/**
 * Created by rhs on 1/6/17.
 */
public class AnnotationsPanel extends Composite {

    @UiField
    Breadcrumb home;

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    interface AnnotationPanelUiBinder extends UiBinder<HTMLPanel, AnnotationsPanel> {
    }

    private static AnnotationPanelUiBinder ourUiBinder = GWT.create(AnnotationPanelUiBinder.class);

    public AnnotationsPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiHandler("home")
    public void home(ClickEvent click) {
        eventBus.fireEventFromSource(new BreadcrumbSelect(null, 1), AnnotationsPanel.this);
    }
}