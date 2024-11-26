package dngsoftware.xmascontrol;

import static android.os.Looper.getMainLooper;
import static androidx.core.content.ContextCompat.getString;
import static dngsoftware.xmascontrol.Functions.capFirst;
import static dngsoftware.xmascontrol.Functions.fromHtml;
import static dngsoftware.xmascontrol.Functions.getCameraPhotoOrientation;
import static dngsoftware.xmascontrol.Functions.getRESTCommand;
import static dngsoftware.xmascontrol.Functions.isAvailable;
import static dngsoftware.xmascontrol.Functions.loadBitmap;
import static dngsoftware.xmascontrol.Functions.loadBrowser;
import static dngsoftware.xmascontrol.Functions.loadFalconActivity;
import static dngsoftware.xmascontrol.Functions.loadFppActivity;
import static dngsoftware.xmascontrol.Functions.secondsToTime;
import static dngsoftware.xmascontrol.Functions.sendFalconCommand;
import static dngsoftware.xmascontrol.FppCommands.fppStatus;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;


public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private ItemLongClickListener mLongClickListener;
    private ItemClickListener mSettingsClickListener;
    private final listItem[] listItems;
    private final Context context;


    public MainAdapter(Context context, listItem[] items) {
        this.context = context;
        listItems = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Handler handler = new Handler(getMainLooper());
        holder.itemName.setText(fromHtml(listItems[position].name));
        holder.itemUrl.setText(listItems[position].url);
        holder.itemTag.setText(listItems[position].ipath);

        if (position != 0) {
            holder.moveBtn.setImageDrawable(AppCompatResources.getDrawable(context, android.R.drawable.stat_sys_upload_done));
            holder.moveBtn.setVisibility(View.VISIBLE);
            holder.moveBtn.setOnClickListener(v -> {
                if (mSettingsClickListener != null) {
                    mSettingsClickListener.onItemClick(v, position);
                }
            });
        }
        else {
            holder.moveBtn.setVisibility(View.GONE);
        }

        if (holder.itemInfo2.getText().toString().isEmpty()) {
            holder.itemInfo.setText("");
            holder.itemInfo2.setText(R.string.lbloffline);
        }

        new Thread(() -> {

            if (holder.icon.getDrawable() == null) {
                setLogo(holder, position, handler);
            }

            try {
                String tmpHost = listItems[position].url;

                if (tmpHost.contains(":")) {
                    String[] spl1;
                    spl1 = tmpHost.split(":");
                    tmpHost = spl1[0];
                }

                if (InetAddress.getByName(tmpHost).isReachable(3000)) {

                    try {
                        JSONObject status = new JSONObject(getRESTCommand(listItems[position].url, fppStatus));
                        JSONArray sensorArr = status.getJSONArray("sensors");
                        JSONObject sensors = sensorArr.getJSONObject(0);
                        String tmpCpu = sensors.getString("formatted");
                        if (tmpCpu.isEmpty()) {
                            tmpCpu = "0";
                        }

                        String currSeq;
                        String sequence = status.getString("current_sequence");
                        if (!sequence.isEmpty()) {
                            String remaining = status.getString("seconds_remaining");
                            String tmpRem = "Remaining: " + secondsToTime(Integer.parseInt(remaining));
                            currSeq = "Playing: " + sequence + "\n" + tmpRem;
                        } else {
                            currSeq = "Idle";
                        }

                        String advJs = status.getString("advancedView");
                        JSONObject adv = new JSONObject(advJs);
                        String fppType = status.getString("mode_name");
                        final String tVar = adv.getString("Variant");
                        final String tCpu = tmpCpu;
                        final String cSeq = currSeq;

                        handler.post(() -> {

                            if (listItems[position].ipath == null || listItems[position].ipath.isEmpty()) {
                                holder.icon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.fpp));
                            }
                            if (tVar.contains("Pi 5")) {
                                holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.pi5));
                            } else if (tVar.contains("Pi 4") || tVar.contains("Pi Compute Module 4")) {
                                holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.pi4));
                            } else if (tVar.contains("Pi 3")) {
                                holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.pi3));
                            } else if (tVar.contains("Pi 2")) {
                                holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.pi2));
                            } else if (tVar.contains("Pi Model") || tVar.contains("Pi Zero")) {
                                holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.pi));
                            } else if (tVar.contains("PocketBeagle") || tVar.contains("Green") || tVar.contains("Black") || tVar.contains("BeagleBone")) {
                                holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.bb));
                            } else if (tVar.contains("Orange")) {
                                holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.orange));
                            } else if (tVar.contains("Pine")) {
                                holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.pine));
                            } else {
                                holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.blank));
                            }
                            holder.itemInfo.setText(MessageFormat.format("{0}{1}", context.getString(R.string.cpu_temp), tCpu));
                            holder.itemInfo2.setText(MessageFormat.format("Mode: {0}\n{1}", capFirst(fppType), cSeq.replace(".fseq", "")));
                            holder.imgBtn.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.twotone_settings_24));
                            holder.imgBtn.setVisibility(View.VISIBLE);
                            holder.imgBtn.setOnClickListener(v -> loadBrowser(context, holder.itemUrl.getText().toString()));
                            holder.isFpp = true;
                        });

                    } catch (JSONException e) {

                        try {
                            JSONObject f16Status = new JSONObject(sendFalconCommand(listItems[position].url,"Q","ST","1"));
                            JSONObject status = f16Status.getJSONObject("P");
                            String tmp1 = status.getString("T1");
                            tmp1 = tmp1.substring(0, 2) + "." + tmp1.substring(2);
                            String tmp2 = status.getString("T2");
                            tmp2 = tmp2.substring(0, 2) + "." + tmp2.substring(2);
                            String tmpCpu = status.getString("PT");
                            tmpCpu = tmpCpu.substring(0, 2) + "." + tmpCpu.substring(2);
                            JSONObject f16info = new JSONObject(sendFalconCommand(listItems[position].url,"Q","ST"));
                            JSONObject info = f16info.getJSONObject("P");
                            String bld = info.getString("FW");
                            final String tCpu = tmpCpu;
                            final String tCpu1 = tmp1;
                            final String tCpu2 = tmp2;
                            final String tBld = bld;

                            handler.post(() -> {
                                if (listItems[position].ipath == null || listItems[position].ipath.isEmpty()) {
                                    holder.icon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.falcon));
                                }
                                holder.itemInfo2.setText(MessageFormat.format("{0}{1}{2}{3}{4}{5}", context.getString(R.string.temp1), tCpu1, context.getString(R.string.temp2), tCpu2, context.getString(R.string.build), tBld));
                                holder.itemInfo.setText(MessageFormat.format("{0}{1}", context.getString(R.string.cpu_temp), tCpu));
                                holder.imgBtn.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.twotone_settings_24));
                                holder.imgBtn.setVisibility(View.VISIBLE);
                                holder.imgBtn.setOnClickListener(v -> loadBrowser(context, holder.itemUrl.getText().toString()));
                                holder.isFpp = false;
                            });

                        } catch (JSONException ignore) {
                            handler.post(() -> {
                                if (holder.itemInfo2.getText().toString().isEmpty() || holder.itemInfo2.getText().toString().equals(getString(context, R.string.lbloffline))) {
                                    holder.itemInfo2.setText(R.string.lblonline);
                                    holder.itemInfo.setText("");
                                }
                            });
                        }
                    }
                } else {
                    handler.post(() -> {
                        holder.itemInfo2.setText(R.string.lbloffline);
                        holder.itemInfo.setText("");
                        holder.imgBtn.setVisibility(View.GONE);
                        holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.blank));
                        setLogo(holder, position, handler);
                    });
                }
            } catch (Exception ignored) {
                handler.post(() -> {
                    holder.itemInfo2.setText(R.string.lbloffline);
                    holder.itemInfo.setText("");
                    holder.imgBtn.setVisibility(View.GONE);
                    holder.iconp.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.blank));
                    setLogo(holder, position, handler);
                });
            }
        }).start();
    }

    private void setLogo(ViewHolder holder, int position, Handler handler) {
        try {
            if (listItems[position].ipath != null && !listItems[position].ipath.isEmpty()) {
                try {
                    String filename = listItems[position].ipath;
                    File tmpFile = new File(filename);
                    Bitmap bitmap = null;
                    if (tmpFile.exists()) {
                        bitmap = loadBitmap(filename);
                    }
                    assert bitmap != null;
                    final Bitmap bit_map = Bitmap.createScaledBitmap(bitmap, 128, 128, false);
                    handler.post(() -> {
                        holder.icon.setImageBitmap(bit_map);
                        holder.icon.setRotation(getCameraPhotoOrientation(filename));
                    });
                } catch (Exception e) {
                    handler.post(() -> {
                        holder.icon.setImageDrawable(listItems[position].icon);
                        holder.icon.setRotation(0);
                    });
                }
            } else {
                handler.post(() -> {
                    holder.icon.setImageDrawable(listItems[position].icon);
                    holder.icon.setRotation(0);
                });
            }
        } catch (Exception ignored) {}
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return listItems.length;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        TextView itemName;
        TextView itemUrl;
        TextView itemTag;
        ImageView icon;
        ImageView iconp;
        TextView itemInfo;
        TextView itemInfo2;
        ImageView imgBtn;
        ImageView moveBtn;
        boolean isFpp;

        ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            itemUrl = itemView.findViewById(R.id.itemUrl);
            itemTag = itemView.findViewById(R.id.itemTag);
            icon = itemView.findViewById(R.id.logo);
            iconp = itemView.findViewById(R.id.logop);
            imgBtn = itemView.findViewById(R.id.imageButton);
            itemInfo = itemView.findViewById(R.id.itemInfo);
            itemInfo2 = itemView.findViewById(R.id.itemInfo2);
            moveBtn = itemView.findViewById(R.id.moveitem);
            isFpp = false;

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            moveBtn.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Handler handler = new Handler(getMainLooper());
            TextView urlTxt = view.findViewById(R.id.itemUrl);
            TextView infoTxt = view.findViewById(R.id.itemInfo2);
            String url = urlTxt.getText().toString();
            String info = infoTxt.getText().toString();
            if (!info.equalsIgnoreCase("offline")) {
                new Thread(() -> {
                        if (isAvailable("http://" + url)) {
                            if (isFpp) {
                                loadFppActivity(context, url);
                            } else {
                                loadFalconActivity(context, url);
                            }
                        }
                        else {
                            handler.post(() -> Toast.makeText(context, R.string.device_unavailable, Toast.LENGTH_SHORT).show());
                        }
                }).start();
            } else {
                Toast.makeText(context, R.string.lblpoffline, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mLongClickListener != null) {
                mLongClickListener.onItemLongClick(v, getAbsoluteAdapterPosition());
            }
            return true;
        }

    }

    public void setSettingsClickListener(ItemClickListener SettingsClickListener) {
        this.mSettingsClickListener = SettingsClickListener;
    }

    public void setLongClickListener(ItemLongClickListener itemLongClickListener) {
        this.mLongClickListener = itemLongClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface ItemLongClickListener {
        void onItemLongClick(View view, int position);
    }

    public listItem getItem(int position)
    {
        return listItems[position];
    }

    public void moveItem(int from_pos, int to_pos) {
        try {
            if (from_pos < to_pos) {
                for (int i = from_pos; i < to_pos; i++) {
                    Collections.swap(Arrays.asList(listItems), i, i + 1);
                }
            } else {
                for (int i = from_pos; i > to_pos; i--) {
                    Collections.swap(Arrays.asList(listItems), i, i - 1);
                }
            }
            notifyItemMoved(from_pos, to_pos);
        } catch (Exception ignored) {
        }
    }


}