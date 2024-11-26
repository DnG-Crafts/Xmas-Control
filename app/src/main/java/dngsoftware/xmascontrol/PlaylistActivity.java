package dngsoftware.xmascontrol;

import static android.net.Uri.encode;
import static dngsoftware.xmascontrol.FppCommands.fppCommand;
import static dngsoftware.xmascontrol.FppCommands.fppStartPlaylistAtBody;
import static dngsoftware.xmascontrol.Functions.GetSetting;
import static dngsoftware.xmascontrol.Functions.SaveSetting;
import static dngsoftware.xmascontrol.Functions.convertJsonArrayToList;
import static dngsoftware.xmascontrol.Functions.getRESTCommand;
import static dngsoftware.xmascontrol.Functions.postRESTCommand;
import static dngsoftware.xmascontrol.Functions.secondsToTime;
import static dngsoftware.xmascontrol.Functions.showToast;
import static dngsoftware.xmascontrol.Functions.stepTimeToFPS;
import static dngsoftware.xmascontrol.FppCommands.fppNextPlaylistItem;
import static dngsoftware.xmascontrol.FppCommands.fppPlaylist;
import static dngsoftware.xmascontrol.FppCommands.fppPlaylists;
import static dngsoftware.xmascontrol.FppCommands.fppPrevPlaylistItem;
import static dngsoftware.xmascontrol.FppCommands.fppSequenceMeta;
import static dngsoftware.xmascontrol.FppCommands.fppStatus;
import static dngsoftware.xmascontrol.FppCommands.fppStopPlaylist;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class PlaylistActivity extends Activity {
    private String restHost = "127.0.0.1";
    private ScheduledExecutorService scheduler;
    ListAdapter adapter;
    TextView textTitle;
    TextView textInfo;
    ListView listView;
    Spinner spinner;
    CheckBox repeat;
    Context context = this;
    private int selectedPlaylist;
    private int selectedSequence = -1;
    private final ArrayList<JSONObject> items = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        setContentView(R.layout.activity_playlist);

        textTitle = findViewById(R.id.textTitle);
        textInfo = findViewById(R.id.textInfo);
        listView = findViewById(R.id.listView);
        spinner = findViewById(R.id.spinner);
        repeat = findViewById(R.id.checkBox);
        repeat.setChecked(GetSetting(context, "repeat", false));
        repeat.setOnClickListener(v -> SaveSetting(context, "repeat", repeat.isChecked()));
        if (extras != null) {
            restHost = extras.getString("HOST");
            loadPlaylists();
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

    void loadSequenceList(String cPlaylist) {
        if (cPlaylist != null && !cPlaylist.isEmpty()) {

            new Thread(() -> {
                try {

                    JSONObject playList = new JSONObject(getRESTCommand(restHost, String.format(fppPlaylist, cPlaylist)));
                    JSONArray plistArr = playList.getJSONArray("mainPlaylist");
                    items.clear();
                    for (int i = 0; i < plistArr.length(); i++) {
                        items.add(plistArr.getJSONObject(i));
                    }

                    runOnUiThread(() -> {
                        listView.invalidate();
                        listView.setAdapter(null);
                        adapter = new ListAdapter(this, items);
                        listView.setAdapter(adapter);
                        listView.setOnItemClickListener((parent, view, position, id) -> new Thread(() -> {
                            try {
                                selectedSequence = position + 1;
                                JSONObject item = new JSONObject(adapter.getItem(position).toString());
                                JSONObject sequenceMeta = new JSONObject(getRESTCommand(restHost, String.format(fppSequenceMeta, item.getString("sequenceName"))));
                                JSONObject headers = new JSONObject(sequenceMeta.getString("variableHeaders"));

                                runOnUiThread(() -> {
                                    try {
                                        textTitle.setText(item.getString("sequenceName"));
                                        String info = "FPS: " + stepTimeToFPS(sequenceMeta.getInt("StepTime")) +
                                                "\nDuration: " + item.getString("duration") +
                                                "\nChannels: " + sequenceMeta.getString("ChannelCount") +
                                                "\nVersion: " + sequenceMeta.getString("Version") +
                                                "\nRenderer: " + headers.getString("sp");
                                        textInfo.setText(info);
                                    } catch (JSONException e) {
                                        Log.e("PlaylistActivity", Log.getStackTraceString(e));
                                    }
                                });
                            } catch (JSONException e) {
                                Log.e("PlaylistActivity", Log.getStackTraceString(e));
                            }
                        }).start());
                    });
                } catch (Exception e) {
                    Log.e("PlaylistActivity", Log.getStackTraceString(e));
                }
            }).start();
        }
    }


    void selectSequence(String sequence, int remaining) {
        new Thread(() -> {
            try {
                JSONObject item = new JSONObject(sequence);
                JSONObject sequenceMeta = new JSONObject(getRESTCommand(restHost, String.format(fppSequenceMeta, item.getString("sequenceName"))));
                JSONObject headers = new JSONObject(sequenceMeta.getString("variableHeaders"));
                runOnUiThread(() -> {
                    try {
                        textTitle.setText(item.getString("sequenceName"));
                        String info = "FPS: " + stepTimeToFPS(sequenceMeta.getInt("StepTime")) +
                                "\nDuration: " + secondsToTime(Math.round(Float.parseFloat(item.getString("duration")))) +
                                "\nRemaining: " + secondsToTime(remaining) +
                                "\nChannels: " + sequenceMeta.getString("ChannelCount") +
                                "\nVersion: " + sequenceMeta.getString("Version") +
                                "\nRenderer: " + headers.getString("sp");

                        textInfo.setText(info);

                    } catch (Exception e) {
                        Log.e("PlaylistActivity", Log.getStackTraceString(e));
                    }
                });
            } catch (JSONException e) {
                Log.e("PlaylistActivity", Log.getStackTraceString(e));
            }
        }).start();
    }


    void loadPlaylists() {
        new Thread(() -> {
            try {
                JSONArray arrLists = new JSONArray(getRESTCommand(restHost, fppPlaylists));
                if (arrLists.length() != 0) {
                    List<String> pLists = convertJsonArrayToList(arrLists);
                    runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pLists);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adapter);
                        spinner.setSelection(selectedPlaylist);
                        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                                String sItem = spinner.getSelectedItem().toString();
                                selectedPlaylist = spinner.getSelectedItemPosition();
                                if (!sItem.isEmpty()) {
                                    try {
                                        loadSequenceList(sItem);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parentView) {}
                        });
                    });
                } else {
                    showToast(this, R.string.no_playlists_found);
                }
            } catch (Exception ignored) {
            }
        }).start();
    }


    public void stopPlayList(View v) {
        sendRest(fppStopPlaylist);
        textInfo.setText("");
        textTitle.setText("");
    }

    public void nextPlaylistItem(View v) {
        sendRest(fppNextPlaylistItem);
    }

    public void prevPlaylistItem(View v) {
        sendRest(fppPrevPlaylistItem);
    }

    public void startPlayList(View v) {
        if (selectedSequence <= 0)
        {
            selectedSequence = 1;
        }
        sendRest(fppCommand , String.format(fppStartPlaylistAtBody, encode(spinner.getSelectedItem().toString(), "UTF-8") ,selectedSequence, repeat.isChecked()));
    }


    void updateStatus() {
        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            try {
                JSONObject status = new JSONObject(getRESTCommand(restHost, fppStatus));
                String sequence = status.getString("current_sequence");
                if (!sequence.isEmpty()) {
                    int remaining = status.getInt("seconds_remaining");
                    for (int i = 0; i < items.size(); i++) {
                        if (sequence.equals(items.get(i).getString("sequenceName"))) {
                            int finalI = i;
                            runOnUiThread(() -> {
                                listView.setItemChecked(finalI, true);
                                selectSequence(items.get(finalI).toString(), remaining);
                            });
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("PlaylistActivity", Log.getStackTraceString(e));
            }
        };
        scheduler.scheduleWithFixedDelay(task, 0, 5, TimeUnit.SECONDS);
    }


    public static class ListAdapter extends BaseAdapter {
        private final Context context;
        private final ArrayList<JSONObject> items;

        public ListAdapter(Context context, ArrayList<JSONObject> items) {
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
            JSONObject item = items.get(position);
            TextView sequenceName = view.findViewById(R.id.listitemname);
            try {
                sequenceName.setText(item.getString("sequenceName"));
            } catch (JSONException ignored) {}
            return view;
        }
    }
}
