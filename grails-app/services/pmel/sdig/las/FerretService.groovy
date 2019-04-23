package pmel.sdig.las

import com.sun.glass.ui.Application
import grails.gorm.transactions.Transactional
import grails.util.Holders


@Transactional
class FerretService {

	DateTimeService dateTimeService
	ResultsService resultsService
	ProductService productService

	def ferretColorsMap = [
			red:"(69.7, 7.58, 22.73)",
			green:"(19.05, 57.14, 23.81)",
			yellow:"(50.50, 44.55, 4.95)",
			blue:"(17.54, 25.92, 56.54)",
			orange:"(57.78, 30.66, 11.56)",
			purple:"(40.85, 8.45, 50.70)",
			cyan: "(12.64,40.61,46.74)",
			magenta:"(46.15,9.62,44.23)",
			lime:"(38.28,47.90,13.83)",
			pink:"(39.68,30.16,30.16)",
			teal:"(19.07,41.69,39.24)",
			lavendar:"(34.07,28.15,37.78)",
			brown:"(53.29,34.26,12.46)",
			beige:"(36.17,35.46,28.37)",
			maroon:"(50, 0, 0)",
			mint: "(27.42, 41.13,31.45)",
			olive: "(50, 50, 0)",
			apricot: "(39.35, 33.33, 27.31)",
			navy: "(0, 0, 45)"
	]
	def ferretColorsNames=[
			"red",
			"green",
			"yellow",
			"blue",
			"orange",
			"purple",
			"cyan",
			"magenta",
			"lime",
			"pink",
			"teal",
			"lavendar",
			"brown",
			"beige",
			"maroon",
			"mint",
			"olive",
			"apricot",
			"navy"
	]
	def ferretColorsValues=[
			"(69.7, 7.58, 22.73)",
			"(19.05, 57.14, 23.81)",
			"(50.50, 44.55, 4.95)",
			"(17.54, 25.92, 56.54)",
			"(57.78, 30.66, 11.56)",
			"(40.85, 8.45, 50.70)",
			"(12.64,40.61,46.74)",
			"(46.15,9.62,44.23)",
			"(38.28,47.90,13.83)",
			"(39.68,30.16,30.16)",
			"(19.07,41.69,39.24)",
			"(34.07,28.15,37.78)",
			"(53.29,34.26,12.46)",
			"(36.17,35.46,28.37)",
			"(50, 0, 0)",
			"(27.42, 41.13,31.45)",
			"(50, 50, 0)",
			"(39.35, 33.33, 27.31)",
			"(0, 0, 45)"
	]
	def Map runScript (StringBuffer script) {

		def result = [:]

		def ferret = Ferret.first()


		File sp = File.createTempFile("script", ".jnl", new File(ferret.tempDir));

		sp.withWriter { out ->
			out.writeLine(script.toString().stripIndent())
		}

		Task task = new Task(ferret, sp.getAbsolutePath() )
		try {
			task.run()
		} catch (Exception e ) {
			task.appendError("ERROR: Exception running task.  ")
			task.appendError(e.getMessage())
		}


		if ( task.hasErrors() ) {
			result["error"] = true
			result["message"] = task.getErrors().toString();
		} else {
			result["error"] = false
		}

		return result

	}

	def getFerretColor(String key) {
		ferretColorsMap.get(key)
	}
	def getFerretColorName(int index) {
		index = index % ferretColorsNames.size()
		ferretColorsNames.get(index)
	}
	def getFerretColorValue(int index) {
		index = index % ferretColorsValues.size()
		ferretColorsValues.get(index)
	}

