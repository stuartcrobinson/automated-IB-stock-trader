
import apidemo.ApiDemo;
import static com.ib.controller.Formats.TIME;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;

class Hat {
    int size;

    public Hat(int size) {
	this.size = size;
    }

    public String toString() {
	return String.valueOf(size);
    }
}

public class teststuart {

    static <T> T castRight(Object object, Class<T> cls) {
	return cls.cast(object);
    }

    private static int convert(Integer five) {
	return 5;
    }

//    <T> T doSomething(Class<T> cls) {
//	Object o;
//	// snip
//	return cls.cast(o);
//    }

    enum keyEnum { height, color; }
    
    public static void main(String[] args) {
//	Map<keyEnum, Object> map= new HashMap();
//	String value1 = "red";
//	Double value2 = 3.2;
////	map.put(keyEnum.color, value1);
//	map.put(keyEnum.height, value2);
//	
//	double x = (Double) map.get(keyEnum.height) + 10;
//	
//	Class myClass = keyEnum.class;
//	System.out.println(map.get(keyEnum.color));
//	
////	
////	
////	Object five = new Integer(5);
//	
//	Class cls = Integer.class;
//	int six = cls.cast(five) + 1;
	
	
//	List<Integer> asdf  = new ArrayList();
//	
//	asdf.addAll(Arrays.asList(new Integer[]{1, 2, 3, 4}));
//	
//	System.out.println(asdf);
	
	for (String s : ApiDemo.Col.y_symbol.ni) {
	    System.out.println(s);
	}
	
    }

    private static void doStuff(List<Hat> asdf) {

//	var at;
	
	asdf.get(4).size = 1;

    }


}

//	int x = theClass.cast(seven) + 5;

//        ArrayList a = null;
//	Object o = a;
//	System.out.println(o.getClass().isInstance(new String()));
//	Class c = new ArrayList().getClass();
//	
//	Map<Integer, String> asdf = new HashMap();
//	
//	
//	asdf.put(1, "one");
//	asdf.put(2, "two");
//	asdf.put(3, "three");
//	
//	for (Object : asdf){
//	    
//	}
//	
//
//////"IBIO",0.50,"iBio, Inc. Common","AMEX",617K,-0.073,0.00,0.00,0.00,0.10,-4.3M,49.10,
////
//        String input = "\"IBIO\",0.50,\"iBio, Inc. Common\",\"AMEX\",617K,-0.073,0.00,0.00,0.00,0.10,-4.3M,49.10,";
////                "value1, value2, value3, value4, \"value5, 1234\", "
////                + "value6, value7, \"value8\", value9, \"value10, 123.23\"";
//
//
//        String[] values = input.split("\"?(,|$)(?=(([^\"]*\"){2})*[^\"]*$) *\"?");
//
////String[] values = input.split("\"?(,|$)(?=(([^\"]*\"){2})*[^\"]*$) *\"?");
//        for (String s : values)
//            System.out.println(s);
//
//        
////        String asdf = "aw,awe,gwer,awer,\"asdgasdg\"asdgage,g,we,g,weg";
////        
////        System.out.println(asdf.replace(",", "").replace("\"", ""));
