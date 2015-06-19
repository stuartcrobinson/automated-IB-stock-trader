only ApiDemo.java was written by me.

Interactive Brokers (IB) is an online stock broker that provides an API capable of total automation.

Several brokers provide an API but IB's is the oldest and most powerful.  However it is also probably the most poorly documented.  

Their only form of documentation is a small stand-alone Java application with a simple java GUI.  The only way to use the code is essentially reverse-engineer their provided java program.

Thus, ALL of this project is out-of-the-box IB code EXCEPT ApiDemo.java.  

Again, I only wrote ONE file in the project, ApiDemo.java

This was over a year ago so these details will be spotty.  

It was able to get real-time and historic data from multiple exchanges around the world.  If a stock priced changed a specified amount, then it bought it and held it until a certain event or a certain amount of time.

It also kept detailed logs of purchase/sale times, and which condition led to the purchase (it checked for several conditions at once).

This did not use real money.  IB provides a "paper trading" account which is exactly like real-life, except your trades are fake money.  Your paper trading account starts with a certain amount.

I believe that this process was fully automated.  The end goal was to set up a virtual server where this would run continuously.  

However, after running for a few days on my personal lap top, I decided I needed more data.  That's when I embarked on the current project that is currently divorced from an actual trading platform. (thus purely research-based)

One interesting note is that, while recording tracks for a Bombadil album, I got a call from a developer at Interactive Brokers telling me that I crashed their paper trading service for a few hours due to unusually high trading volume.  I noticed that my client crashed, but apparently it affected everyone using the paper trading service.  I quickly reduced the program's trading load. 