	def makeThumbnail(String dhash, String vhash) {
		File outputFile = Holders.grailsApplication.mainContext.getResource("output").file
		String outputPath = outputFile.getAbsolutePath()
		Dataset dataset = Dataset.findByHash(dhash)
		if ( dataset ) {
			Variable variable = dataset.variables.find{it.hash == vhash}
			if ( variable ) {


				String variable_url = variable.getUrl()
				String variable_name = variable.getName()
				String variable_title = variable.getTitle()

				def x = variable.getGeoAxisX()
				def y = variable.getGeoAxisY()
				def z = variable.getVerticalAxis()
				def t = variable.getTimeAxis()

				File ddir = new File("${outputPath}${File.separator}${dhash}")
				if ( !ddir.exists() ) ddir.mkdirs()


				def hash = "${dhash}${File.separator}${vhash}"

				def opname;
				def opaction;
				def opview;
				if (variable.getIntervals().startsWith("xy")) {
					opname = "Plot_2D_XY"
					opaction = "Plot_2D_XY"
					opview = "xy"
				} else if (variable.getIntervals().startsWith("t")) {
					opname = "Time"
					opaction = "Plot_1D"
					opview = "t"
				}

				StringBuffer jnl = new StringBuffer()

				jnl.append("DEFINE SYMBOL data_0_dataset_name = ${dataset.title}\n")
				jnl.append("DEFINE SYMBOL data_0_dataset_url = ${variable_url}\n")
				jnl.append("DEFINE SYMBOL data_0_grid_type = regular\n")
				jnl.append("DEFINE SYMBOL data_0_name = ${variable_name}\n")
				jnl.append("DEFINE SYMBOL data_0_ID = ${variable_name}\n")
				jnl.append("DEFINE SYMBOL data_0_region = region_0\n")
				jnl.append("DEFINE SYMBOL data_0_title = ${variable_title}\n")
				if (variable.units) jnl.append("DEFINE SYMBOL data_0_units = ${variable.units}\n")
				jnl.append("DEFINE SYMBOL data_0_url = ${variable_url}\n")
				jnl.append("DEFINE SYMBOL data_0_var = ${variable_name}\n")


				if (t) {
					String sd = dateTimeService.ferretFromIso(t.getStart(), t.getCalendar())
					String fd = dateTimeService.ferretFromIso(t.getEnd(), t.getCalendar())

					if (opview.equals("xy")) {
						// Use the last day
						jnl.append("DEFINE SYMBOL region_0_t_lo = ${fd}\n")
					} else if (opview.equals("t")) {
						// Use the first day, timeseries the entire time range. :-)
						jnl.append("DEFINE SYMBOL region_0_t_lo = ${sd}\n")
					}
					jnl.append("DEFINE SYMBOL region_0_t_hi = ${fd}\n")
				}
				if (x) {
					jnl.append("DEFINE SYMBOL region_0_x_hi = ${x.getMax()}\n")
					jnl.append("DEFINE SYMBOL region_0_x_lo = ${x.getMin()}\n")
				}
				if (y) {
					jnl.append("DEFINE SYMBOL region_0_y_hi = ${y.getMax()}\n")
					jnl.append("DEFINE SYMBOL region_0_y_lo = ${y.getMin()}\n")
				}
				if (z) {
					jnl.append("DEFINE SYMBOL region_0_z_lo = ${z.getMin()}\n")
					jnl.append("DEFINE SYMBOL region_0_z_hi = ${z.getMin()}\n")
				}


				jnl.append("DEFINE SYMBOL data_count = 1\n")
				jnl.append("DEFINE SYMBOL ferret_annotations = file\n")
				if (variable.getIntervals().startsWith("xy")) {
					jnl.append("DEFINE SYMBOL ferret_service_action = Plot_2D_XY\n")
					jnl.append("DEFINE SYMBOL operation_name = Plot_2D_XY\n")

				}
				if (variable.getIntervals().startsWith("t")) {
					jnl.append("DEFINE SYMBOL ferret_service_action = Plot_1D\n")
					jnl.append("DEFINE SYMBOL operation_name = Time\n")
				}
				jnl.append("DEFINE SYMBOL ferret_size = .456\n")
				jnl.append("DEFINE SYMBOL ferret_view = ${opview}\n")
				jnl.append("DEFINE SYMBOL las_debug = false\n")
				jnl.append("DEFINE SYMBOL las_output_type = xml\n")
				jnl.append("DEFINE SYMBOL operation_ID = ${opname}\n")
				jnl.append("DEFINE SYMBOL operation_key = ${dhash}/${vhash}\n")
				jnl.append("DEFINE SYMBOL operation_service = ferret\n")


				jnl.append("DEFINE SYMBOL ferret_service_action = ${opaction}\n")
				jnl.append("DEFINE SYMBOL operation_name = ${opname}\n")

				// TODO this has to come from the config
				jnl.append("DEFINE SYMBOL product_server_ps_timeout = 3600\n")
				jnl.append("DEFINE SYMBOL product_server_ui_timeout = 10\n")
				jnl.append("DEFINE SYMBOL product_server_use_cache = true\n")


				def resultSet = resultsService.getThumbnailResults()
				resultSet.results.each { Result result ->
					// All we care about is the plot
					result.url = "output${File.separator}${hash}_${result.name}${result.suffix}"
					result.filename = "${outputPath}${File.separator}${hash}_${result.name}${result.suffix}"
				}

				// TODO separate thumbnails into their own directory and then again by data set hash
				for (int i = 0; i < resultSet.getResults().size(); i++) {

					def result = resultSet.getResults().get(i)

					jnl.append("DEFINE SYMBOL result_${result.name}_ID = ${result.name}\n")
					jnl.append("DEFINE SYMBOL result_${result.name}_filename = ${outputPath}${File.separator}${hash}_${result.name}${result.suffix}\n")
					jnl.append("DEFINE SYMBOL result_${result.name}_type = ${result.type}\n")

				}
				jnl.append("go ${opaction}\n")


				def ferretResult = runScript(jnl)
				def error = ferretResult["error"];
				if (!error) {
					ResultSet allResults = new ResultSet()
					addResults(resultSet, allResults, "${opname}")
					Result r = allResults.results.find { it.name == "plot_image" }
					return r.getUrl()
				}
			}

		}
		return null;
	}
	def makeAndSaveThumbnail(Dataset dataset, Variable variable) {
		def url = makeThumbnail(dataset.hash, variable.hash)
		if ( url ) {
			variable.setUrl(url)
			variable.save()
		}
	}
	def addResults(ResultSet resultSet, ResultSet allResults, String product) {
		// Loop through the results and treat them according to their name and type
		// Only some results require post-processing special treatment
		def results = resultSet.results
		for (int i = 0; i < results.size(); i++) {
			def result = results[i]
			def name = result.name
			if (name == "map_scale") {
				def mapScale = productService.makeMapScale(result.filename)
				allResults.setMapScale(mapScale)
			} else if (name == "annotations") {
				def annotationGroups = productService.makeAnnotations(result.filename);
				allResults.setAnnotationGroups(annotationGroups)
				// TODO this name has to be more specific to the animation product
			} else if (name == "ferret_listing" && (product == "Animation_2D_XY" || product == "Animation_2D_XY_vector") ) {
				def animation = productService.makeAnimationList(result.filename)
				allResults.setAnimation(animation)
			}
			allResults.addToResults(result)
		}
	}
	def makeThumbnails() {
		log.debug("STARTED making thumbnails for all existing variables.")
		Variable.list().each{Variable variable ->
			def dhash = variable.getDataset().getHash();
			def vhash = variable.getHash()
			String url = makeThumbnail(dhash, vhash)
			if ( url ) {
				variable.setThumbnail(url)
				variable.save()
			}
		}
		log.debug("FINISHED making thumbnails for all existing variables.")
	}
}

