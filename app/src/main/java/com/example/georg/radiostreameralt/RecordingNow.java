package com.example.georg.radiostreameralt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.amigold.fundapter.BindDictionary;
import com.amigold.fundapter.FunDapter;
import com.amigold.fundapter.extractors.StringExtractor;

import java.util.ArrayList;
import java.util.Locale;

public class RecordingNow extends Fragment
{
	private ArrayList<RecordingRadio> recordingNowRadios = new ArrayList<>();
	private FunDapter adapter;
	private BroadcastReceiver serviceReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals("RECORDING_ADDED"))
			{
				if (intent.getIntExtra("position", 998) == 0 || (intent.getIntExtra("position", 747) == 2))
				{
					recordingNowRadios.clear();
				}
				recordingNowRadios.add(new RecordingRadio(intent.getStringExtra("name"), intent.getIntExtra("key", 200), intent.getLongExtra("time", -1), intent.getIntExtra("size", -1)));
				if ((intent.getIntExtra("position", 747) == 1) || (intent.getIntExtra("position", 747) == 2))
				{
					adapter.updateData(recordingNowRadios);
				}
			} 
			else if (intent.getAction().equals("RECORDING_STOPPED"))
			{
				int keyToDelete = intent.getIntExtra("key", -1);
				for (int i = 0; i < recordingNowRadios.size(); i++)
				{
					if (recordingNowRadios.get(i).getId() == keyToDelete)
						recordingNowRadios.remove(i);
				}
				adapter.updateData(recordingNowRadios);
			}
		}
	};
	
	public RecordingNow()
	{
		// Required empty public constructor
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (serviceReceiver != null)
		{
			getActivity().registerReceiver(serviceReceiver, new IntentFilter("RECORDING_ADDED"));
			getActivity().registerReceiver(serviceReceiver, new IntentFilter("RECORDING_STOPPED"));
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_recording_now, container, false);
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		
//		TextView tvrecordingNowRadio = (TextView) getActivity().findViewById(R.id.tvRecordingNow);
		
		BindDictionary<RecordingRadio> dictionary = new BindDictionary<>();
		dictionary.addStringField(R.id.tvRecordingNow, new StringExtractor<RecordingRadio>()
		{
			@Override
			public String getStringValue(RecordingRadio item, int position)
			{
				return item.getName();
			}
		});
		dictionary.addStringField(R.id.tvRecordingNowSize, new StringExtractor<RecordingRadio>()
		{
			@Override
			public String getStringValue(RecordingRadio item, int position)
			{
				return prettySize(item.getSize());
			}
		});
		dictionary.addStringField(R.id.tvRecordingNowTime, new StringExtractor<RecordingRadio>()
		{
			@Override
			public String getStringValue(RecordingRadio item, int position)
			{
				return prettyTime(item.getTime());
			}
		});
		
		adapter = new FunDapter<>(view.getContext(), recordingNowRadios, R.layout.recording_now_layout, dictionary);
		
		ListView listView = (ListView) getActivity().findViewById(R.id.recordingNowListView);
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				send("STOP", recordingNowRadios.get(position).getId());
			}
		});
	}
	
	public void send(String actionToSend, int key)
	{
		Intent serviceIntent = new Intent(getContext(), Recorder.class);
		serviceIntent.putExtra("Action", actionToSend);
		serviceIntent.putExtra("key", key);
		getActivity().startService(serviceIntent);
	}
	
	public String prettyTime(long timeInSeconds)
	{
		return String.format(Locale.US,"%02d:%02d",timeInSeconds/60,timeInSeconds%60);
	}
	
	public String prettySize(int sizeInKB)
	{
		if (sizeInKB<1024)
		{
			return (String.valueOf(sizeInKB)+" KB ");
		}
		else /*if (sizeInKB<1048576)*/
		{
			return String.format(Locale.US,"%.2f",(double)sizeInKB/1024)+" MB ";
		}
	}
	
//	//function to check if a service is running
	
//	private boolean isMyServiceRunning(Class<?> serviceClass)
//	{
//		ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
//		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
//		{
//			if (serviceClass.getName().equals(service.service.getClassName()))
//			{
//				return true;
//			}
//		}
//		return false;
//	}
}
