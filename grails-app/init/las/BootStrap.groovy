package las

import grails.core.GrailsApplication
import grails.util.Holders
import org.apache.shiro.authc.credential.PasswordService
import pmel.sdig.las.*

class BootStrap {

    InitializationService initializationService
    IngestService ingestService
    AsyncIngestService asyncIngestService
    AsyncFerretService asyncFerretService
    PasswordService credentialMatcher
    GrailsApplication grailsApplication

    def init = { servletContext ->
        def v = Holders.grailsApplication.metadata['app.version']
        log.info("This is LAS v" + v);
        log.debug("Starting the init bootstrap closure...")

        Ferret ferret = Ferret.first()
        FerretEnvironment ferretEnvironment = FerretEnvironment.first()

        if (!ferret || !ferretEnvironment) {
            initializationService.initEnvironment()
        }

        initializationService.createProducts()

        initializationService.createDefaultRegions()

        // TODO this is the default data sets - make sure it's on for distribution
        initializationService.loadDefaultLasDatasets()

        def priv = new Site([title: "Private Data"])
        priv.save();

        def admin_pw = grailsApplication.config.getProperty('admin.password')
        def adminUser = ShiroUser.findByUsername('admin')

        if (!adminUser) {
            def userRole = new ShiroRole(name: "Admin")
            if ( admin_pw ) {
                adminUser = new ShiroUser(username: "admin", passwordHash: credentialMatcher.encryptPassword(admin_pw))
            } else {
                adminUser = new ShiroUser(username: "admin", passwordHash: credentialMatcher.encryptPassword('default'))
            }
            adminUser.addToRoles(userRole)
            adminUser.addToPermissions("admin:*")
            adminUser.save()
        } else if ( admin_pw ) {
            adminUser.setPasswordHash(credentialMatcher.encryptPassword(admin_pw))
            adminUser.save()
        }

    }
    def destroy = {
    }
}
