package pmel.sdig.installer.client.main;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class AdminLayout extends Composite {
    interface AdminLayoutUiBinder extends UiBinder<Widget, AdminLayout> {
    }

    private static AdminLayoutUiBinder ourUiBinder = GWT.create(AdminLayoutUiBinder.class);

    Widget rootElement;
    public AdminLayout() {
        rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
    }
}
