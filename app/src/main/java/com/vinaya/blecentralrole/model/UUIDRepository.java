package com.vinaya.blecentralrole.model;

import android.content.res.Resources;

import com.vinaya.blecentralrole.R;

import java.util.UUID;

/**
 * For hiding the implementation on how to get the UUID values
 * TODO: in the future the UUID may not be hard-coded but by webservice, or etc.
 */
public class UUIDRepository {

	private Resources resources;

	public UUIDRepository(Resources resources) {
		this.resources = resources;
	}

	public UUID getServiceID() {
		final String uuid = resources.getString(R.string.service_uuid);
		return UUID.fromString(uuid);
	}

	public UUID getTXCharacteristic() {
		final String uuid = resources.getString(R.string.tx_char_uuid);
		return UUID.fromString(uuid);
	}

	public UUID getRXCharacteristic() {
		final String uuid = resources.getString(R.string.rx_char_uuid);
		return UUID.fromString(uuid);
	}
}

