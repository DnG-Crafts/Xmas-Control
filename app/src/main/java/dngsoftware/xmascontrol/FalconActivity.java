package dngsoftware.xmascontrol;

import static dngsoftware.xmascontrol.Functions.sendFalconCommand;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class FalconActivity extends Activity {
    private Context context = this;
    private int SelectedTest;
    private String apiHost = "127.0.0.1";
    private TextView tDbg;
    private ScheduledExecutorService scheduler;
    private String testType = "1";
    private final ArrayList<JSONObject> items = new ArrayList<>();
    ListAdapter adapter;
    ListView listView;
    String SelectedProp = "-1";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        setContentView(R.layout.activity_falcon);
        context = this;
        listView = findViewById(R.id.listView);

        if (extras != null) {
            final String rHost = extras.getString("HOST");
            if (rHost != null && !rHost.isEmpty()) {
                apiHost = rHost;
                tDbg = findViewById(R.id.txtDbg);
                StringBuilder tmpJsn = new StringBuilder();
                String[] modes = {"Alternate", "RGB", "RGBW", "RGBWB", "Red Ramp", "Green Ramp", "Blue Ramp", "White Ramp", "White",
                        "Color Wash", "Chase", "Number", "Smart Remote Number", "4 Channel RGBW", "CMY", "3 Channel Color", "4 Channel Color", "RGB Fade", "Rainbow"};
                final Spinner spinner = findViewById(R.id.spinner);
                ArrayAdapter<String> sadapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, modes);
                sadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(sadapter);
                spinner.setSelection(SelectedTest);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                        SelectedTest = spinner.getSelectedItemPosition();
                        testType = String.valueOf(SelectedTest + 1);
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {}
                });


                new Thread(() -> {
                    try {
                        String tmpJson = sendFalconCommand(apiHost,"Q","SP","0");
                        if (!tmpJson.equals("[]")) {
                            tmpJsn.append(tmpJson);
                            JSONObject strings = new JSONObject(tmpJson);
                            JSONObject stringP = strings.getJSONObject("P");
                            JSONArray plistArr = stringP.getJSONArray("A");
                            for (int i = 0; i < plistArr.length(); i++) {
                                items.add(plistArr.getJSONObject(i));
                            }
                            if (!tmpJsn.toString().contains("\"F\":1,\"")) {
                                int i = 1;
                                do {
                                    tmpJson = sendFalconCommand(apiHost, "Q", "SP", String.valueOf(i));
                                    tmpJsn.append(tmpJson);
                                    strings = new JSONObject(tmpJson);
                                    stringP = strings.getJSONObject("P");
                                    plistArr = stringP.getJSONArray("A");
                                    for (int y = 0; y < plistArr.length(); y++) {
                                        items.add(plistArr.getJSONObject(y));
                                    }
                                    i++;
                                } while (!tmpJsn.toString().contains("\"F\":1,\""));
                            }


                            runOnUiThread(() -> {


                                adapter = new ListAdapter(this, items);
                                listView.setAdapter(adapter);

                                listView.setOnItemClickListener((parent, view, position, id) -> new Thread(() -> {
                                    try {

                                        JSONObject item = new JSONObject(adapter.getItem(position).toString());
                                        SelectedProp = item.getString("p");
                                        // String name = item.getString("nm");
                                        // String universe = item.getString("u");
                                    } catch (JSONException e) {
                                        Log.e("PlaylistActivity", Log.getStackTraceString(e));
                                    }
                                }).start());

                            });
                        }
                    } catch (Exception e) {
                        Log.e("FalconActivity", Log.getStackTraceString(e));
                    }
                }).start();
            } else {
                finish();
            }
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


    void updateStatus() {
        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            try {

                JSONObject f16Status = new JSONObject(sendFalconCommand(apiHost,"Q","ST","1"));
                JSONObject status = f16Status.getJSONObject("P");
                String tmp1 = status.getString("T1");
                tmp1 = tmp1.substring(0, 2) + "." + tmp1.substring(2);
                String tmp2 = status.getString("T2");
                tmp2 = tmp2.substring(0, 2) + "." + tmp2.substring(2);
                String tmpCpu = status.getString("PT");
                tmpCpu = tmpCpu.substring(0, 2) + "." + tmpCpu.substring(2);
                JSONObject f16info = new JSONObject(sendFalconCommand(apiHost,"Q","ST"));
                JSONObject info = f16info.getJSONObject("P");
                String bld = info.getString("FW");
                final String tCpu = tmpCpu;
                final String tCpu1 = tmp1;
                final String tCpu2 = tmp2;

                String finalStatusMsg = "CPU Temp: " + tCpu + "°C" +
                        "\nTemp1: " + tCpu1 + "°C" +
                        "\nTemp2: " + tCpu2 + "°C" +
                        "\nBuild: " + bld +
                        "\nHost: " + apiHost;
                runOnUiThread(() -> tDbg.setText(finalStatusMsg));

            } catch (Exception e) {
                Log.e("FalconActivity", Log.getStackTraceString(e));
            }

        };
        scheduler.scheduleWithFixedDelay(task, 0, 2, TimeUnit.SECONDS);
    }

    public void startTest(View v) {
        if (!SelectedProp.equals("-1"))
        {
            startTest(SelectedProp);
        }
    }

    void startTest(String prop) {
        new Thread(() -> {
            try {
                sendFalconCommand(apiHost,"S","TS","0","1","0", "{\"E\":\"Y\",\"D\":\"N\",\"S\":20,\"Y\":" + testType + ",\"C\":0,\"A\":[{\"P\":" +  prop  + ",\"R\":0,\"S\":0}]}");
            } catch (Exception ignored) {
            }
        }).start();
    }

    public void stopTest(View v) {
        new Thread(() -> {
            try {
                sendFalconCommand(apiHost,"S","TS","0","1","0", "{\"E\":\"N\",\"D\":\"N\",\"S\":20,\"Y\":" + testType + ",\"C\":0,\"A\":[]}");
            } catch (Exception ignored) {
            }
        }).start();
    }

    public void rebootBoard(View v) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.confirm_dialog);
        dialog.setCanceledOnTouchOutside(false);
        final TextView dlgTitle = dialog.findViewById(R.id.dlgtitle);
        final TextView msg = dialog.findViewById(R.id.txtmsg);
        final Button btnCancel = dialog.findViewById(R.id.btncancel);
        final Button btnOK = dialog.findViewById(R.id.btnok);
        dlgTitle.setText(R.string.confirm);
        dialog.setTitle(R.string.confirm);
        msg.setText(R.string.reboot_board);
        btnOK.setOnClickListener(v1 -> {
            new Thread(() -> {
                try {
                    sendFalconCommand(apiHost,"S","RB");
                } catch (Exception ignored) {
                }
            }).start();
            dialog.dismiss();
            finish();
        });
        btnCancel.setOnClickListener(v1 -> dialog.dismiss());
        dialog.show();
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
                sequenceName.setText(item.getString("nm"));
            } catch (Exception ignored) {
            }
            return view;
        }
    }
}
