package pmel.sdig.las

import grails.converters.JSON
import org.hibernate.CacheMode
import org.hibernate.Criteria
import org.hibernate.FetchMode
import org.hibernate.FlushMode
import org.hibernate.HibernateException
import org.hibernate.LockMode
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Order
import org.hibernate.criterion.Projection
import org.hibernate.sql.JoinType
import org.hibernate.transform.ResultTransformer
import pmel.sdig.las.type.GeometryType

class SiteController {

    static scaffold = Site

    def show() {
        def sid = params.id
        Site site = Site.get(sid)

        // Don't bother to compute now as we're not going to display them.

        def g = Dataset.createCriteria()
        def grids = g.get {
            eq("variableChildren", true)
            eq("geometry", GeometryType.GRID)
            projections {
                count()
            }
        }
        def d = Dataset.createCriteria()
        def discrete = d.get {
            eq("variableChildren", true)
            'in'("geometry", [GeometryType.TRAJECTORY, GeometryType.TIMESERIES, GeometryType.PROFILE])
            projections {
                count()
            }
        }

        def total = grids + discrete
        site.setTotal(total)
        site.setGrids(grids)
        site.setDiscrete(discrete)

        if ( site ) {
            withFormat {
                html { respond site }
                json { respond site}
            }
        } else {
            log.error("No site found for this installation.")
        }
    }
}
