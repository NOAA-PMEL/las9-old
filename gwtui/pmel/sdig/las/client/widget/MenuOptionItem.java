package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import gwt.material.design.addins.client.bubble.MaterialBubble;
import gwt.material.design.client.constants.Position;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialCollectionItem;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialDropDown;
import gwt.material.design.client.ui.MaterialHelpBlock;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialListBox;
import gwt.material.design.client.ui.MaterialModal;
import gwt.material.design.client.ui.MaterialRow;
import gwt.material.design.client.ui.MaterialTitle;
import gwt.material.design.client.ui.MaterialToast;
import gwt.material.design.client.ui.html.Heading;
import gwt.material.design.client.ui.html.Option;
import pmel.sdig.las.client.event.PlotOptionChange;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.MenuItem;
import pmel.sdig.las.shared.autobean.MenuOption;

import java.util.List;

public class MenuOptionItem extends Composite {

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    @UiField
    MaterialListBox menu;
    @UiField
    MaterialLink title;
    @UiField
    MaterialColumn help;

    MenuOption option;

    HTML helph;

    interface MenuOptionItemUiBinder extends UiBinder<MaterialContainer, MenuOptionItem> {
    }

    private static MenuOptionItemUiBinder ourUiBinder = GWT.create(MenuOptionItemUiBinder.class);

    public MenuOptionItem() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }
    public MenuOptionItem(MenuOption option) {
        this.option = option;
        initWidget(ourUiBinder.createAndBindUi(this));
        title.setText(option.getTitle());
        List<MenuItem> menuList = option.getMenuItems();
        for (int i = 0; i < menuList.size(); i++) {
            MenuItem item = menuList.get(i);
            Option menuLink = new Option();
            menuLink.setText(item.getTitle());
            menuLink.setValue(item.getValue());
            menu.add(menuLink);
        }

        helph = new HTML(option.getHelp());

        help.add(helph);
        menu.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                eventBus.fireEventFromSource(new PlotOptionChange(), MenuOptionItem.this);
            }
        });

    }
    public MenuOption getOption() {
        return option;
    }
    public String getSelectedValue() {
        return menu.getSelectedValue();
    }
}