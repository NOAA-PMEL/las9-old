package pmel.sdig.las.latlon

class LatLonUtil {
    /**
     * Stolen from Bob S.
     *
     * This converts an angle (in degrees) into the range &gt;=-180 to &lt;180
     * (180 becomes -180).
     * @param degrees an angle (in degrees)
     * @return the angle (in degrees) in the range &gt;=-180 to &lt;180.
     *   If isMV(angle), it returns 0.
     */
    static final double anglePM180(double degrees) {
        if (!Double.isFinite(degrees))
            return 0;

        while (degrees < -180) degrees += 360;
        while (degrees >= 180) degrees -= 360;

        return degrees;
    }
}
