/*
 * Notifier.java
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

public class Notifier extends Thread {
   private boolean fStopNow;
   private long fStartedCallback;
   private AsyncCallback fList;
   private CallbackRequest fToCallback;
   private boolean fIsCallbackActive = false;

   public Notifier(int id, int pri, AsyncCallback list) {
      super("Notifier "+pri+":"+id);
      setPriority(pri);
      fList = list;

      start();
   }

   public void run() {
      while (!fStopNow) {
         try {
            fList.waitForCallback(this);
            resetCallbackTime();
            fIsCallbackActive = true;
            fToCallback.callback(this);
         } catch (InterruptedException ex) {
            if (fStopNow) return;
            System.out.println("Callback exceeded maximum time or time to stop");
         } catch (Exception ex) {
            System.err.println("Unexpected callback error: " + ex);
            ex.printStackTrace(System.err);
         } finally {
            fIsCallbackActive = false;
         }
      }
   }

   /**
    * Called by CallbackList.waitForCallback() to set the next callback request
    * to execute.
    */
   public void setRequest(CallbackRequest c) {
      fToCallback = c;
   }

   /**
    * Sets the stop flag which will cause the run() method to terminate after
    * completing current activity
    */
   public void timeToStop() {
      fStopNow = true;
      interrupt();
   }

   /**
    * Gets the time since the last callback was started based on the current
    * system time. This value is meaningful only if isCallbackActive()
    * returns true
    */
   public long getElapsedCallbackTime() {
      return getElapsedCallbackTime(System.currentTimeMillis());
   }

   /**
    * Sets the time since the last callback was started based on the system time
    * passed into the method. This value is meaningful only if isCallbackActive()
    * returns true
    */
   public long getElapsedCallbackTime(long curr) {
      return curr - fStartedCallback;
   }

   /**
    * Gets whether this notifier is currently executing a callback.
    */
   public boolean isCallbackActive() {
      return fIsCallbackActive;
   }

   /**
    * Resets the elapsed callback time to 0. This method should be called by
    * a Callback object before each method dispatch.
    */
   public void resetCallbackTime() {
      fStartedCallback = System.currentTimeMillis();
   }

   final void checkForTimeout(long curr, long max) {
      if (!fIsCallbackActive) {
         return;
      }

      if (getElapsedCallbackTime(curr) > max) {
         interrupt();
      }
   }
}

