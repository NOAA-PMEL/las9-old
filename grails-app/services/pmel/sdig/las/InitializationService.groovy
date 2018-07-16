package pmel.sdig.las

import grails.transaction.Transactional
import pmel.sdig.las.Argument
import pmel.sdig.las.Dataset
import pmel.sdig.las.Ferret
import pmel.sdig.las.FerretEnvironment
import pmel.sdig.las.MenuItem
import pmel.sdig.las.MenuOption
import pmel.sdig.las.Operation
import pmel.sdig.las.Product
import pmel.sdig.las.Result
import pmel.sdig.las.ResultSet
import pmel.sdig.las.Site
import pmel.sdig.las.type.GeometryType

@Transactional
class InitializationService {

    IngestService ingestService

    /**
     * Initialize the Ferret environment. If the values of the environment are available, then they are set. If not, the user will be led to a page where they can be filled in on startup.
     * @return
     */
    def initEnvironment() {

        log.debug("Setting up Ferret environment from the runtime environment.")
        // Get their values from the system environment
        def env = System.getenv()

        // Turns out the map constructor does not work when one of the keys is "_"
        // This code clean out that bad value

        def cleanenv = [:]

        // grails and gorm HATE upper case variables names. I don't know why.
        env.each() { key, value ->
            if ( !key.startsWith("_") ) {
                cleanenv.putAt(key.toLowerCase(), value)
            }
        }

        // Use the FER_DIR to set the path to the executable

        def fer_dir = env['FER_DIR']

        if ( !fer_dir ) {
            fer_dir = ""
        }

        def fer_go = cleanenv.get("fer_go")

        URL ferret_go_dir = this.class.classLoader.getResource("ferret/scripts")

        fer_go = fer_go + " " + ferret_go_dir.getPath().replace("file:", "")

        cleanenv.put("fer_go", fer_go)

        def ferretEnvironment = new FerretEnvironment(cleanenv)


        def ferret = new Ferret()
        // TODO figure out python path
        ferret.setPath("/usr/bin/python2.7")
        ferret.setTempDir("/tmp/las")
        ferret.setFerretEnvironment(ferretEnvironment)
        if ( !ferret.validate() ) {
            ferret.errors.each {
                log.debug(it)
            }
        }
        ferret.save(failOnError: true)
        def tempFile = new File(ferret.tempDir);
        if ( !tempFile.exists() ) {
            tempFile.mkdirs()
        }
        ferret.addToArguments(new Argument([value: "-cimport sys; import pyferret; (errval, errmsg) = pyferret.init(sys.argv[1:], True)"]))
        ferret.addToArguments(new Argument([value: "-nodisplay"]))
        ferret.addToArguments(new Argument([value: "-script"]))


    }

