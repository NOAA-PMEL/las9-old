package pmel.sdig.las

import grails.transaction.Transactional
import org.jdom.Document
import org.jdom.Element
import pmel.sdig.las.MapScale

class ProductService {


    List<Product> findProductsByInterval(String grid, String intervals) {
        List<String> combos = ConfigController.combo(intervals);

        def products = new ArrayList<Product>();

        // TODO handle the data view for other geometries.
        // For grids the view and data view are the same.
        combos.each {view ->
            List<Product> productsByGeoAndView = Product.findAllByGeometryAndViewAndHidden(grid, view, false)
            log.debug("Checking view + "+view)
            if ( productsByGeoAndView ) {
                productsByGeoAndView.each {product ->
                    products.add(product)
                }
            }
        }
        products.sort{it.product_order}
        products
    }
    def makeAnnotations(String filename) {

        List<AnnotationGroup> annotationGroups = new ArrayList<AnnotationGroup>();

        Random r = new Random()
        def root = new XmlSlurper().parse(filename)
        root.annotation_group.each {group ->
            AnnotationGroup aGroup = new AnnotationGroup()
            aGroup.setType(group.@type.toString())
            aGroup.setId(r.nextLong())
            List<Annotation> annotationList = new ArrayList<Annotation>()
            group.annotation.each{a ->
                Annotation annotation = new Annotation([type: a.@type.toString(), value: a.value.text()])
                annotationList.add(annotation)
            }
            aGroup.annotations = annotationList;
            annotationGroups.add(aGroup)
        }
        annotationGroups;
    }
    private def Animation makeAnimationList(String filename) {
        Document doc = new Document();
        Animation animation = new Animation();
        JDOMUtils.XML2JDOM(new File(filename), doc)
        Element root = doc.getRootElement();
        if ( root == null ) {
            return animation;
        }
        Element das = root.getChild("dep_axes_scale")
        if ( das ) {
            String dasText = das.getText();
            if ( dasText ) {
                animation.setDep_axis_scale(dasText.trim())
            }
        }
        Element cl = root.getChild("contour_levels")
        if ( cl ) {
            String clText = cl.getText();
            if ( clText ) {
                animation.setContour_levels(clText.trim())
            }

        }
        Element fl = root.getChild("fill_levels")
        if ( fl ) {
            String fillLevels = fl.getText()
            if ( fillLevels ) {
                animation.setFill_levels(fillLevels.trim())
            }
        }
        Element u = root.getChild("units")
        if ( u ) {
            String uText = u.getText()
            if ( uText ) {
                animation.setUnits(uText.trim())
            }
        }
        Element ht = root.getChild("hasT")
        if ( ht ) {
            String hasT = ht.getText()
            if ( hasT != null && hasT.trim().equals("1") ) {
                animation.setHasT(true)
            } else {
                animation.setHasT(false)
            }
        }

        Element framesE = doc.getRootElement().getChild("frames")
        if ( framesE ) {
            List<Element> framesListE = framesE.getChildren("frame")
            List<String> frames = new ArrayList<>()
            for (int i = 0; i < framesListE.size(); i++) {
                frames.add(framesListE.get(i).getText().trim())
            }
            animation.setFrames(frames)
        }
        animation
    }
    def makeMapScale(String filename) {
        HashMap<String, String> scale = new HashMap<String, String>();

        new File(filename).eachLine{String line ->

            if (line.contains("[1-9]:") && line.contains(" / ")) {
                // Split on the : and use the second half...
                String[] halves = line.split(":");
                if (halves.length > 1) {
                    // See below...
                    String[] parts = halves[1].split("\"");
                    scale.put(parts[1], parts[3]);
                }
            }
            /*
             * Simpler style without the mulitple line numbers, ":" and "/"
             * "PPL$XMIN" "122.2" "PPL$XMAX" "288.8" "PPL$YMIN" "-35.00"
             * "PPL$YMAX" "45.00" "PPL$XPIXEL" "776" "PPL$YPIXEL" "483"
             * "PPL$WIDTH" "12.19" "PPL$HEIGHT" "7.602" "PPL$XORG" "1.200"
             * "VP_TOP_MARGIN" "1.4" "VP_RT_MARGIN" "1" "AX_HORIZ" "X"
             * "AX_HORIZ_POSTV" " " "AX_VERT" "Z" "AX_VERT_POSTV" "down"
             * "DATA_EXISTS" "1" "DATA_MIN" "3.608" "DATA_MAX" "8.209"
             */
            else {
                // Just split on the quotes
                // See JavaDoc on split for explanation
                // of why we get 4 parts with the repeated quotes.
                String[] parts = line.split("\"");
                scale.put(parts[1], parts[3]);
            }

        }
        String s1 = scale.get('PPL$XPIXEL');
        String s2 = scale.get('PPL$WIDTH');
        float xppi = 0.0f;
        if (s1 != null && s2 != null) {
            xppi = Float.valueOf(s1) / Float.valueOf(s2);
        }

        float yppi = 0.0f;
        s1 = scale.get('PPL$YPIXEL');
        s2 = scale.get('PPL$HEIGHT');
        if (s1 != null && s2 != null) {
            yppi = Float.valueOf(s1) / Float.valueOf(s2);
        }

        float x_image_size = 0.0f;
        s1 = scale.get('PPL$XPIXEL');
        if (s1 != null) {
            x_image_size = Float.valueOf(s1);
        }

        float y_image_size = 0.0f;
        s1 = scale.get('PPL$YPIXEL');
        if (s1 != null) {
            y_image_size = Float.valueOf(s1);
        }

        float xOffset_left = 0.0f;
        s1 = scale.get('PPL$XORG');
        if (s1 != null) {
            xOffset_left = xppi * Float.valueOf(s1);
        }

        float yOffset_bottom = 0.0f;
        s1 = scale.get('PPL$YORG');
        if (s1 != null) {
            yOffset_bottom = yppi * Float.valueOf(s1);
        }

        float xOffset_right = 0.0f;
        s1 = scale.get('VP_RT_MARGIN');
        if (s1 != null) {
            xOffset_right = xppi * Float.valueOf(s1);
        }

        float yOffset_top = 0.0f;
        s1 = scale.get('VP_TOP_MARGIN');
        if (s1 != null) {
            yOffset_top = yppi * Float.valueOf(s1);
        }

        float plotWidth = 0.0f;
        s1 = scale.get('PPL$XLEN');
        if (s1 != null) {
            plotWidth = xppi * Float.valueOf(s1);
        }

        float plotHeight = 0.0f;
        s1 = scale.get('PPL$YLEN');
        if (s1 != null) {
            plotHeight = yppi * Float.valueOf(s1);
        }

        float axisLLX = 0.0f;
        s1 = scale.get('XAXIS_MIN');
        if (s1 != null) {
            axisLLX = Float.valueOf(s1);
        }
        float axisLLY = 0.0f;
        s1 = scale.get('YAXIS_MIN');
        if (s1 != null) {
            axisLLY = Float.valueOf(s1);
        }

        float axisURX = 0.0f;
        s1 = scale.get('XAXIS_MAX');
        if (s1 != null) {
            axisURX = Float.valueOf(s1);
        }

        float axisURY = 0.0f;
        s1 = scale.get('YAXIS_MAX');
        if (s1 != null) {
            axisURY = Float.valueOf(s1);
        }

        float data_min = 0.0f;
        s1 = scale.get('DATA_MIN');
        if (s1 != null && !s1.equals(' ') && !s1.equals('')) {
            data_min = Float.valueOf(s1);
        }

        float data_max = 0.0f;
        s1 = scale.get('DATA_MAX');
        if (s1 != null && !s1.equals(' ') && !s1.equals('')) {
            data_max = Float.valueOf(s1);
        }

        int data_exists = 0;
        s1 = scale.get('DATA_EXISTS');
        if (s1 != null) {
            data_exists = Integer.valueOf(s1).intValue();
        }

        int xStride = 0;
        s1 = scale.get('XSTRIDE');
        if (s1 != null) {
            xStride = Integer.valueOf(s1).intValue();
        }

        int yStride = 0;
        s1 = scale.get('YSTRIDE');
        if (s1 != null) {
            yStride = Integer.valueOf(s1).intValue();
        }

        String time_min = null;
        s1 = scale.get('HAXIS_TSTART');
        if (s1 != null) {
            time_min = s1;
        }

        String time_max = null;
        s1 = scale.get('HAXIS_TEND');
        if (s1 != null) {
            time_max = s1;
        }

        if (time_min == null || time_max == null) {
            s1 = scale.get('VAXIS_TSTART');
            if (s1 != null) {
                time_min = s1;
            }

            s1 = scale.get('VAXIS_TEND');
            if (s1 != null) {
                time_max = s1;
            }
        }

        String time_units = null;
        s1 = scale.get('HAXIS_TUNITS');
        if (s1 != null) {
            time_units = s1;
        }
        if (time_units == null) {
            s1 = scale.get('VAXIS_TUNITS');
            if (s1 != null) {
                time_units = s1;
            }
        }

        String time_origin = null;
        s1 = scale.get('HAXIS_TORIGIN');
        if (s1 != null) {
            time_origin = s1;
        }
        if (time_origin == null) {
            s1 = scale.get('VAXIS_TORIGIN');
            if (s1 != null) {
                time_origin = s1;
            }
        }

        String calendar = null;
        s1 = scale.get('HAXIS_TCALENDAR');
        if (s1 != null) {
            calendar = s1;
        }
        if ( calendar == null ) {
            s1 = scale.get('VAXIS_TCALENDAR');
            if ( s1 != null ) {
                calendar = s1;
            }
        }

        String levels_string = null;
        s1 = scale.get("LEVELS_STRING")
        if ( s1 != null ) {
            levels_string = s1
        }

        MapScale mapScaleInstance = new MapScale();
        mapScaleInstance.setXxxPixelsPerInch(xppi);
        mapScaleInstance.setYyyPixelsPerInch(yppi);
        mapScaleInstance.setXxxImageSize(String.format("%d", (int)x_image_size));
        mapScaleInstance.setYyyImageSize(String.format("%d", (int)y_image_size));
        mapScaleInstance.setXxxPlotSize(String.format("%d", (int)plotWidth));
        mapScaleInstance.setYyyPlotSize(String.format("%d", (int)plotHeight))
        mapScaleInstance.setXxxOffsetFromLeft(String.format("%d", (int)xOffset_left))
        mapScaleInstance.setYyyOffsetFromBottom(String.format("%d", (int)yOffset_bottom))
        mapScaleInstance.setXxxOffsetFromRight(String.format("%d", (int)xOffset_right))
        mapScaleInstance.setYyyOffsetFromTop(String.format("%d", (int)yOffset_top))
        mapScaleInstance.setXxxAxisLowerLeft(String.format("%.8g", axisLLX));
        mapScaleInstance.setXxxAxisUpperRight(String.format("%.8g",axisURX))
        mapScaleInstance.setAxis_horizontal(scale.get("AX_HORIZ"))
        mapScaleInstance.setAxis_vertical(scale.get("AX_VERT"));
        mapScaleInstance.setAxis_vertical_positive(scale.get("AX_VERT_POSTV"));
        mapScaleInstance.setLevels_string(levels_string)
        if ( mapScaleInstance.getAxis_vertical_positive().equals(" ")) mapScaleInstance.setAxis_vertical_positive("up")

        if ( mapScaleInstance.getAxis_vertical_positive().equals("down") ) {
            mapScaleInstance.setYyyAxisLowerLeft(String.format("%.8g", axisURY))
            mapScaleInstance.setYyyAxisUpperRight(String.format("%.8g", axisLLY))
        } else {
            mapScaleInstance.setYyyAxisLowerLeft(String.format("%.8g", axisLLY))
            mapScaleInstance.setYyyAxisUpperRight(String.format("%.8g", axisURY))
        }


        mapScaleInstance.setAxis_horizontal_positive(scale.get("AX_HORIZ_POSTV"));
        if ( mapScaleInstance.getAxis_horizontal_positive().equals(" ")) mapScaleInstance.setAxis_horizontal_positive("right")
        mapScaleInstance.setData_min(String.format("%.8g",data_min));
        mapScaleInstance.setData_max(String.format("%.8g",data_max));
        mapScaleInstance.setData_exists(String.format("%d",data_exists));
        mapScaleInstance.setXxxStride(scale.get("XSTRIDE"));
        mapScaleInstance.setYyyStride(scale.get("YSTRIDE"));

        // The autobean mechanism on the client requires an ID.
        mapScaleInstance.id = 99l;

        if (time_min != null) {
            mapScaleInstance.setTime_min(time_min);
        }
        if (time_max != null) {
            mapScaleInstance.setTime_max(time_max);
        }
        if (time_units != null) {
            mapScaleInstance.setTime_units(time_units);
        }
        if (time_origin != null) {
            mapScaleInstance.setTime_origin(time_origin);
        }
        if ( calendar != null ) {
            mapScaleInstance.setCalendar(calendar);
        }

        mapScaleInstance

    }

}
