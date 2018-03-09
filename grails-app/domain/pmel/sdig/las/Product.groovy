package pmel.sdig.las

class Product {

    String name
    String title
    String geometry
    String view
    String data_view

    /*
      Use this to put related operations together (like line plots, or 2D slices)
      Currently we do things like:
         Maps
         Line Plots
         Vertical Secion Plots
         Hofmuller Plots

  */
    String ui_group

    List operations
    static hasMany = [operations: Operation]

    static mapping = {
        operations (cascade: 'all-delete-orphan')
    }
    static constraints = {

    }


}
