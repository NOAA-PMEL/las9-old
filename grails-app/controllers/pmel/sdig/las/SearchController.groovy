package pmel.sdig.las

class SearchController {

    def search() {

        def searchQuery = params.search

        def results = Dataset.search(searchQuery)

        def datasetList = []

        results.getSearchResults().each { Dataset result ->
            if (result.id > 0) {
                Dataset full = Dataset.get(result.id)
                // In case the indexes contain dataset that are no longer defined
                if ( full ) {
                    datasetList.add(full)
                }
            }
        }

        render(template: "search",  model: [datasetList: datasetList])

    }
}
