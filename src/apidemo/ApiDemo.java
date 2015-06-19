package apidemo;
/*
 notes:
 1.  877-442-2757 - report bugs here. technical support.
 2.  some london stocks only trade 4x per day
 3.  inactive from EST 4 pm - 6 pm
 */
//TODO - build system that just saves data:
//          1.  open, low, high and close bid & ask for all stocks - with timestamps.  can add other exchanges?  toronto?
//          2.  liabilities data from yahoo:  http://finance.yahoo.com/q/bs?s=ATNY+Balance+Sheet&annual

//TODO start here
//1.  are orders being executed before i submit them??
//2.  control apidemo memory footprint?
//3.  are holidays handled?
//2.  test clock re-set

//TODO cannot calculate PnL by hand!  must use IB's calculation in case there was split!
//TOOD nevermind - must calculate by hand, i think.  WHEN YOU SELL, check to see if the stock split since purchase! 
//http://eoddata.com/splits.aspx -- intl also??
//todo - for empty data, use "" instead of "-"?  for smaller file size. 
//note -- stocks can change names!  AFCE Changed to PLKI on 1/21/2014 12:00:00 AM

//Read more: http://www.nasdaq.com/symbol/afce/guru-analysis/validea-momentum#ixzz2s0AIJ6Lq

//TODO avoid buying stocks whose company just released a statement.  via press release?
//http://www.nasdaq.com/symbol/bont/press-releases
//http://www.bloomberg.com/quote/BONT:US/news
//not as good:  see "press releases" http://www.marketwatch.com/investing/stock/bont/news
//only articles from BUSINESS WIRE or marketwatch? https://www.google.com/finance/company_news?q=NASDAQ%3ABONT&startdate=2014-1-01&enddate=2014-2-01
//this is best! -- http://finance.yahoo.com/q/p?s=BONT+Press+Releases
//small 36-sample test -- removing data for stocks that had press release improved sell change average by 1 percent. (from -3.7 to -2.7 %)

//TODO don't buy stocks that are going to split within the next month.  


import apidemo.TopModel.TopRow;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import apidemo.util.HtmlButton;
import apidemo.util.NewLookAndFeel;
import apidemo.util.NewTabbedPanel;
import apidemo.util.VerticalPanel;
import com.google.common.collect.Lists;
import com.ib.client.Contract;

