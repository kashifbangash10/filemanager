package com.example.apps.solidfilemamnager.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.apps.solidfilemamnager.misc.ConnectionUtils;
import com.example.apps.solidfilemamnager.misc.NotificationUtils;
import com.example.apps.solidfilemamnager.service.ConnectionsService;
import com.example.apps.solidfilemamnager.service.TransferService;
import com.example.apps.solidfilemamnager.transfer.TransferHelper;

import static com.example.apps.solidfilemamnager.misc.ConnectionUtils.ACTION_FTPSERVER_STARTED;
import static com.example.apps.solidfilemamnager.misc.ConnectionUtils.ACTION_FTPSERVER_STOPPED;
import static com.example.apps.solidfilemamnager.misc.ConnectionUtils.ACTION_START_FTPSERVER;
import static com.example.apps.solidfilemamnager.misc.ConnectionUtils.ACTION_STOP_FTPSERVER;
import static com.example.apps.solidfilemamnager.misc.NotificationUtils.FTP_NOTIFICATION_ID;
import static com.example.apps.solidfilemamnager.transfer.TransferHelper.ACTION_START_LISTENING;
import static com.example.apps.solidfilemamnager.transfer.TransferHelper.ACTION_STOP_LISTENING;

public class ConnectionsReceiver extends BroadcastReceiver {

    static final String TAG = ConnectionsReceiver.class.getSimpleName();

    public ConnectionsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (ACTION_START_FTPSERVER.equals(action)) {
            Intent serverService = new Intent(context, ConnectionsService.class);
            Bundle extras = intent.getExtras();
            if (extras != null) {
                serverService.putExtras(extras);
            }
            if (!ConnectionUtils.isServerRunning(context)) {
                context.startService(serverService);
            }
        } else if (ACTION_STOP_FTPSERVER.equals(action)) {
            Intent serverService = new Intent(context, ConnectionsService.class);
            Bundle extras = intent.getExtras();
            if (extras != null) {
                serverService.putExtras(extras);
            }
            context.stopService(serverService);
        } else if (ACTION_FTPSERVER_STARTED.equals(action)) {
            NotificationUtils.createFtpNotification(context, intent, FTP_NOTIFICATION_ID);
        } else if (ACTION_FTPSERVER_STOPPED.equals(action)) {
            NotificationUtils.removeNotification(context, FTP_NOTIFICATION_ID);
        } else if (ACTION_START_LISTENING.equals(action)) {
            Intent serverService = new Intent(context, TransferService.class);
            serverService.setAction(action);
            if (!TransferHelper.isServerRunning(context)) {
                context.startService(serverService);
            }
        } else if (ACTION_STOP_LISTENING.equals(action)) {
            Intent serverService = new Intent(context, TransferService.class);
            serverService.setAction(action);
            context.startService(serverService);
        }
    }
}