    /**
     * Create the options and save them...
     */
    def createOptions() {

        /*


  <optiondef name="interpolate_data">
  <optiondef name="expression">
  <optiondef name="expression2">
  <optiondef name="data_format">
  <optiondef name="stride_quality_factor">

  <optiondef name="time_step">

  <optiondef name="use_graticules">
  <optiondef name="use_ref_map">
  <optiondef name="margins">
  <optiondef name="deg_min_sec">
  <optiondef name="line_or_sym">
  <optiondef name="trend_line">
  <optiondef name="line_color">
  <optiondef name="line_thickness">

  <optiondef name="dep_axis_scale">
  <optiondef name="palette">
  <optiondef name="contour_style">
  <optiondef name="fill_levels">
  <optiondef name="contour_levels">
  <optiondef name="mark_grid">
  <optiondef name="set_aspect">
  <optiondef name="land_type">
  <optiondef name="orientation">
  <optiondef name="do_contour2">
  <optiondef name="contour_levels2">
  <optiondef name="fill_levels2">
  <optiondef name="palette2">
  <optiondef name="vector_subsampling">
  <optiondef name="vector_length">
  <optiondef name="vector_style">
  <optiondef name="gen_script_option">
  <optiondef name="tline_range">

  Later...
  <optiondef name="ge_overlay_style">
  <optiondef name="show_all_ensembles">
  <optiondef name="show_stddev_band">

  Don't know what to do with this since pyFerret allows more specific sizing
  <optiondef name="size">

  All png all the time
  <optiondef name="image_format">

         */

        MenuOption palettes = MenuOption.findByName("palettes")

        if ( !palettes ) {

            palettes = new MenuOption(help: "Set the color scale of the plot. Only applies to shaded plots.", name: "palette",
                    title: "Color palettes", defaultValue: "viridis")



            MenuItem p001 = new MenuItem([value: "rainbow", title: "Rainbow"])
            MenuItem p002 = new MenuItem([value: "rnb2", title: "Rainbow alternative"])
            MenuItem p003 = new MenuItem([value: "light_rainbow", title: "Rainbow pastel"])
            MenuItem p004 = new MenuItem([value: "rainbow_by_levels", title: "Rainbow (repeating by-level)"])
            MenuItem p005 = new MenuItem([value: "light_bottom", title: "Rainbow light bottom"])

            MenuItem p006 = new MenuItem([value: "ocean_temp", title: "Ocean temperature (consistent by-value)"])

            MenuItem p007 = new MenuItem([value: "land_sea", title: "topo: land and sea"])
            MenuItem p008 = new MenuItem([value: "dark_land_sea", title: "topo: land and sea, dark "])
            MenuItem p009 = new MenuItem([value: "land_sea_values", title: "topo: (consistent by value)"])
            MenuItem p010 = new MenuItem([value: "etop_values", title: "topo: etopo land and sea (consistent by value)"])
            MenuItem p011 = new MenuItem([value: "ocean_blue", title: "topo: blue bathymetry"])
            MenuItem p012 = new MenuItem([value: "terrestrial", title: "topo: land only"])
            MenuItem p013 = new MenuItem([value: "dark_terrestrial", title: "topo: land only, dark"])

            MenuItem p014 = new MenuItem([value: "inferno", title: "CM inferno (purple to orange to yellow)"])
            MenuItem p015 = new MenuItem([value: "magma", title: "CM magma (purple to yellow)"])
            MenuItem p016 = new MenuItem([value: "plasma", title: "CM plasma (lighter purple to yellow)"])
            MenuItem p017 = new MenuItem([value: "viridis", title: "CM viridis (blue to green)"])


            MenuItem p018 = new MenuItem([value: "cmocean_algae", title: "CMocean algae (light to dark greens)"])
            MenuItem p019 = new MenuItem([value: "cmocean_amp", title: "CMocean amp (light to dark browns)"])
            MenuItem p020 = new MenuItem([value: "cmocean_balance", title: "CMocean balance (centered blue and brown)"])
            MenuItem p021 = new MenuItem([value: "cmocean_curl", title: "CMocean curl (centered green and brown)"])
            MenuItem p022 = new MenuItem([value: "cmocean_deep", title: "CMocean deep (yellow to blue)"])
            MenuItem p023 = new MenuItem([value: "cmocean_delta", title: "CMocean delta (centered green and blue)"])
            MenuItem p024 = new MenuItem([value: "cmocean_dense", title: "CMocean dense (blues and purples)"])
            MenuItem p025 = new MenuItem([value: "cmocean_gray", title: "CMocean gray (dark to light grays)"])
            MenuItem p026 = new MenuItem([value: "cmocean_haline", title: "CMocean haline (blue to green)"])
            MenuItem p027 = new MenuItem([value: "cmocean_ice", title: "CMocean ice (dark to light blue)"])
            MenuItem p028 = new MenuItem([value: "cmocean_matter", title: "CMocean matter (yellow to brown)"])
            MenuItem p029 = new MenuItem([value: "cmocean_oxy", title: "CMocean oxy (red/ gray/ yellow)"])
            MenuItem p030 = new MenuItem([value: "cmocean_phase", title: "CMocean phase (smoothly varying)"])
            MenuItem p031 = new MenuItem([value: "cmocean_solar", title: "CMocean solar (brown to yellow)"])
            MenuItem p032 = new MenuItem([value: "cmocean_speed", title: "CMocean speed (yellow to green)  "])
            MenuItem p033 = new MenuItem([value: "cmocean_tempo", title: "CMocean tempo (light to dark green)"])
            MenuItem p034 = new MenuItem([value: "cmocean_thermal", title: "CMocean thermal (purple to yellow)"])
            MenuItem p035 = new MenuItem([value: "cmocean_turbid", title: "CMocean turbid (yellow to brown)"])

            MenuItem p036 = new MenuItem([value: "light_centered", title: "centered anomaly"])
            MenuItem p037 = new MenuItem([value: "white_centered", title: "centered w/white at center"])

            MenuItem p038 = new MenuItem([value: "no_blue_centered", title: "centered no-blue"])
            MenuItem p039 = new MenuItem([value: "no_green_centered", title: "centered no-green"])
            MenuItem p040 = new MenuItem([value: "no_red_centered", title: "centered no-red"])

            MenuItem p041 = new MenuItem([value: "bluescale", title: "scale of blues"])
            MenuItem p042 = new MenuItem([value: "bluescale", title: "scale of blues reversed"])
            MenuItem p043 = new MenuItem([value: "redscale", title: "scale of reds"])
            MenuItem p044 = new MenuItem([value: "redscale", title: "scale of blues reversed"])
            MenuItem p045 = new MenuItem([value: "greenscale", title: "scale of greens"])
            MenuItem p046 = new MenuItem([value: "greenscale", title: "scale of greens reversed"])
            MenuItem p047 = new MenuItem([value: "grayscale", title: "scale of grays"])
            MenuItem p048 = new MenuItem([value: "grayscale", title: "scale of grays reversed"])



            palettes.addToMenuItems(p001)
            palettes.addToMenuItems(p002)
            palettes.addToMenuItems(p003)
            palettes.addToMenuItems(p004)
            palettes.addToMenuItems(p005)
            palettes.addToMenuItems(p006)
            palettes.addToMenuItems(p007)
            palettes.addToMenuItems(p008)
            palettes.addToMenuItems(p009)

            palettes.addToMenuItems(p010)
            palettes.addToMenuItems(p011)
            palettes.addToMenuItems(p012)
            palettes.addToMenuItems(p013)
            palettes.addToMenuItems(p014)
            palettes.addToMenuItems(p015)
            palettes.addToMenuItems(p016)
            palettes.addToMenuItems(p017)
            palettes.addToMenuItems(p018)
            palettes.addToMenuItems(p019)

            palettes.addToMenuItems(p020)
            palettes.addToMenuItems(p021)
            palettes.addToMenuItems(p022)
            palettes.addToMenuItems(p023)
            palettes.addToMenuItems(p024)
            palettes.addToMenuItems(p025)
            palettes.addToMenuItems(p026)
            palettes.addToMenuItems(p027)
            palettes.addToMenuItems(p028)
            palettes.addToMenuItems(p029)

            palettes.addToMenuItems(p030)
            palettes.addToMenuItems(p031)
            palettes.addToMenuItems(p032)
            palettes.addToMenuItems(p033)
            palettes.addToMenuItems(p034)
            palettes.addToMenuItems(p035)
            palettes.addToMenuItems(p036)
            palettes.addToMenuItems(p037)
            palettes.addToMenuItems(p038)
            palettes.addToMenuItems(p039)

            palettes.addToMenuItems(p040)
            palettes.addToMenuItems(p041)
            palettes.addToMenuItems(p042)
            palettes.addToMenuItems(p043)
            palettes.addToMenuItems(p044)
            palettes.addToMenuItems(p045)
            palettes.addToMenuItems(p046)
            palettes.addToMenuItems(p047)
            palettes.addToMenuItems(p048)

            palettes.save(failOnError: true)
        }

        YesNoOption interpolate = YesNoOption.findByName("interpolate_data")
        if ( !interpolate ) {
            interpolate = new YesNoOption([name        : "interpolate_data",
                                           title       : "Interpolate Data Normal to the Plot?",
                                           help        : "<p&gt;This interpolation affects the interpretation of coordinates\n" +
                                                   "that lie normal to the current view.\n" +
                                                   "For example, in a lat-long view (a traditional map) the time and\n" +
                                                   "depth axes are normal to the view.  If This interpolation is\n" +
                                                   "on LAS performs an interpolation to the exact specified normal\n" +
                                                   "coordinate(s) --  time and depth for a map view.  If off, LAS\n" +
                                                   "instead uses the data at the nearest grid point.\n" +
                                                   "(To be more precise, it uses the data at the grid point of the\n" +
                                                   "cell that contains the specified coordinate).\n" +
                                                   "</p&gt;\n" +
                                                   "<p&gt;For example:</p&gt;\n" +
                                                   "\n" +
                                                   "<p&gt;If the grid underlying the variable has points defined at Z=5\n" +
                                                   "and at Z=15 (with the grid box boundary at Z=10) and data is\n" +
                                                   "requested at Z=12 then with View interpolation set to &#8217;On&#8217; the\n" +
                                                   "data in the X-Y plane will be obtained by calculating the\n" +
                                                   "interpolated value of data at Z=12 between the Z=5 and Z=15 planes.\n" +
                                                   "With View interpolation set to &#8217;Off&#8217;, the data will be obtained\n" +
                                                   "from the data at Z=15.</p&gt;",
                                           defaultValue: "no"])

            interpolate.save(failOnError: true)
        }

        TextOption fill_levels = TextOption.findByName("fill_levels")
        if ( !fill_levels ) {
            fill_levels = new TextOption([name : "fill_levels",
                                          hint: "Either number of levels or (lo, hi, delta)",
                                          title: "Color Fill Levels",
                                          help : "Set the color levels of the plot. Levels are described using Ferret syntax. The" +
                                                  "number of levels is approximate, and may be changed as the algorithm rounds off the values. " +
                                                  "Examples:" +
                                                  "<ul>" +
                                                  "<li><b>60V</b> Draw 60 levels based on the variance of the data with open-ended extrema\n" +
                                                  "<li><b>30H</b> Draw 30 levels based on a histogram\n" +
                                                  "<li><b>25</b> Draw 25 levels spanning the range of the data\n" +
                                                  "<li><b>30C</b> Draw 30 levels centered at 0\n" +
                                                  "<li><b>(0,100,10)</b>  Bands of color starting at 0, ending at 100, with an interval of 10\n" +
                                                  "<li><b>(-inf)(-10,10,0.25)(inf)</b> Bands of color between -10 and 10 with an additional color at each end of the spectrum representing all values below (-inf) or above (inf)\n" +
                                                  "<li><b>(-100)(-10,10,0.25)(100)</b> Bands of color between -10 and 10 with a additional bands for all outlying values up to +/- 100.\n" +
                                                  "</ul>\n" +
                                                  "Detailed info is available in the Ferret User\\'s Guide., see Levels at\n" +
                                                  "http://ferret.pmel.noaa.gov/Ferret/documentation/users-guide/customizing-plots/CONTOURING#_VPINDEXENTRY_853"])
            fill_levels.save(failOnError: true)
        }


        /*
            <optiondef name="data_format">
                <option>
                    <help>Choose a file format</help>
                    <title>ASCII file format</title>
                    <menu type="options" name="data_format">
                        <item values="tsv">Tab separated</item>
                        <item values="csv">Comma separated</item>
                        <item values="asc">FORTRAN formatted</item>
                    </menu>
                </option>
            </optiondef>

         */

        MenuOption data_format = MenuOption.findByName("data_format")

        if ( !data_format ) {

            data_format = new MenuOption(help: "Choose a file format.", name: "data_format", title: "ASCII file format")



            MenuItem df001 = new MenuItem([value: "tsv", title: "Tab Separated"])
            MenuItem df002 = new MenuItem([value: "csv", title: "Comma Separated"])
            MenuItem df003 = new MenuItem([value: "asc", title: "FORTRAN Formatted"])

            data_format.addToMenuItems(df001)
            data_format.addToMenuItems(df002)
            data_format.addToMenuItems(df003)

            data_format.save(failOnError: true)
        }

        /*

                <optiondef name="stride_quality_factor">
                  <option>
                    <help>When visualizing variables that have a great many data points (high resolution) the system can respond faster by thinning (subsampling) the number of data points.   Setting the Quality to draft will use fewer points, thereby increasing speed but losing details in the images. Setting Quality to best will be slower but will reveal more detail.</help>
                    <title>Quality</title>
                    <menu type="options" name="stride_quality_factor">
                      <item values="1.0">draft(fast)</item>
                      <item values="0.5">medium</item>
                      <item values="0.0">best(slow)</item>
                    </menu>
                  </option>
                </optiondef>

         */

        MenuOption stride_quality_factor = MenuOption.findByName("stride_quality_factor")

        if ( !stride_quality_factor ) {

            stride_quality_factor = new MenuOption(help: "When visualizing variables that have a great many data points (high resolution) the system can respond faster by thinning (subsampling) the number of data points.   Setting the Quality to draft will use fewer points, thereby increasing speed but losing details in the images. Setting Quality to best will be slower but will reveal more detail.", name: "stride_quality_factor", title: "Quality")



            MenuItem sf001 = new MenuItem([value:"1.0", title:"draft(fast)"])
            MenuItem sf002 = new MenuItem([value:"0.5", title:"medium"])
            MenuItem sf003 = new MenuItem([value:"0.0", title:"best(slow)"])

            stride_quality_factor.addToMenuItems(sf001)
            stride_quality_factor.addToMenuItems(sf002)
            stride_quality_factor.addToMenuItems(sf003)

            stride_quality_factor.save(failOnError: true)

        }

        /*

              <!-- animation time step i.e., delta T -->
              <optiondef name="time_step">
                <option>
                  <help>Set the time step for animation. It is between 1 and the number of frames being selected.</help>
                  <title>Time Step</title>
                  <textfield name="time_step"/>
                </option>
              </optiondef>

         */

        TextOption time_step = TextOption.findByName("time_step")

        if ( !time_step ) {

            time_step = new TextOption(help: "Set the time step for animation. It is between 1 and the number of frames being selected.",
                                       name: "time_step",
                                       hint: "10 would show every 10th time step.",
                                       title: "Animation Stride in Time")
            time_step.save(failOnError: true)

        }

        /*

  <optiondef name="use_graticules">
    <option>
      <help>Turn on and off graticule lines on the plot, and set their color. None/No tics turns off both graticules and tic marks along the axes.</help>
      <title>Show graticule</title>
      <menu type="options" name="use_graticules">
        <item values="default">Default</item>
        <item values="black">Black</item>
        <item values="gray">Gray</item>
        <item values="white">White</item>
        <item values="none">None</item>
        <item values="notic">None/No tics</item>
      </menu>
    </option>
  </optiondef>

         */
        MenuOption use_graticules = MenuOption.findByName("use_graticules")
        if ( !use_graticules ) {
            use_graticules = new MenuOption([name        : "use_graticules",
                                              title       : "Show graticule",
                                              help        : "Turn on and off graticule lines on the plot, and set their color. None/No tics turns off both graticules and tic marks along the axes.</help>\n",
                                              defaultValue: "none"])

            MenuItem sf001 = new MenuItem([value:"black", title:"Black"])
            MenuItem sf002 = new MenuItem([value:"gray", title:"Gray"])
            MenuItem sf003 = new MenuItem([value:"white", title:"White"])
            MenuItem sf004 = new MenuItem([value:"none", title:"None"])
            MenuItem sf005 = new MenuItem([value:"notic", title:"None/No tics"])

            use_graticules.addToMenuItems(sf001)
            use_graticules.addToMenuItems(sf002)
            use_graticules.addToMenuItems(sf003)
            use_graticules.addToMenuItems(sf004)
            use_graticules.addToMenuItems(sf005)

            use_graticules.save(failOnError: true)
        }

        /*

          <optiondef name="use_ref_map">
    <option>
      <help>
Draw a map showing the currently selected geographical region If <b>Default</b> is
selected, the server will decide whether it is appropriate to draw the map. If <B>No</B> is selected, the map is never drawn.
      </help>
      <title>Show reference map</title>
      <menu type="options" name="use_ref_map">
        <item values="default">Default</item>
        <item values="false">No</item>
        <item values="true">Yes</item>
      </menu>
    </option>
  </optiondef>


         */

        YesNoOption use_ref_map = YesNoOption.findByName("use_ref_map")
        if ( !use_ref_map ) {
            use_ref_map = new YesNoOption([name        : "use_ref_map",
                                                       title       : "Show reference map",
                                                       help        : "Draw a map showing the currently selected geographical region If <b>Default</b> is\n" +
                                                               "selected, the server will decide whether it is appropriate to draw the map. If <B>No</B> is selected, the map is never drawn.",
                                                       defaultValue: "no"])

            use_ref_map.save(failOnError: true)
        }

        /*
 <optiondef name="margins">
    <option>
      <help>
Make the plot with or without margins: when no margins is chosen, the axes are
at the edges of the plot (WMS-style plots). By default margins are shown.
      </help>
      <title>Margins</title>
      <menu type="options" name="margins">
        <item values="default">Default</item>
        <item values="false">No</item>
        <item values="true">Yes</item>
      </menu>
    </option>
  </optiondef>

         */

        YesNoOption margins = new YesNoOption([name: "margins",
                                                   title: "Show reference map",
                                                   help: "Make the plot with or without margins: when no margins is chosen, the axes are\n" +
                                                           "at the edges of the plot (WMS-style plots). By default margins are shown.",
                                                   defaultValue: "no"])

        margins.save(failOnError: true)

        /*
    <optiondef name="deg_min_sec">
    <option>
      <help>Format the labels on plot axes that are in units of degrees longitude or latitude as degrees,minutes rather than degrees and decimal fractions of degrees.  For axes with other units, this setting will be ignored.
      </help>
      <title>Degrees/Minutes axis labels</title>
      <menu type="options" name="deg_min_sec">
        <item values="default">Default</item>
        <item values="false">No</item>
        <item values="true">Yes</item>
      </menu>
    </option>
  </optiondef>

         */
        YesNoOption deg_min_sec = YesNoOption.findByName("deg_min_sec")
        if ( !deg_min_sec ) {
            deg_min_sec = new YesNoOption([name        : "deg_min_sec",
                                                       title       : "Use degrees/minutes axis labels",
                                                       help        : "Format the labels on plot axes that are in units of degrees longitude or latitude as degrees,minutes rather than degrees and decimal fractions of degrees.  For axes with other units, this setting will be ignored.",
                                                       defaultValue: "no"])

            deg_min_sec.save(failOnError: true)
        }

        /*

  <optiondef name="line_or_sym">
    <option>
      <help>Draw a line or a symbol or both.</help>
      <title>Line Style</title>
      <menu type="options" name="line_or_sym">
        <item values="default">Default</item>
        <item values="sym">Symbol only</item>
        <item values="line">Line only</item>
        <item values="both">Both Symbol and Line</item>
      </menu>
    </option>
  </optiondef>

         */

        MenuOption line_or_sym = MenuOption.findByName("line_or_sym")

        if ( !line_or_sym ) {

            line_or_sym = new MenuOption([name        : "line_or_sym",
                                          title       : "Line Style",
                                          help        : "Draw a line or a symbol or both.",
                                          defaultValue: "line"])



            MenuItem sf001 = new MenuItem([value: "sym", title: "Symbol only"])
            MenuItem sf002 = new MenuItem([value: "line", title: "Line only"])
            MenuItem sf003 = new MenuItem([value: "both", title: "Both Symbol and Line"])

            line_or_sym.addToMenuItems(sf001)
            line_or_sym.addToMenuItems(sf002)
            line_or_sym.addToMenuItems(sf003)

            line_or_sym.save(failOnError: true)
        }



        /*
  <optiondef name="trend_line">
    <option>
      <help>Overlay a trend line computed by least-squares. For the option "Trend Line and Detrended", a second panel is added, showing the variable minus mean and variable minus mean and trend. Note that the slope of the trend is computed using the units of the independent axis. A monthly axis may have underlying units of days, so in such a case the slope will be data_units/days. Line color choices are ignored in this style. The plots may be zoomed - for 2-panel plots zoom on the upper or left panel.</help>
      <title>Trend Line</title>
      <menu type="options" name="trend_line">
        <item values="0">Default</item>
        <item values="0">none</item>
        <item values="1">With Trend Line</item>
        <item values="2">Trend Line and Detrended</item>
      </menu>
    </option>
  </optiondef>         */

        MenuOption trend_line = MenuOption.findByName("trend_line")

        if ( !trend_line ) {

            trend_line = new MenuOption([name        : "trend_line",
                                         title       : "Trend Line",
                                         help        : "Overlay a trend line computed by least-squares. For the option \"Trend Line and Detrended\", a second panel is added, showing the variable minus mean and variable minus mean and trend. Note that the slope of the trend is computed using the units of the independent axis. A monthly axis may have underlying units of days, so in such a case the slope will be data_units/days. Line color choices are ignored in this style. The plots may be zoomed - for 2-panel plots zoom on the upper or left panel.",
                                         defaultValue: "line"])



            MenuItem sf001 = new MenuItem([value:"0", title:"None"])
            MenuItem sf002 = new MenuItem([value:"1", title:"With Trend Line"])
            MenuItem sf003 = new MenuItem([value:"2", title:"Trend Line and Detrended"])

            trend_line.addToMenuItems(sf001)
            trend_line.addToMenuItems(sf002)
            trend_line.addToMenuItems(sf003)

            trend_line.save(failOnError: true)

        }

        /*
  <optiondef name="line_color">
    <option>
      <help>Set the color of the plot symbols and/or line.</help>
      <title>Line color (single-var plots)</title>
      <menu type="options" name="line_color">
        <item values="default">Default</item>
        <item values="black">Black</item>
        <item values="red">Red</item>
        <item values="green">Green</item>
        <item values="blue">Blue</item>
        <item values="lightblue">Light Blue</item>
        <item values="purple">Purple</item>
      </menu>
    </option>
  </optiondef>
         */

        MenuOption line_color = MenuOption.findByName("line_color")

        if ( !line_color ) {

            line_color = new MenuOption([name        : "line_color",
                                         title       : "Line color (single-var plots)",
                                         help        : "Set the color of the plot symbols and/or line.",
                                         defaultValue: "black"])



            MenuItem sf001 = new MenuItem([value:"black", title:"Black"])
            MenuItem sf002 = new MenuItem([value:"red", title:"Red"])
            MenuItem sf003 = new MenuItem([value:"green", title:"Green"])
            MenuItem sf004 = new MenuItem([value:"blue", title:"Blue"])
            MenuItem sf005 = new MenuItem([value:"lightblue", title:"Light Blue"])
            MenuItem sf006 = new MenuItem([value:"purple", title:"Purple"])

            line_color.addToMenuItems(sf001)
            line_color.addToMenuItems(sf002)
            line_color.addToMenuItems(sf003)
            line_color.addToMenuItems(sf004)
            line_color.addToMenuItems(sf005)
            line_color.addToMenuItems(sf006)

            line_color.save(failOnError: true)

        }
        /*
  <optiondef name="line_thickness">
    <option>
      <help>Set the thickness of the plot symbols and/or line.</help>
      <title>Line thickness</title>
      <menu type="options" name="line_thickness">
        <item values="default">Default</item>
        <item values="1">Thin</item>
        <item values="2">Medium</item>
        <item values="3">Thick</item>
      </menu>
    </option>
  </optiondef>

         */
        MenuOption line_thickness = MenuOption.findByName("line_thickness")

        if ( !line_thickness ) {

            line_thickness = new MenuOption([name        : "line_thickness",
                                         title       : "Line thickness",
                                         help        : "Set the thickness of the plot symbols and/or line.",
                                         defaultValue: "this"])



            MenuItem sf001 = new MenuItem([value:"1", title:"Thin"])
            MenuItem sf002 = new MenuItem([value:"2", title:"Medium"])
            MenuItem sf003 = new MenuItem([value:"3", title:"Thick"])


            line_thickness.addToMenuItems(sf001)
            line_thickness.addToMenuItems(sf002)
            line_thickness.addToMenuItems(sf003)

            line_thickness.save(failOnError: true)

        }

        /*
  <optiondef name="dep_axis_scale">
    <option>
      <help>Set scale on the dependent axis lo,hi[,delta] where [,delta] is optional, units are data units. If a delta is given, it will determine the tic mark intervals. The dependent axis is the vertical axis for most plots; for plots of a variable vs height or depth it is the horizontal axis. If the scale is not set, Ferret determines this from the data.</help>
      <title>Dependent axis scale</title>
      <textfield name="dep_axis_scale"/>
    </option>
  </optiondef>

         */

        TextOption dep_axis_scale = TextOption.findByName("dep_axis_scale")

        if ( !dep_axis_scale ) {

            dep_axis_scale = new TextOption(help: "Set scale on the dependent axis lo,hi[,delta] where [,delta] is optional, units are data units. If a delta is given, it will determine the tic mark intervals. The dependent axis is the vertical axis for most plots; for plots of a variable vs height or depth it is the horizontal axis. If the scale is not set, Ferret determines this from the data.",
                                            name: "dep_axis_scale",
                                            hint: "lo, hi [,delta]",
                                            title: "Dependent axis scale")
            dep_axis_scale.save(failOnError: true)

        }

        /*
<optiondef name="contour_style">
    <option>
      <help>What style of contours to draw
Choices are:
<ul>
<li><b>Default</b> -- let LAS decide
<li><b>Raster</b> -- Fill each grid cell with the appropriate color
<li><b>Color filled</b> -- Fill in between contour lines with color
<li><b>Lines</b> -- Just draw lines
<li><b>Raster and lines</b> -- Fill in each grid cell and draw lines on top
<li><b>Color filled and lines</b> -- Fill in between contour lines with color and draw lines on top
</ul>
      </help>
      <title>Contour style</title>
      <menu type="options" name="contour_style">
        <item values="default">Default</item>
        <item values="raster">Raster</item>
        <item values="color_filled_contours">Color filled</item>
        <item values="contour_lines">Lines</item>
        <item values="raster_plus_lines">Raster and lines</item>
        <item values="color_filled_plus_lines">Color filled and lines</item>
      </menu>
    </option>
  </optiondef>

         */

        MenuOption contour_style = MenuOption.findByName("contour_style")

        if ( !contour_style ) {

            contour_style = new MenuOption([name        : "contour_style",
                                             title       : "Line thickness",
                                             help        : "What style of contours to draw\n" +
                                                     "Choices are:\n" +
                                                     "<ul&gt;\n" +
                                                     "<li><b>Raster</b> -- Fill each grid cell with the appropriate color\n" +
                                                     "<li><b>Color filled (default)</b> -- Fill in between contour lines with color\n" +
                                                     "<li><b>Lines</b> -- Just draw lines\n" +
                                                     "<li><b>Raster and lines</b> -- Fill in each grid cell and draw lines on top\n" +
                                                     "<li><b>Color filled and lines</b> -- Fill in between contour lines with color and draw lines on top\n" +
                                                     "</ul&gt;.",
                                             defaultValue: "color_filled_contours"])



            MenuItem sf001 = new MenuItem([value:"raster", title:"Raster"])
            MenuItem sf002 = new MenuItem([value:"color_filled_contours", title:"Color filled"])
            MenuItem sf003 = new MenuItem([value:"raster_plus_lines", title:"Raster and lines"])
            MenuItem sf004 = new MenuItem([value:"color_filled_contours_plus_lines", title:"Rster and lines"])

            contour_style.addToMenuItems(sf001)
            contour_style.addToMenuItems(sf002)
            contour_style.addToMenuItems(sf003)
            contour_style.addToMenuItems(sf004)

            contour_style.save(failOnError: true)
        }

        /*
 <optiondef name="contour_levels">
    <option>
      <help>Set the contour levels of the plot. Contour levels are described using Ferret syntax. Examples:
<ul><li><b>(0,100,10)</b>  Draw lines starting at 0, ending at 100, with an interval of 10
<li><b>25</b> Draw 25 lines
<li><b>10C</b> Draw 10 lines centered at 0
</ul>
Detailed info is available in the Ferret User\'s Guide., see Levels at
http://ferret.pmel.noaa.gov/Ferret/documentation/users-guide/customizing-plots/CONTOURING#_VPINDEXENTRY_853
</help>
      <title>Contour levels</title>
      <textfield name="contour_levels"/>
    </option>
  </optiondef>



         */

        TextOption contour_levels = TextOption.findByName("contour_levels")

        if ( !contour_levels ) {

            contour_levels = new TextOption(help: "<p>Set the contour levels of the plot. Contour levels are described using Ferret syntax. Examples:</p>\n" +
                    "<ul><li><b>(0,100,10)</b>  Draw lines starting at 0, ending at 100, with an interval of 10\n" +
                    "<li><b>25</b> Draw 25 lines\n" +
                    "<li><b>10C</b> Draw 10 lines centered at 0\n" +
                    "</ul>\n" +
                    "Detailed info is available in the Ferret User\\'s Guide., see Levels at\n" +
                    "http://ferret.pmel.noaa.gov/Ferret/documentation/users-guide/customizing-plots/CONTOURING#_VPINDEXENTRY_853",
                    hint: "Number of contours or (lo, hi, delta)",
                    name: "contour_levels", title: "Dependent axis scale")
            contour_levels.save(failOnError: true)

        }

        /*
  <!-- Default is no -->
  <optiondef name="mark_grid">
    <option>
      <help>Draw a mark at the middle of each grid cell on the plot.</help>
      <title>Mark grid points</title>
      <menu type="options" name="mark_grid">
        <item values="no">No</item>
        <item values="all">All Points</item>
        <item values="subsample">Subsampled</item>
      </menu>
    </option>
  </optiondef>
         */

        MenuOption mark_grid = MenuOption.findByName("mark_grid")

        if ( !mark_grid ) {

            mark_grid = new MenuOption([name        : "mark_grid",
                                            title       : "Mark grid points",
                                            help        : "Draw a mark at the middle of each grid cell on the plot.",
                                            defaultValue: "no"])



            MenuItem sf001 = new MenuItem([value:"no", title:"No"])
            MenuItem sf002 = new MenuItem([value:"all", title:"All Points"])
            MenuItem sf003 = new MenuItem([value:"subsample", title:"Subsampled"])

            mark_grid.addToMenuItems(sf001)
            mark_grid.addToMenuItems(sf002)
            mark_grid.addToMenuItems(sf003)

            mark_grid.save(failOnError: true)
        }
        /*
          <optiondef name="set_aspect">
    <option>
      <help>Have LAS calculate a suitable aspect ratio
Choices are:
<ul><li><b>Default</b> -- let LAS decide the aspect ratio
<li><b>Yes</b> -- Force LAS to calculate the aspect ratio of the plot based on the aspect ratio of the geographic region
<li><b>No</b> -- Do not change the aspect ratio based on the region.
</ul>
      </help>
      <title>Keep aspect ratio of region</title>
      <menu type="options" name="set_aspect">
        <item values="default">Default</item>
        <item values="1">Yes</item>
        <item values="0">No</item>
      </menu>
    </option>
  </optiondef>
         */

        YesNoOption set_aspect = YesNoOption.findByName("set_aspect")
        if ( !set_aspect ) {
            set_aspect = new YesNoOption([name        : "set_aspect",
                                           title       : "Keep aspect ratio of regio",
                                           help        : "Have LAS calculate a suitable aspect ratio\n" +
                                                   "Choices are:\n" +
                                                   "<ul><li><b>Default</b> -- let LAS decide the aspect ratio\n" +
                                                   "<li><b>Yes</b> -- Force LAS to calculate the aspect ratio of the plot based on the aspect ratio of the geographic region\n" +
                                                   "<li><b>No</b> -- Do not change the aspect ratio based on the region.\n" +
                                                   "</ul>",
                                           defaultValue: "yes"])

            set_aspect.save(failOnError: true)
        }

        /*
  <optiondef name="land_type">
    <option>
      <help>Style for drawing continents. Only applies to XY plots.
Choices are:
<ul><li><b>Default</b> -- let LAS decide
<li><b>None</b> -- don\'t draw continents
<li><b>Outline</b> -- draw continent outlines
<li><b>Filled</b> -- draw filled continents
</ul>
      </help>
      <title>Land fill style</title>
      <menu type="options" name="land_type">
        <item values="default">Default</item>
        <item values="none">None</item>
        <item values="contour">Outline</item>
        <item values="filled">Filled</item>
      </menu>
    </option>
  </optiondef>
         */
        MenuOption land_type = MenuOption.findByName("land_type")

        if ( !land_type ) {

            land_type = new MenuOption([name        : "land_type",
                                        title       : "Land fill style",
                                        help        : "Style for drawing continents. Only applies to XY plots.\n" +
                                                "Choices are:\n" +
                                                "<ul><li><b>Default</b> -- let LAS decide\n" +
                                                "<li><b>None</b> -- don\\'t draw continents\n" +
                                                "<li><b>Outline</b> -- draw continent outlines\n" +
                                                "<li><b>Filled</b> -- draw filled continents\n" +
                                                "</ul>",
                                        defaultValue: "contour"])



            MenuItem sf001 = new MenuItem([value:"none", title:"None"])
            MenuItem sf002 = new MenuItem([value:"contour", title:"Outline"])
            MenuItem sf003 = new MenuItem([value:"filled", title:"Filled"])

            land_type.addToMenuItems(sf001)
            land_type.addToMenuItems(sf002)
            land_type.addToMenuItems(sf003)

            land_type.save(failOnError: true)
        }
        /*
 <!-- Vector plot Options -->

  <optiondef name="vector_subsampling">
    <option>
      <help>Enter two numbers: m,n. Ferret draws subsampled vectors along two coordinate directions beginning with the first vector requested. By default, Ferret automatically thins vectors to achieve a clear plot; this option gives you control over the sampling; every m-th vector in the horizontal direction, every n-th in the vertical. For FLOWline-style plots, enter one number which will be the "density" parameter. Lower numbers of density result in fewer lines.
      </help>
      <title>Vector xskip,yskip</title>
      <textfield name="vector_subsampling"/>
    </option>
  </optiondef>
  */

        TextOption vector_subsampling = TextOption.findByName("vector_subsampling")

        if ( !vector_subsampling ) {

            vector_subsampling = new TextOption(help: "Enter two numbers: m,n. Ferret draws subsampled vectors along two coordinate directions beginning with the first vector requested. By default, Ferret automatically thins vectors to achieve a clear plot; this option gives you control over the sampling; every m-th vector in the horizontal direction, every n-th in the vertical. For FLOWline-style plots, enter one number which will be the \"density\" parameter. Lower numbers of density result in fewer lines.\n" +
                    "Detailed info is available in the Ferret User\\'s Guide., see Levels at\n" +
                    "http://ferret.pmel.noaa.gov/Ferret/documentation/users-guide/customizing-plots/CONTOURING#_VPINDEXENTRY_853",
                    name: "vector_subsampling",
                    hint: " xskip, yskip",
                    title: "Vector xskip,yskip")
            vector_subsampling.save(failOnError: true)

        }

   /*

  <optiondef name="vector_length">
    <option>
      <help> This associates the value with the standard vector length, normally one half inch. By default this is computed automatically based on the length of the vectors shown. On FLOWline-style plots, this number controls the length of the arrow-heads.
      </help>
      <title>Vector length scale</title>
      <textfield name="vector_length"/>
    </option>
  </optiondef>
  */
        TextOption vector_length = TextOption.findByName("vector_length")

        if ( !vector_length ) {

            vector_length = new TextOption(help: "This associates the value with the standard vector length, normally one half inch. By default this is computed automatically based on the length of the vectors shown. On FLOWline-style plots, this number controls the length of the arrow-heads.",
                    name: "vector_length",
                    hint: "Float value of length in inches",
                    title: "Vector length scale")
            vector_length.save(failOnError: true)

        }
   /*

  <optiondef name="vector_style">
    <option>
      <help> This option sets a choice of standard vector arrows, or a "flowline" style, which draws a pathline integration of a 2-dimensional instantaneous flow field (it is not a streamline calculation). The default is vector arrows.
      </help>
      <title>Vector style</title>
      <menu type="options" name="vector_style">
        <item values="default">Default</item>
        <item values="1">Flowlines</item>
        <item values="0">Arrows</item>
      </menu>
    </option>
  </optiondef>

         */

        MenuOption vector_style = MenuOption.findByName("vector_style")

        if ( !vector_style ) {

            vector_style = new MenuOption([name     : "vector_style",
                                        title       : "Vector style",
                                        help        : "This option sets a choice of standard vector arrows, or a \"flowline\" style, which draws a pathline integration of a 2-dimensional instantaneous flow field (it is not a streamline calculation). The default is vector arrows.",
                                        defaultValue: "arrows"])



            MenuItem sf001 = new MenuItem([value:"1", title:"Flowlines"])
            MenuItem sf002 = new MenuItem([value:"0", title:"Arrows"])

            vector_style.addToMenuItems(sf001)
            vector_style.addToMenuItems(sf002)

            vector_style.save(failOnError: true)
        }
    }

