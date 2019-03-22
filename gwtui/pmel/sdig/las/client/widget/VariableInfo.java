package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.ui.MaterialCardTitle;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialImage;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.util.Util;
import pmel.sdig.las.shared.autobean.GeoAxisX;
import pmel.sdig.las.shared.autobean.GeoAxisY;
import pmel.sdig.las.shared.autobean.TimeAxis;
import pmel.sdig.las.shared.autobean.Variable;
import pmel.sdig.las.shared.autobean.VerticalAxis;

public class VariableInfo extends Composite {

    @UiField
    MaterialCardTitle title;
    @UiField
    MaterialIcon titleIcon;

    @UiField
    MaterialImage thumbnail;

    @UiField
    MaterialLabel lonMin;
    @UiField
    MaterialLabel lonMax;

    @UiField
    MaterialLabel latMin;
    @UiField
    MaterialLabel latMax;

    @UiField
    MaterialLabel timeStart;
    @UiField
    MaterialLabel timeEnd;

    @UiField
    MaterialLabel zMin;
    @UiField
    MaterialLabel zMax;

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    interface VariableInfoUiBinder extends UiBinder<MaterialColumn, VariableInfo> {
    }

    private static VariableInfoUiBinder ourUiBinder = GWT.create(VariableInfoUiBinder.class);

    private void init() {
        MaterialColumn rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
    }
    public VariableInfo() {
        init();
    }
    public VariableInfo(String dhash, Variable variable) {
        init();
        title.setText(variable.getTitle());
        thumbnail.setUrl("product/thumbnail/"+dhash+"/"+variable.getHash());
        GeoAxisX x = variable.getGeoAxisX();
        GeoAxisY y = variable.getGeoAxisY();
        VerticalAxis z = variable.getVerticalAxis();
        TimeAxis t = variable.getTimeAxis();
        if ( x != null ) {
            lonMin.setText(Util.format_four(x.getMin()));
            lonMax.setText(Util.format_four(x.getMax()));
        }
        if ( y != null ) {
            latMin.setText(Util.format_four(y.getMin()));
            latMax.setText(Util.format_four(y.getMax()));
        }
        if ( z != null ) {
            zMin.setText(Util.format_four(z.getMin()));
        } else {
            zMin.setText("n/a");
        }
        if ( t != null ) {
            timeEnd.setText(t.getEnd());
            timeStart.setText(t.getStart());
        } else {
            timeStart.setText("n/a");
        }
        titleIcon.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new NavSelect(variable, 1), VariableInfo.this);
            }
        });
        title.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new NavSelect(variable, 1), VariableInfo.this);
            }
        });
        thumbnail.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new NavSelect(variable, 1), VariableInfo.this);
            }
        });
    }
}