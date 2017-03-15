package com.example.georg.radiostreameralt;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.example.georg.radiostreameralt.MainService.RECORDING;
import static com.example.georg.radiostreameralt.MainService.NOTRECORDING;

public class MainService extends Service
{
	//GENERAL
	private static final int notificationID = 8888;
	private static final int STOPPED = 0;
	private static final int LOADING = 1;
	private static final int PLAYING = 2;
	private int finger;
	private int playerStatus;
	private String playerUrl;
	private boolean isRecording;
	private boolean notificationExists = false;
	private long timeCreated;
	
	
	//ooooooooo.   ooooo              .o.       oooooo   oooo oooooooooooo ooooooooo.   
	//`888   `Y88. `888'             .888.       `888.   .8'  `888'     `8 `888   `Y88. 
	// 888   .d88'  888             .8"888.       `888. .8'    888          888   .d88' 
	// 888ooo88P'   888            .8' `888.       `888.8'     888oooo8     888ooo88P'  
	// 888          888           .88ooo8888.       `888'      888    "     888`88b.    
	// 888          888       o  .8'     `888.       888       888       o  888  `88b.  
	//o888o        o888ooooood8 o88o     o8888o     o888o     o888ooooood8 o888o  o888o
	MediaPlayer streamPlayer = new MediaPlayer();
	private int sleepMinutes = -1;
	private android.os.Handler myTimeHandler = new android.os.Handler();
	private Runnable PINEAPPLE = new Runnable()
	{
		public void run()
		{
			send("timeRemaining", sleepMinutes);
			if (sleepMinutes == 0)
			{
				close();
			}
			if (sleepMinutes != -1)
			{
				sleepMinutes--;
			}
			myTimeHandler.postDelayed(this, 60000);
		}
	};
	
//	ooooooooo.   oooooooooooo   .oooooo.     .oooooo.   ooooooooo.   oooooooooo.   oooooooooooo ooooooooo.   
// 	`888   `Y88. `888'     `8  d8P'  `Y8b   d8P'  `Y8b  `888   `Y88. `888'   `Y8b  `888'     `8 `888   `Y88. 
//	 888   .d88'  888         888          888      888  888   .d88'  888      888  888          888   .d88' 
//	 888ooo88P'   888oooo8    888          888      888  888ooo88P'   888      888  888oooo8     888ooo88P'  
//	 888`88b.     888    "    888          888      888  888`88b.     888      888  888    "     888`88b.    
//	 888  `88b.   888       o `88b    ooo  `88b    d88'  888  `88b.   888     d88'  888       o  888  `88b.  
//	o888o  o888o o888ooooood8  `Y8bood8P'   `Y8bood8P'  o888o  o888o o888bood8P'   o888ooooood8 o888o  o888o
	public final static int RECORDING = 0;
	public final static int NOTRECORDING = 1;
	//	public final static int UNLISTED = 3;
	public final static int FIRSTRECORDING = 0;
	public final static int LASTRECOEDING = 1;
	public final static int FIRSTANDLASTRECORDING = 2;
	public static int activeRecordings = 0;
	private static Integer key;
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, Recording> rec = new HashMap<>();
	private android.os.Handler myHandler = new android.os.Handler();
	private Runnable BANANA = new Runnable()
	{
		public void run()
		{
			int j = 0;
			for (int i = 0; i < rec.size(); i++)
			{
				if (rec.get(i) != null)
				{
					if (rec.get(i).getStatus() != NOTRECORDING)
					{
						j++;
//						if (rec.get(i).getDuration() != -1)
//						{
//							Log.w(Long.toString(System.currentTimeMillis()), "ID:" + Integer.toString(i) + " Time:" + rec.get(i).getCurrentRecordingTimeInSeconds() + "s/" + rec.get(i).getDuration() + "s Size:" + rec.get(i).getCurrentSizeInKB() + "KB");
//						} else
//						{
//							Log.w(Long.toString(System.currentTimeMillis()), "ID:" + Integer.toString(i) + " Time:" + rec.get(i).getCurrentRecordingTimeInSeconds() + "s Size:" + rec.get(i).getCurrentSizeInKB() + "KB");
//						}
						if (activeRecordings == 1)
						{
							broadcastRecording("RECORDING_ADDED", i, rec.get(i).getName(), rec.get(i).getCurrentRecordingTimeInSeconds(), rec.get(i).getCurrentSizeInKB(), FIRSTANDLASTRECORDING);
						} else if (j == 1)
						{
							broadcastRecording("RECORDING_ADDED", i, rec.get(i).getName(), rec.get(i).getCurrentRecordingTimeInSeconds(), rec.get(i).getCurrentSizeInKB(), FIRSTRECORDING);
						} else if (j == activeRecordings)
						{
							broadcastRecording("RECORDING_ADDED", i, rec.get(i).getName(), rec.get(i).getCurrentRecordingTimeInSeconds(), rec.get(i).getCurrentSizeInKB(), LASTRECOEDING);
						} else
							broadcastRecording("RECORDING_ADDED", i, rec.get(i).getName(), rec.get(i).getCurrentRecordingTimeInSeconds(), rec.get(i).getCurrentSizeInKB(), -1);
					}
				}
			}
			myHandler.postDelayed(this, 250);
		}
	};
	
