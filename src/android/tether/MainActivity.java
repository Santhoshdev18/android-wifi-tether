/**
 *  This software is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Use this application at your own risk.
 *
 *  Copyright (c) 2009 by Harald Mueller and Seth Lemons.
 */

package android.tether;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.tether.system.CoreTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private TetherApplication application = null;
	private ProgressDialog progressDialog;

	private ImageButton startBtn = null;
	private ImageButton stopBtn = null;
	
	private TableRow startTblRow = null;
	private TableRow stopTblRow = null;
	
	private static int ID_DIALOG_STARTING = 0;
	private static int ID_DIALOG_STOPPING = 1;
	
	public static final String MSG_TAG = "TETHER -> MainActivity";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(MSG_TAG, "Calling onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Init Application
        this.application = (TetherApplication)this.getApplication();
        
        // Init Table-Rows
        this.startTblRow = (TableRow)findViewById(R.id.startRow);
        this.stopTblRow = (TableRow)findViewById(R.id.stopRow);

        // Check for binaries
        if (this.application.binariesExists() == false || CoreTask.filesetOutdated()) {
        	if (CoreTask.hasRootPermission()) {
        		this.application.installBinaries();
        	}
        	else {
        		LayoutInflater li = LayoutInflater.from(this);
                View view = li.inflate(R.layout.norootview, null); 
    			new AlertDialog.Builder(MainActivity.this)
    	        .setTitle("Not Root!")
    	        .setIcon(R.drawable.warning)
    	        .setView(view)
    	        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
    	                public void onClick(DialogInterface dialog, int whichButton) {
    	                        Log.d(MSG_TAG, "Close pressed");
    	                        MainActivity.this.finish();
    	                }
    	        })
    	        .setNeutralButton("Override", new DialogInterface.OnClickListener() {
		                public void onClick(DialogInterface dialog, int whichButton) {
	                        Log.d(MSG_TAG, "Override pressed");
	                        MainActivity.this.application.installBinaries();
		                }
    	        })
    	        .show();
        	}
        }
        
        // Start Button
        this.startBtn = (ImageButton) findViewById(R.id.startTetherBtn);
		this.startBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "StartBtn pressed ...");
		    	showDialog(MainActivity.ID_DIALOG_STARTING);
				new Thread(new Runnable(){
					public void run(){
						MainActivity.this.application.disableWifi();
						if (MainActivity.this.application.getSync()){
							MainActivity.this.application.disableSync();
						}
						int started = MainActivity.this.application.startTether();
						MainActivity.this.dismissDialog(MainActivity.ID_DIALOG_STARTING);
						Message message = new Message();
						if (started != 0) {
							message.what = started;
						}
						MainActivity.this.viewUpdateHandler.sendMessage(message); 
					}
				}).start();
			}
		});

		// Stop Button
		this.stopBtn = (ImageButton) findViewById(R.id.stopTetherBtn);
		this.stopBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "StopBtn pressed ...");
		    	showDialog(MainActivity.ID_DIALOG_STOPPING);
				new Thread(new Runnable(){
					public void run(){
						MainActivity.this.application.stopTether();
						MainActivity.this.dismissDialog(MainActivity.ID_DIALOG_STOPPING);
						MainActivity.this.viewUpdateHandler.sendMessage(new Message()); 
					}
				}).start();
			}
		});			
		this.toggleStartStop();
		this.openDonateDialog();
    }
    
	public void onStop() {
    	Log.d(MSG_TAG, "Calling onStop()");
		super.onStop();
	}

	public void onDestroy() {
    	Log.d(MSG_TAG, "Calling onDestroy()");
    	super.onDestroy();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean supRetVal = super.onCreateOptionsMenu(menu);
    	SubMenu setup = menu.addSubMenu(0, 0, 0, getString(R.string.setuptext));
    	setup.setIcon(R.drawable.setup);
    	SubMenu accessctr = menu.addSubMenu(0, 3, 0, getString(R.string.accesscontroltext));
    	accessctr.setIcon(R.drawable.acl);    	
    	SubMenu log = menu.addSubMenu(0, 1, 0, getString(R.string.logtext));
    	log.setIcon(R.drawable.log);
    	SubMenu about = menu.addSubMenu(0, 2, 0, getString(R.string.abouttext));
    	about.setIcon(R.drawable.about);    	
    	return supRetVal;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	boolean supRetVal = super.onOptionsItemSelected(menuItem);
    	Log.d(MSG_TAG, "Menuitem:getId  -  "+menuItem.getItemId()); 
    	if (menuItem.getItemId() == 0) {
    		Intent i = new Intent(MainActivity.this, SetupActivity.class);
	        startActivityForResult(i, 0);
    	}
    	else if (menuItem.getItemId() == 1) {
	        Intent i = new Intent(MainActivity.this, LogActivity.class);
	        startActivityForResult(i, 0);
    	}
    	else if (menuItem.getItemId() == 2) {
    		LayoutInflater li = LayoutInflater.from(this);
            View view = li.inflate(R.layout.aboutview, null); 
			new AlertDialog.Builder(MainActivity.this)
	        .setTitle("About")
	        .setIcon(R.drawable.about)
	        .setView(view)
	        .setNeutralButton("Donate", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                        Log.d(MSG_TAG, "Donate pressed");
	    					Uri uri = Uri.parse(getString(R.string.paypalUrl));
	    					startActivity(new Intent(Intent.ACTION_VIEW, uri));
	                }
	        })
	        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                        Log.d(MSG_TAG, "Close pressed");
	                }
	        })
	        .show();
    	} 
    	else if (menuItem.getItemId() == 3) {
	        Intent i = new Intent(MainActivity.this, AccessControlActivity.class);
	        startActivityForResult(i, 0);   		
    	}
    	return supRetVal;
    }    

    @Override
    protected Dialog onCreateDialog(int id) {
    	if (id == ID_DIALOG_STARTING) {
	    	progressDialog = new ProgressDialog(this);
	    	progressDialog.setTitle("Start Tethering");
	    	progressDialog.setMessage("Please wait while starting...");
	    	progressDialog.setIndeterminate(false);
	    	progressDialog.setCancelable(true);
	        return progressDialog;
    	}
    	else if (id == ID_DIALOG_STOPPING) {
	    	progressDialog = new ProgressDialog(this);
	    	progressDialog.setTitle("Stop Tethering");
	    	progressDialog.setMessage("Please wait while stopping...");
	    	progressDialog.setIndeterminate(false);
	    	progressDialog.setCancelable(true);
	        return progressDialog;  		
    	}
    	return null;
    }

    Handler viewUpdateHandler = new Handler(){
        public void handleMessage(Message msg) {
        	if (msg.what == 1) {
        		Log.d(MSG_TAG, "No mobile-data-connection established!");
        		MainActivity.this.displayToastMessage("No mobile-data-connection established!");
        	}
        	else if (msg.what == 2) {
        		Log.d(MSG_TAG, "Unable to start tetering!");
        		MainActivity.this.displayToastMessage("Unable to start tethering!");
        	}
        	MainActivity.this.toggleStartStop();
        	super.handleMessage(msg);
        }
   };

   private void toggleStartStop() {
    	boolean dnsmasqRunning = false;
		try {
			dnsmasqRunning = CoreTask.isProcessRunning(CoreTask.DATA_FILE_PATH+"/bin/dnsmasq");
		} catch (Exception e) {
			MainActivity.this.displayToastMessage("Unable to check if dnsmasq is currently running!");
		}
    	boolean natEnabled = CoreTask.isNatEnabled();
    	if (dnsmasqRunning == true && natEnabled == true) {
    		this.startTblRow.setVisibility(View.GONE);
    		this.stopTblRow.setVisibility(View.VISIBLE);
    		// Notification
    		this.application.showStartNotification();
    	}
    	else if (dnsmasqRunning == false && natEnabled == false) {
    		this.startTblRow.setVisibility(View.VISIBLE);
    		this.stopTblRow.setVisibility(View.GONE);
    		
    		// Notification
        	this.application.notificationManager.cancelAll();
    	}   	
    	else {
    		this.startTblRow.setVisibility(View.VISIBLE);
    		this.stopTblRow.setVisibility(View.VISIBLE);
    		MainActivity.this.displayToastMessage("Your phone is currently in an unknown state - try to reboot!");
    	}
    }
    
   	private void openDonateDialog() {
   		if (this.application.showDonationDialog()) {
   			// Disable donate-dialog for later startups
   			this.application.preferenceEditor.putBoolean("donatepref", false);
   			this.application.preferenceEditor.commit();
   			// Creating Layout
			LayoutInflater li = LayoutInflater.from(this);
	        View view = li.inflate(R.layout.donateview, null); 
	        new AlertDialog.Builder(MainActivity.this)
	        .setTitle("Donate")
	        .setIcon(R.drawable.about)
	        .setView(view)
	        .setNeutralButton("Close", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                        Log.d(MSG_TAG, "Close pressed");
	                        MainActivity.this.displayToastMessage("Thanks, anyway ...");
	                }
	        })
	        .setNegativeButton("Donate", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                        Log.d(MSG_TAG, "Donate pressed");
	    					Uri uri = Uri.parse(getString(R.string.paypalUrl));
	    					startActivity(new Intent(Intent.ACTION_VIEW, uri));
	                }
	        })
	        .show();
   		}
   	}
   
	public void displayToastMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}
}