    def createResults() {

        ResultSet results_debug_image_mapscale_annotations = new ResultSet([name: "results_debug_image_mapscale_annotations"])

        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "text", suffix: ".txt"])
        Result plot_image = new Result([name: "plot_image", mime_type: "image/png", type: "image", suffix: ".png"])
        Result map_scale = new Result([name: "map_scale", mime_type: "text/xml", type: "xml", suffix: ".xml"])
        Result annotations = new Result([name: "annotations", mime_type: "text/xml", type: "xml", suffix: ".xml"])

        results_debug_image_mapscale_annotations.addToResults(debug)
        results_debug_image_mapscale_annotations.addToResults(plot_image)
        results_debug_image_mapscale_annotations.addToResults(map_scale)
        results_debug_image_mapscale_annotations.addToResults(annotations)


        results_debug_image_mapscale_annotations.save(failOnError: true)

    }

    def createProducts() {

        createResults()

        /*

        This are all the line plots, xyzt

         */
        
        // Sort order set by the product_order string. I use first three digits to sort the groups, and the next three digits for the operation within the group.
        Product t_line_plot = Product.findByName("Time")
        if (!t_line_plot) {

            t_line_plot = new Product([name: "Time", title: "Time", view: "t", data_view: "t", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200001"])
            Operation operation_t_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D"])

            // inherit="#expression,#interpolate_data,#image_format,#size,#use_graticules,#margins,#deg_min_sec"/
            // inherit="#Options_Default_7,#line_or_sym,#trend_line,#line_color,#line_thickness,#dep_axis_scale"


            ResultSet results_t_line_plot = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_t_line_plot) {
                ResultSet toned = new ResultSet(results_t_line_plot.properties)
                operation_t_line_plot.setResultSet(toned)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

            TextOption expression = TextOption.findByName("expression")
            if ( expression != null ) {
                operation_t_line_plot.addToTextOptions(new TextOption(expression.properties))
            }
            TextOption dep_axis_scale = TextOption.findByName("dep_axis_scale")
            if ( dep_axis_scale ) {
                operation_t_line_plot.addToTextOptions(new TextOption(dep_axis_scale.properties))
            }
            YesNoOption interpolate_data = YesNoOption.findByName("interpolate_data")
            if ( interpolate_data != null ) {
                operation_t_line_plot.addToYesNoOptions(new YesNoOption(interpolate_data.properties))
            }
            YesNoOption deg_min_sec = YesNoOption.findByName("deg_min_sec")
            if ( deg_min_sec ) {
                operation_t_line_plot.addToYesNoOptions(new YesNoOption(deg_min_sec.properties))
            }
            MenuOption grads = MenuOption.findByName("use_graticules")
            if ( grads ) {
                operation_t_line_plot.addToMenuOptions(new MenuOption(grads.properties))
            }
            MenuOption line_or_sym = MenuOption.findByName("line_or_sym")
            if ( line_or_sym ) {
                operation_t_line_plot.addToMenuOptions(new MenuOption(line_or_sym.properties))
            }
            MenuOption line_color = MenuOption.findByName("line_color")
            if ( line_color ) {
                operation_t_line_plot.addToMenuOptions(new MenuOption(line_color.properties))
            }
            MenuOption line_thickness = MenuOption.findByName("line_thickness")
            if ( line_thickness ) {
                operation_t_line_plot.addToMenuOptions(new MenuOption(line_thickness.properties))
            }

            t_line_plot.addToOperations(operation_t_line_plot)
            t_line_plot.save(failOnError: true)
        }
        
        
        Product z_line_plot = Product.findByName("Longitude")
        if (!z_line_plot) {

            z_line_plot = new Product([name: "Z", title: "Z", view: "z", data_view: "z", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200004"])
            Operation operation_z_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D"])

            ResultSet results_z_line_plot = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_z_line_plot) {
                ResultSet zoned = new ResultSet(results_z_line_plot.properties)
                operation_z_line_plot.setResultSet(zoned)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

            TextOption expression = TextOption.findByName("expression")
            if ( expression != null ) {
                operation_z_line_plot.addToTextOptions(new TextOption(expression.properties))
            }
            TextOption dep_axis_scale = TextOption.findByName("dep_axis_scale")
            if ( dep_axis_scale ) {
                operation_z_line_plot.addToTextOptions(new TextOption(dep_axis_scale.properties))
            }
            YesNoOption interpolate_data = YesNoOption.findByName("interpolate_data")
            if ( interpolate_data != null ) {
                operation_z_line_plot.addToYesNoOptions(new YesNoOption(interpolate_data.properties))
            }
            YesNoOption deg_min_sec = YesNoOption.findByName("deg_min_sec")
            if ( deg_min_sec ) {
                operation_z_line_plot.addToYesNoOptions(new YesNoOption(deg_min_sec.properties))
            }
            MenuOption grads = MenuOption.findByName("use_graticules")
            if ( grads ) {
                operation_z_line_plot.addToMenuOptions(new MenuOption(grads.properties))
            }
            MenuOption line_or_sym = MenuOption.findByName("line_or_sym")
            if ( line_or_sym ) {
                operation_z_line_plot.addToMenuOptions(new MenuOption(line_or_sym.properties))
            }
            MenuOption line_color = MenuOption.findByName("line_color")
            if ( line_color ) {
                operation_z_line_plot.addToMenuOptions(new MenuOption(line_color.properties))
            }
            MenuOption line_thickness = MenuOption.findByName("line_thickness")
            if ( line_thickness ) {
                operation_z_line_plot.addToMenuOptions(new MenuOption(line_thickness.properties))
            }

            z_line_plot.addToOperations(operation_z_line_plot)
            z_line_plot.save(failOnError: true)
        }
        
        
        Product y_line_plot = Product.findByName("Longitude")
        if (!y_line_plot) {

            y_line_plot = new Product([name: "Latitude", title: "Latitude", view: "y", data_view: "y", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200003"])
            Operation operation_y_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D"])

            ResultSet results_y_line_plot = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_y_line_plot) {
                ResultSet yoned = new ResultSet(results_y_line_plot.properties)
                operation_y_line_plot.setResultSet(yoned)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

            TextOption expression = TextOption.findByName("expression")
            if ( expression != null ) {
                operation_y_line_plot.addToTextOptions(new TextOption(expression.properties))
            }
            TextOption dep_axis_scale = TextOption.findByName("dep_axis_scale")
            if ( dep_axis_scale ) {
                operation_y_line_plot.addToTextOptions(new TextOption(dep_axis_scale.properties))
            }
            YesNoOption interpolate_data = YesNoOption.findByName("interpolate_data")
            if ( interpolate_data != null ) {
                operation_y_line_plot.addToYesNoOptions(new YesNoOption(interpolate_data.properties))
            }
            YesNoOption deg_min_sec = YesNoOption.findByName("deg_min_sec")
            if ( deg_min_sec ) {
                operation_y_line_plot.addToYesNoOptions(new YesNoOption(deg_min_sec.properties))
            }
            MenuOption grads = MenuOption.findByName("use_graticules")
            if ( grads ) {
                operation_y_line_plot.addToMenuOptions(new MenuOption(grads.properties))
            }
            MenuOption line_or_sym = MenuOption.findByName("line_or_sym")
            if ( line_or_sym ) {
                operation_y_line_plot.addToMenuOptions(new MenuOption(line_or_sym.properties))
            }
            MenuOption line_color = MenuOption.findByName("line_color")
            if ( line_color ) {
                operation_y_line_plot.addToMenuOptions(new MenuOption(line_color.properties))
            }
            MenuOption line_thickness = MenuOption.findByName("line_thickness")
            if ( line_thickness ) {
                operation_y_line_plot.addToMenuOptions(new MenuOption(line_thickness.properties))
            }


            y_line_plot.addToOperations(operation_y_line_plot)
            y_line_plot.save(failOnError: true)
        }
        
        Product x_line_plot = Product.findByName("Longitude")
        if (!x_line_plot) {

            x_line_plot = new Product([name: "Longitude", title: "Longitude", view: "x", data_view: "x", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200002"])
            Operation operation_x_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D"])

            ResultSet results_x_line_plot = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_x_line_plot) {
                ResultSet xoned = new ResultSet(results_x_line_plot.properties)
                operation_x_line_plot.setResultSet(xoned)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

            TextOption expression = TextOption.findByName("expression")
            if ( expression != null ) {
                operation_x_line_plot.addToTextOptions(new TextOption(expression.properties))
            }
            TextOption dep_axis_scale = TextOption.findByName("dep_axis_scale")
            if ( dep_axis_scale ) {
                operation_x_line_plot.addToTextOptions(new TextOption(dep_axis_scale.properties))
            }
            YesNoOption interpolate_data = YesNoOption.findByName("interpolate_data")
            if ( interpolate_data != null ) {
                operation_x_line_plot.addToYesNoOptions(new YesNoOption(interpolate_data.properties))
            }
            YesNoOption deg_min_sec = YesNoOption.findByName("deg_min_sec")
            if ( deg_min_sec ) {
                operation_x_line_plot.addToYesNoOptions(new YesNoOption(deg_min_sec.properties))
            }
            MenuOption grads = MenuOption.findByName("use_graticules")
            if ( grads ) {
                operation_x_line_plot.addToMenuOptions(new MenuOption(grads.properties))
            }
            MenuOption line_or_sym = MenuOption.findByName("line_or_sym")
            if ( line_or_sym ) {
                operation_x_line_plot.addToMenuOptions(new MenuOption(line_or_sym.properties))
            }
            MenuOption line_color = MenuOption.findByName("line_color")
            if ( line_color ) {
                operation_x_line_plot.addToMenuOptions(new MenuOption(line_color.properties))
            }
            MenuOption line_thickness = MenuOption.findByName("line_thickness")
            if ( line_thickness ) {
                operation_x_line_plot.addToMenuOptions(new MenuOption(line_thickness.properties))
            }

            x_line_plot.addToOperations(operation_x_line_plot)
            x_line_plot.save(failOnError: true)
        }

        /*

        Difference plot, this is "hidden" from the UI and called when an XY grid is to be differenced.
        All the decorations about the title and ui_group are superfluous

         */
        Product compare_plot = Product.findByName("Compare_Plot")
        if (!compare_plot) {

            // #expression,#interpolate_data,#image_format,#size,#use_ref_map,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            compare_plot = new Product([name: "Compare_Plot", title: "Latitude-Longitude", view: "xy", data_view: "xy", ui_group: "Maps", geometry: GeometryType.GRID, hidden: true, product_order: "999999"]) // Not in ui, order unnecessary
            Operation operation_comparePlot = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot"])

            ResultSet results_compare_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_compare_plot_2d) {
                ResultSet twod = new ResultSet(results_compare_plot_2d.properties)
                operation_comparePlot.setResultSet(twod)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }
            MenuOption cpalettes = MenuOption.findByName("palette")
            if (cpalettes) {
//                MenuOption mo = new MenuOption(palettes.properties)
//                mo.save(failOnError: true)
                operation_comparePlot.addToMenuOptions(cpalettes)
            } else {
                log.error("Palette options not available. Did you use service menthod createOptions before calling createOperations?")
            }

            YesNoOption interpolate = YesNoOption.findByName("interpolate_data")
            if (interpolate) {
                operation_comparePlot.addToYesNoOptions(interpolate)
            } else {
                log.error("Interpolate option not available. Did you use service menthod createOptions before calling createOperations?")
            }

            TextOption fill_levels = TextOption.findByName("fill_levels")
            if ( fill_levels ) {
                operation_comparePlot.addToTextOptions(fill_levels)
            } else {
                log.error("Fill levels option not available. Did you use service menthod createOptions before calling createOperations?")
            }


            compare_plot.addToOperations(operation_comparePlot)
            compare_plot.save(failOnError: true)
        }

        /*

        Regular old, lat/lon map xy

        */
        Product plot_2d_xy = Product.findByGeometryAndViewAndData_viewAndHidden(GeometryType.GRID, "xy", "xy", false)

        if (!plot_2d_xy) {

            // #expression,#interpolate_data,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            // Later:
            // #image_format,#size,#use_ref_map,

            plot_2d_xy = new Product([name: "Plot_2D_XY", title: "Latitude-Longitude", view: "xy", data_view: "xy", ui_group: "Maps", geometry: GeometryType.GRID, product_order: "100001"])

            Operation operation_plot_2d_xy = new Operation([output_template: "xy_zoom", service_action: "Plot_2D_XY"])

            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if (results_plot_2d) {
                ResultSet twod = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_xy.setResultSet(twod)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }

            TextOption expression = TextOption.findByName("expression")
            if ( expression != null ) {
                operation_plot_2d_xy.addToTextOptions(new TextOption(expression.properties))
            }

            YesNoOption interpolate = YesNoOption.findByName("interpolate_data")
            if (interpolate) {
                operation_plot_2d_xy.addToYesNoOptions(interpolate)
            } else {
                log.error("Interpolate option not available. Did you use service menthod createOptions before calling createOperations?")
            }
            MenuOption use_graticules = MenuOption.findByName("use_graticules")
            if ( use_graticules ) {
                operation_plot_2d_xy.addToMenuOptions(new MenuOption(use_graticules.properties))
            }

            YesNoOption margins = YesNoOption.findByName("margins")
            if ( margins ) {
                operation_plot_2d_xy.addToYesNoOptions(margins)
            } else {
                log.error("Margins option not available. Did you use service menthod createOptions before calling createOperations?")
            }

            YesNoOption deg_min_sec = YesNoOption.findByName("deg_min_sec")
            if ( deg_min_sec ) {
                operation_plot_2d_xy.addToYesNoOptions(new YesNoOption(deg_min_sec.properties))
            }

            MenuOption palettes = MenuOption.findByName("palette")
            if (palettes) {
//                MenuOption mo = new MenuOption(palettes.properties)
//                mo.save(failOnError: true)
                operation_plot_2d_xy.addToMenuOptions(palettes)
            } else {
                log.error("Palette options not available. Did you use service menthod createOptions before calling createOperations?")
            }

            MenuOption contour_style = MenuOption.findByName("contour_style")
            if ( contour_style ) {
                operation_plot_2d_xy.addToMenuOptions(new MenuOption(contour_style.properties))
            } else {
                log.error("Contour style options not available. Did you use service menthod createOptions before calling createOperations?")
            }

            TextOption fill_levels = TextOption.findByName("fill_levels")
            if ( fill_levels ) {
                operation_plot_2d_xy.addToTextOptions(fill_levels)
            } else {
                log.error("Fill levels option not available. Did you use service menthod createOptions before calling createOperations?")
            }

            TextOption contour_levels = TextOption.findByName("contour_levels")
            if ( contour_levels ) {
                operation_plot_2d_xy.addToTextOptions(new TextOption(contour_levels.properties))
            } else {
                log.error("Contour levels option not available. Did you use service menthod createOptions before calling createOperations?")
            }

            MenuOption mark_grid = MenuOption.findByName("mark_grid")
            if ( mark_grid ) {
                operation_plot_2d_xy.addToMenuOptions(mark_grid)
            } else {
                log.error("Contour levels option not available. Did you use service menthod createOptions before calling createOperations?")
            }

            YesNoOption set_aspect = YesNoOption.findByName("set_aspect")
            if ( set_aspect ) {
                operation_plot_2d_xy.addToYesNoOptions(new YesNoOption(set_aspect.properties))
            } else {
                log.error("Set aspect option not available. Did you use service menthod createOptions before calling createOperations?")

            }

            MenuOption land_type = MenuOption.findByName("land_type")
            if ( land_type ) {
                operation_plot_2d_xy.addToMenuOptions(land_type.properties)
            } else {
                log.error("Land type option not available. Did you use service menthod createOptions before calling createOperations?")
            }

            plot_2d_xy.addToOperations(operation_plot_2d_xy)
            plot_2d_xy.save(failOnError: true)



        }

        /*
        
        Vertical cross sections xz, yz
        
         */
        Product plot_2d_xz = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "xz", "xz")

        if ( !plot_2d_xz) {
            plot_2d_xz = new Product([name: "Plot_2D_xz", title: "Longitude-z", ui_group: "Vertical Cross Sections", view: "xz", data_view: "xz", geometry: GeometryType.GRID, product_order: "300001"])
            Operation operation_plot_2d_xz = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_xz.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }
            MenuOption palettes = MenuOption.findByName("palette")
            if (palettes) {
                operation_plot_2d_xz.addToMenuOptions(palettes)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createOptions before calling createOperations?")
            }
            plot_2d_xz.addToOperations(operation_plot_2d_xz)
            plot_2d_xz.save(failOnError: true)

        }

        Product plot_2d_yz = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "yz", "yz")

        if ( !plot_2d_yz) {
            plot_2d_yz = new Product([name: "Plot_2D_yz", title: "Latitude-z", ui_group: "Vertical Cross Sections", view: "yz", data_view: "yz", geometry: GeometryType.GRID, product_order: "300002"])
            Operation operation_plot_2d_yz = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_yz.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }
            MenuOption palettes = MenuOption.findByName("palette")
            if (palettes) {
                operation_plot_2d_yz.addToMenuOptions(palettes)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createOptions before calling createOperations?")
            }
            plot_2d_yz.addToOperations(operation_plot_2d_yz)
            plot_2d_yz.save(failOnError: true)

        }

        /*
        
        Hovmöller Diagrams, xt, yt and zt
        
         */
        Product plot_2d_xt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "xt", "xt")

        if ( !plot_2d_xt) {
            plot_2d_xt = new Product([name: "Plot_2D_xt", title: "Longitude-time", ui_group: "Hovmöller Diagram", view: "xt", data_view: "xt", geometry: GeometryType.GRID, product_order: "400001"])
            Operation operation_plot_2d_xt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_xt.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }
            MenuOption palettes = MenuOption.findByName("palette")
            if (palettes) {
                operation_plot_2d_xt.addToMenuOptions(palettes)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createOptions before calling createOperations?")
            }
            plot_2d_xt.addToOperations(operation_plot_2d_xt)
            plot_2d_xt.save(failOnError: true)

        }

        Product plot_2d_yt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "yt", "yt")

        if ( !plot_2d_yt) {
            plot_2d_yt = new Product([name: "Plot_2D_yt", title: "Latitude-time", ui_group: "Hovmöller Diagram", view: "yt", data_view: "yt", geometry: GeometryType.GRID, product_order: "400002"])
            Operation operation_plot_2d_yt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_yt.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }
            MenuOption palettes = MenuOption.findByName("palette")
            if (palettes) {
                operation_plot_2d_yt.addToMenuOptions(palettes)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createOptions before calling createOperations?")
            }
            plot_2d_yt.addToOperations(operation_plot_2d_yt)
            plot_2d_yt.save(failOnError: true)

        }

        Product plot_2d_zt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "zt", "zt")

        if ( !plot_2d_zt) {
            plot_2d_zt = new Product([name: "Plot_2D_zt", title: "Z-time", ui_group: "Hovmöller Diagram", view: "zt", data_view: "zt", geometry: GeometryType.GRID, product_order: "400003"])
            Operation operation_plot_2d_zt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_zt.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createReults before calling createOperations?")
            }
            MenuOption palettes = MenuOption.findByName("palette")
            if (palettes) {
                operation_plot_2d_zt.addToMenuOptions(palettes)
            } else {
                log.error("Results sets not available. Did you use the results service menthod createOptions before calling createOperations?")
            }
            plot_2d_zt.addToOperations(operation_plot_2d_zt)
            plot_2d_zt.save(failOnError: true)

        }


        // See if the product already exists by name and title.  This implies name and title combinations should be unique.