import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IBulletinHandler;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.ITimeHandler;
import com.ib.controller.Formats;
import com.ib.controller.NewContract;
import com.ib.controller.NewOrder;
import com.ib.controller.OrderType;
import com.ib.controller.Position;
import com.ib.controller.Types;
import com.ib.controller.Types.Action;
import com.ib.controller.Types.NewsType;
import java.awt.AWTException;

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class ApiDemo implements IConnectionHandler {

    static DecimalFormat decimalFormat = new DecimalFormat("###.####");
    static SimpleDateFormat bigDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    static SimpleDateFormat bigDateAMPMFormat = new SimpleDateFormat("yyyyMMdd hh:mm.ss aa");
    static SimpleDateFormat logfileNameDateFormat = new SimpleDateFormat("yyyyMMdd HH_mm_ss");
    static SimpleDateFormat ibTradePanelDateFormat = new SimpleDateFormat("yyyyMMdd  HH:mm:ss");
    static SimpleDateFormat clearDateFormat = new SimpleDateFormat("EEE MMM dd hh:mm.ss aa yyyy zzz");
    static SimpleDateFormat clearDateFormat3 = new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy");
    static SimpleDateFormat clearDateFormat2 = new SimpleDateFormat("E a h:mm.ss MMMd yyyy zzz");
    static SimpleDateFormat dayDateFormat = new SimpleDateFormat("yyyyMMdd");
    static SimpleDateFormat hmsDateFormat = new SimpleDateFormat("HH:mm:ss");
    static File masterOrdersFile = new File("C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\stocks\\IB\\data\\masterOrders.csv");
    static File completedSalesFile = new File("C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\stocks\\IB\\data\\completedSales.csv");
    static File logsFolder = new File("C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\stocks\\IB\\data\\logs");
    static File logFile;
    static File downloadsDir = new File("C:\\Users\\" + System.getProperty("user.name") + "\\Downloads");
    static PrintWriter masterOrdersWriter, completedSalesWriter, logWriter;
    static String cs = ", ", c = ",", csvRegExSplitter = "\"?(,|$)(?=(([^\"]*\"){2})*[^\"]*$) *\"?";
    public static int curErr;
    public OrderHistory doh = new OrderHistory();   //dummy order history for accessing pseudo-static methods.
    public YahooDataPackage dummy_yahoo = new YahooDataPackage();
    public MyVariablesHolder dstock = new Stock();
    public int orders_count = 0;
    public long orders_timer_start_time = 0;
    public int requests_count = 0;
    public long requests_timer_start_time = 0;

    int[] holdDayss = new int[]{0};//, 1, 2, 5, 10, 15, 20, 25, 30};        //!!!
    double priceChangeMax = -0.3;      //!!!	    //use -0.01  ?
//    double amountToSpend = 100;		   //500;	 ***NOTE ASX HARD-CODED to 500 MINIMUM!***  	    //IB might have a way to tell it how much you want to spend instead of how many shares?
    boolean actuallyPlaceOrders = true;

    public void run() throws IOException, InterruptedException, ParseException, Exception {// throws InterruptedException, IOException, Exception {

	makeGui();
	initialize(true);
	masterLooper(false);
//	doh.close_all_positions_for_exchange_currency_silently(EDx.AMEX);


//	for (EDx x : EDx.values()) {
//
//	    doh.readOrderHistories(x);
//	    doh.writeOrderHistories(x);
//
//	}
////
//	System.exit(0);




//	for (String s : Col.y_symbol.ni) {
//	    System.out.println(s);
//	}

//	for (EDx x : EDx.values()){
//	    doh.readOrderHistories(x);
//	    doh.writeOrderHistories(x);
//	}

//	System.out.println(MyDate.xTime(EDx.ASX).inInterval(EDx.ASX.open, 0, 280));

//        Stock s = new Stock("1398", "SEHK", "HKD", "STK");
//	Stock s = new Stock("BHP", "SMART", "AUD", "STK");
//	s.requestQuote();
//	Thread.sleep(1000);
//	s.printHandler();
//	System.exit(0);
    }

    void masterLooper(Boolean runForReal) throws IOException, InterruptedException, Exception {
	if (!runForReal) for (EDx x : EDx.values()) x.progressBooleansFile.delete();
	myPrintln(("started masterlooper"));
	Date SixAM = hmsDateFormat.parse("05:50:00");
	Date fiveThirtyPM = hmsDateFormat.parse("17:30:00");
	Date fiveTenPM = hmsDateFormat.parse("17:10:00");
	Date fourThirtyFive = hmsDateFormat.parse("16:35:00");
	Date fourFourtyFive = hmsDateFormat.parse("16:45:00");
	Date date_oF_lasT_ED_update = new Date(0);

	while (true) {
	    Date now = new Date(System.currentTimeMillis());
	    for (EDx x : EDx.values()) {
		readProgressBooleans(x);                                                                                 //is this the right place to put it???

		if (!(x == EDx.AMEX
			|| x == EDx.NYSE
			|| x == EDx.NASDAQ
			|| x == EDx.OTCBB))
		    continue;     //!!!

		if (!MyDate.xNowDay(x).isWorkDay()) continue;

//		if (MyDate.xTime(EDx.NYSE).inInterval(fiveThirtyPM, 0, 5) && !DateUtils.isSameDay(now, date_oF_lasT_ED_update)) {       //commented out cos not working.  just use old ED files, is fine.
//		    downloadEodDataFiles();
//		    date_oF_lasT_ED_update = now;
//		    bookkeep_end(x);
//		}

//		if (MyDate.xTime(EDx.NYSE).inInterval(fourThirtyFive, 0, 5) && !EDx.NYSE.clock_set_1) {
//		    set_TWS_logout_to(KeyEvent.VK_4, KeyEvent.VK_3, KeyEvent.VK_0);
//		    EDx.NYSE.clock_set_1 = true;
//		    EDx.NYSE.clock_set_2 = false;
//		    bookkeep_end(x);
//		}
//		if (MyDate.xTime(EDx.NYSE).inInterval(fiveTenPM, 0, 5) && !EDx.NYSE.clock_set_2) {
//		    set_TWS_logout_to(KeyEvent.VK_4, KeyEvent.VK_4, KeyEvent.VK_0);
//		    EDx.NYSE.clock_set_1 = false;
//		    EDx.NYSE.clock_set_2 = true;
//		    bookkeep_end(x);
//		}
		if (MyDate.xNowTime(x).inInterval(x.open, 0, 280) && !x.ordersMaintenanced) {          //usually, do this 110 minutes before close.  but now it should be done first thing.
//		    timeToStartSelling(x);//!!
//		    sellLeftovers(x);			//!!!
////		       Thread.sleep(5_000);	//!!
//		    confirmSales(x);
//		    System.out.println("now " + MyDate.xNowTime(x));
		    doh.delete_sold_and_unpurchased_and_old_unsold_orders(x);
		    doh.delete_orderHistories_whose_buy_change_was_not_low_enough(x);
		    doh.close_undocumented_positions(x);
		    doh.close_all_positions_for_exchange_currency_silently(x);
		    doh.delete_unowned_orderHistory_entries(x);

//		    doh.buy_unowned_but_supposedly_purchased_orderHistory_entries(x.currency);

		    doh.erase_sell_try_data_from_OH(x);
		    confirmSales(x);
		    x.ordersMaintenanced = true;
		    bookkeep_end(x);
		}
//		if (MyDate.xNowTime(x).inInterval(x.open, -10, 0) && !x.symbolsGotten) {			    //!
//		    x.progressBooleansFile.delete();								    //initializes booleans at start of day
//		    x.stocks = read_stocks_file(x);
////		    doh.delete_sold_and_unpurchased_and_old_unsold_orders(x);
////		    doh.buy_unowned_but_supposedly_purchased_orderHistory_entries(x.currency);
//		    x.symbolsGotten = true;
//		    x.salesConfirmed1 = false;
//		    x.salesConfirmed2 = false;
//		    x.salesConfirmedLeftovers = false;
//		    bookkeep_end(x);
//		}
//		if (MyDate.xNowTime(x).inInterval(x.open, 0, 20) && x.symbolsGotten && !x.bought) {	 //!
//		    searchAndBuy(x);
//		    x.bought = true;
//		    bookkeep_end(x);
//		}
//		if (MyDate.xNowTime(x).inInterval(x.open, 20, x.close, 0) && !x.purchasesConfirmed1) {
//		    confirmPurchases(x);
//		    if (curErr == 504) System.exit(0);                                               //exit before saving boolean so we re-do this loop
//		    x.purchasesConfirmed1 = true;
//		    bookkeep_end(x);
//		}
////                System.exit(0);     //!
//		if (MyDate.xNowTime(x).inInterval(x.open, 20, x.close, -20) && x.ordersMaintenanced && !x.salesConfirmedLeftovers) {
//		    confirmSales(x);
//		    if (curErr == 504) System.exit(0);                                               //exit before saving boolean so we re-do this loop
//		    x.salesConfirmedLeftovers = true;
//		    bookkeep_end(x);
//		}
//		if (MyDate.xNowTime(x).inInterval(x.close, -60, 0) && !x.purchasesConfirmed2) {
//		    confirmPurchases(x);
//		    if (curErr == 504) System.exit(0);
//		    x.purchasesConfirmed2 = true;
//		    bookkeep_end(x);
//		}
//		if (MyDate.xNowTime(x).inInterval(x.close, -10, 0) && !x.sold) { //!!
//		    timeToStartSelling(x);//!!
//		    sellLeftovers(x);
//		    x.sold = true;
//		    bookkeep_end(x);
//		}
//		if (MyDate.xNowTime(x).inInterval(x.close, -10, 0) && !x.symbolsUpdated) {	//!	//for US stocks, the whole closing process took between 4 and 5 minutes.
//		    updateSymbolsList(x);
//		    x.symbolsUpdated = true;
//		    bookkeep_end(x);
//		}
		if (MyDate.xNowTime(x).inInterval(x.close, -6, 0) && !x.salesConfirmed1) {

//		    Thread.sleep(5_000);	//!!
		    confirmSales(x);
		    if (curErr == 504) System.exit(0);                                               //exit before saving boolean so we re-do this loop
		    x.salesConfirmed1 = true;
		    bookkeep_end(x);
		}
//		if (MyDate.xNowTime(x).inInterval(x.close, 3, 10) && !x.salesConfirmed2) { //!	
//		    confirmSales(x);
//		    if (curErr == 504) System.exit(0);                                               //exit before saving boolean so we re-do this loop
//		    x.salesConfirmed2 = true;
//		    x.symbolsGotten = false;
//		    x.bought = false;
//		    x.purchasesConfirmed1 = false;
//		    x.purchasesConfirmed2 = false;
//		    x.sold = false;
//		    x.ordersMaintenanced = false;
//		    x.symbolsUpdated = false;                                              //!
//		    bookkeep_end(x);
////                    System.exit(0);     //!!
////                    return;           //!
//		}
//		if (!runForReal)	//!!!
//		    System.exit(0);
	    }
	    Thread.sleep(10);
	}
//        Object j = m_status;
    }

    private void connect() throws InterruptedException {
	myPrintln("connecting...");
	m_controller.connect("127.0.0.1", 7496, 0);
	Thread.sleep(2_000);
    }

    private void initialize(Boolean doConnect) throws InterruptedException, ParseException, IOException, Exception {
	if (!logsFolder.exists()) //creates all necessary subdirectories.
	    logsFolder.mkdirs();
	if (!downloadsDir.exists()) //creates all necessary subdirectories.
	    downloadsDir.mkdirs();
	try {                                       //mandatory try/catch
	    masterOrdersWriter.close();
	    completedSalesWriter.close();
	} catch (Exception e) {
	}
	if (!masterOrdersFile.exists()) {
	    Files.write(masterOrdersFile.toPath(), Arrays.asList(new String[]{doh.format_header()}), StandardCharsets.UTF_8);
	}
	if (!completedSalesFile.exists()) {
	    Files.write(completedSalesFile.toPath(), Arrays.asList(new String[]{doh.format_header()}), StandardCharsets.UTF_8);
	}
	masterOrdersWriter = new PrintWriter(new BufferedWriter(new FileWriter(masterOrdersFile, true)));
	completedSalesWriter = new PrintWriter(new BufferedWriter(new FileWriter(completedSalesFile, true)));

	logFile = new File(logsFolder.getAbsolutePath() + "\\" + logfileNameDateFormat.format((new Date(System.currentTimeMillis()))) + ".log");
	logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile)));

	for (EDx x : EDx.values()) {
	    x.open = hmsDateFormat.parse(x.openStr);		    //!!!
	    x.close = x.hmsDateFormat_TimeZone.parse(x.closeStr);

	    if (!x.progressBooleansFile.exists())
		writeProgressBooleans(x);
	}

	if (doConnect)
	    connect();
    }

    public void downloadEodDataFiles() throws AWTException, InterruptedException {
	
	//TODO:  use this to download in specific folder w/ no stupid keystrokes needed:
	/**
	String downloadFilepath = "C:\\Users\\User\\Documents\\stocks\\data\\newStuffNov2014\\MorningStar Downloads";
	HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
	chromePrefs.put("profile.default_content_settings.popups", 0);
	chromePrefs.put("download.default_directory", downloadFilepath);
	ChromeOptions options = new ChromeOptions();
	HashMap<String, Object> chromeOptionsMap = new HashMap<String, Object>();
	options.setExperimentalOptions("prefs", chromePrefs);
	options.addArguments("--test-type");
	DesiredCapabilities cap = DesiredCapabilities.chrome();
	cap.setCapability(ChromeOptions.CAPABILITY, chromeOptionsMap);
	cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
	cap.setCapability(ChromeOptions.CAPABILITY, options);
	WebDriver driver = new ChromeDriver(cap);
	*/
	
	
	
	
	
	myPrintln(("     downloadEodDataFiles"));

	WebDriver driver = new FirefoxDriver();

	driver.get("http://eoddata.com/register.aspx");
	driver.findElement(By.id("ctl00_cph1_lg1_txtEmail")).sendKeys("stuartcrobinson");
	driver.findElement(By.id("ctl00_cph1_lg1_txtPassword")).sendKeys("isdisit");
	driver.findElement(By.id("ctl00_cph1_lg1_btnLogin")).click();
	myPrintln(driver.getTitle());

	Robot r = new Robot();
	String urlBody = "http://eoddata.com/Data/symbollist.aspx?e=";

	boolean firstTime = true;
	for (EDx x : EDx.values()) {
	    driver.get(urlBody + x);

	    if (firstTime) {                                //interact with frfox download dialog
		Thread.sleep(15_000);

		r.keyPress(KeyEvent.VK_ALT);
		r.keyPress(KeyEvent.VK_S);
		r.keyRelease(KeyEvent.VK_S);
		r.keyPress(KeyEvent.VK_A);
		r.keyRelease(KeyEvent.VK_A);
		r.keyRelease(KeyEvent.VK_ALT);

		r.keyPress(KeyEvent.VK_ENTER);
		r.keyRelease(KeyEvent.VK_ENTER);
		firstTime = false;
	    }
	}
	driver.close();

	ArrayList<File> downloads = new ArrayList(Arrays.asList(downloadsDir.listFiles()));


	for (EDx x : EDx.values()) {                                                                //delete old ED files
	    List<File> xFiles = new ArrayList();
	    for (File file : downloads) {
		if (file.getAbsolutePath().contains(x.name()))
		    xFiles.add(file);
	    }
	    Collections.sort(xFiles,
		    new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
			    return -1 * Long.compare(f1.lastModified(), f2.lastModified());          //file with biggest lastModified value should be first
			}
		    }
	    );
	    xFiles.remove(0);   //save the first one from destruction
	    for (File file : xFiles) {
		file.delete();
		myPrintln("deleted from downloads: " + file.getName());
	    }

	}

	myPrintln(("     finished downloadEodDataFiles"));
    }

    public List<Stock> read_stocks_file(EDx x) throws IOException, ParseException {
	myPrintln(("     start read_stocks_file " + x.name()));

	if (x.stocks_file.exists()) {

	    List<Stock> stocks = new ArrayList();
	    try {
		List<String> stocks_file_lines = Files.readAllLines(x.stocks_file.toPath(), StandardCharsets.UTF_8);
		if (stocks_file_lines.get(0).contains("symbol,")) {
		    stocks_file_lines.remove(0);                                                                                                           //remove header
		}
		for (String fileLine : stocks_file_lines) {
		    stocks.add(new Stock(fileLine, x));
		}

	    } catch (IOException e) {
		myErrPrintln(e);
	    }

	    myPrintln(("read " + stocks.size() + " symbols"));
	    myPrintln(("     finished read_stocks_file " + x.name()));
	    return stocks;
	} else
	    return null;
    }

    public void searchAndBuy(EDx x) throws IOException, InterruptedException, ParseException {
	cancel_all_orders();
	myPrintln(("     start searchAndBuy " + x.name()));
	if (x.stocks == null) {
	    myPrintln("null stocks array, abort!");
	    return;
	}
	myPrintln(x.stocks.size() + " saved stocks loaded");
	doh.readOrderHistories(x);

	Col.set_column_positions(dummy_yahoo.getHeaderStr());


	int numShares, sublist_counter = 0;
	try {
	    toploop:
	    for (List<Stock> stocksSubList : Lists.partition(x.stocks, 99)) {
		sublist_counter++;
		stocksSubList = new ArrayList<>(stocksSubList);					//critical to avoid NoSuchElementException because of removing stocks from the list
		myPrintln("searching for stocks to buy, stock sublist " + sublist_counter);
		long sublist_timer_startTime = System.currentTimeMillis();

		for (Stock stock : stocksSubList) stock.requestQuote();


		{//get yahoo data while waiting for requests
		    StringBuilder yahoo_url_builder = new StringBuilder();
		    yahoo_url_builder.append((new YahooDataPackage()).getURL_prefix());

		    for (Stock stock : stocksSubList) {
			yahoo_url_builder.append(stock.colsMap.get(Col.symbol));
			yahoo_url_builder.append(x.yahooSuffix);
			yahoo_url_builder.append("+");
		    }
		    yahoo_url_builder.append("&f=");
		    yahoo_url_builder.append(dummy_yahoo.getURL_tags());

		    searchAndBuy(EDx.AMEX);	//!!! REMOVE THIS

		    myPrintln("trying to fetch " + stocksSubList.toString());
		    myPrintln("at  " + yahoo_url_builder.toString());

		    InputStream myInputStream = new URL(yahoo_url_builder.toString()).openStream();
		    String yahoo_results_line;
		    BufferedReader br = new BufferedReader(new InputStreamReader(myInputStream));
		    while ((yahoo_results_line = br.readLine()) != null) {
			MyVariablesHolder yahoo = new YahooDataPackage(yahoo_results_line);

			for (Stock stock : stocksSubList) {

			    if (stock.colsMap.get(Col.symbol).equals(yahoo.colsMap.get(Col.symbol))) {
				stock.store_yahoo_data((YahooDataPackage)yahoo);
				break;
			    }
			}
		    }
		}//end get yahoo data


		waitForBidAsk(stocksSubList);
		preenStocksByBidAsk(stocksSubList);

		for (Stock stock : stocksSubList) {
		    orders_timer_start_time = System.currentTimeMillis();
		    orders_count = 0;
		    double priceChange = pctChange(stock.colsMap.get(Col.oldBid), stock.handler.m_ask);

		    if (priceChange < priceChangeMax && priceChange > -0.5) {		//-0.5 to avoid stock splits.  todo but what if it plummets w/out stock split?  :(
//			    if (x == EDx.ASX) amountToSpend = 500;
			if (x == EDx.ASX) numShares = (int)Math.round(600 / stock.handler.m_ask);	//needs to spend $500.  use $600 in case price changes. 
			else numShares = Math.max((int)Math.round(2 / stock.handler.m_ask), 1);          //buy $2's worth of cheap stocks
			if (numShares > 0) {
			    if (orders_count > 6) break toploop; //keep	//!!!
//			    stock.fetchYahooWebpageStuff();	    //!!!
			    stock.buybuybuy(numShares * holdDayss.length);	//numshares per holddays strategy
			    for (int holdDays : holdDayss)
				x.orderHistories.add(new OrderHistory(stock, Strategy.A, Side.BOT, holdDays, numShares, (new Date(System.currentTimeMillis()))));
			}
		    }
		}
		doh.writeOrderHistories(x);
		long timePassed = System.currentTimeMillis() - sublist_timer_startTime;	    //check how much time has passed.  wait until it's 1000 ms total.
		long timeLeftToWait = Math.max(1000 - timePassed, 0);
		Thread.sleep(timeLeftToWait + 500);     //+ 200 just to be safe
	    }
	} catch (NoSuchElementException e) {
	    myErrPrintln(e);
	}
	doh.writeOrderHistories(x);
	myPrintln(("     finished searchAndBuy " + x.name() + ", tried to buy " + orders_count + " stocks"));
    }

    public void confirmPurchases(EDx x) throws InterruptedException, ParseException, IOException {
	myPrintln(("     start confirmPurchases " + x.name()));
	doh.readOrderHistories(x);

//        Thread.sleep(60_000);   //not for testing

	List<TradesPanelRow> tradesPanelRows = readTradesPanel();

	int confirmed = 0;
	for (OrderHistory oh : x.orderHistories) {

	    if (oh.colsMap.get(Col.buyTime) == null) {
		for (TradesPanelRow tradesRow : tradesPanelRows) {

		    Object asdfasdf = Double.class;

		    if (x.dayDateFormat_TimeZone.format(tradesRow.date).equals(
			    x.dayDateFormat_TimeZone.format((new Date(System.currentTimeMillis()))))
			    && DateUtils.addMinutes(tradesRow.date, 1).after((Date)oh.colsMap.get(Col.tryBuyTime)) //sometimes the trade row time is a second before the trybuytime.  
			    && tradesRow.symbol.equals(oh.colsMap.get(Col.symbol))
			    && tradesRow.currency.equals(oh.colsMap.get(Col.cur))
			    && tradesRow.side.equals("BOT")) {				//success!
			oh.recordPurchase(tradesRow);
			confirmed++;
			break;
		    }
		}
	    }
	    write_and_flush(masterOrdersWriter, oh.format_csv());
//            myPrintln(orderHistory.string());
	}
	doh.writeOrderHistories(x);

	myPrintln(("confirmed  " + confirmed + " new purchases"));
	myPrintln(("     finished confirmPurchases " + x.name()));
    }

    private void sellLeftovers(EDx x) throws IOException, ParseException {  //!!!
	myPrintln("     start sellLeftovers " + x);
	try {
	    doh.readOrderHistories(x);

	    orders_timer_start_time = System.currentTimeMillis();
	    orders_count = 0;

	    int numFoundToSell = 0;
	    for (OrderHistory oh : x.orderHistories) {
		if (true) {//oh.isLeftover()) {	//!!! if (oh.isLeftover()) {
		    try {
			int trySellShares = (Integer)oh.colsMap.get(Col.buyShares);
			NewContract nc = oh.newContract();
			(new Stock(nc)).sellsellsell(trySellShares);
			numFoundToSell++;
			oh.recordSaleAttempt((new Date(System.currentTimeMillis())), trySellShares);
		    } catch (InterruptedException e) {
			myErrPrintln(e);
		    }
		}
	    }
	    doh.writeOrderHistories(x);
	    myPrintln("found " + numFoundToSell + " to sell.");
	    myPrintln(("     finished sellLeftovers " + x.name()));
	} catch (IOException | ParseException e2) {
	    myErrPrintln(e2);
	}
    }

    private void timeToStartSelling(EDx x) throws IOException, ParseException {
	myPrintln(("     start selling " + x.name()));
	doh.readOrderHistories(x);

	orders_timer_start_time = System.currentTimeMillis();
	orders_count = 0;                                               //reset so if it happens to be high, we don't have to wait.

	for (OrderHistory oh : x.orderHistories) {
	    if (oh.has_shares_that_should_be_sold_today()) {
		try {
		    int trySellShares = (Integer)oh.colsMap.get(Col.buyShares);
		    NewContract nc = oh.newContract();
		    (new Stock(nc)).sellsellsell(trySellShares);

		    oh.recordSaleAttempt((new Date(System.currentTimeMillis())), trySellShares);
		} catch (InterruptedException e) {
		    myErrPrintln(e);
		}
	    }
	}
	doh.writeOrderHistories(x);
	myPrintln(("     finished timeToStartSelling " + x.name() + ", tried to sell " + orders_count));
    }

    public void confirmSales(EDx x) throws InterruptedException, ParseException, IOException, Exception {

	//TODO start here -- how to ensure that duplicates don't get written to completed sale?  
	//when a sale is confirmed, and it is written to completedsales, should it be removed from orderhistory?
	//no.  orderhistory should note that it is sold.  and if it is already sold, it won'd be sold again.
	//don't confirmed sales that already logged as sold, in OH's.

	myPrintln(("     start confirmSales " + x.name()));
	doh.readOrderHistories(x);

	Thread.sleep(10_000);                                           //wait for orders to trickle through IB

	List<TradesPanelRow> tradesPanelRows = readTradesPanel();

	int count = 0;
	for (OrderHistory oh : x.orderHistories) {

	    if (oh.colsMap.get(Col.trySellTime) != null
		    && oh.colsMap.get(Col.sellTime) == null) {
		for (TradesPanelRow tradesRow : tradesPanelRows) {
//
//                    myPrintln("****************************************************");
//                    myPrintln(tradesRow.symbol);
//                    myPrintln(dayDateFormat_TimeZone.format(tradesRow.date).equals(
//                            dayDateFormat_TimeZone.format((new Date(System.currentTimeMillis())))));
//                    myPrintln(DateUtils.addMinutes(tradesRow.date, 1).after(orderHistory.trySellTime));
//                    myPrintln(tradesRow.symbol.equals(orderHistory.nc.symbol()));
//                    myPrintln(tradesRow.currency.equals(orderHistory.nc.currency()));
//                    myPrintln(tradesRow.side.equals("SLD"));
//                    myPrintln(tradesRow.toString());
//                    myPrintln("****************************************************");

		    if (x.dayDateFormat_TimeZone.format(tradesRow.date).equals(
			    x.dayDateFormat_TimeZone.format((new Date(System.currentTimeMillis()))))
			    && DateUtils.addMinutes(tradesRow.date, 1).after((Date)oh.colsMap.get(Col.trySellTime))
			    && tradesRow.symbol.equals(oh.colsMap.get(Col.symbol))
			    && tradesRow.currency.equals(oh.colsMap.get(Col.cur))
			    && tradesRow.side.equals("SLD")) {
			count++;
			oh.recordSale(tradesRow);
			write_and_flush(completedSalesWriter, (new CompletedSale(oh)).format_csv());
			break;                                                                         //there could be several tradeRow entries for the same stock in OrderHistories, in case it was sold earlier in the day (esp problem during testing)
		    }
		}
	    }
	    write_and_flush(masterOrdersWriter, oh.format_csv());
//            myPrintln(orderHistory.string());
	}
	doh.writeOrderHistories(x);

	myPrintln(("found " + count + " newly confirmed sales"));
	myPrintln(("found " + x.count_sold_order_histories() + " total confirmed sales in the order histories"));
	myPrintln(("     finished confirmSales " + x.name()));
    }

    public void updateSymbolsList(EDx x) throws IOException, Exception {
	myPrintln(("     updateSymbolsList " + x.name()));

	List<Stock> stocks = new ArrayList();

	List<String> eoddataLines = x.downloadedEDDataFile.exists()
		? Files.readAllLines(x.downloadedEDDataFile.toPath(), StandardCharsets.UTF_8)
		: new ArrayList();

	for (String line : eoddataLines) {
	    Stock stock = new Stock(line, x.name());
	    if (!((String)stock.colsMap.get(Col.symbol)).contains("-") && !((String)stock.colsMap.get(Col.symbol)).contains(".")) //NYSE - all recognized symbols 4 letters or shorter, no weird characters//no effect on ASX or HKEX//LSE -- all recognized symbols are 4 letters long or less. max recognized ASX symbol length is 6. but this might also be max asx symbol length.  no "-" or "."'s in LSE  //these usually aren't recognized.  but what's the point of doing this?  there's plenty of time.  there's not plenty of time since we want to do it close to Closing time.  also need to sell then.
		stocks.add(stock);
	}

	updateStockPrices(stocks);

	//sort stocks by bid,  put tiny-priced stocks at the end
	Collections.sort(stocks, new StockPriceComparator());
	int indexFirstAboveCutoff = 0;
	double cutoff = 0.10;
	for (int i = 0; i < stocks.size(); i++)
	    if ((Double)stocks.get(i).colsMap.get(Col.oldBid) >= cutoff) {
		indexFirstAboveCutoff = i;
		break;
	    }

	List<Stock> stocksSmall = stocks.subList(0, indexFirstAboveCutoff);
	List<Stock> stocksBig = stocks.subList(indexFirstAboveCutoff, stocks.size());
	stocksBig.addAll(stocksSmall);
	stocks = stocksBig;

	ArrayList<String> symbolsInfoStrs = new ArrayList();
	symbolsInfoStrs.add(dstock.format_header());
	for (Stock stock : stocks)
	    symbolsInfoStrs.add(stock.format_csv());

	Files.write(x.stocks_file.toPath(), symbolsInfoStrs, StandardCharsets.UTF_8);
	myPrintln(("     finishedupdateSymbolsList " + x.name()));
    }

    public List<TradesPanelRow> readTradesPanel() {
	List<TradesPanelRow> tradesPanelRows = new ArrayList();
	try {                                                   //not ideal
	    tradesPanelRows = readTradesPanel_worker();
	} catch (InterruptedException | ParseException e) {
	    myErrPrintln(e);
	    try {
		tradesPanelRows = readTradesPanel_worker();
	    } catch (InterruptedException | ParseException e2) {
		myErrPrintln(e2);
		try {
		    tradesPanelRows = readTradesPanel_worker();
		} catch (InterruptedException | ParseException e3) {
		    myErrPrintln(e3);
		    myPrintln("failed to get trades panel after 3 tries");
		}
	    }
	}
	myPrintln("num tradesPanelRows " + tradesPanelRows.size());
	return tradesPanelRows;
    }

    public List<TradesPanelRow> readTradesPanel_worker() throws InterruptedException, ParseException {
	myPrintln("readTradesPanel...");
	m_tradingPanel.activated();
	m_tradingPanel.m_tradesPanel.activated();

	myPrintln("status: " + m_connectionPanel.m_status.getText());

	while ((m_tradingPanel.m_tradesPanel.m_trades.isEmpty()
		|| m_tradingPanel.m_tradesPanel.m_trades.
		get(m_tradingPanel.m_tradesPanel.m_trades.size() - 1).m_commissionReport == null)
		&& !m_connectionPanel.m_status.getText().contains("dis"))
	    Thread.sleep(5000);

	myPrintln("status: " + m_connectionPanel.m_status.getText());

	if (m_connectionPanel.m_status.getText().contains("dis"))
	    System.exit(0);

	List<TradesPanelRow> tradesPanelRows = new ArrayList();

	for (TradesPanel.FullExec full : m_tradingPanel.m_tradesPanel.m_trades) {
	    tradesPanelRows.add(new TradesPanelRow(
		    full.m_trade.m_time, full.m_trade.m_acctNumber, full.m_trade.m_side,
		    full.m_trade.m_shares, full.m_contract.description(),
		    full.m_trade.m_price,
		    full.m_commissionReport != null ? full.m_commissionReport.m_commission : null,
		    full.m_contract.currency()));
	}
	myPrintln("finished readTradesPanel...");
	return tradesPanelRows;

    }

    public List<PortfolioModelRow> readPortfolioModel() {
	myPrintln("readPortfolioModel...");
	//gets java.util.ConcurrentModificationException
	List<PortfolioModelRow> portfolioModelRows = new ArrayList();
	try {                                                   //not ideal
	    portfolioModelRows = readPortfolioModel_worker();
	} catch (InterruptedException | ParseException e) {
	    myErrPrintln(e);
	    try {
		portfolioModelRows = readPortfolioModel_worker();
	    } catch (InterruptedException | ParseException e2) {
		myErrPrintln(e2);
		try {
		    portfolioModelRows = readPortfolioModel_worker();
		} catch (InterruptedException | ParseException e3) {
		    myErrPrintln(e3);
		    myPrintln("failed to get portfolio model after 3 tries");
		}
	    }
	}
	myPrintln("finished readPortfolioModel...");

	myPrintln("num portfolio rows " + portfolioModelRows.size());
	return portfolioModelRows;
    }

    public List<PortfolioModelRow> readPortfolioModel_worker() throws InterruptedException, ParseException {

	m_acctInfoPanel.activated();

	while ((m_acctInfoPanel.m_portfolioModel.m_portfolioMap.values() == null
		|| m_acctInfoPanel.m_portfolioModel.m_portfolioMap.values().size() < 100)
		&& !m_connectionPanel.m_status.getText().contains("dis"))
	    Thread.sleep(5000);

	myPrintln("status: " + m_connectionPanel.m_status.getText());
	if (m_connectionPanel.m_status.getText().contains("dis"))
	    System.exit(0);

	List<PortfolioModelRow> portfolioModelRows = new ArrayList();

	for (Position pos : m_acctInfoPanel.m_portfolioModel.m_portfolioMap.values()) {
	    portfolioModelRows.add(
		    new PortfolioModelRow(
			    pos.contract(), pos.position()
		    ));
	}

	return portfolioModelRows;
    }

    private int preenStocksByBidAsk(List<Stock> stocks) throws InterruptedException {

	List<Integer> indicesToRemove = new ArrayList();
	for (int i = 0; i < stocks.size(); i++)
	    if (((Double)stocks.get(i).colsMap.get(Col.oldBid)) <= 0.0001 || stocks.get(i).handler.m_ask <= 0.0001)
		indicesToRemove.add(i);

	for (int i = indicesToRemove.size() - 1; i >= 0; i--)
	    stocks.remove((int)indicesToRemove.get(i));

	return indicesToRemove.size();
    }

    private void waitForBidAsk(List<Stock> stocksSubList) throws InterruptedException {

	long timerStartTime = System.currentTimeMillis();
	double listSize = stocksSubList.size();

	while (true) {
	    double numStocksWithData = 0;

	    for (Stock stock : stocksSubList) {
		if (stock.handler.m_bid > 0 && stock.handler.m_ask > 0) {
		    numStocksWithData++;
		}
	    }
	    if (numStocksWithData == listSize || (System.currentTimeMillis() - timerStartTime) > 3000) {
		return;
	    } else {
		Thread.sleep(50);
	    }
	}
    }

    private void cancel_all_orders() {
	myPrintln("cancel all orders");
	ApiDemo.INSTANCE.controller().cancelAllOrders();
    }

    private void set_TWS_logout_to(int n1, int n2, int n3) throws AWTException, InterruptedException {

	//around 33 steps.  30 * 30 seconds is 15 minutes. //reserve 20 minutes for this.

	int ms = 30_000;
	int q = 60_000;

	Robot robot = new Robot();

	Thread.sleep(2_000);                                                                                                                                           //Thread.sleep(1_000);

	robot.mouseMove(100, 2);
	Thread.sleep(2_000);                                                                                             //Thread.sleep(200);

	// LEFT CLICK
	robot.mousePress(InputEvent.BUTTON1_MASK);
	robot.mouseRelease(InputEvent.BUTTON1_MASK);
	Thread.sleep(ms);                                        //Thread.sleep(1_000);


	robot.keyPress(KeyEvent.VK_ALT);

	robot.keyPress(KeyEvent.VK_F);
	robot.keyRelease(KeyEvent.VK_F);
	Thread.sleep(ms + q);                                              //Thread.sleep(1_000);

	robot.keyPress(KeyEvent.VK_G);
	robot.keyRelease(KeyEvent.VK_G);
	Thread.sleep(ms + q);                                              //Thread.sleep(1_000);

	robot.keyRelease(KeyEvent.VK_ALT);
//        Thread.sleep(p + qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq);                                              //Thread.sleep(4_000);


	robot.keyPress(KeyEvent.VK_L);
	robot.keyRelease(KeyEvent.VK_L);
	Thread.sleep(ms + q);                                              //Thread.sleep(60_000);                //! change this to like 60 seconds 

	robot.keyPress(KeyEvent.VK_O);
	robot.keyRelease(KeyEvent.VK_O);

	robot.keyPress(KeyEvent.VK_C);
	robot.keyRelease(KeyEvent.VK_C);

	robot.keyPress(KeyEvent.VK_K);
	robot.keyRelease(KeyEvent.VK_K);

	robot.keyPress(KeyEvent.VK_SPACE);
	robot.keyRelease(KeyEvent.VK_SPACE);

	robot.keyPress(KeyEvent.VK_A);
	robot.keyRelease(KeyEvent.VK_A);

	robot.keyPress(KeyEvent.VK_N);
	robot.keyRelease(KeyEvent.VK_N);

	robot.keyPress(KeyEvent.VK_D);
	robot.keyRelease(KeyEvent.VK_D);

	robot.keyPress(KeyEvent.VK_SPACE);
	robot.keyRelease(KeyEvent.VK_SPACE);

	robot.keyPress(KeyEvent.VK_E);
	robot.keyRelease(KeyEvent.VK_E);

	robot.keyPress(KeyEvent.VK_X);
	robot.keyRelease(KeyEvent.VK_X);

	robot.keyPress(KeyEvent.VK_I);
	robot.keyRelease(KeyEvent.VK_I);

	robot.keyPress(KeyEvent.VK_I);
	robot.keyRelease(KeyEvent.VK_I);

	robot.keyPress(KeyEvent.VK_T);
	robot.keyRelease(KeyEvent.VK_T);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_TAB);
	robot.keyRelease(KeyEvent.VK_TAB);
	Thread.sleep(ms);

	robot.keyPress(KeyEvent.VK_TAB);
	robot.keyRelease(KeyEvent.VK_TAB);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_UP);
	robot.keyRelease(KeyEvent.VK_UP);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_UP);
	robot.keyRelease(KeyEvent.VK_UP);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_DOWN);
	robot.keyRelease(KeyEvent.VK_DOWN);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	//now "Lock and Exit" is highlighted.

	for (int i : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}) {

	    robot.keyPress(KeyEvent.VK_TAB);
	    robot.keyRelease(KeyEvent.VK_TAB);
	    Thread.sleep(ms);                                              //Thread.sleep(2000);
	}

	robot.keyPress(KeyEvent.VK_BACK_SPACE);
	robot.keyRelease(KeyEvent.VK_BACK_SPACE);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_BACK_SPACE);
	robot.keyRelease(KeyEvent.VK_BACK_SPACE);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_BACK_SPACE);
	robot.keyRelease(KeyEvent.VK_BACK_SPACE);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_BACK_SPACE);
	robot.keyRelease(KeyEvent.VK_BACK_SPACE);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_BACK_SPACE);
	robot.keyRelease(KeyEvent.VK_BACK_SPACE);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_BACK_SPACE);
	robot.keyRelease(KeyEvent.VK_BACK_SPACE);


	robot.keyPress(n1);
	robot.keyRelease(n1);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_SHIFT);
	robot.keyPress(KeyEvent.VK_SEMICOLON);
	robot.keyRelease(KeyEvent.VK_SEMICOLON);
	robot.keyRelease(KeyEvent.VK_SHIFT);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(n2);
	robot.keyRelease(n2);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(n3);
	robot.keyRelease(n3);
	Thread.sleep(ms);                                              //Thread.sleep(2000);



	robot.keyPress(KeyEvent.VK_TAB);
	robot.keyRelease(KeyEvent.VK_TAB);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_TAB);
	robot.keyRelease(KeyEvent.VK_TAB);
	Thread.sleep(ms);                                              //Thread.sleep(2000);

	robot.keyPress(KeyEvent.VK_TAB);
	robot.keyRelease(KeyEvent.VK_TAB);
	Thread.sleep(ms);                                              //Thread.sleep(2000);


	robot.keyPress(KeyEvent.VK_SPACE);
	robot.keyRelease(KeyEvent.VK_SPACE);
	Thread.sleep(ms);                                              //Thread.sleep(2_000);
    }

    private void updateStockPrices_worker(List<Stock> stocks) throws InterruptedException {

	requests_timer_start_time = System.currentTimeMillis();
	requests_count = 0;
	for (Stock stock : stocks) {
	    stock.requestQuote();
	    stock.colsMap.put(Col.oldTime, (new Date(System.currentTimeMillis())));
	}

	waitForBidAsk(stocks);
	for (Stock stock : stocks) {
	    stock.updateHandlerData();
	}
	int preened = preenStocksByBidAsk(stocks);
	myPrintln("preened " + preened);

	if (stocks.size() < 10) {
	    myPrintln("preened stocks list is too small. exiting.");
	}
    }

    private void updateStockPrices(List<Stock> stocks) throws InterruptedException {
	try {                                                   //not ideal
	    updateStockPrices_worker(stocks);
	} catch (InterruptedException e) {
	    myErrPrintln(e);
	    try {
		updateStockPrices_worker(stocks);
	    } catch (InterruptedException e2) {
		myErrPrintln(e2);
		try {
		    updateStockPrices_worker(stocks);
		} catch (InterruptedException e3) {
		    myErrPrintln(e3);
		    myPrintln("failed to update prices after 3 tries");


		}
	    }
	}
    }

    public void readProgressBooleans(EDx x) {
	if (!x.progressBooleansFile.exists()) return;

	try {

//            myPrintln("reading bools..." + x.name());
	    List<String> lines = Files.readAllLines(x.progressBooleansFile.toPath(), StandardCharsets.UTF_8);

	    if (lines.size() > 0) {
		for (String line : lines) {
		    String varName = line.split(",")[0];
		    boolean varValue = Boolean.parseBoolean(line.split(",")[1]);

		    switch (varName) {
			case "clock_set_1": x.clock_set_1 = varValue;
			    break;
			case "clock_set_2": x.clock_set_2 = varValue;
			    break;
			case "symbolsUpdated": x.symbolsUpdated = varValue;
			    break;
			case "symbolsGotten": x.symbolsGotten = varValue;
			    break;
			case "bought": x.bought = varValue;
			    break;
			case "purchasesConfirmed1": x.purchasesConfirmed1 = varValue;
			    break;
			case "purchasesConfirmed2": x.purchasesConfirmed2 = varValue;
			    break;
			case "sold": x.sold = varValue;
			    break;
			case "soldLeftovers": x.ordersMaintenanced = varValue;
			    break;
			case "salesConfirmed1": x.salesConfirmed1 = varValue;
			    break;
			case "salesConfirmed2": x.salesConfirmed2 = varValue;
			    break;
			case "salesConfirmedLeftovers": x.salesConfirmedLeftovers = varValue;
			    break;
//			case "soldUnaccountedFor": x.soldUnaccountedFor = varValue;
//			    break;
//			default:
//			    throw new IllegalArgumentException("Invalid day of the week: " + varName);

		    }
		}
	    }
	} catch (IOException | IllegalArgumentException e) {
	    myErrPrintln(e);
	}
    }

    public void writeProgressBooleans(EDx x) throws IOException, ParseException {
	x.progressBooleansWriter = new PrintWriter(new BufferedWriter(new FileWriter(x.progressBooleansFile)));     //deletes file before writing
	write_and_flush(x.progressBooleansWriter, "clock_set_1" + "," + x.clock_set_1.toString());
	write_and_flush(x.progressBooleansWriter, "clock_set_2" + "," + x.clock_set_2.toString());
	write_and_flush(x.progressBooleansWriter, "symbolsUpdated" + "," + x.symbolsUpdated.toString());
	write_and_flush(x.progressBooleansWriter, "symbolsGotten" + "," + x.symbolsGotten.toString());
	write_and_flush(x.progressBooleansWriter, "bought" + "," + x.bought.toString());
	write_and_flush(x.progressBooleansWriter, "purchasesConfirmed1" + "," + x.purchasesConfirmed1.toString());
	write_and_flush(x.progressBooleansWriter, "purchasesConfirmed2" + "," + x.purchasesConfirmed2.toString());
	write_and_flush(x.progressBooleansWriter, "sold" + "," + x.sold.toString());
	write_and_flush(x.progressBooleansWriter, "soldLeftovers" + "," + x.ordersMaintenanced.toString());
	write_and_flush(x.progressBooleansWriter, "salesConfirmed1" + "," + x.salesConfirmed1.toString());
	write_and_flush(x.progressBooleansWriter, "salesConfirmed2" + "," + x.salesConfirmed2.toString());
	write_and_flush(x.progressBooleansWriter, "salesConfirmedLeftovers" + "," + x.salesConfirmedLeftovers.toString());
	x.progressBooleansWriter.close();
    }

    public void bookkeep_end(EDx x) throws IOException, ParseException {
	writeProgressBooleans(x);
	myPrintln("curErr=" + curErr + ". " + m_connectionPanel.m_status.getText());
	if (m_connectionPanel.m_status.getText().contains("dis"))
	    System.exit(0);
	if (curErr == 504)
	    System.exit(0);    //! 
    }

    private double pctChange(Object _initial, Object _final) {
	return pctChange((Double)_initial, (Double)_final);
    }

    //TODO start here 
    private Double pctChange(Double _initial, Double _final) {
	return (_final - _initial) / _initial;

    }

    private void resetProgressBooleans() {
	for (EDx x : EDx.values())
	    x.progressBooleansFile.delete();
    }

    public enum Col {
	symbol,
	exch_IB,
	exch_prIB,
	x_ED,
	cur,
	secType,
	oldTime,
	oldBid,
	oldAsk,
	oldSpread,
	strat,
	side,
	holdDays,
	tryBuyTime,
	tryBuyShares,
	tryBuyBid,
	tryBuyAsk,
	tryBuySpread,
	tryBuyChange,
	buyTime,
	buyShares,
	buyPrice,
	buyCommission,
	buyChange,
	trySellTime,
	trySellShares,
	sellTime,
	sellShares,
	sellPrice,
	sellCommission,
	sellChange,
	PnL,
	totalCommission,
	rightBuyTime,
	rightSellTime,
	rightSellDay,
	heldDays,
	y_web_hasNewEvent,
	y_web_upgrade,
	y_web_downgrade,
	y_web_DE,
	y_web_profitMargin,
	y_web_returnOnAssets,
	y_web_returnOnEquity,
	y_web_cashPerShare,
	y_web_opinionThisWeek,
	y_web_opinionLastWeek,
	y_symbol,
	y_Last_Trade__Price_Only_,
	y_name,
	y_stock_exchange,
	y_Revenue,
	y_Earnings_per_Share,
	y_EPS_Estimate_Current_Year,
	y_EPS_Estimate_Next_Year,
	y_EPS_Estimate_Next_Quarter,
	y_Book_Value,
	y_EBITDA,
	y_Price_per_Sales,
	y_Price_per_Book,
	y_PE_Ratio,
	y_PE_Ratio__Realtime_,
	y_PEG_Ratio,
	y_Price_per_EPS_Estimate_Current_Year,
	y_Price_per_EPS_Estimate_Next_Year,
	y_Short_Ratio,
	y_Percent_Change_From_200_Day_Moving_Average,
	y_Change_From_50_Day_Moving_Average,
	y_Percent_Change_From_50_Day_Moving_Average,
	y_50_Day_Moving_Average,
	y_200_Day_Moving_Average;



	Class homeClass;
	Class dataClass;

	public List<String> ni = new ArrayList();	//name in
	public int ci;		//column in //set this smartly
	public int co;		//column out - these vars set in order that the cols are listed.

	static {
	    y_web_hasNewEvent.ni.addAll(Arrays.asList(new String[]{"y_web_hasNewEvent"}));
	    y_web_upgrade.ni.addAll(Arrays.asList(new String[]{"y_web_upgrade"}));
	    y_web_downgrade.ni.addAll(Arrays.asList(new String[]{"y_web_downgrade"}));
	    y_web_DE.ni.addAll(Arrays.asList(new String[]{"y_web_DE"}));
	    y_web_profitMargin.ni.addAll(Arrays.asList(new String[]{"y_web_profitMargin"}));
	    y_web_returnOnAssets.ni.addAll(Arrays.asList(new String[]{"y_web_returnOnAssets"}));
	    y_web_returnOnEquity.ni.addAll(Arrays.asList(new String[]{"y_web_returnOnEquity"}));
	    y_web_cashPerShare.ni.addAll(Arrays.asList(new String[]{"y_web_cashPerShare"}));
	    y_web_opinionThisWeek.ni.addAll(Arrays.asList(new String[]{"y_web_opinionThisWeek"}));
	    y_web_opinionLastWeek.ni.addAll(Arrays.asList(new String[]{"y_web_opinionLastWeek"}));
	    y_symbol.ni.addAll(Arrays.asList(new String[]{"y_symbol"}));
	    y_Last_Trade__Price_Only_.ni.addAll(Arrays.asList(new String[]{"y_Last_Trade__Price_Only_"}));
	    y_name.ni.addAll(Arrays.asList(new String[]{"y_name"}));
	    y_stock_exchange.ni.addAll(Arrays.asList(new String[]{"y_stock_exchange"}));
	    y_Revenue.ni.addAll(Arrays.asList(new String[]{"y_Revenue"}));
	    y_Earnings_per_Share.ni.addAll(Arrays.asList(new String[]{"y_Earnings_per_Share"}));
	    y_EPS_Estimate_Current_Year.ni.addAll(Arrays.asList(new String[]{"y_EPS_Estimate_Current_Year"}));
	    y_EPS_Estimate_Next_Year.ni.addAll(Arrays.asList(new String[]{"y_EPS_Estimate_Next_Year"}));
	    y_EPS_Estimate_Next_Quarter.ni.addAll(Arrays.asList(new String[]{"y_EPS_Estimate_Next_Quarter"}));
	    y_Book_Value.ni.addAll(Arrays.asList(new String[]{"y_Book_Value"}));
	    y_EBITDA.ni.addAll(Arrays.asList(new String[]{"y_EBITDA"}));
	    y_Price_per_Sales.ni.addAll(Arrays.asList(new String[]{"y_Price_per_Sales"}));
	    y_Price_per_Book.ni.addAll(Arrays.asList(new String[]{"y_Price_per_Book"}));
	    y_PE_Ratio.ni.addAll(Arrays.asList(new String[]{"y_PE_Ratio"}));
	    y_PE_Ratio__Realtime_.ni.addAll(Arrays.asList(new String[]{"y_PE_Ratio__Realtime_"}));
	    y_PEG_Ratio.ni.addAll(Arrays.asList(new String[]{"y_PEG_Ratio"}));
	    y_Price_per_EPS_Estimate_Current_Year.ni.addAll(Arrays.asList(new String[]{"y_Price_per_EPS_Estimate_Current_Year"}));
	    y_Price_per_EPS_Estimate_Next_Year.ni.addAll(Arrays.asList(new String[]{"y_Price_per_EPS_Estimate_Next_Year"}));
	    y_Short_Ratio.ni.addAll(Arrays.asList(new String[]{"y_Short_Ratio"}));
	    y_Percent_Change_From_200_Day_Moving_Average.ni.addAll(Arrays.asList(new String[]{"y_Percent_Change_From_200_Day_Moving_Average"}));
	    y_Change_From_50_Day_Moving_Average.ni.addAll(Arrays.asList(new String[]{"y_Change_From_50_Day_Moving_Average"}));
	    y_Percent_Change_From_50_Day_Moving_Average.ni.addAll(Arrays.asList(new String[]{"y_Percent_Change_From_50_Day_Moving_Average"}));
	    y_50_Day_Moving_Average.ni.addAll(Arrays.asList(new String[]{"y_50_Day_Moving_Average"}));
	    y_200_Day_Moving_Average.ni.addAll(Arrays.asList(new String[]{"y_200_Day_Moving_Average"}));
	    symbol.ni.addAll(Arrays.asList(new String[]{"symbol"}));
	    exch_IB.ni.addAll(Arrays.asList(new String[]{"x_IB", "ex_IB"}));
	    exch_prIB.ni.addAll(Arrays.asList(new String[]{"x_prIB", "ex_prIB"}));
	    x_ED.ni.addAll(Arrays.asList(new String[]{"x_ED", "ex_ED", "EDx"}));
	    cur.ni.addAll(Arrays.asList(new String[]{"cur"}));
	    secType.ni.addAll(Arrays.asList(new String[]{"secType"}));
	    oldTime.ni.addAll(Arrays.asList(new String[]{"closePriceTime"}));
	    oldBid.ni.addAll(Arrays.asList(new String[]{"closeBid"}));
	    oldAsk.ni.addAll(Arrays.asList(new String[]{"closeAsk"}));
	    oldSpread.ni.addAll(Arrays.asList(new String[]{"spread1"}));
	    strat.ni.addAll(Arrays.asList(new String[]{"strategy"}));
	    side.ni.addAll(Arrays.asList(new String[]{"side"}));
	    holdDays.ni.addAll(Arrays.asList(new String[]{"hold days"}));
	    tryBuyTime.ni.addAll(Arrays.asList(new String[]{"tryBuyTime"}));
	    tryBuyShares.ni.addAll(Arrays.asList(new String[]{"tryBuyShares"}));
	    tryBuyBid.ni.addAll(Arrays.asList(new String[]{"tryBuyBid"}));
	    tryBuyAsk.ni.addAll(Arrays.asList(new String[]{"tryBuyAsk"}));
	    tryBuySpread.ni.addAll(Arrays.asList(new String[]{"spread2"}));
	    tryBuyChange.ni.addAll(Arrays.asList(new String[]{"tryBuyChange"}));
	    buyTime.ni.addAll(Arrays.asList(new String[]{"buyTime"}));
	    buyShares.ni.addAll(Arrays.asList(new String[]{"buyShares"}));
	    buyPrice.ni.addAll(Arrays.asList(new String[]{"buyPrice"}));
	    buyCommission.ni.addAll(Arrays.asList(new String[]{"buyCommission"}));
	    buyChange.ni.addAll(Arrays.asList(new String[]{"buyChange"}));
	    trySellTime.ni.addAll(Arrays.asList(new String[]{"trySellTime"}));
	    trySellShares.ni.addAll(Arrays.asList(new String[]{"trySellShares"}));
	    sellTime.ni.addAll(Arrays.asList(new String[]{"sellTime"}));
	    sellShares.ni.addAll(Arrays.asList(new String[]{"sellShares"}));
	    sellPrice.ni.addAll(Arrays.asList(new String[]{"sellPrice"}));
	    sellCommission.ni.addAll(Arrays.asList(new String[]{"sellCommission"}));
	    sellChange.ni.addAll(Arrays.asList(new String[]{"sellChange"}));
	    PnL.ni.addAll(Arrays.asList(new String[]{"PnL"}));		//start completed sale
	    totalCommission.ni.addAll(Arrays.asList(new String[]{"totalCommission"}));
	    rightBuyTime.ni.addAll(Arrays.asList(new String[]{"rightBuyTime"}));
	    rightSellTime.ni.addAll(Arrays.asList(new String[]{"rightSellTime"}));
	    rightSellDay.ni.addAll(Arrays.asList(new String[]{"rightSellDay"}));
	    heldDays.ni.addAll(Arrays.asList(new String[]{"heldDays"}));

	    y_web_hasNewEvent.homeClass = OrderHistory.class;
	    y_web_upgrade.homeClass = OrderHistory.class;
	    y_web_downgrade.homeClass = OrderHistory.class;
	    y_web_DE.homeClass = OrderHistory.class;
	    y_web_profitMargin.homeClass = OrderHistory.class;
	    y_web_returnOnAssets.homeClass = OrderHistory.class;
	    y_web_returnOnEquity.homeClass = OrderHistory.class;
	    y_web_cashPerShare.homeClass = OrderHistory.class;
	    y_web_opinionThisWeek.homeClass = OrderHistory.class;
	    y_web_opinionLastWeek.homeClass = OrderHistory.class;
	    y_symbol.homeClass = YahooDataPackage.class;
	    y_Last_Trade__Price_Only_.homeClass = YahooDataPackage.class;
	    y_name.homeClass = YahooDataPackage.class;
	    y_stock_exchange.homeClass = YahooDataPackage.class;
	    y_Revenue.homeClass = YahooDataPackage.class;
	    y_Earnings_per_Share.homeClass = YahooDataPackage.class;
	    y_EPS_Estimate_Current_Year.homeClass = YahooDataPackage.class;
	    y_EPS_Estimate_Next_Year.homeClass = YahooDataPackage.class;
	    y_EPS_Estimate_Next_Quarter.homeClass = YahooDataPackage.class;
	    y_Book_Value.homeClass = YahooDataPackage.class;
	    y_EBITDA.homeClass = YahooDataPackage.class;
	    y_Price_per_Sales.homeClass = YahooDataPackage.class;
	    y_Price_per_Book.homeClass = YahooDataPackage.class;
	    y_PE_Ratio.homeClass = YahooDataPackage.class;
	    y_PE_Ratio__Realtime_.homeClass = YahooDataPackage.class;
	    y_PEG_Ratio.homeClass = YahooDataPackage.class;
	    y_Price_per_EPS_Estimate_Current_Year.homeClass = YahooDataPackage.class;
	    y_Price_per_EPS_Estimate_Next_Year.homeClass = YahooDataPackage.class;
	    y_Short_Ratio.homeClass = YahooDataPackage.class;
	    y_Percent_Change_From_200_Day_Moving_Average.homeClass = YahooDataPackage.class;
	    y_Change_From_50_Day_Moving_Average.homeClass = YahooDataPackage.class;
	    y_Percent_Change_From_50_Day_Moving_Average.homeClass = YahooDataPackage.class;
	    y_50_Day_Moving_Average.homeClass = YahooDataPackage.class;
	    y_200_Day_Moving_Average.homeClass = YahooDataPackage.class;
	    symbol.homeClass = Stock.class;
	    exch_IB.homeClass = Stock.class;
	    exch_prIB.homeClass = Stock.class;
	    x_ED.homeClass = Stock.class;
	    cur.homeClass = Stock.class;
	    secType.homeClass = Stock.class;
	    oldTime.homeClass = Stock.class;
	    oldBid.homeClass = Stock.class;
	    oldAsk.homeClass = Stock.class;
	    oldSpread.homeClass = Stock.class;
	    strat.homeClass = OrderHistory.class;
	    side.homeClass = OrderHistory.class;
	    holdDays.homeClass = OrderHistory.class;
	    tryBuyTime.homeClass = OrderHistory.class;
	    tryBuyShares.homeClass = OrderHistory.class;
	    tryBuyBid.homeClass = OrderHistory.class;
	    tryBuyAsk.homeClass = OrderHistory.class;
	    tryBuySpread.homeClass = OrderHistory.class;
	    tryBuyChange.homeClass = OrderHistory.class;
	    buyTime.homeClass = OrderHistory.class;
	    buyShares.homeClass = OrderHistory.class;
	    buyPrice.homeClass = OrderHistory.class;
	    buyCommission.homeClass = OrderHistory.class;
	    buyChange.homeClass = OrderHistory.class;
	    trySellTime.homeClass = OrderHistory.class;
	    trySellShares.homeClass = OrderHistory.class;
	    sellTime.homeClass = OrderHistory.class;
	    sellShares.homeClass = OrderHistory.class;
	    sellPrice.homeClass = OrderHistory.class;
	    sellCommission.homeClass = OrderHistory.class;
	    sellChange.homeClass = OrderHistory.class;
	    PnL.homeClass = CompletedSale.class;
	    totalCommission.homeClass = CompletedSale.class;
	    rightBuyTime.homeClass = CompletedSale.class;
	    rightSellTime.homeClass = CompletedSale.class;
	    rightSellDay.homeClass = CompletedSale.class;
	    heldDays.homeClass = CompletedSale.class;

	    y_web_hasNewEvent.dataClass = Boolean.class;
	    y_web_upgrade.dataClass = Boolean.class;
	    y_web_downgrade.dataClass = Boolean.class;
	    y_web_DE.dataClass = String.class;
	    y_web_profitMargin.dataClass = String.class;
	    y_web_returnOnAssets.dataClass = String.class;
	    y_web_returnOnEquity.dataClass = String.class;
	    y_web_cashPerShare.dataClass = String.class;
	    y_web_opinionThisWeek.dataClass = String.class;
	    y_web_opinionLastWeek.dataClass = String.class;
	    y_symbol.dataClass = String.class;
	    y_Last_Trade__Price_Only_.dataClass = String.class;
	    y_name.dataClass = String.class;
	    y_stock_exchange.dataClass = String.class;
	    y_Revenue.dataClass = String.class;
	    y_Earnings_per_Share.dataClass = String.class;
	    y_EPS_Estimate_Current_Year.dataClass = String.class;
	    y_EPS_Estimate_Next_Year.dataClass = String.class;
	    y_EPS_Estimate_Next_Quarter.dataClass = String.class;
	    y_Book_Value.dataClass = String.class;
	    y_EBITDA.dataClass = String.class;
	    y_Price_per_Sales.dataClass = String.class;
	    y_Price_per_Book.dataClass = String.class;
	    y_PE_Ratio.dataClass = String.class;
	    y_PE_Ratio__Realtime_.dataClass = String.class;
	    y_PEG_Ratio.dataClass = String.class;
	    y_Price_per_EPS_Estimate_Current_Year.dataClass = String.class;
	    y_Price_per_EPS_Estimate_Next_Year.dataClass = String.class;
	    y_Short_Ratio.dataClass = String.class;
	    y_Percent_Change_From_200_Day_Moving_Average.dataClass = String.class;
	    y_Change_From_50_Day_Moving_Average.dataClass = String.class;
	    y_Percent_Change_From_50_Day_Moving_Average.dataClass = String.class;
	    y_50_Day_Moving_Average.dataClass = String.class;
	    y_200_Day_Moving_Average.dataClass = String.class;
	    symbol.dataClass = String.class;
	    exch_IB.dataClass = String.class;
	    exch_prIB.dataClass = String.class;
	    x_ED.dataClass = EDx.class;
	    cur.dataClass = String.class;
	    secType.dataClass = String.class;
	    oldTime.dataClass = Date.class;
	    oldBid.dataClass = Double.class;
	    oldAsk.dataClass = Double.class;
	    oldSpread.dataClass = Double.class;
	    strat.dataClass = Strategy.class;
	    side.dataClass = Side.class;
	    holdDays.dataClass = Integer.class;
	    tryBuyTime.dataClass = Date.class;
	    tryBuyShares.dataClass = Integer.class;
	    tryBuyBid.dataClass = Double.class;
	    tryBuyAsk.dataClass = Double.class;
	    tryBuySpread.dataClass = Double.class;
	    tryBuyChange.dataClass = Double.class;
	    buyTime.dataClass = Date.class;
	    buyShares.dataClass = Integer.class;
	    buyPrice.dataClass = Double.class;
	    buyCommission.dataClass = Double.class;
	    buyChange.dataClass = Double.class;
	    trySellTime.dataClass = Date.class;
	    trySellShares.dataClass = Integer.class;
	    sellTime.dataClass = Date.class;
	    sellShares.dataClass = Integer.class;
	    sellPrice.dataClass = Double.class;
	    sellCommission.dataClass = Double.class;
	    sellChange.dataClass = Double.class;
	    PnL.dataClass = Double.class;
	    totalCommission.dataClass = Double.class;
	    rightBuyTime.dataClass = Boolean.class;
	    rightSellTime.dataClass = Boolean.class;
	    rightSellDay.dataClass = Boolean.class;
	    heldDays.dataClass = Integer.class;

	}


	private static void set_column_positions(String headerStr) {
	    //set co
	    int columnOut = 0;
	    for (Col col : Col.values()) {
		col.ci = -1;			    //default
		col.co = columnOut;
		columnOut++;
	    }

	    //set ci
	    String[] headerAr = headerStr.split(",");
	    for (int i = 0; i < headerAr.length; i++) {
		for (Col col : Col.values()) {
		    if (col.ni.contains(headerAr[i]) || col.toString().equals(headerAr[i])) {
			col.ci = i;
		    }
		}
	    }
	}


    }

    public enum EDx {

	NYSE,
	NASDAQ,
	AMEX,
	OTCBB,
	LSE,
	HKEX,
	ASX;

	private static EDx getX(String string) {
	    if (string.equals((EDx.NYSE.name()))) {
		return EDx.NYSE;
	    }
	    if (string.equals((EDx.NASDAQ.name()))) {
		return EDx.NASDAQ;
	    }
	    if (string.equals((EDx.AMEX.name()))) {
		return EDx.AMEX;
	    }
	    if (string.equals((EDx.OTCBB.name()))) {
		return EDx.OTCBB;
	    }
	    if (string.equals((EDx.LSE.name()))) {
		return EDx.LSE;
	    }
	    if (string.equals((EDx.HKEX.name()))) {
		return EDx.HKEX;
	    } else {
		return EDx.ASX;
	    }
	}

	public Boolean clock_set_1 = false;
	public Boolean clock_set_2 = true;
	public Boolean symbolsUpdated = false;
	public Boolean symbolsGotten = false;
	public Boolean bought = false;
	public Boolean purchasesConfirmed1 = false;
	public Boolean purchasesConfirmed2 = false;
	public Boolean sold = false;
	public Boolean ordersMaintenanced = false;
	public Boolean salesConfirmed1 = false;
	public Boolean salesConfirmed2 = false;
	public Boolean salesConfirmedLeftovers = false;
	public List<OrderHistory> orderHistories = new ArrayList();
	private String openStr;
	private String closeStr;
	public String currency;
	public String yahooSuffix;
	private Date open;
	private Date close;
	public List<Stock> stocks;
	public File orderHistoriesFile = new File(
		"C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\stocks\\IB\\data\\OrderHistories "
		+ this.name() + ".csv");
	public File progressBooleansFile = new File(
		"C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\stocks\\IB\\data\\ProgressBooleans "
		+ this.name() + ".csv");
	public File downloadedEDDataFile = new File(
		"C:\\Users\\" + System.getProperty("user.name") + "\\Downloads\\"
		+ this.name() + ".txt");
	public File stocks_file = new File(
		"C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\stocks\\IB\\data\\stocks "
		+ this.name() + ".csv");
	public int buyOpenOffsetMinutes = 120;  //defaultt to 2 hours
	PrintWriter orderHistoriesWriter, progressBooleansWriter;
	SimpleDateFormat bigDateFormat_TimeZone = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
	SimpleDateFormat dayDateFormat_TimeZone = new SimpleDateFormat("yyyyMMdd");
	SimpleDateFormat hmsDateFormat_TimeZone = new SimpleDateFormat("HH:mm:ss");

	static {

	    NYSE.openStr = "09:30:00";
	    NYSE.closeStr = "16:00:00";

	    NASDAQ.openStr = "09:30:00";
	    NASDAQ.closeStr = "16:00:00";

	    AMEX.openStr = "09:30:00";
	    AMEX.closeStr = "16:00:00";

	    OTCBB.openStr = "09:30:00";
	    OTCBB.closeStr = "16:00:00";

	    LSE.openStr = "08:00:00";
	    LSE.closeStr = "16:20:00";//"16:30:00";	    //LSE closes at 4:30 but auction / preclosing starts at 4:20 so we should end there.  harder to sell afterwards. 

	    HKEX.openStr = "09:15:00";		    //HKEX min purchase shares seems to be between 2000 and 20,000.
	    HKEX.closeStr = "16:00:00";

	    ASX.openStr = "10:00:00";		    //ASX website says 10, not 9:50
	    ASX.closeStr = "16:00:00";		    //from 4 to 4:12 is "pre-closing / auction" according to IB cust service man

	    NYSE.currency = "USD";
	    NASDAQ.currency = "USD";
	    AMEX.currency = "USD";
	    OTCBB.currency = "USD";
	    LSE.currency = "GBP";
	    HKEX.currency = "HKD";
	    ASX.currency = "AUD";

	    NYSE.yahooSuffix = "";
	    NASDAQ.yahooSuffix = "";
	    AMEX.yahooSuffix = "";
	    OTCBB.yahooSuffix = "";
	    LSE.yahooSuffix = ".L";
	    HKEX.yahooSuffix = ".HK";
	    ASX.yahooSuffix = ".AX";

	    AMEX.dayDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    AMEX.bigDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    NYSE.hmsDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    NYSE.dayDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    NYSE.bigDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    NASDAQ.hmsDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    NASDAQ.dayDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    NASDAQ.bigDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    OTCBB.hmsDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    OTCBB.dayDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    OTCBB.bigDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	    LSE.hmsDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("Europe/London"));
	    LSE.dayDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("Europe/London"));
	    LSE.bigDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("Europe/London"));
	    HKEX.hmsDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("Asia/Hong_Kong"));
	    HKEX.dayDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("Asia/Hong_Kong"));
	    HKEX.bigDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("Asia/Hong_Kong"));
	    ASX.hmsDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));
	    ASX.dayDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));
	    ASX.bigDateFormat_TimeZone.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));

	}

	private int count_sold_order_histories() {

	    int counter = 0;
	    for (OrderHistory oh : orderHistories) {
		if (oh.was_sold()) {
		    counter++;
		}
	    }
	    return counter;
	}


    }

    public enum Strategy {
	A, B
    }

    public enum Side {
	BOT, SLD
    }

    public static class MyDate extends Date {

	public static Date read_my_date_string(String dateStr) {
	    try {
		return clearDateFormat2.parse(dateStr);
	    } catch (ParseException e1) {
		try {
		    return clearDateFormat.parse(dateStr);
		} catch (ParseException e2) {
		    try {
			return bigDateFormat.parse(dateStr);
		    } catch (ParseException e3) {

			try {
			    return clearDateFormat3.parse(dateStr);
			} catch (ParseException e4) {

			    try {
				return DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.US).parse(dateStr);
			    } catch (ParseException e5) {
				return null;
			    }
			}
		    }
		}
	    }
	}

	private static String format_my_date(Date date) {
	    return clearDateFormat2.format(date);
	}

	private static MyDate timeOnlyDate(Date date) throws ParseException {
	    return new MyDate(hmsDateFormat.parse(hmsDateFormat.format(date)));
	}

	private static MyDate dayOnlyDate(Date date) throws ParseException {
	    return new MyDate(dayDateFormat.parse(dayDateFormat.format(date)));
	}

	private static Date time_component(Date date) throws ParseException {

	    String timeOnlyString = hmsDateFormat.format(date);

	    return hmsDateFormat.parse(timeOnlyString);
	}

	private static Date day_component(Date date) throws ParseException {

	    String dateOnlyString = dayDateFormat.format(date);

	    return dayDateFormat.parse(dateOnlyString);
	}


	public MyDate(long milli) {
	    super(milli);
	}

	public MyDate(Date date) {
	    super(date.getTime());
	}

	private boolean inInterval(Date targetDate, int minuteStartOffset, int minuteEndOffset) {
//            return this.after(new Date(targetDate.getTime() + minuteStartOffset * 60_000))
//                    && this.before(new Date(targetDate.getTime() + minuteEndOffset * 60_000));

	    return inInterval(targetDate, minuteStartOffset, targetDate, minuteEndOffset);

	}


	private boolean inInterval(Date date_start, int minuteStartOffset, Date date_end, int minuteEndOffset) {
	    minuteStartOffset -= 10000;	   //for testing    //!!!                                                         //!
	    minuteEndOffset += 10000;	    //                //!!!                                                      //!

	    Date earlyBounds = new Date(date_start.getTime() + minuteStartOffset * 60_000);

	    Date lateBounds = new Date(date_end.getTime() + minuteEndOffset * 60_000);

//	    System.out.println("1. " + earlyBounds);
//	    System.out.println("2. " + date_start);
//	    System.out.println("3. " + date_end);
//	    System.out.println("4. " + lateBounds);

	    return this.after(earlyBounds)
		    && this.before(lateBounds);
	}

	private boolean isWorkDay() {
	    return MyDate.isWorkDay(this);
	}

	private static boolean isWorkDay(Date date) {
	    Calendar cal = Calendar.getInstance();
	    cal.setTime(date);
	    return !(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);
	}


	/** workdays only.  there are 0 days between a day and itself */
	private static int days_from_first_to_second(Date first_date, Date second_date) throws Exception {
	    //usage:  there is 1 day between monday and wednesday

	    if (DateUtils.isSameDay(first_date, second_date)) //important so next else if doesn't catch same days
		return 0;
	    else if (first_date.after(second_date))
		return -1;
	    else {
		Date iterator = first_date;

		int days_between = 0;
		while (true) {
		    if (DateUtils.isSameDay(iterator, second_date))
			return days_between;

		    do {
			iterator = DateUtils.addDays(iterator, 1);
		    } while (!MyDate.isWorkDay(iterator));

		    days_between++;

		    if (days_between > 1_000)
			throw new Exception("in days_from_first_to_second, over 1,000 days between the two dates");
		}
	    }
	}

	private Date toDate() {
	    return new Date(this.getTime());
	}


	public static MyDate xNowTime(EDx x) throws ParseException {
	    return new MyDate(hmsDateFormat.parse(
		    x.hmsDateFormat_TimeZone.format(
			    new MyDate(System.currentTimeMillis())
		    )
	    ));
	}

	public static MyDate xNowDay(EDx x) throws ParseException {
	    return new MyDate(
		    xDay(x, new Date(System.currentTimeMillis()))
	    );
	}

	public static MyDate xNowDate(EDx x) throws ParseException {
	    return new MyDate(
		    xDate(x, new Date(System.currentTimeMillis())
		    )
	    );
	}

	public static Date xDay(EDx x, Date date) throws ParseException {
	    return dayDateFormat.parse(
		    x.dayDateFormat_TimeZone.format(date)
	    );
	}

	public static Date xDate(EDx x, Date date) throws ParseException {

	    return bigDateFormat.parse(
		    x.bigDateFormat_TimeZone.format(date)
	    );
	}

    }

    public class StockPriceComparator implements Comparator<Stock> {

	@Override
	public int compare(Stock stock1, Stock stock2) {
	    return ((Double)stock1.colsMap.get(Col.oldBid)).compareTo((Double)stock2.colsMap.get(Col.oldBid));
	}
    }

    public class TradesPanelRow {

	String time, acctNumber, side, symbol, secType, exchange, currency;
	Integer shares;
	double price, commission;
	Date date;

	public TradesPanelRow(String time, String acctNumber, String side, int shares,
		String description, double price, double commission, String currency) throws ParseException {
	    this.time = time;
	    this.acctNumber = acctNumber;
	    this.side = side;
	    this.shares = shares;
	    this.price = price;
	    this.commission = commission;
	    String[] descriptionAr = description.split(" ");
	    this.symbol = descriptionAr[0];
	    this.secType = descriptionAr[1];
	    this.exchange = descriptionAr[2];
	    this.date = ibTradePanelDateFormat.parse(time);
	    this.currency = currency;
	}

	public String string() {
	    StringBuilder sb = new StringBuilder();
	    sb.append(MyDate.format_my_date(date));
	    sb.append(cs);
	    sb.append(time);
	    sb.append(cs);
	    sb.append(acctNumber);
	    sb.append(cs);
	    sb.append(side);
	    sb.append(cs);
	    sb.append(shares);
	    sb.append(cs);
	    sb.append(symbol);
	    sb.append(cs);
	    sb.append(secType);
	    sb.append(cs);
	    sb.append(exchange);
	    sb.append(cs);
	    sb.append(price);
	    sb.append(cs);
	    sb.append(commission);
	    sb.append(cs);
	    sb.append(currency);
	    return sb.toString();
	}
    }

    public class PortfolioModelRow {
	NewContract nc;
	Integer shares;

	public PortfolioModelRow(NewContract newContract, Integer shares) {
	    this.nc = newContract;
	    this.shares = shares;
	}

	private String string() {
	    return String.format("%s, %d", NewContractToString(nc), shares);

	}
    }

    public class MyVariablesHolder {

	Map<Col, Object> colsMap;
	int number_of_variables;

	public MyVariablesHolder(MyVariablesHolder child) {
	    this.colsMap = child == null ? new HashMap() : child.colsMap;
	}

	public MyVariablesHolder(String fileLine, EDx x) throws ParseException {
//	    this.colsMap = child == null ? new HashMap() : child.colsMap;		//is this ever useful?

	    //TODO start here this isn't working very well!!  it thinks dates are null, but clearly listed in OH file

	    colsMap = new HashMap();

	    String[] in = fileLine.split(csvRegExSplitter);
	    for (int i = 0; i < in.length; i++) {
		in[i] = in[i].replace(",", "").replace("\"", "");
	    }

	    if (in.length < 10) {
//		System.out.println(x.toString());
		colsMap.put(Col.x_ED, x);		    //!! todo this should be removed once stocks files has more data -- including EDx

		//old stocks file format. just 7 or 8 columns

		colsMap.put(Col.symbol, in[0]);
		colsMap.put(Col.exch_IB, in[1]);
		colsMap.put(Col.exch_prIB, in[2]);
		colsMap.put(Col.cur, in[3]);
		colsMap.put(Col.secType, in[4]);
		colsMap.put(Col.oldTime, MyDate.read_my_date_string(in[5]));
		try {
		    colsMap.put(Col.oldBid, Double.parseDouble(in[6]));
		} catch (Exception e) {
		}
		try {
		    colsMap.put(Col.oldAsk, Double.parseDouble(in[7]));
		} catch (Exception e) {
		}

	    } else {
		//match array values with class variables.	//does this work for yahoo?
		for (Col col : Col.values()) {
		    try {
			if (!in[col.ci].equals("-")) {
			    if (col.dataClass.equals(Date.class)) colsMap.put(col, MyDate.read_my_date_string(in[col.ci]));
			    if (col.dataClass.equals(Double.class)) colsMap.put(col, Double.parseDouble(in[col.ci]));
			    if (col.dataClass.equals(Integer.class)) colsMap.put(col, Integer.parseInt(in[col.ci]));
			    if (col.dataClass.equals(Strategy.class)) colsMap.put(col, Strategy.valueOf(in[col.ci]));
			    if (col.dataClass.equals(Side.class)) colsMap.put(col, Side.valueOf(in[col.ci]));
			    if (col.dataClass.equals(EDx.class)) colsMap.put(col, EDx.valueOf(in[col.ci]));
			    if (col.dataClass.equals(String.class)) colsMap.put(col, in[col.ci]);
			}
		    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			colsMap.put(col, null);
		    }
		}
	    }
	}

	private boolean hasData() {
	    return !colsMap.isEmpty();
	}

	public String format_header() {
	    StringBuilder sb = new StringBuilder();
//	    try {
//		sb.append(childVariablesHolder.format_header());
//		sb.append(c);
//	    } catch (Exception e) {
//	    }
	    for (Col col : Col.values()) {
//		System.out.println(col);
//		System.out.println(Col.PnL.);
//		System.out.println(col.homeClass);
//		if (col.homeClass.equals(this.getClass())) {
		sb.append(col.toString());
		sb.append(c);
//		}

	    }
	    return sb.toString();
	}

	public String format_csv() {
	    String[] out = new String[Col.values().length];

	    for (Col col : Col.values()) {
		Object value = colsMap.get(col);

		try {
		    if (colsMap.get(col) == null)
			out[col.co] = "-";
		    else if (col.equals(Col.strat))
			out[col.co] = Strategy.A.toString();				//SCR!! they are all forced to say strategy A now
		    else if (col.dataClass.equals(Double.class))
			out[col.co] = decimalFormat.format((Double)colsMap.get(col));
		    else if (col.dataClass.equals(Date.class))
			out[col.co] = MyDate.format_my_date((Date)colsMap.get(col));
		    else
			out[col.co] = colsMap.get(col).toString();
		} catch (Exception e) {
		}
	    }

	    StringBuilder sb = new StringBuilder();
	    for (String str : out) {
		sb.append(str == null ? "-" : str);
		sb.append(c);
	    }
	    return sb.toString();
	}

	public int numberOfVariables() {
	    int counter = 0;
	    for (Col col : Col.values())
		if (col.homeClass.isInstance(this.getClass()))
		    counter++;

	    return counter;
	}

	public NewContract newContract() {
	    Contract contract = new Contract();
	    contract.m_symbol = (String)this.colsMap.get(Col.symbol);			//
	    contract.m_localSymbol = (String)this.colsMap.get(Col.symbol);			// this.symbol;
	    contract.m_secType = (String)this.colsMap.get(Col.secType);			//this.secType;
	    contract.m_exchange = (String)this.colsMap.get(Col.exch_IB);			//this.exch_IB;
	    contract.m_primaryExch = (String)this.colsMap.get(Col.exch_prIB);			// this.exch_prIB;
	    contract.m_currency = (String)this.colsMap.get(Col.cur);			//this.currency;
	    return new NewContract(contract);
	}

	private Date supposed_to_sell_Date() throws ParseException {
	    if (colsMap.get(Col.tryBuyTime) == null) return null;		    //this should never be null todo why is this null?
	    MyDate sellDate = new MyDate((Date)colsMap.get(Col.tryBuyTime));
	    for (int i = 1; i <= (Integer)colsMap.get(Col.holdDays); i++) {
		do {
		    sellDate = new MyDate(DateUtils.addDays(sellDate, 1));
		} while (!sellDate.isWorkDay());
	    }
	    return sellDate.toDate();
	}
    }

    public class YahooDataPackage extends MyVariablesHolder {

	private YahooDataPackage() {
	    super((MyVariablesHolder)null);
	}

	private YahooDataPackage(String yahoo_results_line) throws ParseException {
	    super(yahoo_results_line, EDx.AMEX);	    //!! remove this EDx thing later after stocks files are updated
	}

	private String getURL_tags() {
	    return "sl1nxs6ee7e8e9b4j4p5p6rr2r5r6r7s7m6m7m8m3m4";
	}

	private String getHeaderStr() {
	    /*
	     sl1nxs6ee7e8e9b4j4p5p6rr2r5r6r7s7m6m7m8m3m4:	
	     s: symbol
	     l1: Last Trade (Price Only)
	     n: name
	     x: stock exchange
	     s6: Revenue
	     e: Earnings per Share	
	     e7: EPS Estimate Current Year
	     e8: EPS Estimate Next Year	
	     e9: EPS Estimate Next Quarter
	     b4: Book Value	
	     j4: EBITDA	
	     p5: Price / Sales	
	     p6: Price / Book	
	     r: P/E Ratio	
	     r2: P/E Ratio (Realtime)	
	     r5: PEG Ratio	
	     r6: Price / EPS Estimate Current Year	
	     r7: Price / EPS Estimate Next Year	
	     s7: Short Ratio
	     m6: Percent Change From 200 Day Moving Average
	     m7: Change From 50 Day Moving Average
	     m8: Percent Change From 50 Day Moving Average
	     m3: 50 Day Moving Average
	     m4: 200 Day Moving Average
	     */
	    return "y_symbol,y_Last_Trade__Price_Only_,y_name,y_stock_exchange,y_Revenue,y_Earnings_per_Share,y_EPS_Estimate_Current_Year,y_EPS_Estimate_Next_Year,y_EPS_Estimate_Next_Quarter,y_Book_Value,y_EBITDA,y_Price_per_Sales,y_Price_per_Book,y_PE_Ratio,y_PE_Ratio__Realtime_,y_PEG_Ratio,y_Price_per_EPS_Estimate_Current_Year,y_Price_per_EPS_Estimate_Next_Year,y_Short_Ratio,y_Percent_Change_From_200_Day_Moving_Average,y_Change_From_50_Day_Moving_Average,y_Percent_Change_From_50_Day_Moving_Average,y_50_Day_Moving_Average,y_200_Day_Moving_Average";


	}

	private String getURL_prefix() {
	    return "http://finance.yahoo.com/d/quotes.csv?s=";
	}

	public void fetchYahooFinanceData(EDx x) throws IOException, InterruptedException, ParseException {

	    /*
	     when to do this?  at open.  can this alternate with IB requests so as to not take extra time??
	    
	     //shit ... i could be doing all these tests just w/ yahoo finance data for free... free realtime bid, ask, PE :( fuck
	     //i need to set this up and start it running on the linux vps right now.

	     //limit is 200 symbols per request

	     //options: http://www.gummy-stuff.org/Yahoo-data.htm
	     //ex url: "http://finance.yahoo.com/d/quotes.csv?s=XOM+BBDb.TO+JNJ+MSFT&f=snd1l1yr"
	    //http://www.financialwisdomforum.org/gummy-stuff/Yahoo-data.htm

	     use stringbuilder to build URL.  try all at once first.  see if that works.  just get
	     sl1nxs6ee7e8e9b4j4p5p6rr2r5r6r7s7m6m7m8m3m4:	
	     s: symbol
	     l1: Last Trade (Price Only)
	     n: name
	     x: stock exchange
	     s6: Revenue
	     e: Earnings per Share	
	     e7: EPS Estimate Current Year
	     e8: EPS Estimate Next Year	
	     e9: EPS Estimate Next Quarter
	     b4: Book Value	
	     j4: EBITDA	
	     p5: Price / Sales	
	     p6: Price / Book	
	     r: P/E Ratio	
	     r2: P/E Ratio (Realtime)	
	     r5: PEG Ratio	
	     r6: Price / EPS Estimate Current Year	
	     r7: Price / EPS Estimate Next Year	
	     s7: Short Ratio
	     m6: Percent Change From 200 Day Moving Average
	     m7: Change From 50 Day Moving Average
	     m8: Percent Change From 50 Day Moving Average
	     m3: 50 Day Moving Average
	     m4: 200 Day Moving Average

	     LSE:  .L
	     ASK: .AX
	     HK: .HK
	     US stocks, no suffix
	     */

	    myPrintln(("     start fetchYahooFinanceData " + x.name()));


	    Col.set_column_positions(dummy_yahoo.getHeaderStr());
	    x.stocks = read_stocks_file(x);
	    long yahoo_timer_start_time = System.currentTimeMillis();
	    int goodYahooCounter = 0;

	    for (List<Stock> stocksSubList : Lists.partition(x.stocks, 200)) {

		StringBuilder yahoo_url_builder = new StringBuilder();
		yahoo_url_builder.append((new YahooDataPackage()).getURL_prefix());

		for (Stock stock : stocksSubList) {
		    yahoo_url_builder.append(stock.colsMap.get(Col.symbol));
		    yahoo_url_builder.append(x.yahooSuffix);
		    yahoo_url_builder.append("+");
		}
		yahoo_url_builder.append("&f=");
		yahoo_url_builder.append(dummy_yahoo.getURL_tags());

		searchAndBuy(EDx.AMEX);	//!!! REMOVE THIS

		myPrintln("trying to fetch " + stocksSubList.toString());
		myPrintln("at  " + yahoo_url_builder.toString());

		InputStream myInputStream = new URL(yahoo_url_builder.toString()).openStream();
		String yahoo_results_line;
		BufferedReader br = new BufferedReader(new InputStreamReader(myInputStream));
		while ((yahoo_results_line = br.readLine()) != null) {
		    MyVariablesHolder yahoo = new YahooDataPackage(yahoo_results_line);

		    if (yahoo.hasData()) {
			goodYahooCounter++;
//			System.out.println(goodYahooCounter + " of " + x.stocks.size());
		    }
		    for (Stock stock : stocksSubList) {

			if (stock.colsMap.get(Col.symbol).equals(yahoo.colsMap.get(Col.symbol))) {
			    stock.store_yahoo_data((YahooDataPackage)yahoo);
			    break;
			}
		    }
		}

		long time_passed = System.currentTimeMillis() - yahoo_timer_start_time;	    //check how much time has passed.  wait until it's 1000 ms total.
		long time_left_to_wait = Math.max(1000 - time_passed, 0);
		Thread.sleep(time_left_to_wait + 50);     //+ 50 just to be safe
		yahoo_timer_start_time = System.currentTimeMillis();
	    }

	    ArrayList<String> symbolsInfoStrs = new ArrayList();
	    symbolsInfoStrs.add(dstock.format_header());
	    for (Stock stock : x.stocks) {
		symbolsInfoStrs.add(stock.format_csv());
	    }

	    Files.write(x.stocks_file.toPath(), symbolsInfoStrs, StandardCharsets.UTF_8);

	    myPrintln(("     finished fetchYahooFinanceData, got data for  " + goodYahooCounter + " of " + x.stocks.size() + "stocks."));

	}

    }

    public class Stock extends MyVariablesHolder {

	private TopRow handler;

	@Override
	public String toString() {
	    return (String)this.colsMap.get(Col.symbol);
	}


	public Stock() {
	    super(new YahooDataPackage());
	}

	public Stock(String fileLine, EDx x) throws ParseException {
	    super(fileLine, x);
	}

	public Stock(String symbol, String exchange, String currency, String secType) {
	    this(symbol, exchange, null, currency, secType);
	}

	private Stock(NewContract nc) {
	    this(nc.symbol(), nc.exchange(), nc.primaryExch(), nc.currency(), nc.secType().toString());
	}

	public Stock(String symbol, String exchange, String primaryExchange, String currency, String secType) {
	    super(new YahooDataPackage());
	    this.colsMap.put(Col.symbol, symbol);
	    this.colsMap.put(Col.exch_IB, exchange);
	    this.colsMap.put(Col.exch_prIB, primaryExchange);
	    this.colsMap.put(Col.cur, currency);
	    this.colsMap.put(Col.secType, secType);
	}

	public Stock(String eoddataLine, String eoddatasuffix) {	  //this.secType = "STK";
	    super(new YahooDataPackage());
	    String delimiter = "\t";
	    switch (eoddatasuffix) {
		case "NYSE":
		    this.colsMap.put(Col.exch_IB, "SMART");
		    this.colsMap.put(Col.exch_prIB, "NYSE");
		    this.colsMap.put(Col.cur, "USD");
		    delimiter = "[\\t| ]";                      //tab or space
		    break;
		case "NASDAQ":
		    this.colsMap.put(Col.exch_IB, "SMART");
		    this.colsMap.put(Col.exch_prIB, "NASDAQ");
		    this.colsMap.put(Col.cur, "USD");
		    delimiter = "[\\t| ]";                      //tab or space
		    break;
		case "AMEX":
		    this.colsMap.put(Col.exch_IB, "SMART");
		    this.colsMap.put(Col.exch_prIB, "SMART");
		    this.colsMap.put(Col.cur, "USD");
		    delimiter = "[\\t| ]";                      //tab or space
		    break;
		case "OTCBB":
		    this.colsMap.put(Col.exch_IB, "SMART");
		    this.colsMap.put(Col.exch_prIB, "SMART");
		    this.colsMap.put(Col.cur, "USD");
		    delimiter = "\\.OB";
		    break;
		case "LSE":
		    this.colsMap.put(Col.exch_IB, "SMART");
		    this.colsMap.put(Col.exch_prIB, "LSE");
		    this.colsMap.put(Col.cur, "GBP");
		    delimiter = "\\.L";
		    break;
		case "ASX":
		    this.colsMap.put(Col.exch_IB, "SMART");
		    this.colsMap.put(Col.exch_prIB, "ASX");
		    this.colsMap.put(Col.cur, "AUD");
		    delimiter = "\\.AX";
		    break;
		case "HKEX":
		    this.colsMap.put(Col.exch_IB, "SEHK");
		    this.colsMap.put(Col.exch_prIB, "SEHK");
		    this.colsMap.put(Col.cur, "HKD");
		    delimiter = "\\.HK";
		    break;
	    }
	    String[] lineAr = eoddataLine.split(delimiter);
	    this.colsMap.put(Col.symbol, lineAr[0]);
	    this.colsMap.put(Col.x_ED, EDx.getX(eoddatasuffix));		     //this.x = EDx.getX(eoddatasuffix);
	    this.colsMap.put(Col.secType, "STK");
	}


	public void buybuybuy(int numShares) throws InterruptedException {
	    myPrint("b ");
	    makeOrder(numShares, Action.BUY);
	}

	public void sellsellsell(int numShares) throws InterruptedException {
	    myPrint("s ");
	    makeOrder(numShares, Action.SELL);
	}

	public void makeOrder(int numShares, Types.Action action) throws InterruptedException {

	    //throttles to < 50 / sec

	    if (orders_count % 40 == 0) {
		long time_passed = System.currentTimeMillis() - orders_timer_start_time;	    //check how much time has passed.  wait until it's 1000 ms total.
		long time_left_to_wait = Math.max(1000 - time_passed, 0);
		Thread.sleep(time_left_to_wait + 50);     //+ 50 just to be safe
		orders_timer_start_time = System.currentTimeMillis();
	    }
	    if (actuallyPlaceOrders) {
		//OPG == market on open?  moo
		NewOrder no = new NewOrder();
		no.account("DU178431");                  //Paper trader: DU178431 // old demo account #: DU15182
		no.action(action);
		no.totalQuantity(numShares);
		no.orderType(OrderType.MKT);
		no.tif(Types.TimeInForce.DAY);

		System.out.println(NewContractToString(newContract()) + " shares: "
			+ numShares + ", action: " + action);

		ApiDemo.INSTANCE.controller().placeOrModifyOrder(newContract(), no, null);
		orders_count++;
	    }
	}

	void requestQuote() throws InterruptedException {
	    handler = new TopRow(new TopModel(), newContract().description());
	    ApiDemo.INSTANCE.controller().reqTopMktData(newContract(), "", true, handler);
	}


	private void printHandler() {
	    myPrintln(colsMap.get(Col.symbol) + " " + handler.m_bid + " " + handler.m_ask + " " + handler.m_last + " " + handler.m_volume);
	}

	private String toStringFull() {
	    return handler.m_description + ", " + handler.m_ask + ", " + handler.m_askSize
		    + ", " + handler.m_bid + ", " + handler.m_bidSize + ", " + handler.m_close + ", "
		    + handler.m_last + ", " + handler.m_lastTime + ", " + handler.m_volume + ", " + handler.change();
	}

	private void store_yahoo_data(YahooDataPackage yahoo) {

	    for (Col col : Col.values()) {
		if (col.homeClass.equals(yahoo.getClass())) {
		    this.colsMap.put(col, yahoo.colsMap.get(col));
		}
	    }

	}

	private void updateHandlerData() {
	    colsMap.put(Col.oldBid, handler.m_bid);
	    colsMap.put(Col.oldAsk, handler.m_ask);

	    try {
		colsMap.put(Col.oldSpread, pctChange(colsMap.get(Col.oldBid), colsMap.get(Col.oldAsk)));
	    } catch (Exception e) {
	    }

	}

//	private void fetchYahooWebpageStuff() throws XPatherException, IOException {
//
//	    /*
//	    
//	     look at:
//	     company events (see if event since prev buz day (inclusive) (notice downgrade/upgrade)
//		
//	     y_web_hasNewEvent
//	     y_web_upgrade
//	     y_web_downgrade
//	    
//	     key statistics (for debt/equity AND totay cash per share?)
//	    
//	     y_web_profitMargin
//	     y_web_returnOnAssets
//	     y_web_returnOnEquity
//	     y_web_DE
//	     y_web_cashPerShare
//	    
//	     analyst opinion (for yahoo opinion this week and last)
//	    
//	     y_web_opinionThisWeek
//	     y_web_opinionLastWeek
//	    
//	     -----------------------------   
//	    
//	     y_web_hasNewEvent
//	     y_web_upgrade
//	     y_web_downgrade
//	     y_web_DE
//	     y_web_profitMargin
//	     y_web_returnOnAssets
//	     y_web_returnOnEquity
//	     y_web_cashPerShare
//	     y_web_opinionThisWeek
//	     y_web_opinionLastWeek
//	    
//	     //http://finance.yahoo.com/q/ce?s=GOOG+Company+Events
//	     //http://finance.yahoo.com/q/ks?s=SODA+Key+Statistics
//	     //http://finance.yahoo.com/q/ao?s=BHP.AX+Analyst+Opinion
//	     */
//
//	    /*
//	     html for most recent event date
//	     <b>Recent Events</b></th><th align="right">&nbsp;</th></tr></table><table class="yfnc_datamodoutline1" width="100%" cellpadding="0" cellspacing="0" border="0"><tr valign="top"><td><table border="0" cellpadding="3" cellspacing="1" width="100%"><tr><th scope="col" class="yfnc_tablehead1" align="left" width="1%" nowrap>Date</th><th scope="col" class="yfnc_tablehead1" align="left">Event</th></tr><tr><td class="yfnc_tabledata1" nowrap align="left">30-Jan-14</td><td class="yfnc_tabledata1">
//	     */
//
////TODO start here -- just blindly copied/pasted from eclipse sports stuff
//	    
//	    HtmlCleaner cleaner = new HtmlCleaner();
//	    CleanerProperties props = cleaner.getProperties();
//	    props.setAllowHtmlInsideAttributes(true);
//	    props.setAllowMultiWordAttributes(true);
//	    props.setRecognizeUnicodeChars(true);
//	    props.setOmitComments(true);
//
//	    URL url = new URL("");
//	    URLConnection con = url.openConnection();
//	    con.setConnectTimeout(20_000);
//	    con.setReadTimeout(20_000);
//
//	    //TODO what happens when it hits a read time out?  does it try it again?  or just lose the data....
//	    InputStream in1 = con.getInputStream();
//
//	    String source = IOUtils.toString(in1);
//	    IOUtils.closeQuietly(in1);
//	    InputStreamReader isr = new InputStreamReader(IOUtils.toInputStream(source));
//	    TagNode node = cleaner.clean(isr);
//
//	    Object[] table_array = node.evaluateXPath("//*[@id='innercontent']/table/tbody");
//
//	    if (table_array.length > 0) {
//		TagNode table = (TagNode)table_array[0];
//
//		Object[] leagueNameCells = table.evaluateXPath("//tr[position() > 1]/td[1]");
//		Object[] teamNameCells = table.evaluateXPath("//tr[position() > 1]/td[2]");
//		Object[] teamScoreCells = table.evaluateXPath("tr[position() > 1]/td[3]");
//		Object[] betPickCells = table.evaluateXPath("tr[position() > 1]/td[4]");
//		Object[] betResultCells = table.evaluateXPath("tr[position() > 1]/td[6]");
//		Object[] unitsCells = table.evaluateXPath("tr[position() > 1]/td[7]");
//
//		if (leagueNameCells.length == 0) {
//		    System.out.format("%nempty table %s", bets_url);
//		    throw new Exception("Empty table");
//		}
//
//		for (int j = 0; j < teamNameCells.length; j += 2) {
//
//		    String tm1 = "", tm2 = "", tm1Pts = "", tm2Pts = "", betCell1 = "", betCell2 = "", resultCell1 = "", resultCell2 = "", unitsCell1 = "", unitsCell2 = "";
//		    String lineATS = "", lineOU = "", betATS = "", betOU = "", didWinATS = "", didWinOU = "", unitsATS = "", unitsOU = "";
//
//		    dateLeague = ((TagNode)leagueNameCells[j]).getText().toString().trim();
//
//
//
//		}
//
//
//	    }
//
//
//
//	}


    }

    public class OrderHistory extends MyVariablesHolder {

	//searchAndBuy ONLY(?) uses this.  confirmPurhcases just updates the existing order history
	public OrderHistory(Stock stock, Strategy strat, Side side, int holdDays, int tryBuyShares, Date tryBuyTime) {
	    super(stock);
//	    this.child = stock;

	    this.colsMap.put(Col.strat, strat);
	    this.colsMap.put(Col.side, side);
	    this.colsMap.put(Col.holdDays, holdDays);
	    this.colsMap.put(Col.tryBuyTime, tryBuyTime);
	    this.colsMap.put(Col.tryBuyShares, tryBuyShares);
	    this.colsMap.put(Col.tryBuyBid, stock.handler.m_bid);
	    this.colsMap.put(Col.tryBuyAsk, stock.handler.m_ask);
	    try {
		this.colsMap.put(Col.tryBuySpread, pctChange(stock.handler.m_bid, stock.handler.m_ask));
	    } catch (Exception e) {
		this.colsMap.put(Col.tryBuySpread, null);
	    }
	    try {
		this.colsMap.put(Col.tryBuyChange, pctChange(stock.colsMap.get(Col.oldBid), stock.handler.m_ask));
	    } catch (Exception e) {
		this.colsMap.put(Col.tryBuyChange, null);
	    }
	}

	public OrderHistory() {
	    super(new Stock());
	}

	public OrderHistory(String fileLine) throws ParseException {
	    super(fileLine, EDx.AMEX);					//!! remove EDx once stocks files are updated
	}

	private void recordPurchase(TradesPanelRow tradesRow) {
	    this.colsMap.put(Col.buyTime, tradesRow.date);
	    this.colsMap.put(Col.buyShares, tradesRow.shares);
	    this.colsMap.put(Col.buyPrice, tradesRow.price);
	    this.colsMap.put(Col.buyCommission, tradesRow.commission);
	    try {
		this.colsMap.put(Col.buyChange, pctChange(colsMap.get(Col.oldBid), colsMap.get(Col.buyPrice)));
	    } catch (Exception e) {
		this.colsMap.put(Col.buyChange, null);
	    }
	}

	private void recordSaleAttempt(Date trySellTime, int trySellShares) {
	    this.colsMap.put(Col.side, Side.SLD);
	    this.colsMap.put(Col.trySellTime, trySellTime);
	    this.colsMap.put(Col.trySellShares, trySellShares);
	}

	private void recordSale(TradesPanelRow tradesRow) {
	    this.colsMap.put(Col.side, Side.SLD);
	    this.colsMap.put(Col.sellTime, tradesRow.date);
	    this.colsMap.put(Col.sellShares, tradesRow.shares);
	    this.colsMap.put(Col.sellPrice, tradesRow.price);
	    this.colsMap.put(Col.sellCommission, tradesRow.commission);
	    try {
		this.colsMap.put(Col.sellChange, pctChange(colsMap.get(Col.buyPrice), colsMap.get(Col.sellPrice)));
	    } catch (Exception e) {
		this.colsMap.put(Col.buyChange, null);
	    }
	}


	/** bought but unsold and past sell date*/
	private boolean isLeftover() throws ParseException {

//	    System.out.println(colsMap.get(Col.x_ED));
//	    System.out.println(EDx.valueOf(colsMap.get(Col.x_ED).toString()));
	    return colsMap.get(Col.buyShares) != null
		    && colsMap.get(Col.sellShares) == null
		    && MyDate.xNowDay(EDx.valueOf(colsMap.get(Col.x_ED).toString())).
		    //TODO start here! prev line throws exception cos x is null
		    after(MyDate.xDay((EDx)colsMap.get(Col.x_ED), super.supposed_to_sell_Date()));
	}

	private boolean has_shares_that_should_be_sold_today() throws ParseException {
	    return DateUtils.isSameDay(super.supposed_to_sell_Date(), new Date(System.currentTimeMillis()))
		    && this.colsMap.get(Col.buyShares) != null
		    && this.colsMap.get(Col.sellShares) == null;
	}


	private boolean was_sold() {
	    return (colsMap.get(Col.sellPrice) != null && (Double)colsMap.get(Col.sellPrice) > 0);
	}

	private boolean wasnt_purchased() {
	    return (colsMap.get(Col.buyTime) == null);
	}

	private int days_past_sellDate() throws ParseException, Exception {

	    return MyDate.days_from_first_to_second(
		    super.supposed_to_sell_Date(), new Date(System.currentTimeMillis()));

	}

	private void sell_positions_whose_buy_change_was_not_low_enough(String currency) throws InterruptedException {
	    myPrintln("       sell_positions_whose_buy_change_was_not_low_enough -0.01");
	    orders_count = 0;
	    orders_timer_start_time = System.currentTimeMillis();
	    List<PortfolioModelRow> portfolioRows = readPortfolioModel();

	    for (EDx x : EDx.values()) {
		if (x.currency.equals(currency)) {


		    for (PortfolioModelRow row : portfolioRows) {

			for (OrderHistory oh : x.orderHistories) {
			    if (row.nc.currency().equals(currency) && row.nc.symbol().equals(oh.colsMap.get(Col.symbol))) {

				if ((Double)oh.colsMap.get(Col.buyChange) > -0.01) {

				    if ((row.shares) > 0) {

					System.out.println(row.string());

					if (!row.nc.exchange().equals("SEHK"))
					    row.nc.m_exchange = "SMART";			    //!! won't work for sehk?

					(new Stock(row.nc)).sellsellsell(row.shares);
					oh.recordSaleAttempt((new Date(System.currentTimeMillis())), row.shares);
				    }
//                                    if ((row.shares) < 0) {
//                                        (new Stock(row.nc)).buybuybuy(row.shares);
//                                    }
				}
				break;

			    }

			}
		    }
		}
	    }

	    myPrintln("       finished, attempted to close" + orders_count);

	}

	private void delete_orderHistories_whose_buy_change_was_not_low_enough(EDx x) throws InterruptedException, IOException, ParseException {

	    myPrintln("     start purge_orderHistories_whose_buy_change_was_not_low_enough " + x.name());

	    doh.readOrderHistories(x);

	    int originalSize = x.orderHistories.size();

	    List<OrderHistory> histoires_to_remove = new ArrayList();

	    for (OrderHistory oh : x.orderHistories) {
		if ((Double)oh.colsMap.get(Col.buyChange) > -0.001)
		    histoires_to_remove.add(oh);
	    }
	    x.orderHistories.removeAll(histoires_to_remove);

	    int smallerSize = x.orderHistories.size();

	    myPrintln("removed " + (originalSize - smallerSize) + " order histories");
	    doh.writeOrderHistories(x);
	    doh.readOrderHistories(x);

	    myPrintln("     purge_orderHistories_whose_buy_change_was_not_low_enough delete_unowned_orderHistory_entries " + x.name());
	}

	private void erase_sell_try_data_from_OH(EDx x) throws IOException, ParseException, Exception {
	    myPrintln("     erase_sell_try_data_from_OH " + x.name());

	    doh.readOrderHistories(x);

	    for (OrderHistory oh : x.orderHistories) {
		oh.colsMap.put(Col.trySellShares, null);
		oh.colsMap.put(Col.trySellTime, null);
	    }
	    doh.writeOrderHistories(x);
	    doh.readOrderHistories(x);
	    myPrintln("     finished erase_sell_try_data_from_OH " + x.name());

	}

	private void delete_unowned_orderHistory_entries(EDx x) throws IOException, ParseException, Exception {
	    myPrintln("     start delete_unowned_orderHistory_entries " + x.name());

	    doh.readOrderHistories(x);


	    List<PortfolioModelRow> portfolioRows = readPortfolioModel();


	    int originalSize = x.orderHistories.size();

	    List<OrderHistory> histoires_to_remove = new ArrayList();

	    for (OrderHistory oh : x.orderHistories) {

		//shares owned

		int sharesOwned = 0;
		for (PortfolioModelRow row : portfolioRows) {
		    if (row.nc.symbol().equals(oh.colsMap.get(Col.symbol)) && row.nc.currency().equals(oh.colsMap.get(Col.cur)))
			sharesOwned = row.shares;
		}

		if (sharesOwned == 0) {
		    histoires_to_remove.add(oh);
		}
	    }

	    x.orderHistories.removeAll(histoires_to_remove);

	    int smallerSize = x.orderHistories.size();

	    myPrintln("removed " + (originalSize - smallerSize) + " order histories");
	    doh.writeOrderHistories(x);
	    doh.readOrderHistories(x);

	    myPrintln("     finished delete_unowned_orderHistory_entries " + x.name());


	}

	private void delete_sold_and_unpurchased_and_old_unsold_orders(EDx x) throws IOException, ParseException, Exception {

	    //i rarely want to do this.  if an order was never sold, maybe it was never really bought.  or maybe sold early.
	    //      in that case, i want to re-buy it so when i do sell it, i can record the sell price.  


	    myPrintln("     start delete_sold_and_unpurchased_and_old_unsold_orders " + x.name());

	    doh.readOrderHistories(x);

	    int originalSize = x.orderHistories.size();
	    myPrintln(originalSize);

	    List<OrderHistory> histoires_to_remove = new ArrayList();

	    for (OrderHistory oh : x.orderHistories) {
//		System.out.println("0. "+ oh.);
//		System.out.println("1. oh.wasnt_purchased() " + oh.wasnt_purchased());
//		System.out.println("2. oh.was_sold() " + oh.was_sold());
//		System.out.println("3. oh.isLeftover() " + oh.isLeftover());
//		System.out.println("4. oh.oh.days_past_sellDate() > 31() " + (oh.days_past_sellDate() > 31));

		//todo start here, not working.  
		/*
		
		 finished erase_sell_try_data_from_OH LSE
		 start remove_unpurchased_OrderHistories LSE
		 read 1012 LSE order histories.
		 1. oh.wasnt_purchased() true
		 2. oh.was_sold() false
		 java.lang.NullPointerException
		 at apidemo.ApiDemo$MyDate.<init>(ApiDemo.java:1651)
		 at apidemo.ApiDemo$MyVariablesHolder.supposed_to_sell_Date(ApiDemo.java:1951)
		 at apidemo.ApiDemo$MyVariablesHolder.access$2800(ApiDemo.java:1830)
		 at apidemo.ApiDemo$OrderHistory.isLeftover(ApiDemo.java:2347)
		 at apidemo.ApiDemo$OrderHistory.purge_sold_and_unpurchased_and_old_unsold_orders(ApiDemo.java:2512)
		 at apidemo.ApiDemo$OrderHistory.access$500(ApiDemo.java:2252)
		 at apidemo.ApiDemo.masterLooper(ApiDemo.java:210)
		 at apidemo.ApiDemo.run(ApiDemo.java:146)
		 at apidemo.ApiDemo.main(ApiDemo.java:2827)
		 BUILD SUCCESSFUL (total time: 7 seconds)

		 */

		if (oh.wasnt_purchased() || oh.was_sold() || (oh.isLeftover() && oh.days_past_sellDate() > 31)) {
		    histoires_to_remove.add(oh);
		}
	    }
	    x.orderHistories.removeAll(histoires_to_remove);

	    int smallerSize = x.orderHistories.size();

	    myPrintln("removed " + (originalSize - smallerSize) + " order histories");
	    doh.writeOrderHistories(x);
	    doh.readOrderHistories(x);

	    myPrintln("     finished delete_sold_and_unpurchased_and_old_unsold_orders " + x.name());

	}

	public void readOrderHistories(EDx x) throws IOException, ParseException {
	    x.orderHistories = new ArrayList();
//        if (!x.orderHistoriesFile.exists()) x.orderHistoriesFile.c
	    List<String> lines = x.orderHistoriesFile.exists()
		    ? Files.readAllLines(x.orderHistoriesFile.toPath(), StandardCharsets.UTF_8)
		    : new ArrayList();

//	    x.orderHistoriesFile.delete();

	    if (lines != null && !lines.isEmpty()) {
		String ohHeader = lines.get(0);
		Col.set_column_positions(ohHeader);

//		//todo delete.  for testing:
//		for (Col col : Col.values()) {		//!!
//		    System.out.format("%45s %4d%n", col, col.ci);
//		}


		lines.remove(0);                                                       //skip header											//skip header
		for (String line : lines) {
		    OrderHistory loadedOrderHistory = new OrderHistory(line);

		    x.orderHistories.add(loadedOrderHistory);
		}
	    }
	    myPrintln("read " + x.orderHistories.size() + " " + x.name() + " order histories.");
	}

	public void writeOrderHistories(EDx x) throws IOException {
	    x.orderHistoriesWriter = new PrintWriter(new BufferedWriter(new FileWriter(x.orderHistoriesFile)));     //deletes file before writing
	    write_and_flush(x.orderHistoriesWriter, doh.format_header());
	    int orderHistorySize = x.orderHistories.size();
	    for (OrderHistory oh : x.orderHistories)
		write_and_flush(x.orderHistoriesWriter, oh.format_csv());
	    x.orderHistoriesWriter.close();
	    myPrintln("attempted to write " + orderHistorySize + " " + x.name() + " order histories.");
	}


	private void close_undocumented_positions(EDx edx) throws InterruptedException, ParseException, IOException {
	    //close positions whose shares aren't listed in OrderHistories
	    myPrintln("     close undocumented");
	    String currency = edx.currency;
	    myPrintln("currency: " + currency);

	    cancel_all_orders();
	    //TODO improve this so it sells stocks in portfolio AND an order hisotry if i have too many shares (unlikely so do this later)
	    List<PortfolioModelRow> rows = readPortfolioModel();

	    orders_count = 0;
	    orders_timer_start_time = System.currentTimeMillis();

	    for (EDx x : EDx.values())
		doh.readOrderHistories(x);

	    for (PortfolioModelRow row : rows)
		if (row.shares != 0 && row.nc.currency().equals(currency)) {

		    boolean notFound = true;

		    for (EDx x : EDx.values())
			for (OrderHistory oh : x.orderHistories)
			    if (row.nc.symbol().equals(oh.colsMap.get(Col.symbol))) //this might not be sufficient if expand to markets where different securities can have the same currency and the same symbol
				notFound = false;

		    if (notFound) {


			if (!row.nc.exchange().equals("SEHK"))
			    row.nc.m_exchange = "SMART";			    //!! won't work for sehk?
			if (row.shares > 0)
			    (new Stock(row.nc)).sellsellsell(row.shares);
			if (row.shares < 0)
			    (new Stock(row.nc)).buybuybuy(-1 * row.shares);
		    }
		}

	    myPrintln("curErr=" + curErr);
	    if (curErr == 504) System.exit(0);    //! 
	    myPrintln("finished sell undocumented, tried to close " + orders_count);
	}

	private void close_all_positions_for_exchange_currency_silently(EDx edx) throws InterruptedException, ParseException, IOException {
	    myPrintln("     close all positions " + edx);
	    String currency = edx.currency;
	    myPrintln("currency: " + currency);

	    cancel_all_orders();
	    //TODO improve this so it sells stocks in portfolio AND an order hisotry if i have too many shares (unlikely so do this later)
	    List<PortfolioModelRow> rows = readPortfolioModel();

	    orders_count = 0;
	    orders_timer_start_time = System.currentTimeMillis();

	    for (EDx x : EDx.values())
		doh.readOrderHistories(x);

	    for (PortfolioModelRow row : rows) {
		if (row.shares != 0 && row.nc.currency().equals(currency)) {

		    if (!row.nc.exchange().equals("SEHK"))
			row.nc.m_exchange = "SMART";			    //!! won't work for sehk?

		    if (row.shares > 0)
			(new Stock(row.nc)).sellsellsell(row.shares);
		    else if (row.shares < 0)
			(new Stock(row.nc)).buybuybuy(-1 * row.shares);
		}
	    }

	    myPrintln("curErr=" + curErr);
	    if (curErr == 504) System.exit(0);    //! 
	    myPrintln("    finished close all, tried to close " + orders_count);
	}

	private void buy_unowned_but_supposedly_purchased_orderHistory_entries(String currency) throws InterruptedException, ParseException, IOException, Exception {
	    //must read/write orderhistories external to calling this.  in the calling method

//make list of all stocks in the order history.  add up shares.  see how many shares exist in portfolio.
	    //purchase or sell the difference

	    myPrintln("     buy_unowned_but_supposedly_purchased_orderHistory_entries");


	    List<PortfolioModelRow> portfolioRows = readPortfolioModel();

	    orders_count = 0;
	    orders_timer_start_time = System.currentTimeMillis();

	    for (EDx x : EDx.values()) {
		if (x.currency.equals(currency)) {

		    delete_sold_and_unpurchased_and_old_unsold_orders(x);
		    erase_sell_try_data_from_OH(x);

		    Set<String> symbols = new HashSet();

		    for (OrderHistory oh : x.orderHistories) {
			symbols.add((String)oh.colsMap.get(Col.symbol));
		    }
		    for (String symbol : symbols) {
			NewContract symbolNC = null;
			int numShares_oh = 0;
			for (OrderHistory oh : x.orderHistories) {
			    if (oh.colsMap.get(Col.symbol).equals(symbol)) {
				numShares_oh += (Integer)oh.colsMap.get(Col.buyShares);
				symbolNC = oh.newContract();
			    }
			}
			int numShares_portfolio = 0;
			for (PortfolioModelRow row : portfolioRows) {
			    if (row.nc.symbol().equals(symbol) && row.nc.currency().equals(currency)) {
				numShares_portfolio += row.shares;
			    }
			}
			if (numShares_oh != numShares_portfolio) {
			    if (numShares_oh - numShares_portfolio > 0)
				(new Stock(symbolNC)).buybuybuy(numShares_oh - numShares_portfolio);
			    if (numShares_portfolio - numShares_oh < 0)
				(new Stock(symbolNC)).sellsellsell(numShares_portfolio - numShares_oh);
			}


		    }
		}
	    }

	    myPrintln("     finished buy_unowned_but_supposedly_purchased_orderHistory_entries " + currency + "stocks, tried to re-purchase " + orders_count);
	}
    }

    public class CompletedSale extends MyVariablesHolder {
	//TODO start here start here
	//check out current completedSale file's header.  if different from the header we want to write,
	//then make a new file.


	public CompletedSale() {
	    super(new OrderHistory());
	}

	public CompletedSale(OrderHistory oh) throws Exception {
	    super(oh);

	    this.colsMap.put(Col.totalCommission,
		    ((Double)colsMap.get(Col.buyCommission)) + ((Double)colsMap.get(Col.sellCommission)));
	    this.colsMap.put(Col.PnL,
		    ((Integer)colsMap.get(Col.sellShares))
		    * ((Double)colsMap.get(Col.sellPrice) - ((Double)colsMap.get(Col.buyPrice)))
		    - (Double)colsMap.get(Col.totalCommission));

	    try {
		//this needs to account for time zone!
		this.colsMap.put(Col.heldDays,
			MyDate.days_from_first_to_second(
				MyDate.xDate(EDx.valueOf(colsMap.get(Col.x_ED).toString()), (Date)colsMap.get(Col.buyTime)),
				MyDate.xDate(EDx.valueOf(colsMap.get(Col.x_ED).toString()), (Date)colsMap.get(Col.sellTime))
			)
		);
	    } catch (Exception e) {
//		e.printStackTrace();
	    }

	    try {
		this.colsMap.put(Col.rightBuyTime,
			MyDate.timeOnlyDate((Date)colsMap.get(Col.buyTime))
			.inInterval(
				EDx.valueOf(colsMap.get(Col.x_ED).toString()).open,
				0,
				30
			)
		);
	    } catch (Exception e) {
//		e.printStackTrace();
	    }
	    try {
		this.colsMap.put(Col.rightSellTime,
			MyDate.timeOnlyDate((Date)colsMap.get(Col.sellTime))
			.inInterval(
				EDx.valueOf(colsMap.get(Col.x_ED).toString()).close,
				-30,
				0
			)
		);
	    } catch (Exception e) {
//		e.printStackTrace();
	    }

	    try {
		this.colsMap.put(Col.rightSellDay,
			DateUtils.isSameDay(super.supposed_to_sell_Date(), (Date)colsMap.get(Col.sellTime))
		);
	    } catch (Exception e) {
//		e.printStackTrace();
	    }

	}

    }

    public static String NewContractToString(NewContract nc) {
	StringBuilder sb = new StringBuilder();
	sb.append(nc.symbol());
	sb.append(c);
	sb.append(nc.secType().toString());         //does this look right? "STK"?
	sb.append(c);
	sb.append(nc.exchange());
	sb.append(c);
	sb.append(nc.primaryExch());
	sb.append(c);
	sb.append(nc.currency());
	return sb.toString();
    }

    public static String timeStamp(Object text) {
	return bigDateAMPMFormat.format((new Date(System.currentTimeMillis()))) + ">  " + text.toString();
    }

    public static String timeStamp() {
	return bigDateAMPMFormat.format((new Date(System.currentTimeMillis()))) + ">  ";
    }

    public static void myPrint(Object s) {
	System.out.print(s);
	logWriter.print(s);
	logWriter.flush();
    }

    public static void myPrintln(Object s) {
	System.out.println(s);
	logWriter.println(timeStamp(s));
	logWriter.flush();
    }

    public static void myErrPrintln(Exception e) {
	logWriter.print(timeStamp());
	e.printStackTrace(logWriter);
	logWriter.flush();
	e.printStackTrace(System.err);
    }

    public static void write_and_flush(PrintWriter pw, Object text) {
	pw.println(text.toString());
	pw.flush();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    static {
	NewLookAndFeel.register();
    }
    static ApiDemo INSTANCE = new ApiDemo();

    private final JTextArea m_inLog = new JTextArea();
    private final JTextArea m_outLog = new JTextArea();
    private final Logger m_inLogger = new Logger(m_inLog);
    private final Logger m_outLogger = new Logger(m_outLog);
    private final ApiController m_controller = new ApiController(this, m_inLogger, m_outLogger);
    private final ArrayList<String> m_acctList = new ArrayList();
    private final JFrame m_frame = new JFrame();
    private final NewTabbedPanel m_tabbedPanel = new NewTabbedPanel(true);
    private final ConnectionPanel m_connectionPanel = new ConnectionPanel();
    private final MarketDataPanel m_mktDataPanel = new MarketDataPanel();
    private final ContractInfoPanel m_contractInfoPanel = new ContractInfoPanel();
    private final TradingPanel m_tradingPanel = new TradingPanel();
    private final AccountInfoPanel m_acctInfoPanel = new AccountInfoPanel();
    private final OptionsPanel m_optionsPanel = new OptionsPanel();
    private final AdvisorPanel m_advisorPanel = new AdvisorPanel();
    private final ComboPanel m_comboPanel = new ComboPanel();
    private final StratPanel m_stratPanel = new StratPanel();
    private final JTextArea m_msg = new JTextArea();

    // getter methods
    public ArrayList<String> accountList() {
	return m_acctList;
    }

    public ApiController controller() {
	return m_controller;
    }

    public JFrame frame() {
	return m_frame;
    }

    public static void makeNewInstance() {

	INSTANCE = new ApiDemo();
    }

    public static void main(String[] args) throws InterruptedException, ParseException, IOException {// throws InterruptedException, IOException, Exception {
	try {
	    INSTANCE.run();
	} catch (Exception e) {
	    myErrPrintln(e);
	    System.exit(0);
	}
    }



    public void makeGui() {

	m_tabbedPanel.addTab("Connection", m_connectionPanel);
	m_tabbedPanel.addTab("Market Data", m_mktDataPanel);
	m_tabbedPanel.addTab("Trading", m_tradingPanel);
	m_tabbedPanel.addTab("Account Info", m_acctInfoPanel);
	m_tabbedPanel.addTab("Options", m_optionsPanel);
	m_tabbedPanel.addTab("Combos", m_comboPanel);
	m_tabbedPanel.addTab("Contract Info", m_contractInfoPanel);
	m_tabbedPanel.addTab("Advisor", m_advisorPanel);
	// m_tabbedPanel.addTab( "Strategy", m_stratPanel); in progress

	m_msg.setEditable(false);
	m_msg.setLineWrap(true);
	JScrollPane msgScroll = new JScrollPane(m_msg);
	msgScroll.setPreferredSize(new Dimension(10000, 120));

	JScrollPane outLogScroll = new JScrollPane(m_outLog);
	outLogScroll.setPreferredSize(new Dimension(10000, 120));

	JScrollPane inLogScroll = new JScrollPane(m_inLog);
	inLogScroll.setPreferredSize(new Dimension(10000, 120));

	NewTabbedPanel bot = new NewTabbedPanel();
	bot.addTab("Messages", msgScroll);
	bot.addTab("Log (out)", outLogScroll);
	bot.addTab("Log (in)", inLogScroll);

	m_frame.add(m_tabbedPanel);
	m_frame.add(bot, BorderLayout.SOUTH);
	m_frame.setSize(600, 768);              //was 1024 SCR
	m_frame.setVisible(true);
	m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    @Override
    public void connected() {
	show("connected");
	m_connectionPanel.m_status.setText("connected");

	m_controller.reqCurrentTime(new ITimeHandler() {
	    @Override
	    public void currentTime(long time) {
		show("Server date/time is " + Formats.fmtDate(time * 1000));
	    }
	});

	m_controller.reqBulletins(true, new IBulletinHandler() {
	    @Override
	    public void bulletin(int msgId, NewsType newsType, String message, String exchange) {
		String str = String.format("Received bulletin:  type=%s  exchange=%s", newsType, exchange);
		show(str);
		show(message);
	    }
	});
    }

    @Override
    public void disconnected() {
	show("disconnected");
	m_connectionPanel.m_status.setText("disconnected");
    }

    @Override
    public void accountList(ArrayList<String> list) {
	show("Received account list");
	m_acctList.clear();
	m_acctList.addAll(list);
    }

    @Override
    public void show(final String str) {
	myPrintln(": " + str);               //SCR!
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		m_msg.append(str);
		m_msg.append("\n\n");

		Dimension d = m_msg.getSize();
		m_msg.scrollRectToVisible(new Rectangle(0, d.height, 1, 1));
	    }
	});
    }

    @Override
    public void error(Exception e) {
	show(e.toString());
    }

    @Override
    public void message(int id, int errorCode, String errorMsg) {
	show(id + " " + errorCode + " " + errorMsg);





    }

    private class ConnectionPanel extends JPanel {

	private final JTextField m_host = new JTextField(7);
	private final JTextField m_port = new JTextField("7496", 7);
	private final JTextField m_clientId = new JTextField("0", 7);
	private final JLabel m_status = new JLabel("Disconnected");

	public ConnectionPanel() {
	    HtmlButton connect = new HtmlButton("Connect") {
		@Override
		public void actionPerformed() {
		    onConnect();
		}
	    };

	    HtmlButton disconnect = new HtmlButton("Disconnect") {
		@Override
		public void actionPerformed() {
		    m_controller.disconnect();
		}
	    };

	    JPanel p1 = new VerticalPanel();
	    p1.add("Host", m_host);
	    p1.add("Port", m_port);
	    p1.add("Client ID", m_clientId);

	    JPanel p2 = new VerticalPanel();
	    p2.add(connect);
	    p2.add(disconnect);
	    p2.add(Box.createVerticalStrut(20));

	    JPanel p3 = new VerticalPanel();
	    p3.setBorder(new EmptyBorder(20, 0, 0, 0));
	    p3.add("Connection status: ", m_status);

	    JPanel p4 = new JPanel(new BorderLayout());
	    p4.add(p1, BorderLayout.WEST);
	    p4.add(p2);
	    p4.add(p3, BorderLayout.SOUTH);

	    setLayout(new BorderLayout());
	    add(p4, BorderLayout.NORTH);
	}

	protected void onConnect() {
	    int port = Integer.parseInt(m_port.getText());
	    int clientId = Integer.parseInt(m_clientId.getText());
	    m_controller.connect(m_host.getText(), port, clientId);
	}
    }

    private static class Logger implements ILogger {

	final private JTextArea m_area;

	Logger(JTextArea area) {
	    m_area = area;
	}

	@Override
	public void log(final String str) {
	    SwingUtilities.invokeLater(new Runnable() {
		@Override
		public void run() {
		}
	    });
	}
    }
}

