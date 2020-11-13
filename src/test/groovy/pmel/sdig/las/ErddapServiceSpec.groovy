package pmel.sdig.las

import grails.gorm.transactions.NotTransactional
import grails.testing.mixin.integration.Integration
import grails.testing.services.ServiceUnitTest
import grails.transaction.Rollback
import org.springframework.beans.factory.annotation.Autowired
import pmel.sdig.las.latlon.LatLonUtil
import spock.lang.Specification

class ErddapServiceSpec extends Specification implements ServiceUnitTest<ErddapService> {

    def setup() {
    }

    def cleanup() {
    }
    @NotTransactional
    void "Test Longitude Queries, Data: -180:180, Query: -180:180"() {

        List<String> reply

        // -180:180 data with queries in the same range

        when: "Case 1.1: Data: -180 to 180, xlo< xhi, both negative in range"
        reply = LatLonUtil.getLonConstraint(-123.5, -90.0, true, "-180:180", "longitude")
        String a = reply.get(0)
        System.out.println("Case 1.1: " + a)
        then:
        !a.isEmpty() && a.equals("&longitude>=-123.5&longitude<=-90.0")

        when: "Case 1.2: Data: -180 to 180, xlo < xhi, both positive in range"
        reply = LatLonUtil.getLonConstraint(123.5, 155.0, true, "-180:180", "longitude")
        String b = reply.get(0)
        if ( b.isEmpty() ) {
            System.out.println("No lon constraint with this input.")
        } else {
            System.out.println("Case 1.2: " + b)
        }
        then:
        b.equals("&longitude>=123.5&longitude<=155.0")

        when:"Case 1.3: Data: -180 to 180, xlo<xhi, one negative one positive"
        reply = LatLonUtil.getLonConstraint(-123.5, 71.0, true, "-180:180", "longitude")
        String c = reply.get(0)
        System.out.println("Case 1.3:" + c)
        then:
        c.equals("&longitude>=-123.5&longitude<=71.0")


        when:"Case 2.1: Data: -180 to 180, xlo>xhi, this wraps west to east across 180, needs lon360"
        reply = LatLonUtil.getLonConstraint(45, -84.0, true, "-180:180", "longitude")
        String d = reply.get(0)
        System.out.println("Case 2.1: " + d)
        then:
        d.equals("&lon360>=45.0&lon360<=276.0")


        when:"Case 2.2: Data: -180 to 180, xlo>xhi, this wraps west to east across 180, needs lon360, result it two queries"
        reply = LatLonUtil.getLonConstraint(45, -84.0, false, "-180:180", "longitude")
        System.out.println("Case 2.2:" + reply.get(0))
        System.out.println("Case 2.2:" + reply.get(1))
        then:
        reply.get(0).equals("&longitude>=45.0&longitude<180.0") && reply.get(1).equals("&longitude>=-180.0&longitude<=-84.0")


        when:"Case 3.1: Data: -180 to 180, xlo>xhi, 'full' globe"
        reply = LatLonUtil.getLonConstraint(-177.5, 177.5, false, "-180:180", "longitude")
        if ( reply.size() ==0 ) {
            System.out.println("Case 3.1: Whole globe, no constraint")
        } else {
            System.out.print("Oops. Expected no constraints")
        }
        then:
        reply.size() == 0

        when:"Case 4.1: Data: -180 to 180, xlo>xhi, both values negative"
        reply = LatLonUtil.getLonConstraint(-15.5, -60.5, false, "-180:180", "longitude")
        System.out.println("Case 4.1: " + reply.get(0))
        System.out.println("Case 4.1: " + reply.get(1))
        then:
        reply.get(0).equals("&longitude>=-15.5&longitude<180.0") && reply.get(1).equals("&longitude>=-180.0&longitude<=-60.5")


    }
    @NotTransactional
    void "Test Longitude Queries Data: -180:180, Query: 0:360"() {

        List<String> reply

        // -180:180 data with queries in the 0:360 range

        when: "Case 1.1: Data: -180 to 180, xlo< xhi"
        reply = LatLonUtil.getLonConstraint(236.5, 270.0, false, "-180;180", "longitude")
        String a = reply.get(0)
        System.out.println("Case 1.1: " + a)
        then:
        !a.isEmpty() && a.equals("&longitude>=-123.5&longitude<=-90.0")

        when: "Case 1.2: Data: -180 to 180, xlo < xhi, both positive in range"
        reply = LatLonUtil.getLonConstraint(123.5, 155.0, false, "-180;180", "longitude")
        String b = reply.get(0)
        if ( b.isEmpty() ) {
            System.out.println("No lon constraint with this input.")
        } else {
            System.out.println("Case 1.2: " + b)
        }
        then:
        b.equals("&longitude>=123.5&longitude<=155.0")

        when:"Case 1.3: Data: -180 to 180, xlo<xhi"
        reply = LatLonUtil.getLonConstraint(236.5, 71.0, false, "-180;180", "longitude")
        String c = reply.get(0)
        System.out.println("Case 1.3: " + c)
        then:
        c.equals("&longitude>=-123.5&longitude<=71.0")

        when:"Case 2.1: Data: -180 to 180, xlo>xhi, this wraps west to east across 180, needs lon360, result it two queries"
        reply = LatLonUtil.getLonConstraint(45, 276.0, true, "-180:180", "longitude")
        System.out.println("Case 2.1: " + reply.get(0))
        then:
        reply.get(0).equals("&lon360>=45.0&lon360<=276.0")

        when:"Case 2.2: Data: -180 to 180, xlo>xhi, this wraps west to east across 180, needs lon360"
        reply = LatLonUtil.getLonConstraint(45, 276.0, false, "-180;180", "longitude")
        String d = reply.get(0)
        String d1 = reply.get(1)
        System.out.println("Case 2.2: " + d)
        System.out.println("Case 2.2: " + d1)
        then:
        reply.size() == 2 && d.equals("&longitude>=45.0&longitude<=180.0") && d1.equals("&longitude>=-180.0&longitude<=-84.0")

        when:"Case 3: Data: -180 to 180, xlo>xhi, 'full' globe"
        reply = LatLonUtil.getLonConstraint(2.5, 358.5, false, "-180;180", "longitude")
        if ( reply.size() == 0 ) {
            System.out.println("Case 3: Whole globe, no constraint")
        } else {
            System.out.print("Oops. Expected no constraints")
        }
        then:
        reply.size() == 0

        when:"Case 4: Data: -180 to 180, xlo>xhi, crosses both (same as case 2)"
        reply = LatLonUtil.getLonConstraint(344.5d, 299.5d, false, "-180:180", "longitude")
        System.out.println("Case 4.1: " + reply.get(0))
        System.out.println("Case 4.1: " + reply.get(1))
        then:
        reply.get(0).equals("&longitude>=-15.5&longitude<180.0") && reply.get(1).equals("&longitude>=-180.0&longitude<=-60.5")


    }
    void "Test Longitude Queries, Data: 0:360, Query: -180:180"() {

        List<String> reply

        // -180:180 data with queries in the same range

        when: "Case 1.1: Data: 0 to 360, xlo< xhi, both negative in range"
        reply = LatLonUtil.getLonConstraint(-123.5, -90.0, false, "0:360", "longitude")
        String a = reply.get(0)
        System.out.println("Case 1.1: " + a)
        then:
        !a.isEmpty() && a.equals("&longitude>=236.5&longitude<=270.0")

        when: "Case 1.2: Data: 0 to 360, xlo < xhi, both positive in range"
        reply = LatLonUtil.getLonConstraint(123.5, 155.0, false, "0:360", "longitude")
        String b = reply.get(0)
        if ( b.isEmpty() ) {
            System.out.println("No lon constraint with this input.")
        } else {
            System.out.println("Case 1.2: " + b)
        }
        then:
        b.equals("&longitude>=123.5&longitude<=155.0")

        when:"Case 1.3: Data: 0 to 360, xlo<xhi, crosses 0, need -180 to 180 to do this one"
        reply = LatLonUtil.getLonConstraint(-123.5, 71.0, false, "0:360", "longitude")
        String c1 = reply.get(0)
        String c2 = reply.get(1)
        System.out.println("Case 1.3: " + c1)
        System.out.println("Case 1.3: " + c2)
        then:
        c1.equals("&longitude>=236.5&longitude<360.0") && c2.equals("&longitude>=0.0&longitude<=71.0")


        when:"Case 2.1: Data: 0 to 360, xlo>xhi, this wraps west to east across 180, straight up 0 360 query"
        reply = LatLonUtil.getLonConstraint(45, -84.0, false, "0:360", "longitude")
        String d = reply.get(0)
        System.out.println("Case 2.1: " + d)
        then:
        d.equals("&longitude>=45.0&longitude<=276.0")


        when:"Case 2.2: Data: 0 to 360, Same since there is no lon360"
        reply = LatLonUtil.getLonConstraint(45, -84.0, false, "0:360", "longitude")
        System.out.println("Case 2.2:" + reply.get(0))
        then:
        reply.get(0).equals("&longitude>=45.0&longitude<=276.0")


        when:"Case 3.1: Data: 0 to 360, xlo>xhi, 'full' globe"
        reply = LatLonUtil.getLonConstraint(-177.5, 177.5, false, "0:360", "longitude")
        if ( reply.size() ==0 ) {
            System.out.println("Case 3.1: Whole globe, no constraint")
        } else {
            System.out.print("Oops. Expected no constraints")
        }
        then:
        reply.size() == 0

        when:"Case 4.1: Data: 0 to 360, xlo>xhi, both values negative"
        reply = LatLonUtil.getLonConstraint(-15.5, -60.5, false, "0:360", "longitude")
        System.out.println("Case 4.1: " + reply.get(0))
        System.out.println("Case 4.1: " + reply.get(1))
        then:
        reply.get(0).equals("&longitude>=344.5&longitude<360.0") && reply.get(1).equals("&longitude>=0.0&longitude<=299.5")


    }
    void "Test Longitude Queries, Data: 0:360, Query: 0:360"() {

        List<String> reply

        // -180:180 data with queries in the same range

        when: "Case 1.1: Data: 0 to 360, xlo< xhi, both negative in range"
        reply = LatLonUtil.getLonConstraint(236.5, 270.0, false, "0:360", "longitude")
        String a = reply.get(0)
        System.out.println("Case 1.1: " + a)
        then:
        !a.isEmpty() && a.equals("&longitude>=236.5&longitude<=270.0")

        when: "Case 1.2: Data: 0 to 360, xlo < xhi, both positive in range"
        reply = LatLonUtil.getLonConstraint(123.5, 155.0, false, "0:360", "longitude")
        String b = reply.get(0)
        System.out.println("Case 1.2: " + b)
        then:
        b.equals("&longitude>=123.5&longitude<=155.0")

        when:"Case 1.3: Data: 0 to 360, xlo<xhi, crosses 0"
        reply = LatLonUtil.getLonConstraint(236.5, 71.0, false, "0:360", "longitude")
        String c1 = reply.get(0)
        String c2 = reply.get(1)
        System.out.println("Case 1.3: " + c1)
        System.out.println("Case 1.3: " + c2)
        then:
        c1.equals("&longitude>=236.5&longitude<360.0") && c2.equals("&longitude>=0.0&longitude<=71.0")

        when:"Case 2.1: Data: 0 to 360, xlo>xhi, this wraps west to east across 180, straight up 0 360 query"
        reply = LatLonUtil.getLonConstraint(45, -84.0, false, "0:360", "longitude")
        String d = reply.get(0)
        System.out.println("Case 2.1: " + d)
        then:
        d.equals("&longitude>=45.0&longitude<=276.0")


        when:"Case 2.2: Data: 0 to 360, Same since there is no lon360"
        reply = LatLonUtil.getLonConstraint(45, 276.0, false, "0:360", "longitude")
        System.out.println("Case 2.2:" + reply.get(0))
        then:
        reply.get(0).equals("&longitude>=45.0&longitude<=276.0")


        when:"Case 3.1: Data: 0 to 360, xlo>xhi, 'full' globe"
        reply = LatLonUtil.getLonConstraint(182.5, 177.5, false, "0:360", "longitude")
        if ( reply.size() == 0 ) {
            System.out.println("Case 3.1: Whole globe, no constraint")
        } else {
            System.out.print("Oops. Expected no constraints")
        }
        then:
        reply.size() == 0

        when:"Case 4.1: Data: 0 to 360, xlo>xhi, both values negative"
        reply = LatLonUtil.getLonConstraint(344.5, 299.5, false, "0:360", "longitude")
        System.out.println("Case 4.1: " + reply.get(0))
        System.out.println("Case 4.1: " + reply.get(1))
        then:
        reply.get(0).equals("&longitude>=344.5&longitude<360.0") && reply.get(1).equals("&longitude>=0.0&longitude<=299.5")


    }
}
