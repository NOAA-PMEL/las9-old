package las

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }
        "/product/thumbnail/$dhash/$vhash"{
            controller = "product"
            action = "thumbnail"
        }
        /* goes to default grails CRUD view which should be turned off for production

        "/"(view:"/index")

        */
        /* comment out this line and set line from comment below to prevent bare URL from being shown */

        /* set line below for sending the bare URL to the ui*/
        "/"("UI.html")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