//
//        Product timeseries_plot = Product.findByNameAndTitle("Timeseries Plot", "Timeseries")
//        if ( !timeseries_plot ) {
//            timeseries_plot = new Product([name: "Timeseries Plot", title: "Timeseries", ui_group: "Line Plots", view: "t", data_view: "xyt", geometry: GeometryType.TIMESERIES])
//            Operation operation_timeseries_plot = new Operation([service_action: "client_plot"])
//            timeseries_plot.addToOperations(operation_timeseries_plot)
//            timeseries_plot.save(failOnError: true)
//        }

        Product charts_timeseries_plot = Product.findByNameAndTitle("Charts Timeseries Plot", "Timeseries Plot");
        if ( !charts_timeseries_plot ) {
            charts_timeseries_plot = new Product([name: "Timeseries Plot", title: "Timeseries Plot", ui_group: "Line Plots", view: "t", data_view: "xyt", geometry: GeometryType.TIMESERIES, product_order: "dontknowyet"])
            Operation operation_timeseries_plot = new Operation([service_action: "client_plot"])
            charts_timeseries_plot.addToOperations(operation_timeseries_plot)
            charts_timeseries_plot.save(failOnError: true)
        }
    }
    def loadDefaultLasDatasets() {

        // Only load default datasets if none exist...
        def count = Dataset.count()

        log.debug("Data set count = "+count);


        if ( count  == 0 ) {

            log.debug("No data sets found. Setting up default data sets.  Entered method...");

            // Set up the default LAS
            // We're going to use fer_data to find them, so Ferret must be configured first.
            Site site = Site.first();
            if (!site) {

                site = new Site([title: "Default LAS Site"])
                // No site configured, so build the default site.

                FerretEnvironment ferretEnvironment = FerretEnvironment.first()
                if (!ferretEnvironment) {
                    return
                }

                def dsets = ferretEnvironment.fer_dsets

                def coads
                def ocean_atlas
                def leetmaaSurface
                def leetmaaDepth


                if (dsets) {
                    if (dsets.contains(" ")) {
                        def lookin = dsets.split("\\s")
                        lookin.each { datadir ->
                            if (new File("$datadir" + File.separator + "data" + File.separator + "coads_climatology.cdf").exists()) {
                                coads = "$datadir" + File.separator + "data" + File.separator + "coads_climatology.cdf"
                                ocean_atlas = "$datadir" + File.separator + "data" + File.separator + "ocean_atlas_subset.nc"
                            }
                        }
                    } else {
                        coads = "$dsets" + File.separator + "data" + File.separator + "coads_climatology.cdf"
                        ocean_atlas = "$dsets" + File.separator + "data" + File.separator + "ocean_atlas_subset.nc"

                    }
                } else {

                }
                if (coads) {
                    log.debug("Ingesting COADS")
                    def coadshash = IngestService.getDigest(coads)
                    Dataset coadsDS = Dataset.findByHash(coadshash)
                    if (!coadsDS) {
                        coadsDS = ingestService.ingest(coads)
                        coadsDS.setTitle("COADS")
                        coadsDS.setStatus(Dataset.INGEST_FINISHED)
                        coadsDS.save(flush: true)
                    }
                    if (coadsDS) {
                        site.addToDatasets(coadsDS)
                    }
                }
                if (ocean_atlas) {
                    log.debug("Ingesting the ocean atlas subset.")
                    def oahash = IngestService.getDigest(ocean_atlas)
                    Dataset ocean_atlasDS = Dataset.findByHash(oahash)
                    if (!ocean_atlasDS) {
                        ocean_atlasDS = ingestService.ingest(ocean_atlas)
                        ocean_atlasDS.setTitle("Ocean Atlas Subset")
                        ocean_atlasDS.setStatus(Dataset.INGEST_FINISHED)
                        ocean_atlasDS.save(flush: true)
                    }
                    if (ocean_atlasDS) {
                        site.addToDatasets(ocean_atlasDS)
                    }
                }

//                log.debug("Ingesting carbon tracker THREDDS catalog.")
//                def carbonThredds = "http://ferret.pmel.noaa.gov/pmel/thredds/carbontracker.xml"
//                def carbon = Dataset.findByHash(IngestService.getDigest(carbonThredds))
//                if ( !carbon ) {
//                    carbon = ingestService.ingestFromThredds(carbonThredds, null)
//                    carbon.setStatus(Dataset.INGEST_FINISHED)
//                    carbon.save(flush: true)
//                    site.addToDatasets(carbon)
//                }

                log.debug("Ingesting example Timeseries DSG from ERDDAP")
                def ts = "http://ferret.pmel.noaa.gov/engineering/erddap/tabledap/15min_w20_fdd7_a060"
                List<AddProperty> properties = new ArrayList<>()
                AddProperty hours = new AddProperty([name: "hours", value: ".25"])
                properties.add(hours)
                AddProperty display_hi = new AddProperty([name: "display_hi", value: "2018-02-20T00:00:00.000Z"])
                properties.add(display_hi)
                AddProperty display_lo = new AddProperty(([name: "display_lo", value: "2018-02-05T00:00:00.000Z"]))
                properties.add(display_lo)
                def dsgDataset = Dataset.findByHash(IngestService.getDigest(ts))
                if ( !dsgDataset ) {
                    dsgDataset = ingestService.ingestFromErddap(ts, properties)
                    dsgDataset.setStatus(Dataset.INGEST_FINISHED)
                    dsgDataset.save(flush: true)
                    site.addToDatasets(dsgDataset)
                }

//                log.debug("Ingesting UAF THREDDS server")
//                //def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalog.xml"
//                def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/ecowatch.ncddc.noaa.gov/thredds/oceanNomads/aggs/catalog_g_ncom_aggs.xml"
//                // def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/woa13/catalog.xml"
//                def erddap = "http://upwell.pfeg.noaa.gov/erddap/"
//                def uafDataset = Dataset.findByHash(IngestService.getDigest(uaf))
//                if ( ! uafDataset ) {
//                    uafDataset = ingestService.ingestFromThredds(uaf, erddap)
//                    uafDataset.setStatus(Dataset.INGEST_FINISHED)
//                    uafDataset.save(flush: true)
//                    site.addToDatasets(uafDataset)
//                }

                ingestService.cleanup(site)
                site.save(failOnError: true)
            }
        }
    }
}
