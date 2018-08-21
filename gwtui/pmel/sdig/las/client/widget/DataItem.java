package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.constants.CollectionType;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.IconPosition;
import gwt.material.design.client.constants.IconSize;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.ui.MaterialBadge;
import gwt.material.design.client.ui.MaterialCheckBox;
import gwt.material.design.client.ui.MaterialCollectionItem;
import gwt.material.design.client.ui.MaterialCollectionSecondary;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialRadioButton;
import gwt.material.design.client.ui.MaterialRow;
import pmel.sdig.las.client.event.AddVariable;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.util.Constants;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.Variable;

/**
 * Created by rhs on 12/30/16.
 */
public class DataItem extends MaterialCollectionItem {
    Object selection;

    // Data sets are links.
    MaterialLink link = new MaterialLink();
    MaterialIcon badge = new MaterialIcon(IconType.INFO, Color.BLUE, Color.WHITE);

    // Variables are radio buttons until the plot type allows multiple selections
    MaterialRadioButton radio = new MaterialRadioButton();

    // Then variables turn into check boxes
    MaterialCheckBox check = new MaterialCheckBox();

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
            badge.setFloat(Style.Float.RIGHT);
            link.setVerticalAlign(Style.VerticalAlign.MIDDLE);
            link.add(badge);
            link.setText(d.getTitle());
            link.setTextColor(Color.BLUE);
            add(link);

            link.setPaddingTop(16);
            link.setVerticalAlign(Style.VerticalAlign.MIDDLE);
            link.setMargin(2);
            link.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    eventBus.fireEventFromSource(new NavSelect(selection, 1), selection);
                }
            });
            setMarginRight(4);

        } else if ( selection instanceof Variable) {
            Variable v = (Variable) selection;
            check.setText(v.getTitle());
            check.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                    if ( valueChangeEvent.getValue() ) {
                        eventBus.fireEventFromSource(new AddVariable((Variable) selection, 1, true), selection);
                    } else {
                        eventBus.fireEventFromSource(new AddVariable((Variable) selection, 1, false), selection);
                    }
                }
            });
            radio.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
                    if ( valueChangeEvent.getValue() ) {
                        eventBus.fireEventFromSource(new NavSelect(selection, 1), selection);
                    }
                }
            });

            radio.setName("variable");
            radio.setText(v.getTitle());
            add(radio);
        }
    }
    public void toCheck() {
        remove(radio);
        if ( radio.getValue() ) {
            check.setValue(true);
        }
        add(check);
        radio.setValue(false);
    }
    public void toRadio() {
        remove(check);
        if ( check.getValue() ) {
            radio.setValue(true);
        }
        check.setValue(false);
        add(radio);
    }
    public void setRadioSelected() {
        radio.setValue(true);
    }
    public boolean isChecked() {
        return check.getValue();
    }
    public String getTitle() {
        if ( selection instanceof Dataset ) {
            Dataset d = (Dataset) selection;
            return d.getTitle();
        } else {
            Variable v = (Variable) selection;
            return v.getTitle();
        }
    }
    public void addNavSelectClickHandler() {
//        addClickHandler(new ClickHandler() {
//            @Override
//            public void onClick(ClickEvent event) {
//                eventBus.fireEventFromSource(new NavSelect(selection, 1), selection);
//            }
//        });
    }
    public Object getSelection() {
        return selection;
    }
    public boolean getRadioSelected() {
        return radio.getValue();
    }
}
