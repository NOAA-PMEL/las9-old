package pmel.sdig.las.client.util;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Util {

	public Util() {

	}

	public static String padRight(String s, int n) {
		int pad = n = s.length();
		for ( int i = 0; i < pad; i++ ) {
			s = s + " ";
		}
		return s;
	}
	public static String format_two(int i) {
		// Really an error for i<10 and i>99, but these are 1<days<31 and 0<hours<23.
		if ( i < 10 ) {
			return "0"+i;
		} else {
			return String.valueOf(i);
		}
	}
	public static String format_four (int i) {
		// Really an error for i<100 and i>9999, but these are years which start at 0001 or at worst 0000.
		if ( i < 10 ) {
			return "000"+i;
		} else if ( i >= 10 && i < 100 ) {
			return "00"+i;
		} else if ( i >= 100 && i < 1000 ) {
			return "0"+i;
		} else {
			return String.valueOf(i);
		}
	}

	public static String format_two(double d) {
		NumberFormat dFormat = NumberFormat.getFormat("########.##");
		return dFormat.format(d);
	}
	public static String format_four(double d) {
		NumberFormat dFormat = NumberFormat.getFormat("########.####");
		return dFormat.format(d);
	}
	public static String[] getParameterStrings(String name) {
		Map<String, List<String>> parameters = Window.Location.getParameterMap();
		List param = parameters.get(name);
		if ( param != null ) {
			int i = 0;
			String[] ps = new String[param.size()];
			for (Iterator paramIt = param.iterator(); paramIt.hasNext(); ) {
				String p = (String) paramIt.next();
				ps[i] = p;
				i++;
			}
			return ps;
		}
		return null;
	}
	public static String getParameterString(String name) {
		Map<String, List<String>> parameters = Window.Location.getParameterMap();
		List param = parameters.get(name);
		if ( param != null ) {
			return (String) param.get(0);
		}
		return null;
	}
}