//        Stock s = new Stock("1398", "SEHK", "HKD", "STK");
//	Stock s = new Stock("BHP", "SMART", "AUD", "STK");
//	s.requestQuote();
//	Thread.sleep(1000);
//	s.printHandler();
//	m_controller.disconnect(); //uuuuasdgsgggg 5sdf

//
//        cancel_all_orders();
//
//        confirmSales(EDx.LSE);
//
//        delete_sold_and_unpurchased_and_old_unsold_orders(EDx.LSE);
//
//
//        readOrderHistories(EDx.LSE);
//        doh.buy_unowned_orderHistory_entries("GBP");
//        writeOrderHistories(EDx.LSE);
//
//        Thread.sleep(60_000);
//
//        readOrderHistories(EDx.LSE);
//        doh.sell_positions_whose_buy_change_was_not_low_enough("GBP");
//        writeOrderHistories(EDx.LSE);
//
//        Thread.sleep(60_000);
//
//        confirmSales(EDx.LSE);
//
//        delete_sold_and_unpurchased_and_old_unsold_orders(EDx.LSE);
//
//        delete_unowned_orderHistory_entries(EDx.LSE);



//        System.exit(0);
/**
 //clean up files.  
 for (EDx x : EDx.values()) {
 doh.erase_sell_try_data_from_OH(x);
 doh.purge_sold_and_unpurchased_and_old_unsold_orders(x);
 doh.purge_orderHistories_whose_buy_change_was_not_low_enough(x);
 }*/

