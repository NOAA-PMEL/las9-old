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
        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
