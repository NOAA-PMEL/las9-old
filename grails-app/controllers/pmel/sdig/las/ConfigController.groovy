package pmel.sdig.las

import grails.converters.JSON

class ConfigController {

    ProductService productService;
    /**
     * A config is the set of Products and Regions that apply to a variable.
     *
     *   The set of Products is defined by all the combinations of Product.geometry, Product.view and Product.data_view
     *
     *   Right now a data view is implied by the Product.geometry. For example, a profile map has a view of "xy" and a
     *   data_view of "xyzt".
     */
    def json() {

        def id = params.id

        Variable v = Variable.get(id)

        Dataset parent = v.getDataset()
        DatasetProperty p = parent.getDatasetProperties().find{it.name=="default"}

        //TODO use the default data set property to find operations if defined.


        def grid = v.geometry;
        def intervals = v.intervals;

        def products = productService.findProductsByInterval(grid, intervals)



        def regions = new ArrayList<Region>()
        regions.addAll(Region.findAll())

        def config = [products: products, regions: regions, variable: v]

        JSON.use("deep") {
            render config as JSON
        }


    }

    /**.
     * Get all the possible combinations of the characters in a string.  Combo routines based on code by Robert Sedgewick and Kevin Wayne.
     * from their book Introduction to Programming in Java published by Adison Wesley.
     * @param s
     * @return
     */
    public static List<String> combo(String s) {
        return combo("", s);
    }
    /**
     * Get combinations of the characters in a string.
     * @param prefix A prefix for the combinations
     * @param s the string to scramble
     * @return the combinations
     */
    public static List<String> combo(String prefix, String s) {
        ArrayList<String> comboList = new ArrayList<String>();

        if (!prefix.equals("")) {
            comboList.add(prefix);
        }

        if ( s.equals("") ) {
            return comboList;
        }
        for ( int i = 0; i < s.length(); i++ ) {
            comboList.addAll(combo(prefix + s.charAt(i), s.substring(i+1)));
        }
        return comboList;
    }


    def productsByInterval (){
        String intervals = params.intervals
        String grid = params.grid
        List<Product> products = productService.findProductsByInterval(grid, intervals)
        JSON.use("deep") {
            render products as JSON
        }
    }
}