/*

 //            System.out.println("array length: " + lineAr.length);

 //old format has 27 columns.  new has 30 columns
 if (lineAr.length < 30) {

 this.side = Side.getSide(lineAr[0]);

 Contract contract = new Contract();
 contract.m_symbol = lineAr[1];
 contract.m_localSymbol = lineAr[2];
 contract.m_secType = lineAr[3];
 contract.m_exchange = lineAr[4];
 contract.m_primaryExch = lineAr[5];
 contract.m_currency = lineAr[6];
 this.x = EDx.getX(lineAr[7]);
 this.holdDays = Integer.parseInt(lineAr[8]);

 this.closePriceTime = StuartDate.read_my_date_string(lineAr[9]);
 this.close_bid = Double.parseDouble(lineAr[10]);

 try {
 this.tryBuyTime = StuartDate.read_my_date_string(lineAr[11]);
 this.tryBuyShares = Integer.parseInt(lineAr[12]);
 this.tryBuyAsk = Double.parseDouble(lineAr[13]);
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.buyTime = StuartDate.read_my_date_string(lineAr[14]);
 this.buyShares = Integer.parseInt(lineAr[15]);
 this.buyPrice = Double.parseDouble(lineAr[16]);
 this.buyCommission = Double.parseDouble(lineAr[17]);
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.trySellTime = StuartDate.read_my_date_string(lineAr[19]);
 this.trySellShares = Integer.parseInt(lineAr[20]);
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.sellTime = StuartDate.read_my_date_string(lineAr[22]);
 this.sellShares = Integer.parseInt(lineAr[23]);
 this.sellPrice = Double.parseDouble(lineAr[24]);
 this.sellCommission = Double.parseDouble(lineAr[25]);
 } catch (ParseException | NumberFormatException e) {
 }
 } //old format has 27 columns.  new has 30
 else if (lineAr[0].length() > 1) {

 this.side = Side.getSide(lineAr[0]);

 Contract contract = new Contract();
 contract.m_symbol = lineAr[1];
 contract.m_localSymbol = lineAr[2];
 contract.m_secType = lineAr[3];
 contract.m_exchange = lineAr[4];
 contract.m_primaryExch = lineAr[5];
 contract.m_currency = lineAr[6];
 this.nc = new NewContract(contract);

 this.x = EDx.getX(lineAr[7]);
 this.holdDays = Integer.parseInt(lineAr[8]);

 try {
 this.closePriceTime = StuartDate.read_my_date_string(lineAr[9]);
 this.close_bid = Double.parseDouble(lineAr[10]);
 this.close_ask = Double.parseDouble(lineAr[11]);
 //spread: col 12
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.tryBuyTime = StuartDate.read_my_date_string(lineAr[13]);
 this.tryBuyShares = Integer.parseInt(lineAr[14]);
 this.tryBuyBid = Double.parseDouble(lineAr[15]);
 this.tryBuyAsk = Double.parseDouble(lineAr[16]);
 //spread: col 17
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.buyTime = StuartDate.read_my_date_string(lineAr[18]);
 this.buyShares = Integer.parseInt(lineAr[19]);
 this.buyPrice = Double.parseDouble(lineAr[20]);
 this.buyCommission = Double.parseDouble(lineAr[21]);
 //price change, index 22
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.trySellTime = StuartDate.read_my_date_string(lineAr[23]);
 this.trySellShares = Integer.parseInt(lineAr[24]);
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.sellTime = StuartDate.read_my_date_string(lineAr[25]);
 this.sellShares = Integer.parseInt(lineAr[26]);
 this.sellPrice = Double.parseDouble(lineAr[27]);
 this.sellCommission = Double.parseDouble(lineAr[28]);
 //price change, index 29
 } catch (ParseException | NumberFormatException e) {
 }
 } else {      //30 or more columns and first element has length 1 (strategy)

 this.strategy = Strategy.getStrategy(lineAr[0]);
 this.side = Side.getSide(lineAr[1]);

 Contract contract = new Contract();
 contract.m_symbol = lineAr[2];
 contract.m_localSymbol = lineAr[3];
 contract.m_secType = lineAr[4];
 contract.m_exchange = lineAr[5];
 contract.m_primaryExch = lineAr[6];
 contract.m_currency = lineAr[7];
 this.nc = new NewContract(contract);

 this.x = EDx.getX(lineAr[8]);
 this.holdDays = Integer.parseInt(lineAr[9]);

 try {
 this.closePriceTime = StuartDate.read_my_date_string(lineAr[10]);
 this.close_bid = Double.parseDouble(lineAr[11]);
 this.close_ask = Double.parseDouble(lineAr[12]);
 //spread: col 13
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.tryBuyTime = StuartDate.read_my_date_string(lineAr[14]);
 this.tryBuyShares = Integer.parseInt(lineAr[15]);
 this.tryBuyBid = Double.parseDouble(lineAr[16]);
 this.tryBuyAsk = Double.parseDouble(lineAr[17]);
 //spread: col 18
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.buyTime = StuartDate.read_my_date_string(lineAr[19]);
 this.buyShares = Integer.parseInt(lineAr[20]);
 this.buyPrice = Double.parseDouble(lineAr[21]);
 this.buyCommission = Double.parseDouble(lineAr[22]);
 //price change, index 23
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.trySellTime = StuartDate.read_my_date_string(lineAr[24]);
 this.trySellShares = Integer.parseInt(lineAr[25]);
 } catch (ParseException | NumberFormatException e) {
 }
 try {
 this.sellTime = StuartDate.read_my_date_string(lineAr[26]);
 this.sellShares = Integer.parseInt(lineAr[27]);
 this.sellPrice = Double.parseDouble(lineAr[28]);
 this.sellCommission = Double.parseDouble(lineAr[29]);
 //price change, index 30
 } catch (ParseException | NumberFormatException e) {
 }
 }


 try {
 this.yahoo.symbol = lineAr[8];
 this.yahoo.Last_Trade__Price_Only_ = lineAr[9];
 this.yahoo.name = lineAr[10];
 this.yahoo.stock_exchange = lineAr[11];
 this.yahoo.Revenue = lineAr[12];
 this.yahoo.Earnings_per_Share = lineAr[13];
 this.yahoo.EPS_Estimate_Current_Year = lineAr[14];
 this.yahoo.EPS_Estimate_Next_Year = lineAr[15];
 this.yahoo.EPS_Estimate_Next_Quarter = lineAr[16];
 this.yahoo.Book_Value = lineAr[17];
 this.yahoo.EBITDA = lineAr[18];
 this.yahoo.Price_per_Sales = lineAr[19];
 this.yahoo.Price_per_Book = lineAr[20];
 this.yahoo.PE_Ratio = lineAr[21];
 this.yahoo.PE_Ratio__Realtime_ = lineAr[22];
 this.yahoo.PEG_Ratio = lineAr[23];
 this.yahoo.Price_per_EPS_Estimate_Current_Year = lineAr[24];
 this.yahoo.Price_per_EPS_Estimate_Next_Year = lineAr[25];
 this.yahoo.Short_Ratio = lineAr[26];
 this.yahoo.Percent_Change_From_200_Day_Moving_Average = lineAr[27];
 this.yahoo.Change_From_50_Day_Moving_Average = lineAr[28];
 this.yahoo.Percent_Change_From_50_Day_Moving_Average = lineAr[29];
 this.yahoo._50_Day_Moving_Average = lineAr[30];
 this.yahoo._200_Day_Moving_Average = lineAr[31];
 } catch (Exception e) {
 myErrPrintln(e);
 }

 */
