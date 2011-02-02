/*
 * Logger.java
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

package logger;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;

import server.WordMoleServerImpl;
import server.callbacks.AsyncCallback;
import server.callbacks.Callback;
import server.callbacks.Notifier;

/**
 * An asynchrous logging class that places messages in a list displayed by a
 * dialog box. Messages are logged asynchronously using a minimum priority
 * thread, which allows the server to quickly respond to client requests
 * without being bogged down by I/O.
 */
public class Logger extends JDialog implements Callback {
   //asynchronous logging with pool of one thread at lowest priority
   //not all thread pools have to be big - having one thread ensures
   //log entries are serialized
	
   AsyncCallback fLogCallback = new AsyncCallback(Thread.MIN_PRIORITY, 1);
   WordMoleServerImpl fServer;
   JList fLog = new JList(new DefaultListModel());
   
   public Logger(WordMoleServerImpl server) {
      fServer = server;
      
      fLogCallback.start();
      
      JScrollPane sp = new JScrollPane(fLog);
      getContentPane().add(sp);
      
      setSize(200, 300);
      pack();
   }
   
   public void log(String msg) {
      fLogCallback.doCallback(this, msg);
   }
   
   public void log(Exception ex) {
      fLogCallback.doCallback(this, ex);
   }
   
   public void executeCallback(Notifier n, Object arg) {
      ((DefaultListModel)fLog.getModel()).addElement(arg);
      fLog.repaint();
   }
}
    
    