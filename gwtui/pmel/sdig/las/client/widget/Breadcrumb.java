package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.constants.ChipType;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.FlexWrap;
import gwt.material.design.client.constants.IconPosition;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.ui.MaterialChip;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.html.Div;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.Variable;

/**
 * Created by rhs on 9/15/15.
 */
public class Breadcrumb extends MaterialPanel {

    String type;  // Should be one of BreadcrumbType

    Object selected;

    MaterialChip chip = new MaterialChip();

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();


    public Breadcrumb () {
        super();
        chip.setDisplay(Display.INLINE);
        chip.setOverflow(Style.Overflow.HIDDEN);
        chip.addStyleName("LAS-chip");
        add(chip);
    }
    public Breadcrumb(String text, IconType iconType) {
        setText(text);
        setTitle(text);
        setIconType(iconType);
        chip.setDisplay(Display.INLINE);
        chip.setOverflow(Style.Overflow.HIDDEN);
        chip.addStyleName("LAS-chip");
        add(chip);
    }
    public Breadcrumb(Object selected, boolean carret) {
        this.selected = selected;
        if ( selected instanceof Dataset ) {
            Dataset dataset = (Dataset) selected;
            if ( carret ) {
                chip.setText(" > " + dataset.getTitle());
            } else {
                chip.setText(dataset.getTitle());
            }
            chip.setTitle(dataset.getTitle());
            type = BreadcrumbType.DATASET;
        } else if ( selected instanceof Variable ) {
            Variable variable = (Variable) selected;
            chip.setText(" > " + variable.getTitle());
            chip.setTitle(variable.getTitle());
            type = BreadcrumbType.VARIABLE;
        }
        chip.addStyleName("LAS-chip");
        chip.setDisplay(Display.INLINE);
        chip.setOverflow(Style.Overflow.HIDDEN);
        add(chip);
    }

    public void setIconType(IconType type) {
        chip.setIconType(type);
    }
    public void setIconType(String type) {
        chip.setIconType(IconType.HOME);
    }
    public void setText(String text) {
        chip.setText(text);
    }

    public Object getSelected() {
        return selected;
    }

    public void setSelected(Object selected) {
        this.selected = selected;
    }
}