/*

 private void construct_from_array(String[] yahoo_data_array) {
 try {
 symbol = yahoo_data_array[0];
 Last_Trade__Price_Only_ = yahoo_data_array[1];
 name = yahoo_data_array[2];
 stock_exchange = yahoo_data_array[3];
 Revenue = yahoo_data_array[4];
 Earnings_per_Share = yahoo_data_array[5];
 EPS_Estimate_Current_Year = yahoo_data_array[6];
 EPS_Estimate_Next_Year = yahoo_data_array[7];
 EPS_Estimate_Next_Quarter = yahoo_data_array[8];
 Book_Value = yahoo_data_array[9];
 EBITDA = yahoo_data_array[10];
 Price_per_Sales = yahoo_data_array[11];
 Price_per_Book = yahoo_data_array[12];
 PE_Ratio = yahoo_data_array[13];
 PE_Ratio__Realtime_ = yahoo_data_array[14];
 PEG_Ratio = yahoo_data_array[15];
 Price_per_EPS_Estimate_Current_Year = yahoo_data_array[16];
 Price_per_EPS_Estimate_Next_Year = yahoo_data_array[17];
 Short_Ratio = yahoo_data_array[18];
 Percent_Change_From_200_Day_Moving_Average = yahoo_data_array[19];
 Change_From_50_Day_Moving_Average = yahoo_data_array[20];
 Percent_Change_From_50_Day_Moving_Average = yahoo_data_array[21];
 _50_Day_Moving_Average = yahoo_data_array[22];
 _200_Day_Moving_Average = yahoo_data_array[23];
 } catch (Exception e) {
 myErrPrintln(e);
 }
 }

 */

