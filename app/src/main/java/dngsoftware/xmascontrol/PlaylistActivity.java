package dngsoftware.xmascontrol;

import static android.net.Uri.encode;
import static dngsoftware.xmascontrol.FppCommands.fppCommand;
import static dngsoftware.xmascontrol.FppCommands.fppStartPlaylistAtBody;
import static dngsoftware.xmascontrol.Functions.GetSetting;
import static dngsoftware.xmascontrol.Functions.SaveSetting;
import static dngsoftware.xmascontrol.Functions.convertJsonArrayToList;
import static dngsoftware.xmascontrol.Functions.getRESTCommand;
import static dngsoftware.xmascontrol.Functions.postRESTCommand;
import static dngsoftware.xmascontrol.Functions.secondsToLongTime;
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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
    RecycleAdapter recycleAdapter;
    TextView textTitle;
    TextView textInfo;
    RecyclerView recyclerView;
    Spinner spinner;
    CheckBox repeat;
    Context context = this;
    private int selectedPlaylist;
    private int selectedSequence = -1;
    private final ArrayList<JSONObject> items = new ArrayList<>();
    private String FPPAuth = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        setContentView(R.layout.activity_playlist);
        textTitle = findViewById(R.id.textTitle);
        textInfo = findViewById(R.id.textInfo);
        recyclerView = findViewById(R.id.recyclerview);
        spinner = findViewById(R.id.spinner);
        repeat = findViewById(R.id.checkBox);
        repeat.setChecked(GetSetting(context, "repeat", false));
        repeat.setOnClickListener(v -> SaveSetting(context, "repeat", repeat.isChecked()));
        if (extras != null) {
            if (extras.containsKey("AUTH"))
            {
                FPPAuth = extras.getString("AUTH");
            }
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
                getRESTCommand(restHost, Cmd, FPPAuth);

            } catch (Exception ignored) {
            }
        }).start();

    }

    void sendRest(String Cmd, String dat) {
        new Thread(() -> {
            try {
                postRESTCommand(restHost, Cmd, dat, FPPAuth);
            } catch (Exception ignored) {
            }
        }).start();
    }

    void loadSequenceList(String cPlaylist) {
        if (cPlaylist != null && !cPlaylist.isEmpty()) {
            new Thread(() -> {
                try {
                    JSONObject playList = new JSONObject(getRESTCommand(restHost, String.format(fppPlaylist, cPlaylist), FPPAuth));
                    runOnUiThread(() -> {
                        try {
                            JSONObject plistLInfo = new JSONObject(String.valueOf(playList.getJSONObject("playlistInfo")));
                            textTitle.setText(playList.getString("name"));
                            String info = "Total Items: " + plistLInfo.getString ("total_items") +
                                    "\nTotal Duration: " + secondsToLongTime(Math.round(Float.parseFloat(plistLInfo.getString("total_duration")))) +
                                    "\nRepeat: " + playList.getString("repeat") +
                                    "\nLoop Count: " + playList.getString("loopCount") +
                                    "\nRandomize: " + switch (playList.getString("random")) {case "1" -> "Once per load";case "2" -> "Every iteration";default -> "Off";};
                            textInfo.setText(info);
                        } catch (Exception e) {
                            Log.e("PlaylistActivity", Log.getStackTraceString(e));
                        }
                    });

                    JSONArray plistLin = playList.getJSONArray("leadIn");
                    JSONArray plistMain= playList.getJSONArray("mainPlaylist");
                    JSONArray plistLout = playList.getJSONArray("leadOut");
                    items.clear();
                    if (plistLin.length() > 0) {
                        for (int i = 0; i < plistLin.length(); i++) {
                            plistLin.getJSONObject(i).put("part","in");
                            items.add(plistLin.getJSONObject(i));
                        }
                    }
                    if (plistMain.length() > 0) {
                        for (int i = 0; i < plistMain.length(); i++) {
                            plistMain.getJSONObject(i).put("part","main");
                            items.add(plistMain.getJSONObject(i));
                        }
                    }
                    if (plistLout.length() > 0) {
                        for (int i = 0; i < plistLout.length(); i++) {
                            plistLout.getJSONObject(i).put("part","out");
                            items.add(plistLout.getJSONObject(i));
                        }
                    }
                    runOnUiThread(() -> {
                        recyclerView.invalidate();
                        recyclerView.setAdapter(null);
                        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
                        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                        layoutManager.scrollToPosition(0);
                        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), layoutManager.getOrientation());
                        recyclerView.addItemDecoration(dividerItemDecoration);
                        recyclerView.setLayoutManager(layoutManager);
                        recycleAdapter = new RecycleAdapter(this, items);
                        recycleAdapter.setHasStableIds(true);
                        recyclerView.setAdapter(recycleAdapter);
                        recycleAdapter.setClickListener((view, position) -> new Thread(() -> {
                            try {
                                selectedSequence = position + 1;
                                JSONObject item = new JSONObject(recycleAdapter.getItem(position).toString());
                                JSONObject sequenceMeta = new JSONObject(getRESTCommand(restHost, String.format(fppSequenceMeta, item.getString("sequenceName")), FPPAuth));
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
                JSONObject sequenceMeta = new JSONObject(getRESTCommand(restHost, String.format(fppSequenceMeta, item.getString("sequenceName")), FPPAuth));
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
                JSONArray arrLists = new JSONArray(getRESTCommand(restHost, fppPlaylists, FPPAuth));
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
        recycleAdapter.SelectItem(-1);
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
                JSONObject status = new JSONObject(getRESTCommand(restHost, fppStatus, FPPAuth));
                String sequence = status.getString("current_sequence");
                if (!sequence.isEmpty()) {
                    int remaining = status.getInt("seconds_remaining");
                    for (int i = 0; i < items.size(); i++) {
                        if (sequence.equals(items.get(i).getString("sequenceName"))) {
                            int finalI = i;
                            runOnUiThread(() -> {
                                recycleAdapter.SelectItem(finalI);
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




    public static class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.ViewHolder> {
        private final Context context;
        private final ArrayList<JSONObject> items;
        private ItemClickListener mClickListener;
        int selectedPosition=-1;
        public RecycleAdapter(Context context, ArrayList<JSONObject> items) {
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.setItem(holder.itemView, items.get(position));
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }


        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            public ViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View v) {
                if (mClickListener != null) {
                    mClickListener.onItemClick(v, getAbsoluteAdapterPosition());
                    selectedPosition = getAbsoluteAdapterPosition();
                    notifyDataSetChanged();
                }
            }

            public void setItem(View view, JSONObject item) {
                TextView sequenceName = view.findViewById(R.id.playlistitemname);
                LinearLayout frame = itemView.findViewById(R.id.playlistitemframe);
                try {
                    sequenceName.setText(item.getString("sequenceName"));
                    if (item.getString("part").equals("in")) {
                        sequenceName.setTextColor(Color.parseColor("#90EE90"));
                    } else if (item.getString("part").equals("out")) {
                        sequenceName.setTextColor(Color.parseColor("#FA8072"));
                    }
                } catch (Exception ignored) {}
                if (selectedPosition == getAbsoluteAdapterPosition()) {
                    frame.setBackgroundColor(context.getResources().getColor(R.color.material_dynamic_neutral_variant50));
                } else {
                    frame.setBackgroundColor(context.getResources().getColor(R.color.material_dynamic_neutral_variant20));
                }
            }
        }

        public void setClickListener(ItemClickListener itemClickListener) {
            this.mClickListener = itemClickListener;
        }

        public interface ItemClickListener {
            void onItemClick(View view, int position);
        }

        public Object getItem(int position){
            return items.get(position);
        }

        @SuppressLint("NotifyDataSetChanged")
        public void SelectItem(int position){
            selectedPosition = position;
            notifyDataSetChanged();
        }

    }
}
