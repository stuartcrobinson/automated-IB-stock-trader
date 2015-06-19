/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package testscr;

import java.text.DecimalFormat;

/**
 *
 * @author User
 */
public class NewClass {
 
    
    public static void main (String [] args){
	
	String hi = "hi";
	String two = "hi";
	
	String num1 = "23452345241.3";
	String num2 = "1.0003000";
	
	 double answer = 5.030004;
   DecimalFormat df = new DecimalFormat("#.####");
  System.out.println(df.format(answer));
	
	System.out.format("%s %s %n", df.format(Double.parseDouble(num1)), df.format(Double.parseDouble(num2)));
    }
    
    
}