/*
 //    public class YahooDataPackage extends MyVariablesHolder {

 //	public String symbol;
 //	public String Last_Trade__Price_Only_;
 //	public String name;
 //	public String stock_exchange;
 //	public String Revenue;
 //	public String Earnings_per_Share;
 //	public String EPS_Estimate_Current_Year;
 //	public String EPS_Estimate_Next_Year;
 //	public String EPS_Estimate_Next_Quarter;
 //	public String Book_Value;
 //	public String EBITDA;
 //	public String Price_per_Sales;
 //	public String Price_per_Book;
 //	public String PE_Ratio;
 //	public String PE_Ratio__Realtime_;
 //	public String PEG_Ratio;
 //	public String Price_per_EPS_Estimate_Current_Year;
 //	public String Price_per_EPS_Estimate_Next_Year;
 //	public String Short_Ratio;
 //	public String Percent_Change_From_200_Day_Moving_Average;
 //	public String Change_From_50_Day_Moving_Average;
 //	public String Percent_Change_From_50_Day_Moving_Average;
 //	public String _50_Day_Moving_Average;
 //	public String _200_Day_Moving_Average;

 //	Map<Col, Object> colsMap;

 //	public static final int number_of_yahoo_specific_variables = 24;

 //	private void setMap() {
 //
 //	    colsMap = new HashMap();
 //	    colsMap.put(Col.y_symbol, symbol);
 //	    colsMap.put(Col.y_Last_Trade__Price_Only_, Last_Trade__Price_Only_);
 //	    colsMap.put(Col.y_name, name);
 //	    colsMap.put(Col.y_stock_exchange, stock_exchange);
 //	    colsMap.put(Col.y_Revenue, Revenue);
 //	    colsMap.put(Col.y_Earnings_per_Share, Earnings_per_Share);
 //	    colsMap.put(Col.y_EPS_Estimate_Current_Year, EPS_Estimate_Current_Year);
 //	    colsMap.put(Col.y_EPS_Estimate_Next_Year, EPS_Estimate_Next_Year);
 //	    colsMap.put(Col.y_EPS_Estimate_Next_Quarter, EPS_Estimate_Next_Quarter);
 //	    colsMap.put(Col.y_Book_Value, Book_Value);
 //	    colsMap.put(Col.y_EBITDA, EBITDA);
 //	    colsMap.put(Col.y_Price_per_Sales, Price_per_Sales);
 //	    colsMap.put(Col.y_Price_per_Book, Price_per_Book);
 //	    colsMap.put(Col.y_PE_Ratio, PE_Ratio);
 //	    colsMap.put(Col.y_PE_Ratio__Realtime_, PE_Ratio__Realtime_);
 //	    colsMap.put(Col.y_PEG_Ratio, PEG_Ratio);
 //	    colsMap.put(Col.y_Price_per_EPS_Estimate_Current_Year, Price_per_EPS_Estimate_Current_Year);
 //	    colsMap.put(Col.y_Price_per_EPS_Estimate_Next_Year, Price_per_EPS_Estimate_Next_Year);
 //	    colsMap.put(Col.y_Short_Ratio, Short_Ratio);
 //	    colsMap.put(Col.y_Percent_Change_From_200_Day_Moving_Average, Percent_Change_From_200_Day_Moving_Average);
 //	    colsMap.put(Col.y_Change_From_50_Day_Moving_Average, Change_From_50_Day_Moving_Average);
 //	    colsMap.put(Col.y_Percent_Change_From_50_Day_Moving_Average, Percent_Change_From_50_Day_Moving_Average);
 //	    colsMap.put(Col.y_50_Day_Moving_Average, _50_Day_Moving_Average);
 //	    colsMap.put(Col.y_200_Day_Moving_Average, _200_Day_Moving_Average);
 //	}
 */
