package com.vinaya.blecentralrole.viewadapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vinaya.blecentralrole.R;
import com.vinaya.blecentralrole.model.Peripheral;

import java.util.List;


public class PeripheralListAdapter extends BaseAdapter {
	private List<Peripheral> list;
	private LayoutInflater layoutInflater;
	private OnItemClickListener onItemClickListener;
	private DisableFilter disableFilter;


	public interface OnItemClickListener {
		void onClick(Peripheral peripheral);
	}

	public interface DisableFilter {
		boolean isDisable(Peripheral peripheral);
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
		final String deviceName = getItem(position).getName();
		return deviceName.hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout.item_list, parent, false);
		final Peripheral peripheral = list.get(position);

		final ImageView imageIcon = (ImageView) layout.findViewById(R.id.imageViewIcon);
		final TextView textViewDevName = (TextView) layout.findViewById(R.id.textViewDevName);
		final TextView textViewDevAddress = (TextView) layout.findViewById(R.id.textViewDevAddress);

		//feature 2: Display peripherals found and their RSSI values.
		textViewDevName.setText(peripheral.getName());
		textViewDevAddress.setText(peripheral.getAddress());

		//feature 3: greying out the Peripherals that do not fulfill the condition
		if (disableFilter != null && disableFilter.isDisable(peripheral)) {
			imageIcon.setImageResource(R.drawable.icon_gray);
			textViewDevName.setTextColor(Color.GRAY);
			textViewDevAddress.setTextColor(Color.GRAY);
			layout.setEnabled(false);
			return layout;
		}

		//otherwise, set the text color to black, and show the colorful icon
		imageIcon.setImageResource(R.drawable.icon);
		textViewDevName.setTextColor(Color.BLACK);
		textViewDevAddress.setTextColor(Color.BLACK);
		layout.setEnabled(true);

		//feature 4: allow user to connect to peripherals with Service
		if (onItemClickListener != null) {
			layout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onItemClickListener.onClick(peripheral);
				}
			});
		}

		return layout;
	}


	public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}


	public void setDisableFilter(DisableFilter disableFilter) {
		this.disableFilter = disableFilter;
	}


	public void setPeripheralList(List<Peripheral> list) {
		this.list = list;
	}

}
