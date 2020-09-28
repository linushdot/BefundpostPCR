package linushdot.befundpostpcr;

import android.accounts.Account;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;

import androidx.core.app.NotificationCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String URL = "https://ihrlabor.befundpost.at/ajax/getCovResult.php";
    private static final String PENDING = "kein Resultat verf";

    public static final String EXTRA_DO_NOT_NOTIFY = "do-not-notify";
    public static final String EXTRA_SYNC_ALL = "sync-all";

    ContentResolver contentResolver;

    TestDao testDao;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        contentResolver = context.getContentResolver();
        testDao = AppDatabase.getInstance(getContext().getApplicationContext()).testDao();
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs, ContentResolver contentResolver) {
        super(context, autoInitialize, allowParallelSyncs);
        this.contentResolver = contentResolver;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, final SyncResult syncResult) {
        final boolean doNotNotify = extras.getBoolean(EXTRA_DO_NOT_NOTIFY, false);
        final boolean syncAll = extras.getBoolean(EXTRA_SYNC_ALL, false);
        final List<Test> data = (syncAll) ? testDao.getAll() : testDao.getPending();
        if(data != null) {
            for (final Test test : data) {
                final RequestQueue queue = Volley.newRequestQueue(getContext());
                final StringRequest request = new StringRequest(Request.Method.POST, URL,
                        new Response.Listener<String>() {
                            @Override
                        public void onResponse(String response) {
                            if (!response.contains(PENDING)) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    response = Html.fromHtml(response, Html.FROM_HTML_MODE_LEGACY).toString();
                                } else {
                                    response = Html.fromHtml(response).toString();
                                }
                                final StringBuilder sb = new StringBuilder();
                                for(char c : response.toCharArray()) {
                                    if(c < 128) {
                                        sb.append(c);
                                    }
                                }
                                if(test.resultDateTime == null ||
                                        !Objects.equals(test.result, sb.toString())) {
                                    test.resultDateTime = new Date();
                                }
                                test.result = sb.toString();
                                new UpdateTask(testDao, test).execute();
                                syncResult.stats.numUpdates++;
                                if(!doNotNotify) {
                                    SyncAdapter.this.notify(test);
                                }
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            syncResult.stats.numIoExceptions++;
                        }
                    }) {
                    @Override
                    public String getBodyContentType() {
                        return "application/x-www-form-urlencoded";
                    }

                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        return ("resultNumber=" + test.code).getBytes();
                    }
                };
                queue.add(request);
            }
        }
    }

    private static final String CHANNEL_ID = "results";
    private static final String CHANNEL_NAME = "Results";
    private static final String CHANNEL_DESCRIPTION = "Test Results";

    private void notify(Test test) {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(test.getName())
                .setContentText(test.result)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManager notificationManager =
                (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(test.id, builder.build());
    }

    private boolean channelCreated = false;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(!channelCreated) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
                channel.setDescription(CHANNEL_DESCRIPTION);
                NotificationManager notificationManager = getContext().getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
                channelCreated = true;
            }
        }
    }

    public static class UpdateTask extends AsyncTask<Void,Void,Void> {
        private final WeakReference<TestDao> reference;
        private final Test test;

        public UpdateTask(TestDao dao, Test test) {
            this.reference = new WeakReference<>(dao);
            this.test = test;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final TestDao dao = reference.get();
            if(dao != null) {
                dao.updateAll(test);
            }
            return null;
        }
    }
}
