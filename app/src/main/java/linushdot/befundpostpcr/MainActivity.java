package linushdot.befundpostpcr;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final long POLL_FREQUENCY = 15 * 60; // in seconds

    private RecyclerView recyclerView;
    private TestAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private FloatingActionButton fabAdd, fabSync;
    private TextView updateText;

    private TestDao testDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String contentAuthority = getString(R.string.content_authority);
        final String accountType = getString(R.string.account_type);
        final String accountName = getString(R.string.account_name);
        final Account account = new Account(accountName, accountType);

        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        accountManager.addAccountExplicitly(account,null, null);
        ContentResolver.setIsSyncable(account, contentAuthority, 1);
        ContentResolver.setSyncAutomatically(account, contentAuthority, true);
        ContentResolver.addPeriodicSync(account, contentAuthority, Bundle.EMPTY, POLL_FREQUENCY);

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
                new ResetResultsTask(testDao, contentAuthority, account).execute();
            }
        });

        updateText = findViewById(R.id.update);
        new UpdateChecker(this).check(new UpdateChecker.Handler() {
            @Override
            public void needsUpdate(String url) {
                updateText.setText(String.format(getString(R.string.update_available), url));
                updateText.setVisibility(View.VISIBLE);
            }
        });
    }

    public static class CreateTask extends AsyncTask<Void,Void,Void> {
        private final WeakReference<TestDao> reference;
        private final Date date;
        private final String code;
        private final String contentAuthority;
        private final Account account;

        public CreateTask(TestDao dao, Date date, String code, String contentAuthority, Account account) {
            this.reference = new WeakReference<>(dao);
            this.date = date;
            this.code = code;
            this.contentAuthority = contentAuthority;
            this.account = account;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final TestDao dao = reference.get();
            if(dao != null) {
                final Test test = new Test();
                test.date = date;
                test.code = code;
                dao.insertAll(test);
                ContentResolver.requestSync(account, contentAuthority, Bundle.EMPTY);
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
        private final String contentAuthority;
        private final Account account;

        public ResetResultsTask(TestDao dao, String contentAuthority, Account account) {
            this.reference = new WeakReference<>(dao);
            this.contentAuthority = contentAuthority;
            this.account = account;
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
            ContentResolver.requestSync(account, contentAuthority, bundle);
            return null;
        }
    }
}