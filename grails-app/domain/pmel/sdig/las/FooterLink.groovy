package pmel.sdig.las

class FooterLink {

    String url
    String text
    int index

    static belongsTo = [Site]

    static constraints = {
    }
}