	@Override
	public void onCreate()
	{
		myHandler.postDelayed(BANANA, 250);
		key = 0;
		
		timeCreated = System.currentTimeMillis();
		Log.w("timeCreated", Long.toString(timeCreated));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		myTimeHandler.postDelayed(PINEAPPLE, 0);
		
		switch (intent.getAction())
		{
			case "PLAYER_PLAY":
			{
				if (intent.hasExtra("url"))
				{
					playerUrl = intent.getStringExtra("url");
					finger = intent.getIntExtra("finger", -1);
				}
				play(playerUrl);
				buildNotification();
				break;
			}
			case "PLAYER_STOP":
			{
				stop();
				buildNotification();
				break;
			}
			case "REQUEST_PLAYER_STATUS":
			{
				send(Integer.toString(playerStatus));
				break;
			}
			case "CLOSE":
			{
				close();
				break;
			}
			case "SLEEPERTIMER":
			{
				sleepMinutes = intent.getIntExtra("sleepTime", -1);
				break;
			}
			case "RECORD":
			{
				String urlString = intent.getStringExtra("urlString");
				
				rec.put(key, new Recording(date(), urlString, intent.getLongExtra("duration", -1), intent.getStringExtra("name")));
				rec.get(key).start();
				broadcastRecording("SIMPLE_RECORDING_ADDED"); //send  main the key for hash address
				//Log.w("Recorder", "REC " + key + " START");
				key++;
				activeRecordings++;
//				showNotification(false);
				Log.w("activeRecordings", String.valueOf(activeRecordings));
				break;
			}
			case "STOP":
			{
				int passedKey = intent.getIntExtra("key", -1);
				rec.get(passedKey).stop();
				broadcastRecording("RECORDING_STOPPED", passedKey);
				//Log.w("Recorder", "REC " + passedKey + " END");
				while (rec.get(passedKey).getStatus() != NOTRECORDING) ;
				Log.w("activeRecordings", String.valueOf(activeRecordings));
				//rec.remove(passedKey);
//				showNotification(false);
				if (activeRecordings == 0)
				{
					myHandler.removeCallbacks(BANANA);
					rec.clear();
					Log.w("Recorder", "service destroyed");
					stopSelf();
				}
				break;
			}
		}
		return START_STICKY;
	}
	