/*

 //
 //	Strategy strat;
 //	Side side;
 //
 //	Integer holdDays;
 //
 //	Date tryBuyTime;
 //	Integer tryBuyShares;
 //	Double tryBuyBid;
 //	Double tryBuyAsk;
 //
 //	Date buyTime;
 //	Integer buyShares;
 //	Double buyPrice;
 //
 //	Date trySellTime;
 //	Integer trySellShares;
 //
 //	Date sellTime;
 //	Integer sellShares;
 //	Double sellPrice;
 //
 //	Double buyCommission;
 //	Double sellCommission;
 //
 //	Double tryBuySpread;
 //	Double buyChange;
 //	Double sellChange;

 //	Map<Col, Object> colsMap;
 //
 //	private void setMap() {
 //	    colsMap = new HashMap();
 //	    colsMap.put(Col.strat, strat);
 //	    colsMap.put(Col.side, side);
 //	    colsMap.put(Col.holdDays, holdDays);
 //	    colsMap.put(Col.tryBuyTime, tryBuyTime);
 //	    colsMap.put(Col.tryBuyShares, tryBuyShares);
 //	    colsMap.put(Col.tryBuyBid, tryBuyBid);
 //	    colsMap.put(Col.tryBuyAsk, tryBuyAsk);
 //	    colsMap.put(Col.tryBuySpread, tryBuySpread);
 //	    colsMap.put(Col.buyTime, buyTime);
 //	    colsMap.put(Col.buyShares, buyShares);
 //	    colsMap.put(Col.buyPrice, buyPrice);
 //	    colsMap.put(Col.buyCommission, buyCommission);
 //	    colsMap.put(Col.buyChange, buyChange);
 //	    colsMap.put(Col.trySellTime, trySellTime);
 //	    colsMap.put(Col.trySellShares, trySellShares);
 //	    colsMap.put(Col.sellTime, sellTime);
 //	    colsMap.put(Col.sellShares, sellShares);
 //	    colsMap.put(Col.sellPrice, sellPrice);
 //	    colsMap.put(Col.sellCommission, sellCommission);
 //	    colsMap.put(Col.sellChange, sellChange);
 //	}

 //	public OrderHistory() {
 ////	    this.setMap();
 //	    stock = new Stock();
 //	}
 */
