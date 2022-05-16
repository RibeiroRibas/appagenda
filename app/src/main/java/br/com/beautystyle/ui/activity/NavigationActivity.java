package br.com.beautystyle.ui.activity;

import static br.com.beautystyle.ui.activity.ContantsActivity.REQUEST_CODE_NEW_EVENT;
import static br.com.beautystyle.ui.fragment.ConstantFragment.KEY_CLIENT;
import static br.com.beautystyle.ui.fragment.ConstantFragment.KEY_JOB;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.example.beautystyle.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.util.List;

import br.com.beautystyle.AddExpenseWorker;
import br.com.beautystyle.ViewModel.CalendarViewModel;
import br.com.beautystyle.ViewModel.EventViewModel;
import br.com.beautystyle.database.room.references.EventWithJobs;
import br.com.beautystyle.model.entities.Client;
import br.com.beautystyle.model.entities.Event;
import br.com.beautystyle.model.entities.Job;
import br.com.beautystyle.repository.EventRepository;
import br.com.beautystyle.repository.ResultsCallBack;
import br.com.beautystyle.ui.fragment.ConstantFragment;
import br.com.beautystyle.ui.fragment.event.EventListFragment;
import br.com.beautystyle.ui.fragment.expense.ExpenseListFragment;
import br.com.beautystyle.ui.fragment.report.ReportFragment;
import br.com.beautystyle.util.CalendarUtil;

public class NavigationActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> activityResultLauncher;
    private BottomNavigationView bottomNavigationView;
    private CalendarViewModel calendarViewModel;
    private EventRepository repository;
    private static final String ID_EVENT_FRAGMENT = "home";
    private static final String ID_REPORT_FRAGMENT = "report";
    private static final String ID_EXPENSE_FRAGMENT = "expense";
    private final WorkRequest workRequest = new OneTimeWorkRequest.Builder(AddExpenseWorker.class).build();
    private EventViewModel eventViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        if(CalendarUtil.selectedDate==null)
        CalendarUtil.selectedDate = LocalDate.now();

        initWidgets();

        removeShadowBottomNavigation();
        startHomeFragment(savedInstanceState);

        setBottomNavigationListener();
        setFabNavigationListener();

        registerActivityResult(); // SAVE NEW EVENT

        calendarObserve(); // CLICK ON CALENDAR NAVIGATION
        WorkManager.getInstance(this).enqueue(workRequest);
    }

    private void initWidgets() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        bottomNavigationView = findViewById(R.id.activity_navigation_bottom);
        repository = new EventRepository(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    private void removeShadowBottomNavigation() {

        bottomNavigationView.setBackground(null);
    }

    private void startHomeFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_navigation_container,
                            EventListFragment.class,null)
                    .commit();
        }
    }

    private void setBottomNavigationListener() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case (R.id.home):
                    CalendarUtil.selectedDate = LocalDate.now();
                    replaceContainer(ID_EVENT_FRAGMENT, new EventListFragment());
                    return true;
                case (R.id.report):
//                    CreateListsUtil.reportListRef = false;
                    replaceContainer(ID_REPORT_FRAGMENT, new ReportFragment());
                    return true;
                case (R.id.expense):
                    CalendarUtil.monthValue = LocalDate.now().getMonthValue();
                    CalendarUtil.year = LocalDate.now().getYear();
                    replaceContainer(ID_EXPENSE_FRAGMENT, new ExpenseListFragment());
                    return true;
                case (R.id.calendar):
                    calendarViewModel.inflateCalendar(this);
                    return true;
            }
            return false;
        });
    }

    private void replaceContainer(String id, Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_navigation_container, fragment,null)
                .commit();
    }

    private void setFabNavigationListener () {
        FloatingActionButton fabNavigation = findViewById(R.id.activity_navigation_fab_new_event);
        fabNavigation.setOnClickListener(V -> {
            Intent intent = new Intent(this, NewEventActivity.class);
            activityResultLauncher.launch(intent);
        });
    }

    private void calendarObserve() {
        calendarViewModel = new ViewModelProvider(this).get(CalendarViewModel.class);
        calendarViewModel.getDate().observe(this, this::setDate);
    }

    private void setDate(LocalDate date) {
        CalendarUtil.selectedDate = date;
        replaceContainer(ID_EVENT_FRAGMENT,
                new EventListFragment());
        bottomNavigationView.getMenu().getItem(0).setChecked(true);
    }

    private void registerActivityResult() {
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == REQUEST_CODE_NEW_EVENT) {
                Intent intent = result.getData();
                if (intent != null) {
                    List<Job> jobList = (List<Job>) intent.getSerializableExtra(KEY_JOB);
                    EventWithJobs eventWithJobs = (EventWithJobs) intent.getSerializableExtra(ConstantFragment.KEY_INSERT_EVENT);
                    Client client = (Client) intent.getSerializableExtra(KEY_CLIENT);
                    insertEvent(eventWithJobs.getEvent(), jobList, client);
                }
            }
        });
    }

    private void insertEvent(Event event, List<Job> jobList, Client client) {
            repository.insert(event, jobList, client, new ResultsCallBack<Event>() {
                @Override
                public void onSuccess(Event event) {
                    eventViewModel.add(event.getEventDate());
                }

                @Override
                public void onError(String erro) {
                    showErrorMessage(erro);
                }
            });
    }

    private void showErrorMessage(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }
}