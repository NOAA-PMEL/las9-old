package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.ui.MaterialCardTitle;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialRow;
import pmel.sdig.las.client.event.Browse;
import pmel.sdig.las.client.event.Info;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.Variable;

public class DatasetInfo extends Composite {

    int count = 0;

    @UiField
    MaterialPanel rows;

    @UiField
    MaterialCardTitle title;
    @UiField
    MaterialIcon titleIcon;

    @UiField
    MaterialLink prev10;
    @UiField
    MaterialLink prev;
    @UiField
    MaterialLink next;
    @UiField
    MaterialLink next10;
    @UiField
    MaterialRow navRow;

    MaterialRow currentRow;

    String dhash;

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    interface DatasetInfoUiBinder extends UiBinder<MaterialPanel, DatasetInfo> {
    }

    private static DatasetInfoUiBinder ourUiBinder = GWT.create(DatasetInfoUiBinder.class);

    private void init() {
        MaterialPanel rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
    }
    public DatasetInfo() {
        init();
    }
    public DatasetInfo(Dataset dataset) {
        init();
        dhash = dataset.getHash();
        title.setText(dataset.getTitle());
        title.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new NavSelect(dataset, 1), title);
            }
        });
        titleIcon.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new NavSelect(dataset, 1), title);
            }
        });
    }

    public void clear() {
        count = 0;
        rows.clear();
    }

    public void addVariable(Variable variable) {
        int position = count%4;
        if ( position == 0 ) {
            MaterialRow row = new MaterialRow();
            currentRow = row;
            rows.add(row);
        }
        VariableInfo info = new VariableInfo(dhash, variable);
        currentRow.add(info);
        count++;
    }
    public void showNav(int offset, long nextID, long prevID) {
        navRow.setDisplay(Display.INLINE_BLOCK);
        int offset_prev = offset - 10;
        int offset_next = offset + 10;
        next.setVisible(false);
        next10.setVisible(false);
        prev.setVisible(false);
        prev10.setVisible(false);
        if (offset >= 10) {
            prev10.setVisible(true);
            prev10.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    eventBus.fireEventFromSource(new Browse(offset_prev), prev10);
                }
            });
        }
        if (nextID > 0) {
            next.setVisible(true);
            next.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    eventBus.fireEventFromSource(new Info(nextID), next);
                }
            });
        }
        if ( prevID > 0 ) {
            prev.setVisible(true);
            prev.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    eventBus.fireEventFromSource(new Info(prevID), prev);
                }
            });
        }
        next10.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new Browse(offset_next), next10);
            }
        });
        next10.setVisible(true);
    }
}