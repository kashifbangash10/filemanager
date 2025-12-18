package com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.transfer;

import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.BuildConfig;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.DocumentsActivity;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.DocumentsApplication;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.R;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.RootsCache;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.model.RootInfo;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.service.TransferService;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.setting.SettingsActivity;
import com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.transfer.model.TransferStatus;

import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.NotificationUtils.RECEIVE_CHANNEL;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.NotificationUtils.TRANSFER_CHANNEL;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.Utils.EXTRA_ROOT;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.misc.Utils.isWatch;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.transfer.TransferHelper.ACTION_STOP_LISTENING;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.transfer.TransferHelper.ACTION_STOP_TRANSFER;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.transfer.TransferHelper.EXTRA_ID;
import static com.nextenglishapp.filesgooogle.fileexplorer.myfilemanagerx.transfer.TransferHelper.EXTRA_TRANSFER;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    private static final int NOTIFICATION_ID = 1;
    private Service mService;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private PendingIntent mIntent;

    private boolean mListening = false;
    private int mNumTransfers = 0;

    private int mNextId = 2;

    /**
     * Create a notification manager for the specified service
     * @param service service to manage
     */
    public NotificationHelper(Service service) {
        mService = service;
        mNotificationManager = (NotificationManager) mService.getSystemService(
                Service.NOTIFICATION_SERVICE);
        RootsCache roots = DocumentsApplication.getRootsCache(mService);
        RootInfo root = roots.getTransferRoot();

        long when = System.currentTimeMillis();
        CharSequence stopText = mService.getString(R.string.ftp_notif_stop_server);
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_ROOT, root);
        Intent notificationIntent = new Intent(mService, DocumentsActivity.class);
        notificationIntent.setPackage(BuildConfig.APPLICATION_ID);
        notificationIntent.setData(root.getUri());
        notificationIntent.putExtras(args);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mIntent = PendingIntent.getActivity(mService, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);


        mBuilder = new NotificationCompat.Builder(mService, TRANSFER_CHANNEL)
                .setContentIntent(mIntent)
                .setContentTitle(mService.getString(R.string.service_transfer_server_title))
                .setColor(SettingsActivity.getPrimaryColor())
                .setSmallIcon(R.drawable.ic_stat_server)
                .setLocalOnly(true)
                .setWhen(when)
                .setOngoing(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setShowWhen(false);

        Intent stopIntent = new Intent(ACTION_STOP_LISTENING);
        stopIntent.setPackage(BuildConfig.APPLICATION_ID);
        stopIntent.putExtras(args);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(mService, 0,
                stopIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        boolean isWatch = isWatch(mService);
        NotificationCompat.Action.Builder actionBuilder =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_action_stop, stopText, stopPendingIntent);

        if(isWatch){
            final NotificationCompat.Action.WearableExtender inlineActionForWear =
                    new NotificationCompat.Action.WearableExtender()
                            .setHintDisplayActionInline(true)
                            .setHintLaunchesActivity(false);
            actionBuilder.extend(inlineActionForWear);
            mBuilder.extend(new NotificationCompat.WearableExtender()
                    .setHintContentIntentLaunchesActivity(true));
        }

        NotificationCompat.Action stopAction = actionBuilder.build();
        mBuilder.addAction(stopAction);
    }

    /**
     * Retrieve the next unique integer for a transfer
     * @return new ID
     *
     * The notification with ID equal to 1 is for the persistent notification
     * shown while the service is active.
     */
    public synchronized int nextId() {
        return mNextId++;
    }

    /**
     * Indicate that the server is listening for transfers
     */
    public synchronized void startListening() {
        mListening = true;
        updateNotification();
    }

    /**
     * Indicate that the server has stopped listening for transfers
     */
    public synchronized void stopListening() {
        mListening = false;
        stop();
    }

    /**
     * Stop the service if no tasks are active
     */
    public synchronized void stopService() {
        stop();
    }

    /**
     * Remove a notification
     */
    public void removeNotification(int id) {
        mNotificationManager.cancel(id);
    }

    /**
     * Add a new transfer
     */
    synchronized void addTransfer(TransferStatus transferStatus) {
        mNumTransfers++;
        updateNotification();


        removeNotification(transferStatus.getId());
    }

    /**
     * Update a transfer in progress
     */
    synchronized void updateTransfer(TransferStatus transferStatus, Intent intent) {
        if (transferStatus.isFinished()) {
            Log.i(TAG, String.format("#%d finished", transferStatus.getId()));


            mNotificationManager.cancel(transferStatus.getId());


            if (transferStatus.getState() != TransferStatus.State.Succeeded ||
                    transferStatus.getBytesTotal() > 0) {


                CharSequence contentText;
                int icon;

                if (transferStatus.getState() == TransferStatus.State.Succeeded) {
                    contentText = mService.getString(
                            R.string.service_transfer_status_success,
                            transferStatus.getRemoteDeviceName()
                    );
                    icon = R.drawable.ic_action_done;
                } else {
                    contentText = mService.getString(
                            R.string.service_transfer_status_error,
                            transferStatus.getRemoteDeviceName(),
                            transferStatus.getError()
                    );
                    icon = R.drawable.ic_action_close;
                }


                boolean notifications = TransferHelper.makeNotificationSound();
                NotificationCompat.Builder builder = new NotificationCompat.Builder(mService, RECEIVE_CHANNEL)
                        .setDefaults(notifications ? NotificationCompat.DEFAULT_ALL : 0)
                        .setContentIntent(mIntent)
                        .setContentTitle(mService.getString(R.string.service_transfer_server_title))
                        .setContentText(contentText)
                        .setSmallIcon(icon);


                if (transferStatus.getState() == TransferStatus.State.Failed &&
                        transferStatus.getDirection() == TransferStatus.Direction.Send) {



                    intent.setClass(mService, TransferService.class);
                    intent.putExtra(EXTRA_ID, transferStatus.getId());


                    builder.addAction(
                            new NotificationCompat.Action.Builder(
                                    R.drawable.ic_refresh,
                                    mService.getString(R.string.service_transfer_action_retry),
                                    PendingIntent.getService(
                                            mService, transferStatus.getId(),
                                            intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
                                    )
                            ).build()
                    );
                }


                mNotificationManager.notify(transferStatus.getId(), builder.build());
            }

            mNumTransfers--;


            if (stop()) {
                return;
            }


            updateNotification();

        } else {


            CharSequence contentText;
            int icon;

            if (transferStatus.getDirection() == TransferStatus.Direction.Receive) {
                contentText = mService.getString(
                        R.string.service_transfer_status_receiving,
                        transferStatus.getRemoteDeviceName()
                );
                icon = android.R.drawable.stat_sys_download;
            } else {
                contentText = mService.getString(
                        R.string.service_transfer_status_sending,
                        transferStatus.getRemoteDeviceName()
                );
                icon = android.R.drawable.stat_sys_upload;
            }


            Intent stopIntent = new Intent(mService, TransferService.class)
                    .setAction(ACTION_STOP_TRANSFER)
                    .putExtra(EXTRA_TRANSFER, transferStatus.getId());


            mNotificationManager.notify(
                    transferStatus.getId(),
                    new NotificationCompat.Builder(mService, RECEIVE_CHANNEL)
                            .setContentIntent(mIntent)
                            .setContentTitle(mService.getString(R.string.service_transfer_title))
                            .setContentText(contentText)
                            .setOngoing(true)
                            .setProgress(100, transferStatus.getProgress(), false)
                            .setSmallIcon(icon)
                            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                            .addAction(
                                    new NotificationCompat.Action.Builder(
                                            R.drawable.ic_action_stop,
                                            mService.getString(R.string.service_transfer_action_stop),
                                            PendingIntent.getService(mService, transferStatus.getId(), stopIntent, PendingIntent.FLAG_IMMUTABLE)
                                    ).build()
                            )
                            .build()
            );
        }
    }

    /**
     * Create a notification for URLs
     * @param url full URL
     */
    void showUrl(String url) {
        int id = nextId();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                mService,
                id,
                new Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                PendingIntent.FLAG_IMMUTABLE
        );
        mNotificationManager.notify(
                id,
                new NotificationCompat.Builder(mService, RECEIVE_CHANNEL)
                        .setContentIntent(pendingIntent)
                        .setContentTitle(mService.getString(R.string.service_transfer_notification_url))
                        .setContentText(url)
                        .setSmallIcon(R.drawable.ic_url)
                        .build()
        );
    }

