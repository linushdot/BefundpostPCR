package linushdot.befundpostpcr;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String ACCOUNT_TYPE = "linushdot.befundpostpcr";
    public static final String ACCOUNT = "Befundpost PCR";
    public static final String AUTHORITY = "linushdot.befundpostpcr.provider";
    public static final long POLL_FREQUENCY = 15 * 60; // in seconds

    private static final Account account = new Account(ACCOUNT, ACCOUNT_TYPE);

    private RecyclerView recyclerView;
    private TestAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private FloatingActionButton fabAdd, fabSync;

    private TestDao testDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        accountManager.addAccountExplicitly(account,null, null);
        ContentResolver.setIsSyncable(account, AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
        ContentResolver.addPeriodicSync(account, AUTHORITY, Bundle.EMPTY, POLL_FREQUENCY);

        recyclerView = (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(false);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new TestAdapter();
        mAdapter.setItemListener(new TestAdapter.ItemListener() {
            @Override
            public void onClick(Test test) {
                if(test.toBeDeleted) {
                    final Test newTest = test.copy();
                    newTest.toBeDeleted = false;
                    new UpdateTask(testDao, newTest).execute();
                }
            }
            @Override
            public boolean onLongClick(Test test) {
                if(!test.toBeDeleted) {
                    final Test newTest = test.copy();
                    newTest.toBeDeleted = true;
                    new UpdateTask(testDao, newTest).execute();
                } else {
                    new DeleteTask(testDao, test).execute();
                }
                return true;
            }
        });
        recyclerView.setAdapter(mAdapter);

        testDao = AppDatabase.getInstance(getApplicationContext()).testDao();
        testDao.getAllLive().observe(this, new Observer<List<Test>>() {
            @Override
            public void onChanged(List<Test> tests) {
                mAdapter.submitList(tests);
            }
        });

        fabAdd = (FloatingActionButton) findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CreateTestDialogFragment(testDao).show(getSupportFragmentManager(), "create");
            }
        });
        fabSync = (FloatingActionButton) findViewById(R.id.fabSync);
        fabSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ResetResultsTask(testDao).execute();
            }
        });
    }

    public static class CreateTask extends AsyncTask<Void,Void,Void> {
        private final WeakReference<TestDao> reference;
        private final Date date;
        private final String code;

        public CreateTask(TestDao dao, Date date, String code) {
            this.reference = new WeakReference<>(dao);
            this.date = date;
            this.code = code;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final TestDao dao = reference.get();
            if(dao != null) {
                final Test test = new Test();
                test.date = date;
                test.code = code;
                dao.insertAll(test);
                ContentResolver.requestSync(account, AUTHORITY, Bundle.EMPTY);
            }
            return null;
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
            if(dao != null && test != null) {
                dao.updateAll(test);
            }
            return null;
        }
    }

    public static class DeleteTask extends AsyncTask<Void,Void,Void> {
        private final WeakReference<TestDao> reference;
        private final Test test;

        public DeleteTask(TestDao dao, Test test) {
            this.reference = new WeakReference<>(dao);
            this.test = test;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final TestDao dao = reference.get();
            if(dao != null && test != null) {
                dao.delete(test);
            }
            return null;
        }
    }

    public static class ResetResultsTask extends AsyncTask<Void,Void,Void> {
        private final WeakReference<TestDao> reference;

        public ResetResultsTask(TestDao dao) {
            this.reference = new WeakReference<>(dao);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final TestDao dao = reference.get();
            if(dao != null) {
                final List<Test> list = dao.getAll();
                if(list != null) {
                    for(final Test test : list) {
                        test.result = null;
                    }
                    dao.updateAll(list.toArray(new Test[0]));
                }
            }
            final Bundle bundle = new Bundle();
            bundle.putBoolean(SyncAdapter.EXTRA_DO_NOT_NOTIFY, true);
            bundle.putBoolean(SyncAdapter.EXTRA_SYNC_ALL, true);
            ContentResolver.requestSync(account, AUTHORITY, bundle);
            return null;
        }
    }
}