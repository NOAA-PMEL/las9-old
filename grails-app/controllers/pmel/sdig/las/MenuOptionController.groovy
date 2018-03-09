package pmel.sdig.las

import grails.transaction.Transactional

@Transactional(readOnly = true)
class MenuOptionController {
    static scaffold = MenuOption
}
