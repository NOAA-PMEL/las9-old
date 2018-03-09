package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.ui.MaterialCollection;
import gwt.material.design.client.ui.MaterialContainer;
import pmel.sdig.las.shared.autobean.MenuOption;
import pmel.sdig.las.shared.autobean.RequestProperty;

import java.util.ArrayList;
import java.util.List;

public class MenuOptionsWidget extends Composite {
    @UiField
    MaterialContainer menuoptions;

    interface MenuOptionsWidgetUiBinder extends UiBinder<MaterialContainer, MenuOptionsWidget> {
    }

    private static MenuOptionsWidgetUiBinder ourUiBinder = GWT.create(MenuOptionsWidgetUiBinder.class);

    public MenuOptionsWidget() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
    public MenuOptionsWidget(List<MenuOption> options) {
        initWidget(ourUiBinder.createAndBindUi(this));
        for (int i = 0; i < options.size(); i++) {
            MenuOption option = options.get(i);
            MenuOptionItem item = new MenuOptionItem(option);
            menuoptions.add(item);
        }
    }
    public List<RequestProperty> getOptions() {
        List<RequestProperty> olist = new ArrayList<>();
        for ( int i = 0; i < menuoptions.getChildrenList().size(); i++ ) {
            MenuOptionItem moi = (MenuOptionItem) menuoptions.getChildrenList().get(i);
            MenuOption option = moi.getOption();
            String value = moi.getSelectedValue();
            String defaultValue = option.getDefaultValue();
            if ( !value.equals(defaultValue) ) {
                RequestProperty rp = new RequestProperty();
                rp.setType("ferret");
                rp.setName(option.getName());
                rp.setValue(value);
                olist.add(rp);
            }
        }
        return olist;
    }
}