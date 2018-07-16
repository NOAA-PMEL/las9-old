package pmel.sdig.las

class  MenuOption {
	
	String name
    String title
    String help
    String defaultValue
    List menuItems

	static hasMany = [menuItems: MenuItem, operations: Operation]
    static belongsTo = Operation

    static constraints = {
        help type: "text"
        operations nullable: true
        defaultValue nullable: true
    }
}
