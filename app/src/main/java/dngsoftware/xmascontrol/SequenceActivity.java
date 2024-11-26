package dngsoftware.xmascontrol;

import static dngsoftware.xmascontrol.Functions.convertJsonArrayToList;
import static dngsoftware.xmascontrol.Functions.getRESTCommand;
import static dngsoftware.xmascontrol.Functions.postRESTCommand;
import static dngsoftware.xmascontrol.Functions.secondsToTime;
import static dngsoftware.xmascontrol.Functions.showToast;
import static dngsoftware.xmascontrol.Functions.stepTimeToSeconds;
import static dngsoftware.xmascontrol.FppCommands.fppCommand;
import static dngsoftware.xmascontrol.FppCommands.fppSequence;
import static dngsoftware.xmascontrol.FppCommands.fppSequenceMeta;
import static dngsoftware.xmascontrol.FppCommands.fppStartSequenceAsPlaylistBody;
import static dngsoftware.xmascontrol.FppCommands.fppStatus;
import static dngsoftware.xmascontrol.FppCommands.fppStopPlaylist;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class SequenceActivity extends Activity {
    private String restHost = "127.0.0.1";
    private ScheduledExecutorService scheduler;
    ListAdapter adapter;
    TextView textTitle;
    TextView textInfo;
    ListView listView;
    private String selectedSequence;
    List<String> items;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        setContentView(R.layout.activity_sequence);

        textTitle =  findViewById(R.id.textTitle);
        textInfo  =  findViewById(R.id.textInfo);
        listView = findViewById(R.id.listView);

        if (extras != null) {
            restHost = extras.getString("HOST");
            loadSequenceList();
        } else {
            finish();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    void sendRest(String Cmd) {
        new Thread(() -> {
            try {
                getRESTCommand(restHost, Cmd);

            } catch (Exception ignored) {
            }
        }).start();

    }

    void sendRest(String Cmd, String dat) {
        new Thread(() -> {
            try {
                postRESTCommand(restHost, Cmd, dat);
            } catch (Exception ignored) {
            }
        }).start();
    }

    void loadSequenceList() {
        new Thread(() -> {
            try {

                JSONArray arrLists = new JSONArray(getRESTCommand(restHost, fppSequence));
                items = convertJsonArrayToList(arrLists);

                runOnUiThread(() -> {
                    adapter = new ListAdapter(this, items);
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener((parent, view, position, id) -> new Thread(() -> {
                        try {
                            selectedSequence = adapter.getItem(position).toString();
                            JSONObject sequenceMeta = new JSONObject(getRESTCommand(restHost, String.format(fppSequenceMeta, selectedSequence)));
                            JSONObject headers = new JSONObject(sequenceMeta.getString("variableHeaders"));

                            runOnUiThread(() -> {
                                try {

                                    textTitle.setText(selectedSequence);
                                    int duration = stepTimeToSeconds(sequenceMeta.getInt("StepTime"), sequenceMeta.getInt("NumFrames"));
                                    String info = "FPS: " + sequenceMeta.getInt("StepTime") +
                                            "\nDuration: " + secondsToTime(duration) +
                                            "\nChannels: " + sequenceMeta.getString("ChannelCount") +
                                            "\nVersion: " + sequenceMeta.getString("Version") +
                                            "\nRenderer: " + headers.getString("sp");

                                    textInfo.setText(info);
                                } catch (JSONException e) {
                                    Log.e("SequenceActivity", Log.getStackTraceString(e));
                                }
                            });
                        } catch (JSONException e) {
                            Log.e("SequenceActivity", Log.getStackTraceString(e));
                        }
                    }).start());
                });
            } catch (Exception e) {
                Log.e("SequenceActivity", Log.getStackTraceString(e));
            }
        }).start();
    }


    void selectSequence(String sequence, int remaining) {
        new Thread(() -> {
            try {
                JSONObject sequenceMeta = new JSONObject(getRESTCommand(restHost, String.format(fppSequenceMeta, sequence)));
                JSONObject headers = new JSONObject(sequenceMeta.getString("variableHeaders"));
                int duration = stepTimeToSeconds(sequenceMeta.getInt("StepTime"), sequenceMeta.getInt("NumFrames"));
                runOnUiThread(() -> {
                    try {
                        int tmpRemaining = remaining;
                        if (tmpRemaining < 0)
                        {
                            tmpRemaining = duration;
                        }
                        textTitle.setText(sequence);
                        String info = "FPS: " + sequenceMeta.getInt("StepTime") +
                                "\nDuration: " + secondsToTime(duration) +
                                "\nRemaining: " + secondsToTime(tmpRemaining) +
                                "\nChannels: " + sequenceMeta.getString("ChannelCount") +
                                "\nVersion: " + sequenceMeta.getString("Version") +
                                "\nRenderer: " + headers.getString("sp");
                        textInfo.setText(info);
                    } catch (Exception e) {
                        Log.e("SequenceActivity", Log.getStackTraceString(e));
                    }
                });
            } catch (JSONException e) {
                Log.e("SequenceActivity", Log.getStackTraceString(e));
            }
        }).start();
    }


    public void stopSequence(View v) {
        sendRest(fppStopPlaylist);
        textInfo.setText("");
        textTitle.setText("");
    }


    public void startSequence(View v) {
        if (adapter.getCount() != 0)
        {
            if (selectedSequence == null || selectedSequence.isEmpty()) {
                selectedSequence = adapter.getItem(0).toString();
                listView.setItemChecked(0, true);
                selectSequence(selectedSequence, -1);
            }
            sendRest(fppCommand, String.format(fppStartSequenceAsPlaylistBody, selectedSequence, "false"));
        }
    }


    void updateStatus() {
        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            try {
                JSONObject status = new JSONObject(getRESTCommand(restHost, fppStatus));
                String sequence = status.getString("current_sequence");
                int remaining = status.getInt("seconds_remaining");
                if (!sequence.isEmpty()) {
                    for (int i = 0; i < items.size(); i++) {
                        if (sequence.startsWith(items.get(i))) {
                            int finalI = i;
                            runOnUiThread(() -> {
                                listView.setItemChecked(finalI, true);
                                selectSequence(items.get(finalI), remaining);
                            });
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SequenceActivity", Log.getStackTraceString(e));
            }
        };
        scheduler.scheduleWithFixedDelay(task, 0, 5, TimeUnit.SECONDS);
    }


    public static class ListAdapter extends BaseAdapter {
        private final Context context;
        private final List<String> items;

        public ListAdapter(Context context, List<String> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_list, parent, false);
            }
            TextView sequenceName = view.findViewById(R.id.listitemname);
            sequenceName.setText(items.get(position));
            return view;
        }
    }

}


