package com.unathemastudios.bitrate;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by Thanos on 17-Sep-17.
 */

public class AlarmEventDialog extends DialogFragment implements TimePickerFragment.NoticeDialogListener {


	public interface NoticeDialogListener {
		public void onDialogPositiveClick(String url);
	}
	public AlarmEventDialog.NoticeDialogListener mListener;

	private TextView tvTime, tvDate;
	private CheckBox checkBox;
	private Spinner spinner;
	private Calendar calendar;
	private int hour = 0;
	private int minute = 0;
	

	@RequiresApi(api = Build.VERSION_CODES.N)
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		LayoutInflater factory = LayoutInflater.from(getContext());
		final View textEntryView = factory.inflate(R.layout.add_event_dialog, null);
		tvDate = (TextView) textEntryView.findViewById(R.id.tvDateSelected);
		tvTime = (TextView) textEntryView.findViewById(R.id.tvTimeSelectedForEvent);
		checkBox = (CheckBox) textEntryView.findViewById(R.id.cbWithDate);
		spinner = (Spinner) textEntryView.findViewById(R.id.spChooseRadio);
		tvDate.setEnabled(false);
		calendar = Calendar.getInstance();
		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) tvDate.setEnabled(true);
			}
		});

		//Time TextBox
		tvTime.setText((CharSequence) calendar.getTime());
		tvTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TimePickerFragment timePickerFragment = new TimePickerFragment();
				timePickerFragment.setTargetFragment(AlarmEventDialog.this, 50);
				timePickerFragment.show(getFragmentManager(), "GIMMETIME");
			}
		});

		//Spinner
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R
				.layout.simple_list_item_1);
		for (Radio a:((MainActivity)getActivity()).radiosList){
			arrayAdapter.add(a.getName());
		}
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(arrayAdapter);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(textEntryView)
				.setTitle("Add Alarm Event")
				.setPositiveButton("Add", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						AlarmEventDialog.this.dismiss();
					}
				});

		
		return builder.create();
	}

	// Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the host
			mListener = (AlarmEventDialog.NoticeDialogListener) getTargetFragment();
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement NoticeDialogListener");
		}
	}

	@Override
	public void show(FragmentManager manager, String tag) {
		super.show(manager, tag);
	}

	@Override
	public void onDialogPositiveClick(int hourOfTHeDay, int minute) {
		hour = hourOfTHeDay;
		this.minute = minute;
		tvTime.setText(hourOfTHeDay + ":" + minute);
	}
}