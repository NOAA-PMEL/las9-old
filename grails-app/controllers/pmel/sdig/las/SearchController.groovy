package pmel.sdig.las

import grails.converters.JSON
import grails.plugins.elasticsearch.ElasticSearchService

class SearchController {

    def search() {

        def searchQuery = params.search

        def results = Dataset.search(searchQuery)

        Dataset resultsDataset = new Dataset([title: "Search Results"])
        if ( results.total == 0 ) {
            resultsDataset.setMessage("No results found matching search.")
        }
        boolean deep = true;
        results.getSearchResults().each {Dataset result ->
            deep = deep && result.variableChildren
            resultsDataset.addToDatasets(result)
        }
        resultsDataset.setVariableChildren(deep)
        if ( resultsDataset.variableChildren ) {
            JSON.use("deep") {
                render resultsDataset as JSON
            }
        } else {
            respond resultsDataset, formats: ['json'], view: 'dataset'
        }

    }
}
