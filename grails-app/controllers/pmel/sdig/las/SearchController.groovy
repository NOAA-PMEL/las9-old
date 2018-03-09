package pmel.sdig.las

import grails.converters.JSON

class SearchController {

    def elasticSearchService

    def search() {

        def searchQuery = params.search

        def results = Dataset.search(searchQuery)

        def datasets = []

        results.searchResults.each {
            if(it instanceof Dataset) {
                datasets.add(it)
            }
        }

        def variables = []

        def variableResults = Variable.search(searchQuery)
        variableResults.searchResults.each {
            if ( it instanceof Variable ) {
                variables.add(it)
            }
        }
        results = [datasets: datasets, variables: variables]
        render results as JSON
    }
}
