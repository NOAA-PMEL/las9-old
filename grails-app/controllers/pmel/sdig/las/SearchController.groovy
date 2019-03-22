package pmel.sdig.las

import grails.converters.JSON
import grails.plugins.elasticsearch.ElasticSearchService

class SearchController {

    def search() {

        def searchQuery = params.search

        def results = Dataset.search(searchQuery)

        Dataset dataset = new Dataset([title: "Search Results"])
        if ( results.total == 0 ) {
            dataset.setMessage("No results found matching search.")
        }
        results.getSearchResults().each {Dataset result ->
            dataset.addToDatasets(result)
        }

        respond dataset, formats: ['json'], view: 'dataset'

    }
}