/*

 //	private String format_header() {
 //
 //
 //	    StringBuilder sb = new StringBuilder();
 //	    sb.append(stock.format_header());
 //	    sb.append(c);
 //
 //
 //	    for (Col col : Col.values()) {
 //		if (col.homeClass.isInstance(OrderHistory.class)) {
 //		    sb.append(col.toString());
 //		    sb.append(c);
 //		}
 //
 //	    }
 //	    return sb.toString();
 //
 //
 //	}
 //
 //	private OrderHistory(String fileLine) throws ParseException {
 //	    this.setMap();
 //
 //	    int i = //first_index_of_nonstock_data = 
 //		    (new YahooDataPackage()).numberOfVariables()
 //		    + (new Stock()).number_of_variables;
 //
 //
 //	    String[] lineAr = fileLine.split(",");
 //
 //	    stock = new Stock(fileLine);
 //	    try {
 //		strat = Strategy.getStrategy(lineAr[i + 0]);
 //	    } catch (Exception e) {
 //	    }
 //
 //	    try {
 //		side = Side.getSide(lineAr[i + 1]);
 //	    } catch (Exception e) {
 //	    }
 //	    try {
 //		holdDays = Integer.parseInt(lineAr[i + 2]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		tryBuyTime = MyDate.read_my_date_string(lineAr[i + 3]);
 //	    } catch (ParseException e) {
 //	    }
 //	    try {
 //		tryBuyShares = Integer.parseInt(lineAr[i + 4]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		tryBuyBid = Double.parseDouble(lineAr[i + 5]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		tryBuyAsk = Double.parseDouble(lineAr[i + 6]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		tryBuySpread = Double.parseDouble(lineAr[i + 7]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		buyTime = MyDate.read_my_date_string(lineAr[i + 8]);
 //	    } catch (ParseException e) {
 //	    }
 //	    try {
 //		buyShares = Integer.parseInt(lineAr[i + 9]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		buyPrice = Double.parseDouble(lineAr[i + 10]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		buyCommission = Double.parseDouble(lineAr[i + 11]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		buyChange = Double.parseDouble(lineAr[i + 12]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		trySellTime = MyDate.read_my_date_string(lineAr[i + 13]);
 //	    } catch (ParseException e) {
 //	    }
 //	    try {
 //		trySellShares = Integer.parseInt(lineAr[i + 14]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		sellTime = MyDate.read_my_date_string(lineAr[i + 15]);
 //	    } catch (ParseException e) {
 //	    }
 //	    try {
 //		sellShares = Integer.parseInt(lineAr[i + 16]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		sellPrice = Double.parseDouble(lineAr[i + 17]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		sellCommission = Double.parseDouble(lineAr[i + 18]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	    try {
 //		sellChange = Double.parseDouble(lineAr[i + 19]);
 //	    } catch (NumberFormatException e) {
 //	    }
 //	}
 //
 //	public String format_csv() {
 //
 //	    String[] out = new String[Stock.number_of_variables];
 //
 //	    for (Col col : Col.values()) {
 //		if (col.homeClass.isInstance(YahooDataPackage.class)) {
 //
 //		    Object value = colsMap.get(col);
 //
 //		    try {
 //			if (value.getClass().isInstance(Double.class))
 //			    out[col.co] = String.format("%.4f", (Double)colsMap.get(col));
 //			if (value.getClass().isInstance(Date.class))
 //			    out[col.co] = MyDate.format_my_date((Date)colsMap.get(col));
 //			else
 //			    out[col.co] = colsMap.get(col).toString();
 //		    } catch (Exception e) {
 //		    }
 //		}
 //	    }
 //
 //	    StringBuilder sb = new StringBuilder();
 ////	    sb.append(yahoo.format_csv());	    //todo
 //	    sb.append(c);
 //
 //	    for (String str : out) {
 //		sb.append(str == null ? "-" : str);
 //		sb.append(c);
 //	    }
 //	    return sb.toString();
 //	}



 ////	Map<Col, Object> colsMap;
 //
 //	private void setMap() {
 //	    colsMap = new HashMap();
 //	    colsMap.put(Col.PnL, PnL);
 //	    colsMap.put(Col.totalCommission, totalCommission);
 //	    colsMap.put(Col.rightBuyTime, rightBuyTime);
 //	    colsMap.put(Col.rightSellTime, rightSellTime);
 //	    colsMap.put(Col.rightSellDay, rightSellDay);
 //	    colsMap.put(Col.heldDays, heldDays);
 //	}
 //
 //	private String format_header() {
 //	    StringBuilder sb = new StringBuilder();
 //	    sb.append(orderHistory.format_header());
 //	    sb.append(c);
 //
 //	    for (Col col : Col.values()) {
 //		if (col.homeClass.isInstance(CompletedSale.class)) {
 //		    sb.append(col.toString());
 //		    sb.append(c);
 //		}
 //
 //	    }
 //	    return sb.toString();
 //	}
 //
 //	public String format_csv() {
 //	    StringBuilder sb = new StringBuilder();
 //	    sb.append(orderHistory.format_csv()); //5                                                                     //0
 //	    sb.append(c);
 //	    sb.append(PnL == null ? "-" : PnL);
 //	    sb.append(c);
 //	    sb.append(totalCommission == null ? "-" : totalCommission);
 //	    sb.append(c);
 //	    sb.append(rightBuyTime == null ? "-" : (rightBuyTime ? 1 : 0));
 //	    sb.append(c);
 //	    sb.append(rightSellTime == null ? "-" : (rightSellTime ? 1 : 0));
 //	    sb.append(c);
 //	    sb.append(rightSellDay == null ? "-" : (rightSellDay ? 1 : 0));
 //	    sb.append(c);
 //	    sb.append(heldDays == null ? "-" : heldDays);
 //	    return sb.toString();
 //	}
 */
//
//	Double PnL;
//	Double totalCommission;
//
//	Boolean rightBuyTime;
//	Boolean rightSellTime;
//	Boolean rightSellDay;
//	Integer heldDays;
