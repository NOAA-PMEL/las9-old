package pmel.sdig.installer.client.main;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;

public class Admin implements EntryPoint {
    AdminLayout layout = new AdminLayout();
    public void onModuleLoad() {
        RootPanel.get("load").getElement().setInnerHTML("");
        RootPanel.get("las_admin").add(layout);
    }
}
