package pmel.sdig.las.client.state;

import com.google.gwt.core.client.GWT;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import pmel.sdig.las.client.util.Constants;
import pmel.sdig.las.shared.autobean.*;

/**
 * Created by rhs on 6/2/16.
 */
public class State {

    // MyAutoBeanFactory beanFactory = GWT.create(MyAutoBeanFactory.class);

    int panelCount = 1;

    PanelState panelState01 = new PanelState();
    PanelState panelState02 = new PanelState();

    public int getPanelCount() {
        return panelCount;
    }

    public void setPanelCount(int panelCount) {
        this.panelCount = panelCount;
    }

    public PanelState getPanelState(String title) {
        if ( title.equals(Constants.PANEL01) ) {
            return panelState01;
        } else if ( title.equals(Constants.PANEL02) ) {
            return panelState02;
        }
        return null;
    }
    public PanelState getPanelState(int i) {
        String title = "panel0"+i;
        return getPanelState(title);
    }

}

