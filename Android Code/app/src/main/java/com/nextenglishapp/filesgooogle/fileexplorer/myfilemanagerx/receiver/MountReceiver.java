/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.RootsCache;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.Utils;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.ExternalStorageProvider;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.provider.UsbStorageProvider;

public class MountReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		RootsCache.updateRoots(context, ExternalStorageProvider.AUTHORITY);
		if(Utils.checkUSBDevices()) {
			RootsCache.updateRoots(context, UsbStorageProvider.AUTHORITY);
		}
	}
}
