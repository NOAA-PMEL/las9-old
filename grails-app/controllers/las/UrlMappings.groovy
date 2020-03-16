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

        /* goes to default grails CRUD view which should be turned off for production */
        /* comment out this line and set line from comment below to prevent bare URL from showing the grails CURD interface */
//        "/"(view:"/index")



        /*
        set line below for sending the bare URL to the ui
        */
        "/"("UI.html")

        // Directs admin URL to index.gsp which loads the angular app.
        // Access to admin URL space protected by shiro grails plugin
        "/admin/"(uri: "/admin/index")

        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