//    @SuppressLint("ForegroundServiceType")
//    private void updateNotification() {
//        Log.i(TAG, String.format("updating notification with %d transfer(s)...", mNumTransfers));
//
//        if (mNumTransfers == 0) {
//            mBuilder.setContentText(mService.getString(
//                    R.string.service_transfer_server_listening_text));
//        } else {
//            mBuilder.setContentText(mService.getResources().getQuantityString(
//                    R.plurals.service_transfer_server_transferring_text,
//                    mNumTransfers, mNumTransfers));
//        }
//        mService.startForeground(NOTIFICATION_ID, mBuilder.build());
//    }

//    public NotificationHelper(Service service) {
//        mService = service;
//        mNotificationManager = (NotificationManager) mService.getSystemService(Service.NOTIFICATION_SERVICE);
//
//        // Ensure notification channel is created
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    TRANSFER_CHANNEL,
//                    "Transfer Notifications",
//                    NotificationManager.IMPORTANCE_LOW);
//            mNotificationManager.createNotificationChannel(channel);
//        }
//
//        // Set up notification builder
//        mBuilder = new NotificationCompat.Builder(mService, TRANSFER_CHANNEL)
//                .setContentTitle("Your Title")
//                .setContentText("Your Content Text")
//                .setSmallIcon(R.drawable.ic_stat_server) // Make sure this resource exists
//                .setOngoing(true);
//    }
    @SuppressLint({"ForegroundServiceType", "WrongConstant", "NewApi"})
    private void updateNotification() {
        Log.i(TAG, String.format("Updating notification with %d transfer(s)...", mNumTransfers));

        if (mNumTransfers == 0) {
            mBuilder.setContentText(mService.getString(R.string.service_transfer_server_listening_text));
        } else {
            mBuilder.setContentText(mService.getResources().getQuantityString(
                    R.plurals.service_transfer_server_transferring_text, mNumTransfers, mNumTransfers));
        }

        try {



            if (SDK_INT >= 33) {

//                mService.startForeground(NOTIFICATION_ID, mBuilder.build());
            } else if (SDK_INT >= 30) {
                mService.startForeground(NOTIFICATION_ID, mBuilder.build());
            } else {
                mService.startForeground(NOTIFICATION_ID, mBuilder.build());
            }



//            mService.startForeground(NOTIFICATION_ID, mBuilder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to start foreground service: Missing foregroundServiceType in manifest or missing permissions.", e);
        }
    }
//    @SuppressLint("ForegroundServiceType")
//    private void updateNotification() {
//        Log.i(TAG, String.format("Updating notification with %d transfer(s)...", mNumTransfers));
//
//        if (mNumTransfers == 0) {
//            mBuilder.setContentText(mService.getString(R.string.service_transfer_server_listening_text));
//        } else {
//            mBuilder.setContentText(mService.getResources().getQuantityString(
//                    R.plurals.service_transfer_server_transferring_text,
//                    mNumTransfers, mNumTransfers));
//        }
//
//        try {
//            mService.startForeground(NOTIFICATION_ID, mBuilder.build());
//        } catch (SecurityException e) {
//            Log.e(TAG, "Failed to start foreground service: Missing foregroundServiceType in manifest or missing permissions.", e);
//        }
//    }


    private boolean stop() {
        if (!mListening && mNumTransfers == 0) {
            Log.i(TAG, "not listening and no transfers, shutting down...");

            mService.stopSelf();
            return true;
        }
        return false;
    }
}