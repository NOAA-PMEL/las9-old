package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.IconPosition;
import gwt.material.design.client.constants.IconSize;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.ui.MaterialBadge;
import gwt.material.design.client.ui.MaterialCollectionItem;
import gwt.material.design.client.ui.MaterialCollectionSecondary;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialRow;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.Variable;

/**
 * Created by rhs on 12/30/16.
 */
public class DataItem extends MaterialCollectionItem {
    Object selection;
    MaterialLink link = new MaterialLink();
    MaterialBadge badge = new MaterialBadge("?", Color.WHITE, Color.BLUE);
    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();
        public DataItem(Object selection) {
        super();
        this.selection = selection;
        badge.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Window.alert("Open the data set information page now!");
                event.stopPropagation();
            }
        });
        if ( selection instanceof Dataset ) {
            Dataset d = (Dataset) selection;
            badge.setCircle(false);
            link.add(badge);
            link.setText(d.getTitle());
            add(link);
        } else if ( selection instanceof Variable) {
            Variable v = (Variable) selection;
            link.setText(v.getTitle());
            add(link);
        }
        addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                eventBus.fireEventFromSource(new NavSelect(selection, 0), selection);
            }
        });
        link.addStyleName("datasetwrap");
        link.setWidth("250px");
        link.setPaddingTop(15);
    }
}
