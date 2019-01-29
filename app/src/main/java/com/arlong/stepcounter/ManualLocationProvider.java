/*
 * Created on Mar 9, 2012
 * Author: Paul Woelfel
 * Email: frig@frig.at
 */
package com.arlong.stepcounter;

import android.content.Context;

import java.util.Date;

public class ManualLocationProvider extends LocationProviderImpl {

	public ManualLocationProvider(Context ctx) {
		this(ctx, LocationServiceFactory.getLocationService());
	}

	public ManualLocationProvider(Context ctx, LocationService locationService) {
		super(ctx, locationService);

	}

	public void updateCurrentPosition(float x, float y) {
		loc = new Location(getProviderName(), x, y, 0, new Date());
		if (locationService != null)
			locationService.updateLocation(loc);

		if (listener != null) {
			listener.onLocationChange(loc);
		}
	}

}