	//play pause resume stop close swap functions
	public void play(String urlString)
	{
		if (playerStatus != STOPPED)
		{
			stop();
		}
		playerStatus = LOADING;
		send(Integer.toString(playerStatus)); //broadcast media player status for main and notification
		
		//mediaplayer
		streamPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//		streamPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener()
//		{
//			@Override
//			public void onBufferingUpdate(MediaPlayer streamPlayer, int percent) {
//				Log.w("media", "Buffering: " + percent);
//			}
//		});
//		streamPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
//			@Override
//			public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
//				Log.w("asd", "MediaPlayer.OnInfoListener: " + i);
//				return false;
//			}
//		});
		try
		{
//			streamPlayer.setDataSource(this,Uri.parse(urlString));
			streamPlayer.setDataSource(urlString);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		streamPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
		{
			@Override
			public void onPrepared(MediaPlayer mediaPlayer)
			{
				mediaPlayer.start();
				
				playerStatus = PLAYING;
				buildNotification();
				send(Integer.toString(playerStatus)); //broadcast media player status for main and notification
			}
		});
		streamPlayer.prepareAsync();
	}
	
	public void stop()
	{
		if (streamPlayer.isPlaying())
		{
			streamPlayer.stop();
		}
//		streamPlayer.release();
		streamPlayer.reset();
		playerStatus = STOPPED;
		send(Integer.toString(playerStatus)); //broadcast media player status for main and notification
	}
	
	public void close()
	{
		stop();
		streamPlayer.release();
		stopSelf(); //stop media player service
	}
	
	private void buildNotification()
	{
		//intent to call mainactivity but not a new one
		Intent intent = new Intent(this, MainActivity.class).addCategory(Intent.CATEGORY_LAUNCHER).setAction(Intent.ACTION_MAIN);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		//notification PLAY button
		Intent playIntent = new Intent(getApplicationContext(),MainService.class);
		playIntent.setAction("PLAYER_PLAY");
		PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, 0);
		NotificationCompat.Action playAction = new NotificationCompat.Action(R.drawable.ic_play, "Play", playPendingIntent);
//		Notification.Action playAction = new Notification.Action.Builder(Icon.createWithResource(getApplicationContext(), R.drawable.ic_play), "Play", playPendingIntent).build();
		
		//notification STOP button
		Intent stopIntent = new Intent(getApplicationContext(),MainService.class);
		stopIntent.setAction("PLAYER_STOP");
		PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);
		NotificationCompat.Action stopAction = new NotificationCompat.Action(R.drawable.ic_media_stop, "Stop", stopPendingIntent);
		
		//notification EXIT button
		Intent closeIntent = new Intent(getApplicationContext(),MainService.class);
		closeIntent.setAction("CLOSE");
		PendingIntent closePendingIntent = PendingIntent.getService(this, 0, closeIntent, 0);
		NotificationCompat.Action closeAction = new NotificationCompat.Action(R.drawable.poweroff, "Exit", closePendingIntent);
		
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
		notificationBuilder.setSmallIcon(R.drawable.ic_media_stop);
		notificationBuilder.setContentTitle("Stream Player"+" "+timeCreated);
		
		if (playerStatus == LOADING)
		{
			notificationBuilder.setContentText("Loading");
		}
		else if (playerStatus == PLAYING)
		{
			notificationBuilder.setContentText("Playing");
			notificationBuilder.addAction(stopAction);
		}
		else if (playerStatus == STOPPED)
		{
			notificationBuilder.setContentText("Stopped");
			notificationBuilder.addAction(playAction);
		}
		
		notificationBuilder.setOngoing(true);
		notificationBuilder.setContentIntent(pendingIntent);
		notificationBuilder.addAction(closeAction);
		
		if (notificationExists)
		{
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(notificationID, notificationBuilder.build());
		}
		else 
		{
			startForeground(notificationID,notificationBuilder.build());
		}
	}
	
	private String date()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
		{
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-kkmmss");
			return sdf.format(calendar.getTime());
		} else
		{
			Date d = new Date();
			CharSequence s = DateFormat.format("yyMMdd-kkmmss", d.getTime());
			return (String) s;
		}
	}
	
	//send function to broadcast an action
	public void send(String actionToSend)
	{
		Intent intent = new Intent();
		intent.setAction(actionToSend);
		sendBroadcast(intent);
	}
	
	public void send(String actionToSend, int sleepTimeRemaining)
	{
		Intent intent = new Intent();
		intent.setAction(actionToSend);
		intent.putExtra("timeRemainingInt", sleepTimeRemaining);
		sendBroadcast(intent);
	}
	
	public void broadcastRecording(String action, int key, String name, long currentTime, int sizeInKb, int position)
	{
		Intent intent = new Intent();
		intent.setAction(action);
		intent.putExtra("key", key);
		intent.putExtra("name", name);
		intent.putExtra("time", currentTime);
		intent.putExtra("size", sizeInKb);
		intent.putExtra("position", position);
		sendBroadcast(intent);
	}
	
	public void broadcastRecording(String action, int key)
	{
		Intent intent = new Intent();
		intent.setAction(action);
		intent.putExtra("key", key);
		sendBroadcast(intent);
	}
	
	public void broadcastRecording(String action)
	{
		Intent intent = new Intent();
		intent.setAction(action);
		sendBroadcast(intent);
	}
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}

