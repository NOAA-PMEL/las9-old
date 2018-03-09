package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import pmel.sdig.las.client.main.ClientFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rhs on 1/21/17.
 */
public class ButtonDropDown extends Composite {

//    @UiField
//    Button button;
//    @UiField
//    DropDownMenu menu;

    Map<String, String> values = new HashMap<String, String>();
    String currentValue;

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    interface ButtonDropDownUiBinder extends UiBinder<HTMLPanel, ButtonDropDown> {
    }

    private static ButtonDropDownUiBinder ourUiBinder = GWT.create(ButtonDropDownUiBinder.class);

    public ButtonDropDown() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
    public void init(String initialValue, List<String> labels, List<String> inputValues) {
//        button.setText(initialValue);
//        for ( int i = 0; i < labels.size(); i++ ) {
//            String label = labels.get(i);
//            values.put(label, inputValues.get(i));
//            final AnchorListItem a = new AnchorListItem(label);
//            a.addClickHandler(new ClickHandler() {
//                @Override
//                public void onClick(ClickEvent event) {
//                    String label = a.getText();
//                    button.setText(label);
//                    currentValue = values.get(label);
//                    final String v = currentValue;
//                    eventBus.fireEventFromSource(new ButtonDropDownSelect(v), ButtonDropDown.this);
//                }
//            });
//            menu.add(a);
//        }
    }

    public String getValue() {
        return currentValue;
    }
}