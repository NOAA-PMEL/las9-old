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

    public static final String PANEL01 = "panel01"; // Main panel, upper left
    public static final String PANEL02 = "panel02"; // Second panel; upper right
    public static final String PANEL03 = "panel03"; // lower left
    public static final String PANEL04 = "panel04"; // lower right
    public static final String PANEL05 = "panel05"; // animation
    public static final String PANEL06 = "panel06"; // Show values... kinda pointless
    public static final String PANEL07 = "panel07"; // Save as... but it has no actual panel

    public static final String PANEL08 = "panel08"; // correlation viewer


    public static String GRID = "grid";
    public static String TRAJECTORY = "trajectory";
    public static String PROFILE = "profile";
    public static String TIMESERIES = "timeseries";

    public static String ANIMATE_CANCEL = "Push the cancel button to stop making new frames and animate the ones already downloaded.";
    public static String ANIMATE_SUBMIT = "Select a time range, the number of time steps to skip between frames, and submit";

}
