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
    /**
     * This converts an angle (in degrees) into the range &gt;=0 to
     *   &lt;360.
     * <UL>
     * <LI> If isMV(angle), it returns 0.
     * </UL>
     */
    static final double angle0360(double degrees) {
        if (!Double.isFinite(degrees))
            return 0;

        while (degrees < 0)
            degrees += 360;
        while (degrees >= 360)
            degrees -= 360;

        //causes slight bruising
        //degrees = frac(degrees / 360) * 360;
        //now it is -360..360
        //if (degrees < 0)
        //    degrees += 360;

        return degrees;
    }
}
