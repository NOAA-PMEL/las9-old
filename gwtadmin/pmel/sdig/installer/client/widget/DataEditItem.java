package pmel.sdig.installer.client.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.IconPosition;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.ui.MaterialCollectionItem;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialPanel;
import pmel.sdig.las.client.event.Info;
import pmel.sdig.las.shared.autobean.Dataset;

public class DataEditItem extends MaterialCollectionItem {
    MaterialLink link = new MaterialLink();
    MaterialIcon badge = new MaterialIcon(IconType.EDIT);
    MaterialPanel wrapper = new MaterialPanel();
    public DataEditItem(Dataset dataset, boolean edit) {
        wrapper.addStyleName("valign-wrapper");
        link.setMarginLeft(8);
        link.setDisplay(Display.FLEX);
        link.setText(dataset.getTitle());
        badge.setIconPosition(IconPosition.RIGHT);
        if (edit) {
            badge.setDisplay(Display.FLEX);
        } else {
            badge.setDisplay(Display.NONE);
        }
        badge.setMarginRight(4);
        badge.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
//                eventBus.fireEventFromSource(new EditDataset(dataset.getId()), badge);
            }
        });
        wrapper.add(link);
        wrapper.add(badge);
    }
}
