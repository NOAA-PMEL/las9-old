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

        // Always init the environment so it can be changed
        // between restarts.
        initializationService.initEnvironment()


        // These 3 methods check to see if the objects exist before creating them.
        // Unless reinit is set to true
        def reinit = false;
        def property = grailsApplication.config.getProperty('admin.reinit')
        if ( property ) {
            reinit = property == "true"
        }
        // Pass in reinit = true to remake products and regions
        if ( !reinit )
        // TODO implement by dumping stuff at the beginning if reinit = true then proceeding.
        initializationService.createProducts(reinit)

        initializationService.createDefaultRegions(reinit)

        // Not part of reinit, handled by admin interface
        initializationService.loadDefaultLasDatasets()

        def priv = Site.findByTitle("Private Data")
        if ( !priv ) {
            priv = new Site([title: "Private Data"])
            priv.save();
        }

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
