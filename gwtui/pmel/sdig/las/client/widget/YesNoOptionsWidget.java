package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialPanel;
import pmel.sdig.las.shared.autobean.YesNoOption;

import java.util.List;

public class YesNoOptionsWidget extends Composite {
    @UiField
    MaterialPanel yesnooptions;
    interface YesNoOptionsWidgetUiBinder extends UiBinder<MaterialPanel, YesNoOptionsWidget> {
    }

    private static YesNoOptionsWidgetUiBinder ourUiBinder = GWT.create(YesNoOptionsWidgetUiBinder.class);

    public YesNoOptionsWidget() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
    public YesNoOptionsWidget(List<YesNoOption> options) {
        initWidget(ourUiBinder.createAndBindUi(this));
        for (int i = 0; i < options.size(); i++) {
            YesNoOption yn = options.get(i);
            YesNoOptionItem yno = new YesNoOptionItem(yn);
            yesnooptions.add(yno);
        }
    }
}