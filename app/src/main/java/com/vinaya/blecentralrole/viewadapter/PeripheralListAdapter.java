package com.vinaya.blecentralrole.viewadapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vinaya.blecentralrole.R;
import com.vinaya.blecentralrole.model.Peripheral;

import java.util.List;


public class PeripheralListAdapter extends BaseAdapter {
	private List<Peripheral> list;
	private LayoutInflater layoutInflater;
	private OnItemClickListener onItemClickListener;
	private DisableFilter disableFilter;

	private int rssiThreshold;
	private RssiAlertListener rssiAlertListener;


	public interface OnItemClickListener {
		void onClick(Peripheral peripheral);
	}

	public interface DisableFilter {
		boolean isDisable(Peripheral peripheral);
	}

	public interface RssiAlertListener {
		void onAlertRssi(Peripheral peripheral);
	}


	public PeripheralListAdapter(Context context) {
		this.layoutInflater = LayoutInflater.from(context);
	}


	@Override
	public int getCount() {
		if (list==null) return 0;
		return list.size();
	}

	@Override
	public Peripheral getItem(int position) {
		if (list==null) return null;
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		//TODO: find a better way to generate the uniqueness
		final String address = getItem(position).getAddress();
		if (address == null) return 0;
		return address.hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view = layoutInflater.inflate(R.layout.item_list, parent, false);
		final Peripheral peripheral = list.get(position);

		final ImageView imageIcon = (ImageView) view.findViewById(R.id.imageViewIcon);
		final TextView textViewDevName = (TextView) view.findViewById(R.id.textViewDevName);
		final TextView textViewDevAddress = (TextView) view.findViewById(R.id.textViewDevAddress);
		final TextView textViewRssi = (TextView) view.findViewById(R.id.textViewRssi);


		//set the background to green is the peripheral is connected
		if (peripheral.isConnected()) {
			view.setBackgroundColor(Color.GREEN);

			//feature 10: Display an alert to the user whenever the RSSI value of the connected Peripheral goes below a certain value
			if (rssiAlertListener != null && peripheral.getRssi() < rssiThreshold) {
				rssiAlertListener.onAlertRssi(peripheral);
			}
		}
		else {
			view.setBackgroundColor(Color.WHITE);
		}

		//feature 2: Display peripherals found and their RSSI values.
		textViewDevName.setText(peripheral.getName());
		textViewDevAddress.setText(peripheral.getAddress());
		textViewRssi.setText(String.valueOf(peripheral.getRssi()));

		//feature 3: greying out the Peripherals that do not fulfill the condition
		if (disableFilter != null && disableFilter.isDisable(peripheral)) {
			imageIcon.setImageResource(R.drawable.icon_gray);
			textViewDevName.setTextColor(Color.GRAY);
			textViewDevAddress.setTextColor(Color.GRAY);
			textViewRssi.setTextColor(Color.GRAY);
			view.setEnabled(false);
			return view;
		}

		//otherwise, set the text color to black, and show the colorful icon
		imageIcon.setImageResource(R.drawable.icon);
		textViewDevName.setTextColor(Color.BLACK);
		textViewDevAddress.setTextColor(Color.BLACK);
		textViewRssi.setTextColor(
			(rssiAlertListener != null && peripheral.getRssi() < rssiThreshold) ?
				Color.RED : Color.BLUE
		);
		view.setEnabled(true);

		//feature 4: allow user to connect to peripherals with Service
		if (onItemClickListener != null) {
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onItemClickListener.onClick(peripheral);
				}
			});
		}

		return view;
	}


	public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	public void setOnRssiAlertListener(int rssiThreshold, RssiAlertListener listener) {
		this.rssiThreshold = rssiThreshold;
		this.rssiAlertListener = listener;
	}


	public void setDisableFilter(DisableFilter disableFilter) {
		this.disableFilter = disableFilter;
	}


	public void setPeripheralList(List<Peripheral> list) {
		this.list = list;
	}

}