class Recording
{
	private String name;
	private String date;
	private String urlString;
	private long duration;
	private boolean stopped;
	private int status;
	private int bytesRead;
	private long startTimeInSeconds;
	
	
	Recording(String date, String urlString, long duration, String name)
	{
		this.date = date;
		this.urlString = urlString;
		this.duration = duration;
		this.name = name;
		stopped = false;
		bytesRead = 0;
		startTimeInSeconds = System.currentTimeMillis() / 1000;
	}
	
	void start()
	{
		status = RECORDING;
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					status = RECORDING;
					FileOutputStream fileOutputStream;
					File streamsDir = new File(Environment.getExternalStorageDirectory() + "/Streams");
					if (!streamsDir.exists())
					{
						boolean directoryCreated = streamsDir.mkdirs();
						Log.w("Recorder", "Created directory");
						if (!directoryCreated)
						{
							Log.w("Recorder", "Failed to create directory");
						}
					}
					File outputSource = new File(streamsDir, name + date + ".mp3");
					fileOutputStream = new FileOutputStream(outputSource);
					
					//ICY 200 OK ERROR FIX FOR KITKAT
					if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
					{
						Log.w("Version", String.valueOf(Build.VERSION.SDK_INT));
						OkHttpClient client = new OkHttpClient();
						Request request = new Request.Builder().url(urlString).build();
						Response response = client.newCall(request).execute();
						InputStream inputStream = response.body().byteStream();
						
						int c;
						while (((c = inputStream.read()) != -1) && !stopped && (duration == -1 || ((System.currentTimeMillis() / 1000) < (startTimeInSeconds + duration))))
						{
							fileOutputStream.write(c);
							bytesRead++;
						}
					} else
					{
						Log.w("Version", String.valueOf(Build.VERSION.SDK_INT));
						URL url = new URL(urlString);
						InputStream inputStream = url.openStream();
						
						int c;
						while (((c = inputStream.read()) != -1) && !stopped && (duration == -1 || ((System.currentTimeMillis() / 1000) < (startTimeInSeconds + duration))))
						{
							fileOutputStream.write(c);
							bytesRead++;
						}
					}
					
					Log.w("Recorder", String.valueOf(bytesRead / 1024) + " KBs downloaded.");
					
					fileOutputStream.close();
					
					//Log.w("Recorder", "finished");
					Recorder.activeRecordings--;
					status = NOTRECORDING;
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	int getCurrentSizeInKB()
	{
		return (bytesRead / 1024);
	}
	
	long getCurrentRecordingTimeInSeconds()
	{
		return (System.currentTimeMillis() / 1000) - startTimeInSeconds;
	}
	
	int getStatus()
	{
		return status;
	}
	
	long getDuration()
	{
		return duration;
	}
	
	void stop()
	{
		stopped = true;
	}
	
	public String getName()
	{
		return name;
	}
}

//	Notification notification = new Notification.Builder(this)
//			.setOngoing(true)
//			.setSmallIcon(R.drawable.ic_launchersmall)
//			.setContentTitle(Long.toString(timeCreated))
//			.build();
//	
//	startForeground(notificationID, notification);
