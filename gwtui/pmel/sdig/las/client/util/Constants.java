package pmel.sdig.las.client.util;

import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.core.client.GWT;

/**
 * Created by rhs on 5/31/16.
 */
public class Constants {
    public static final String base = GWT.getHostPageBaseURL();
    public static final String siteJson = base + "site/show";        // with {id}.json
    public static final String datasetJson = base + "dataset/show";  // requests look like dataset/show/{id}.json
    public static final String configJson = base + "config/json";
    public static final String adminAdd = base + "admin/addDataset";
    public static final String productCreate = base + "product/make";
    public static final String erddapDataRequest = base + "product/erddapDataRequest";
    public static final String datatable = base + "product/datatable";
    public static final String productsByInterval = base + "config/productsByInterval";

    public final static int navWidth = 400;
    public final static int imageBorderFactor = 200;


    public static final int rndRedColor = 244;
    public static final int rndGreenColor = 154;
    public static final int rndBlueColor = 0;
    public static final double rndAlpha = .45;

    public final static CssColor randomColor = CssColor.make("rgba(" + rndRedColor + ", " + rndGreenColor + "," + rndBlueColor + ", " + rndAlpha + ")");

    public static final String PANEL01 = "panel01";
    public static final String PANEL02 = "panel02";
    public static final String PANEL03 = "panel03";
    public static final String PANEL04 = "panel04";

    public static String GRID = "grid";
    public static String TRAJECTORY = "trajectory";
    public static String PROFILE = "profile";
    public static String TIMESERIES = "timeseries";

}
