package pmel.sdig.las

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper

@Transactional
class AdminController {
    IngestService ingestService
    UpdateDatasetJobService updateDatasetJobService;
    def index() {
        respond "You logged in."
    }
    def saveSite() {
        def requestJSON = request.JSON
        def map = requestJSON as Map
        def site = Site.get(1);
        site.properties = map;
        site.save(flush: true, failOnError: true);
        JSON.use("deep") {
            render site as JSON
        }
    }
    def status() {
        List<Dataset> failedDatasets = Dataset.withCriteria{
            eq("variableChildren", true)
            eq("status", Dataset.INGEST_FAILED)
            isEmpty("variables")
        }
        render view: "status", model: [failedDatasets: failedDatasets]
    }
    def resetFailed() {
        List<Dataset> failedDatasets = Dataset.withCriteria{
            eq("variableChildren", true)
            eq("status", Dataset.INGEST_FAILED)
            isEmpty("variables")
        }
        failedDatasets.each {Dataset d ->
            d.setStatus(Dataset.INGEST_NOT_STARTED)
            d.save(flush: true)
        }
        render view: "status", model: [failedDatasets: failedDatasets]
    }
    def saveDatasetUpdateSpec() {
        def requestJSON = request.JSON
        def map = requestJSON as Map;
        /*
         This payload is a map with the following keys:
                dataset - is a the ID of the data set to be modified.
                property - is a object that looks like a DatasetProperty that should be of the form:
                type: "update"
                name: "cron"
                value: some string of the cron spec for frequency of updates e.g. "30 * * * *" thirty past the hour, every hour
         */
        def id = map.get("dataset")
        def cron_spec = map.get("cron_spec")

        Dataset d = Dataset.get(id)
        def done = false;
        if ( d.datasetProperties ) {
            Iterator<DatasetProperty> it = d.datasetProperties.iterator()
            while (it.hasNext()) {
                def p = it.next()
                if ( p.type == "update") {
                    done = true
                    p.type = "update"
                    p.name = "cron_spec"
                    p.value = cron_spec
                    p.save(flush: true)
                    d.save(flush: true)
                    if ( !p.value.isEmpty() ) {
                        updateDatasetJobService.addDatasetUpdate(d.id, p.value)
                    } else {
                        updateDatasetJobService.unscheuleUpdate(d.id)
                    }
                }
            }
        }
        if ( !done ) {
            DatasetProperty p = new DatasetProperty()
            p.type = "update"
            p.name = "cron_spec"
            p.value = cron_spec
            d.addToDatasetProperties(p)
            d.save(flush: true)
            if ( !p.value.isEmpty() ) {
                updateDatasetJobService.addDatasetUpdate(d.id, p.value)
            } else {
                updateDatasetJobService.unscheuleUpdate(d.id)
            }
        }

        def parent
        if ( d.parent == null ) {
            parent = Site.get(1)
        } else {
            parent = Dataset.get(d.parent.id);
        }
        JSON.use("deep") {
            render parent as JSON
        }
    }
    def saveDataset() {
        def requestJSON = request.JSON
        def map = requestJSON as Map;

        /*
         This payload is a map with the following keys:
                dataset - is a map of the changes primitive properties (string and numbers) it should include the
                          which is needed to get the data set to modify
                variables - is a map of maps, each map modifies one of the variables in this data set. id is included
                            as they outer key
                geoAxisX - similar to variables as a map of maps where the key of the outer map is the id
                geoAxisY - same as X
                verticalAxis - same as X
                timeAxis - same as X
         */

        def parent

        // Should only be at most one data set
        if ( map.has("dataset") ) {
            def datasetChanges = map.get("dataset")
            def id = datasetChanges.id
            log.debug("Saving a change for data set " + id)
            Dataset d = Dataset.get(id)
            d.properties = datasetChanges;
            d.save(failOnError: true)
            if ( d.parent == null ) {
                parent = Site.get(1)
            } else {
                parent = Dataset.get(d.parent.id);
            }
        }


        if ( map.has("variables") ) {
            def variablesToChange = map.get("variables")
            variablesToChange.each {
                def vid = it.key
                def vproperties = it.value
                Variable v = Variable.get(vid)
                v.properties = vproperties
                v.save(flush: true, failOnError: true)
            }
        }

        if ( map.has("geoAxisX") ) {
            def geoAxisXToChange = map.get("geoAxisX")
            geoAxisXToChange.each {
                def xid = it.key
                def xproperties = it.value
                GeoAxisX x = GeoAxisX.get(xid)
                x.properties = xproperties
                x.save(flush: true, failOnError: true)
            }
        }



        if ( map.has("geoAxisY") ) {
            def geoAxisYToChange = map.get("geoAxisY")
            geoAxisYToChange.each {
                def yid = it.key
                def yproperties = it.value
                GeoAxisY y = GeoAxisY.get(yid)
                y.properties = yproperties
                y.save(flush: true, failOnError: true)
            }
        }

        if ( map.has("verticalAxis") ) {
            def verticalAxisToChange = map.get("verticalAxis")
            verticalAxisToChange.each {
                def zid = it.key
                def zproperties = it.value
                VerticalAxis z = VerticalAxis.get(zid)
                z.properties = zproperties
                z.save(flush: true, failOnError: true)
            }
        }

        if ( map.has("timeAxis") ) {
            def timeAxisToChange = map.get("timeAxis")
            timeAxisToChange.each {
                def tid = it.key
                def tproperties = it.value
                TimeAxis t = TimeAxis.get(tid)
                t.properties = tproperties
                t.save(flush: true, failOnError: true)
            }
        }

        if ( !parent ) parent = Site.get(1);
        JSON.use("deep") {
            render parent as JSON
        }
    }
    def moveDataset() {
        def requestJSON = request.JSON

        AddRequest addReq = new AddRequest(requestJSON)

        def move_to_id
        def move_from_id
        def move_to_type

        List<AddProperty> props = addReq.getAddProperties()
        for (int i = 0; i < props.size(); i++) {
            AddProperty property = props.get(i)
            if (property.name == "move_to_id") {
                move_to_id = property.value;
            } else if (property.name == "move_from_id") {
                move_from_id = property.value;
            } else if ( property.name == 'move_to_type' ) {
                move_to_type = property.value
            }
        }
        if ( move_to_id && move_from_id ) {
            def destination
            if ( move_to_type == 'site' ) {
                destination = Site.get(move_to_id)
            } else {
                destination = Dataset.get(move_to_id)
            }

            def move = Dataset.get(move_from_id);
            if ( destination && move ) {
                def move_from_parent
                if ( addReq.getType() == 'show' ) {
                    // The the moment all hidden data sets are just stashed in a second site...
                    move_from_parent = Site.get(2)
                } else {
                    move_from_parent = move.getParent();
                    // If parent is null, parent is site
                    if (!move_from_parent) {
                        move_from_parent = Site.get(1)
                    }
                }

                move_from_parent.removeFromDatasets(move);
                if ( move_to_type == 'site') move.setParent(null)
                destination.addToDatasets(move)
                destination.save(flush: true, failOnError: true)
                move_from_parent.save(flush: true, failOnError: true)

                def both
                if ( addReq.getType() == 'show') {
                    both = [destination: move_from_parent, origin: destination]
                } else {
                    both = [destination: destination, origin: move_from_parent]
                }
                JSON.use("deep") {
                    render both as JSON
                }
            }
        }
        //TODO render error
    }
    def addDataset() {

        def requestJSON = request.JSON

        AddRequest addReq = new AddRequest(requestJSON)

        List<AddProperty> props = addReq.getAddProperties()

        def parent_type;
        def parent_id;
        def empty_name;

        def parent;

        for (int i = 0; i < props.size(); i++) {
            AddProperty property = props.get(i)
            if ( property.name == "parent_type") {
                parent_type = property.value;
            } else if ( property.name == "parent_id" ) {
                parent_id = property.value;
            } else if ( property.name == "name" ) {
                empty_name = property.value;
            }
        }

        if ( parent_type == "site" ) {
            parent = Site.get(parent_id);
        } else if ( parent_type == "dataset" ) {
            parent = Dataset.get(parent_id);
        }

        if ( addReq.url && addReq.type ) {

            Dataset dataset = ingestService.processRequset(addReq, parent);
            if ( dataset.status == Dataset.INGEST_FAILED ) {
                JSON.use("deep") {
                    render dataset as JSON
                }
            }

            dataset.setStatus(Dataset.INGEST_FINISHED)
            if ( !dataset.validate() ) {
                dataset.errors.each{
                    log.debug(it.toString())
                }
            }

            parent.addToDatasets(dataset)

        } else if ( addReq.type && addReq.type == "empty" ) {
            Dataset newd = new Dataset([title: empty_name, hash: IngestService.getDigest(empty_name)])
            parent.addToDatasets(newd)
        }

        parent.save(flush: true, failOnError: true);

        if ( parent instanceof Site ) {
            withFormat {
                json {respond parent}
            }
        } else {
            JSON.use("deep") {
                render parent as JSON
            }
        }

    }
    def deleteDataset() {
        def did = params.id
        def parent;
        if ( did ) {
            Dataset dead = Dataset.get(did)
            if (!dead.parent) {
                parent = Site.get(1)
                parent.removeFromDatasets(dead)
                parent.save(flush: true)
            } else {
                parent = dead.getParent()
                parent.removeFromDatasets(dead)
                parent.save(flush: true)
            }
            updateDatasetJobService.unscheuleUpdate(dead.id)
            dead.delete()
        }
        JSON.use("deep") {
            render parent as JSON
        }
    }
    def start() {
        ingestService.addVariablesToAll()
    }
}
