/*
 * AsyncCallback.java
 * Copyright (C) 2010  Chris Barton
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   Questions/Comments: c.chris.b@gmail.com
 *   WordMole is available free at http://wordmole.sourceforge.net/
 */

package server.callbacks;

import java.util.*;

public class AsyncCallback {
   private int fNumNotifiers;
   private int fPriority;
   private Vector<CallbackRequest> fCallbacks = new Vector<CallbackRequest>();
   private Vector<Notifier> fNotifiers = new Vector<Notifier>();
   private long fMaxCalltime = SIXTY_SECONDS;
   private Thread fSweeper = new SweeperThread();

   public static final long TEN_SECONDS = 10000;
   public static final long THIRTY_SECONDS = 30000;
   public static final long SIXTY_SECONDS = 60000;
   public static int SWEEP_RATE = 4000;

   /**
    * Constructor
    * @param priority -  int -  Priority of the Callback threads.
    * @param num - int - Number of Callback threads
    */
   public AsyncCallback(int priority, int num) {
      if (priority<Thread.MIN_PRIORITY || priority>Thread.MAX_PRIORITY) {
         throw new IllegalArgumentException("Invalid thread priority: " +
                                             priority);
      }
      
      fNumNotifiers = num;
      fPriority = priority;
      fSweeper.start();
   }

   public synchronized void start() {
      Notifier n;

      if (fNumNotifiers == 0) return;

      for (int i=fNumNotifiers; i>0; i--) {
         n = new Notifier(i, fPriority, this);
         fNotifiers.addElement(n);
      }
   }

   public synchronized void stop() {
      Enumeration<Notifier> e = fNotifiers.elements();
      Notifier n;

      while (e.hasMoreElements()) {
         n = (Notifier)e.nextElement();
         n.timeToStop();
      }

      fNotifiers.removeAllElements();
   }

   /**
    *
    */
   public void doCallback(Callback c) {
      doCallback(c, null);
   }

   /**
    *
    */
   public void doCallback(Callback c, Object arg) {
      CallbackRequest cbr = new CallbackRequest(c, arg);
      doCallback(cbr);
   }


   public synchronized void doCallback(CallbackRequest c) {
      fCallbacks.addElement(c);
      notify();
   }
 
   /**
    * 
    */
   public int getNumThreads() {
      return fNumNotifiers;
   }

   /**
    * 
    */
   public void setMaxCalltime(long t) {
      fMaxCalltime = t;
   }

   /**
    * 
    */
   public long getMaxCalltime() {
      return fMaxCalltime;
   }

   public void checkForTimeout(long curr, long max) {
      Enumeration<Notifier> e = fNotifiers.elements();

      while (e.hasMoreElements()) {
         Notifier n = (Notifier)e.nextElement();
         n.checkForTimeout(curr, max);
      }
   }

   /**
    *
    */
   public void waitForCallback(Notifier n) throws InterruptedException {
      CallbackRequest req = null;

      synchronized(this) {
         while (fCallbacks.size() == 0) {
            wait();
         }

         req = (CallbackRequest)fCallbacks.elementAt(0);
         fCallbacks.removeElementAt(0);
      }

      n.setRequest(req);
   }

   private class SweeperThread extends Thread {
	  public SweeperThread() {
         setPriority(MIN_PRIORITY);
         setDaemon(true);
      }

      public void run() {
         while (true) {
            try {
               sleep(SWEEP_RATE);
            } catch (InterruptedException ex) {}
            
            long max = fMaxCalltime;
            long curr = System.currentTimeMillis();

            checkForTimeout(curr, max);
         }
      }
   }
}
