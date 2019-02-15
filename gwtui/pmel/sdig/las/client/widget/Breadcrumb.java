package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.html.Div;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.Variable;

/**
 * Created by rhs on 9/15/15.
 */
public class Breadcrumb extends Div {

    String type;  // Should be one of BreadcrumbType

    Object selected;

    MaterialLink link = new MaterialLink();
    MaterialIcon icon = new MaterialIcon();

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();


    public Breadcrumb () {
        super();
        add(icon);
        add(link);
        setDisplay(Display.INLINE);
    }
    public Breadcrumb(String text, IconType iconType) {
        link.setText(text);
        link.setTitle(text);
        icon.setIconType(iconType);
        add(link);
        add(icon);
        setDisplay(Display.INLINE);
    }
    public Breadcrumb(Object selected, boolean carret) {
        this.selected = selected;
        if ( selected instanceof Dataset ) {
            Dataset dataset = (Dataset) selected;
            if ( carret ) {
                link.setText(" > " + dataset.getTitle());
            } else {
                link.setText(dataset.getTitle());
            }
            link.setTitle(dataset.getTitle());
            type = BreadcrumbType.DATASET;
        } else if ( selected instanceof Variable ) {
            Variable variable = (Variable) selected;
            link.setText(" > " + variable.getTitle());
            link.setTitle(variable.getTitle());
            type = BreadcrumbType.VARIABLE;
        }
        add(link);
        add(icon);
        setDisplay(Display.INLINE);
    }

    public void setText(String text) {
        link.setText(text);
    }

    public void setIconType(IconType iconType) {
        icon.setIconType(iconType);
    }
    public Object getSelected() {
        return selected;
    }

    public void setSelected(Object selected) {
        this.selected = selected;
    }
}
